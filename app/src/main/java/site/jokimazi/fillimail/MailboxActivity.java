package site.jokimazi.fillimail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;

import site.jokimazi.fillimail.adapter.EmailAdapter;
import site.jokimazi.fillimail.databinding.ActivityMailboxBinding;
import site.jokimazi.fillimail.model.EmailAccount;
import site.jokimazi.fillimail.model.EmailMessage;

public class MailboxActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FilliMail_Net";
    private static final HashMap<String, List<EmailMessage>> memoryCache = new HashMap<>();

    private static final int ID_ALL_MAIL = 100;
    private static final int ID_SETTINGS = 9001;
    private static final int ID_ACCOUNTS_MANAGE = 9002;
    private static final int ID_ABOUT = 9003;

    private ActivityMailboxBinding binding;
    private List<EmailAccount> accountList = new ArrayList<>();
    private EmailAdapter emailAdapter;

    private final HashMap<String, List<String>> foldersCache = new HashMap<>();
    private final HashMap<Integer, String> accountItemMap = new HashMap<>();
    private final HashMap<Integer, FolderAction> folderActionMap = new HashMap<>();

    private String expandedEmail = null;
    private int dynamicIdCounter = 1000;
    private volatile long currentLoadId = 0;

    private String currentEmailKey = "all";
    private String serverFolderName = "INBOX";
    private String currentDisplayFolder;

    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMailboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            currentEmailKey = savedInstanceState.getString("currentEmailKey", "all");
            serverFolderName = savedInstanceState.getString("serverFolderName", "INBOX");
            currentDisplayFolder = savedInstanceState.getString("currentDisplayFolder", getString(R.string.nav_inbox));
            expandedEmail = savedInstanceState.getString("expandedEmail", null);
        } else {
            currentDisplayFolder = getString(R.string.nav_inbox);
        }

        setSupportActionBar(binding.toolbar);

        binding.toolbar.setTitle(currentDisplayFolder);
        binding.toolbar.setSubtitle(currentEmailKey.equals("all") ? getString(R.string.nav_all_mail) : currentEmailKey);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar, R.string.dialog_yes, R.string.dialog_no);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        toggle.getDrawerArrowDrawable().setColor(typedValue.data);

        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        emailAdapter = new EmailAdapter(new EmailAdapter.OnEmailClickListener() {
            @Override
            public void onEmailClick(EmailMessage email) {
                Intent intent = new Intent(MailboxActivity.this, ReadEmailActivity.class);
                intent.putExtra("email_object", email);
                startActivity(intent);
            }

            @Override
            public void onSelectionChanged(int count) {
                if (count > 0) {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback);
                    }
                    actionMode.setTitle(String.valueOf(count));
                } else if (actionMode != null) {
                    actionMode.finish();
                }
            }
        });

        binding.recyclerEmails.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEmails.setAdapter(emailAdapter);

        binding.swipeRefresh.setOnRefreshListener(() -> {
            String cacheKey = currentEmailKey + "_" + serverFolderName;
            memoryCache.remove(cacheKey);
            loadEmailsForAccount(currentEmailKey, serverFolderName, currentDisplayFolder);
        });

        binding.fabCompose.setOnClickListener(v -> startActivity(new Intent(this, ComposeActivity.class)));

        foldersCache.put("all", new ArrayList<>(Arrays.asList("INBOX", "Sent", "Trash")));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentEmailKey", currentEmailKey);
        outState.putString("serverFolderName", serverFolderName);
        outState.putString("currentDisplayFolder", currentDisplayFolder);
        outState.putString("expandedEmail", expandedEmail);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccountsAndBuildMenu();
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.action_delete))
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            boolean isTrash = serverFolderName.toLowerCase().contains("trash") || serverFolderName.toLowerCase().contains("корзина");
            if (isTrash) {
                menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.action_restore))
                        .setIcon(android.R.drawable.ic_menu_revert)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == 1) {
                processSelectedEmails(false);
                mode.finish();
                return true;
            } else if (item.getItemId() == 2) {
                processSelectedEmails(true);
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            emailAdapter.clearSelection();
            actionMode = null;
        }
    };

    private void processSelectedEmails(boolean isRestore) {
        List<EmailMessage> selected = emailAdapter.getSelectedEmails();
        if (selected.isEmpty()) return;

        Toast.makeText(this, getString(isRestore ? R.string.toast_restoring : R.string.toast_deleting), Toast.LENGTH_SHORT).show();

        emailAdapter.removeEmails(selected);
        String cacheKey = currentEmailKey + "_" + serverFolderName;
        if (memoryCache.containsKey(cacheKey)) {
            memoryCache.get(cacheKey).removeAll(selected);
        }

        new Thread(() -> {
            try {
                Map<Integer, Map<String, List<Long>>> grouped = new HashMap<>();
                for (EmailMessage em : selected) {
                    if (!grouped.containsKey(em.accountId)) {
                        grouped.put(em.accountId, new HashMap<>());
                    }
                    Map<String, List<Long>> folderMap = grouped.get(em.accountId);
                    if (!folderMap.containsKey(em.folderName)) {
                        folderMap.put(em.folderName, new ArrayList<>());
                    }
                    folderMap.get(em.folderName).add(em.uid);
                }

                for (Map.Entry<Integer, Map<String, List<Long>>> accEntry : grouped.entrySet()) {
                    EmailAccount targetAccount = null;
                    for (EmailAccount acc : accountList) {
                        if (acc.getId() == accEntry.getKey()) {
                            targetAccount = acc;
                            break;
                        }
                    }
                    if (targetAccount == null) continue;

                    Properties props = new Properties();
                    props.setProperty("mail.store.protocol", "imaps");
                    Session session = Session.getInstance(props);
                    Store store = session.getStore("imaps");
                    store.connect(targetAccount.getImapHost(), targetAccount.getImapPort(), targetAccount.getEmail(), targetAccount.getPassword());

                    Folder destFolder = null;
                    if (isRestore) {
                        destFolder = store.getFolder("INBOX");
                    } else {
                        for (Folder f : store.getDefaultFolder().list("*")) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("trash") || name.contains("корзина") || name.contains("deleted")) {
                                destFolder = f;
                                break;
                            }
                        }
                    }

                    for (Map.Entry<String, List<Long>> folderEntry : accEntry.getValue().entrySet()) {
                        String fName = folderEntry.getKey();
                        List<Long> uidsList = folderEntry.getValue();

                        long[] uids = new long[uidsList.size()];
                        for (int i = 0; i < uidsList.size(); i++) {
                            uids[i] = uidsList.get(i);
                        }

                        Folder srcFolder = store.getFolder(fName);
                        srcFolder.open(Folder.READ_WRITE);

                        javax.mail.Message[] msgs = ((UIDFolder) srcFolder).getMessagesByUID(uids);

                        boolean isTrash = fName.toLowerCase().contains("trash") || fName.toLowerCase().contains("корзина");

                        if (isRestore) {
                            if (destFolder != null && !destFolder.getFullName().equals(srcFolder.getFullName())) {
                                srcFolder.copyMessages(msgs, destFolder);
                            }
                        } else {
                            if (!isTrash && destFolder != null && !destFolder.getFullName().equals(srcFolder.getFullName())) {
                                srcFolder.copyMessages(msgs, destFolder);
                            }
                        }

                        srcFolder.setFlags(msgs, new Flags(Flags.Flag.DELETED), true);
                        srcFolder.expunge();
                        srcFolder.close(false);
                    }
                    store.close();
                }

                runOnUiThread(() -> Toast.makeText(this, getString(isRestore ? R.string.toast_restored : R.string.toast_deleted), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(isRestore ? R.string.toast_restore_error : R.string.toast_delete_error), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadAccountsAndBuildMenu() {
        new Thread(() -> {
            accountList = App.getInstance().getDatabase().accountDao().getAllAccounts();
            for (EmailAccount acc : accountList) {
                if (!foldersCache.containsKey(acc.getEmail())) {
                    foldersCache.put(acc.getEmail(), new ArrayList<>(Arrays.asList("INBOX", "Sent", "Trash")));
                }
            }
            runOnUiThread(() -> {
                rebuildMenu();
                for (EmailAccount account : accountList) fetchFoldersFromServer(account);
                loadEmailsForAccount(currentEmailKey, serverFolderName, currentDisplayFolder);
            });
        }).start();
    }

    private void triggerRebuildMenu() { binding.navView.post(this::rebuildMenu); }

    private void rebuildMenu() {
        Menu menu = binding.navView.getMenu();
        menu.clear();
        accountItemMap.clear();
        folderActionMap.clear();
        dynamicIdCounter = 1000;

        MenuItem allMailItem = menu.add(Menu.NONE, ID_ALL_MAIL, 0, getString(R.string.nav_all_mail));
        allMailItem.setIcon(android.R.drawable.ic_dialog_email);
        if ("all".equals(expandedEmail)) drawFolders(menu, "all", 1);

        for (int i = 0; i < accountList.size(); i++) {
            EmailAccount account = accountList.get(i);
            int accountItemId = dynamicIdCounter++;
            int order = (i + 1) * 100;
            MenuItem accountItem = menu.add(Menu.NONE, accountItemId, order, account.getEmail());
            accountItem.setIcon(android.R.drawable.ic_menu_myplaces);
            accountItemMap.put(accountItemId, account.getEmail());
            if (account.getEmail().equals(expandedEmail)) drawFolders(menu, account.getEmail(), order + 1);
        }

        SubMenu systemMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, 9999, getString(R.string.nav_system));
        systemMenu.add(Menu.NONE, ID_SETTINGS, 10000, getString(R.string.nav_settings)).setIcon(android.R.drawable.ic_menu_preferences);
        systemMenu.add(Menu.NONE, ID_ACCOUNTS_MANAGE, 10001, getString(R.string.nav_accounts_manage)).setIcon(android.R.drawable.ic_menu_myplaces);
        systemMenu.add(Menu.NONE, ID_ABOUT, 10002, getString(R.string.nav_about)).setIcon(android.R.drawable.ic_menu_info_details);
    }

    private void drawFolders(Menu menu, String emailKey, int startOrder) {
        List<String> folders = foldersCache.get(emailKey);
        if (folders != null) {
            List<String> sorted = sortFolders(folders);

            for (String folderName : sorted) {
                int folderItemId = dynamicIdCounter++;
                String displayName = folderName;
                String iconChar = "📁 ";

                if (folderName.equalsIgnoreCase("INBOX")) {
                    displayName = getString(R.string.nav_inbox);
                    iconChar = "📥 ";
                } else if (folderName.equalsIgnoreCase("Sent") || folderName.equalsIgnoreCase("Отправленные")) {
                    displayName = getString(R.string.nav_sent);
                    iconChar = "📤 ";
                } else if (folderName.equalsIgnoreCase("Trash") || folderName.equalsIgnoreCase("Корзина")) {
                    displayName = getString(R.string.nav_trash);
                    iconChar = "🗑 ";
                }

                String fullItemText = "▶ " + iconChar + displayName;
                menu.add(Menu.NONE, folderItemId, startOrder++, fullItemText);
                folderActionMap.put(folderItemId, new FolderAction(emailKey, folderName, displayName));
            }
        }
    }

    private List<String> sortFolders(List<String> folders) {
        List<String> sorted = new ArrayList<>();
        List<String> others = new ArrayList<>();
        for (String f : folders) {
            if (f.equalsIgnoreCase("INBOX")) sorted.add(0, f);
            else if (f.equalsIgnoreCase("Sent") || f.equalsIgnoreCase("Отправленные")) {
                if (sorted.size() > 1) sorted.add(1, f); else sorted.add(f);
            }
            else if (f.equalsIgnoreCase("Trash") || f.equalsIgnoreCase("Корзина")) others.add(f);
            else others.add(f);
        }
        sorted.addAll(others);
        return sorted;
    }

    private void fetchFoldersFromServer(EmailAccount account) {
        new Thread(() -> {
            List<String> folderNames = new ArrayList<>();
            try {
                Properties props = new Properties();
                props.setProperty("mail.store.protocol", "imaps");
                props.setProperty("mail.imaps.timeout", "6000");
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect(account.getImapHost(), account.getImapPort(), account.getEmail(), account.getPassword());
                for (Folder f : store.getDefaultFolder().list("*")) if ((f.getType() & Folder.HOLDS_MESSAGES) != 0) folderNames.add(f.getName());
                store.close();
            } catch (Exception e) { e.printStackTrace(); folderNames.addAll(Arrays.asList("INBOX", "Sent", "Trash")); }
            runOnUiThread(() -> { foldersCache.put(account.getEmail(), folderNames); if (account.getEmail().equals(expandedEmail)) triggerRebuildMenu(); });
        }).start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (actionMode != null) {
            actionMode.finish();
        }

        int id = item.getItemId();
        if (id == ID_ALL_MAIL) {
            expandedEmail = "all".equals(expandedEmail) ? null : "all";
            triggerRebuildMenu();
            return true;
        } else if (id == ID_SETTINGS || id == ID_ACCOUNTS_MANAGE || id == ID_ABOUT) {
            if (id == ID_ACCOUNTS_MANAGE) {
                startActivity(new Intent(this, ManageAccountsActivity.class));
            } else if (id == ID_SETTINGS) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == ID_ABOUT) {
                startActivity(new Intent(this, AboutActivity.class));
            }
            binding.drawerLayout.close(); return true;
        }
        if (accountItemMap.containsKey(id)) {
            String clickedEmail = accountItemMap.get(id);
            expandedEmail = clickedEmail.equals(expandedEmail) ? null : clickedEmail;
            triggerRebuildMenu();
            return true;
        }
        if (folderActionMap.containsKey(id)) {
            FolderAction action = folderActionMap.get(id);
            if (action != null) loadEmailsForAccount(action.accountEmail, action.serverFolderName, action.displayFolderName);
            binding.drawerLayout.close(); return true;
        }
        return false;
    }

    private void loadEmailsForAccount(String email, String folderName, String displayFolderName) {
        this.currentEmailKey = email;
        this.serverFolderName = folderName;
        this.currentDisplayFolder = displayFolderName;

        binding.toolbar.setTitle(displayFolderName);
        binding.toolbar.setSubtitle(email.equals("all") ? getString(R.string.nav_all_mail) : email);

        final long myLoadId = System.currentTimeMillis();
        currentLoadId = myLoadId;

        emailAdapter.setShowReceiver(email.equals("all"));

        String cacheKey = email + "_" + folderName;
        if (memoryCache.containsKey(cacheKey)) {
            List<EmailMessage> cached = memoryCache.get(cacheKey);
            emailAdapter.setEmails(new ArrayList<>(cached));
            binding.progressLoading.setVisibility(View.GONE);
            binding.tvEmptyState.setVisibility(View.GONE);
        } else {
            emailAdapter.clear();
            binding.progressLoading.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setVisibility(View.GONE);
        }

        new Thread(() -> {
            try {
                List<EmailAccount> targetAccounts = new ArrayList<>();
                if (email.equals("all")) targetAccounts.addAll(accountList);
                else for (EmailAccount acc : accountList) if (acc.getEmail().equals(email)) targetAccounts.add(acc);

                for (EmailAccount account : targetAccounts) {
                    if (currentLoadId != myLoadId) return;
                    try {
                        Properties props = new Properties();
                        props.setProperty("mail.store.protocol", "imaps");
                        Session session = Session.getInstance(props);
                        Store store = session.getStore("imaps");
                        store.connect(account.getImapHost(), account.getImapPort(), account.getEmail(), account.getPassword());
                        Folder folder = store.getFolder(folderName);
                        if (!folder.exists()) folder = store.getFolder("INBOX");
                        folder.open(Folder.READ_ONLY);

                        UIDFolder uidFolder = (folder instanceof UIDFolder) ? (UIDFolder) folder : null;
                        int messageCount = folder.getMessageCount();
                        if (messageCount > 0) {
                            int start = Math.max(1, messageCount - 19);
                            for (javax.mail.Message msg : folder.getMessages(start, messageCount)) {
                                if (currentLoadId != myLoadId) break;

                                long msgUid = (uidFolder != null) ? uidFolder.getUID(msg) : -1;

                                Date messageDate = msg.getReceivedDate();
                                if (messageDate == null) messageDate = msg.getSentDate();
                                if (messageDate == null) messageDate = new Date(0);

                                String senderName = "";
                                String senderEmail = "";

                                boolean isSentFolder = folderName.equalsIgnoreCase("Sent") || folderName.equalsIgnoreCase("Отправленные");

                                if (isSentFolder) {
                                    javax.mail.Address[] recipients = msg.getRecipients(javax.mail.Message.RecipientType.TO);
                                    if (recipients != null && recipients.length > 0) {
                                        if (recipients[0] instanceof InternetAddress) {
                                            InternetAddress addr = (InternetAddress) recipients[0];
                                            senderName = addr.getPersonal();
                                            senderEmail = addr.getAddress();
                                        } else {
                                            senderEmail = recipients[0].toString();
                                        }
                                    }
                                } else {
                                    if (msg.getFrom() != null && msg.getFrom().length > 0) {
                                        if (msg.getFrom()[0] instanceof InternetAddress) {
                                            InternetAddress addr = (InternetAddress) msg.getFrom()[0];
                                            senderName = addr.getPersonal();
                                            senderEmail = addr.getAddress();
                                        } else {
                                            senderEmail = msg.getFrom()[0].toString();
                                        }
                                    }
                                }

                                EmailMessage newEmail = new EmailMessage(msgUid, account.getId(), folderName, account.getEmail(), senderName, senderEmail, msg.getSubject(), "", messageDate);

                                runOnUiThread(() -> {
                                    if (currentLoadId == myLoadId) {
                                        if (!emailAdapter.hasEmail(newEmail.uid, newEmail.accountId)) {
                                            emailAdapter.addEmailSorted(newEmail);

                                            List<EmailMessage> currentCache = memoryCache.get(cacheKey);
                                            if (currentCache == null) {
                                                currentCache = new ArrayList<>();
                                                memoryCache.put(cacheKey, currentCache);
                                            }
                                            currentCache.add(newEmail);
                                        }
                                        binding.progressLoading.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }
                        folder.close(false); store.close();
                    } catch (Exception accountException) {
                        Log.e(TAG, "Error", accountException);
                    }
                }

                runOnUiThread(() -> {
                    if (currentLoadId == myLoadId) {
                        binding.progressLoading.setVisibility(View.GONE);
                        binding.swipeRefresh.setRefreshing(false);
                        if (emailAdapter.getItemCount() == 0) {
                            binding.tvEmptyState.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                runOnUiThread(() -> {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                });
            }
        }).start();
    }

    private static class FolderAction {
        String accountEmail, serverFolderName, displayFolderName;
        FolderAction(String accountEmail, String serverFolderName, String displayFolderName) {
            this.accountEmail = accountEmail; this.serverFolderName = serverFolderName; this.displayFolderName = displayFolderName;
        }
    }
}