package site.jokimazi.fillimail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

import site.jokimazi.fillimail.adapter.EmailAdapter;
import site.jokimazi.fillimail.databinding.ActivityMailboxBinding;
import site.jokimazi.fillimail.model.EmailAccount;
import site.jokimazi.fillimail.model.EmailMessage;

public class MailboxActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FilliMail_Net";
    private ActivityMailboxBinding binding;
    private List<EmailAccount> accountList = new ArrayList<>();
    private EmailAdapter emailAdapter;

    private final HashMap<String, List<String>> foldersCache = new HashMap<>();
    private final HashMap<Integer, String> accountItemMap = new HashMap<>();
    private final HashMap<Integer, FolderAction> folderActionMap = new HashMap<>();

    private String expandedEmail = null;
    private int dynamicIdCounter = 1000;
    private volatile long currentLoadId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMailboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.open());
        binding.navView.setNavigationItemSelectedListener(this);

        emailAdapter = new EmailAdapter(email -> {
            Intent intent = new Intent(this, ReadEmailActivity.class);
            intent.putExtra("email_object", email);
            startActivity(intent);
        });

        binding.recyclerEmails.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEmails.setAdapter(emailAdapter);

        binding.fabCompose.setOnClickListener(v -> startActivity(new Intent(this, ComposeActivity.class)));

        binding.toolbar.setTitle(getString(R.string.nav_inbox));
        binding.toolbar.setSubtitle(getString(R.string.nav_all_mail));

        foldersCache.put("all", new ArrayList<>(Arrays.asList("INBOX", "Sent", "Trash")));
        loadAccountsAndBuildMenu();
    }

    private void loadAccountsAndBuildMenu() {
        new Thread(() -> {
            accountList = App.getInstance().getDatabase().accountDao().getAllAccounts();
            for (EmailAccount acc : accountList) {
                foldersCache.put(acc.getEmail(), new ArrayList<>(Arrays.asList("INBOX", "Sent", "Trash")));
            }
            runOnUiThread(() -> {
                rebuildMenu();
                for (EmailAccount account : accountList) fetchFoldersFromServer(account);
                loadEmailsForAccount("all", "INBOX", getString(R.string.nav_inbox));
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

        MenuItem allMailItem = menu.add(Menu.NONE, R.id.nav_all_mail, 0, getString(R.string.nav_all_mail));
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
        systemMenu.add(Menu.NONE, R.id.nav_settings, 10000, getString(R.string.nav_settings)).setIcon(android.R.drawable.ic_menu_preferences);
        systemMenu.add(Menu.NONE, R.id.nav_accounts_manage, 10001, getString(R.string.nav_accounts_manage)).setIcon(android.R.drawable.ic_menu_myplaces);
        systemMenu.add(Menu.NONE, R.id.nav_about, 10002, getString(R.string.nav_about)).setIcon(android.R.drawable.ic_menu_info_details);
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
        int id = item.getItemId();
        if (id == R.id.nav_all_mail) {
            expandedEmail = "all".equals(expandedEmail) ? null : "all";
            triggerRebuildMenu();
            loadEmailsForAccount("all", "INBOX", getString(R.string.nav_inbox));
            return true;
        } else if (id == R.id.nav_settings || id == R.id.nav_accounts_manage || id == R.id.nav_about) {
            if (id == R.id.nav_accounts_manage) {
                startActivity(new Intent(this, ManageAccountsActivity.class));
            }
            binding.drawerLayout.close(); return true;
        }
        if (accountItemMap.containsKey(id)) {
            String clickedEmail = accountItemMap.get(id);
            expandedEmail = clickedEmail.equals(expandedEmail) ? null : clickedEmail;
            triggerRebuildMenu();
            loadEmailsForAccount(clickedEmail, "INBOX", getString(R.string.nav_inbox));
            return true;
        }
        if (folderActionMap.containsKey(id)) {
            FolderAction action = folderActionMap.get(id);
            if (action != null) loadEmailsForAccount(action.accountEmail, action.serverFolderName, action.displayFolderName);
            binding.drawerLayout.close(); return true;
        }
        return false;
    }

    private void loadEmailsForAccount(String email, String serverFolderName, String displayFolderName) {
        binding.toolbar.setTitle(displayFolderName);
        binding.toolbar.setSubtitle(email.equals("all") ? getString(R.string.nav_all_mail) : email);

        final long myLoadId = System.currentTimeMillis();

        currentLoadId = myLoadId;
        emailAdapter.clear();
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);

        new Thread(() -> {
            boolean hasEmails = false;
            try {
                List<EmailAccount> targetAccounts = new ArrayList<>();
                if (email.equals("all")) targetAccounts.addAll(accountList);
                else for (EmailAccount acc : accountList) if (acc.getEmail().equals(email)) targetAccounts.add(acc);

                for (EmailAccount account : targetAccounts) {
                    if (currentLoadId != myLoadId) return;
                    Properties props = new Properties();
                    props.setProperty("mail.store.protocol", "imaps");
                    Session session = Session.getInstance(props);
                    Store store = session.getStore("imaps");
                    store.connect(account.getImapHost(), account.getImapPort(), account.getEmail(), account.getPassword());
                    Folder folder = store.getFolder(serverFolderName);
                    if (!folder.exists()) folder = store.getFolder("INBOX");
                    folder.open(Folder.READ_ONLY);

                    // ИСПРАВЛЕНИЕ: Получаем доступ к UIDFolder
                    UIDFolder uidFolder = null;
                    if (folder instanceof UIDFolder) {
                        uidFolder = (UIDFolder) folder;
                    }

                    int messageCount = folder.getMessageCount();
                    if (messageCount > 0) {
                        hasEmails = true;
                        int start = Math.max(1, messageCount - 19);
                        for (javax.mail.Message msg : folder.getMessages(start, messageCount)) {
                            if (currentLoadId != myLoadId) break;

                            // Извлекаем UID
                            long msgUid = -1;
                            if (uidFolder != null) {
                                msgUid = uidFolder.getUID(msg);
                            }

                            // Сохраняем UID и ID аккаунта в модель
                            EmailMessage newEmail = new EmailMessage(msgUid, account.getId(), msg.getFrom()[0].toString(), msg.getSubject(), "");

                            runOnUiThread(() -> {
                                if (currentLoadId == myLoadId) {
                                    emailAdapter.addEmail(newEmail);
                                    binding.progressLoading.setVisibility(View.GONE);
                                    binding.tvEmptyState.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                    folder.close(false); store.close();
                }
                final boolean result = hasEmails;
                runOnUiThread(() -> {
                    if (currentLoadId == myLoadId) {
                        binding.progressLoading.setVisibility(View.GONE);
                        if (!result) binding.tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> binding.progressLoading.setVisibility(View.GONE));
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