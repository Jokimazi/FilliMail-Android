package site.jokimazi.fillimail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import site.jokimazi.fillimail.databinding.ActivityMailboxBinding;
import site.jokimazi.fillimail.model.EmailAccount;
import site.jokimazi.fillimail.model.EmailMessage;

public class MailboxActivity extends AppCompatActivity {

    private ActivityMailboxBinding binding;
    private EmailAdapter adapter;
    private EmailAccount currentAccount;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMailboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Открытие шторки по клику на иконку "гамбургера" в тулбаре
        binding.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.open());

        // Обработка кликов по меню
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_all_mail) {
                Toast.makeText(this, "Показываем всю почту", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Открываем настройки", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_accounts_manage) {
                Toast.makeText(this, "Управление аккаунтами", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_about) {
                Toast.makeText(this, "О программе", Toast.LENGTH_SHORT).show();
            }

            // Закрываем шторку после клика
            binding.drawerLayout.close();
            return true;
        });

        // Инициализируем адаптер и передаем действие по клику (открыть письмо)
        adapter = new EmailAdapter(email -> {
            Intent intent = new Intent(MailboxActivity.this, ReadEmailActivity.class);
            intent.putExtra("email_data", email);
            startActivity(intent);
        });

        binding.recyclerEmails.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEmails.setAdapter(adapter);

        List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
        if (!accounts.isEmpty()) {
            currentAccount = accounts.get(0);
            binding.toolbar.setSubtitle(currentAccount.getEmail());
            fetchEmails();
        }

        // Кнопка создания нового письма
        binding.fabCompose.setOnClickListener(v -> {
            Intent intent = new Intent(MailboxActivity.this, ComposeActivity.class);
            startActivity(intent);
        });
    }

    private void fetchEmails() {
        Toast.makeText(this, "Подключение к серверу...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                Properties props = new Properties();
                String protocol = currentAccount.isUseSSL() ? "imaps" : "imap";
                props.put("mail.store.protocol", protocol);
                props.put("mail." + protocol + ".host", currentAccount.getImapHost());
                props.put("mail." + protocol + ".port", String.valueOf(currentAccount.getImapPort()));

                Session session = Session.getInstance(props, null);
                Store store = session.getStore(protocol);
                store.connect(currentAccount.getImapHost(), currentAccount.getEmail(), currentAccount.getPassword());

                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                int messageCount = inbox.getMessageCount();
                List<EmailMessage> emailList = new ArrayList<>();

                if (messageCount > 0) {
                    int start = Math.max(1, messageCount - 14);
                    Message[] messages = inbox.getMessages(start, messageCount);

                    for (int i = messages.length - 1; i >= 0; i--) {
                        Message msg = messages[i];
                        String sender = (msg.getFrom() != null && msg.getFrom().length > 0)
                                ? msg.getFrom()[0].toString()
                                : "Неизвестный отправитель";
                        String subject = msg.getSubject();

                        // Извлекаем текст
                        String body = getTextFromMessage(msg);

                        emailList.add(new EmailMessage(sender, subject, body));
                    }
                }

                inbox.close(false);
                store.close();

                runOnUiThread(() -> {
                    adapter.setEmails(emailList);
                    Toast.makeText(this, "Письма загружены!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Вспомогательный метод для парсинга текста из JavaMail
    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    return bodyPart.getContent().toString();
                } else if (bodyPart.isMimeType("text/html")) {
                    return bodyPart.getContent().toString();
                }
            }
        }
        return "Текст письма недоступен (или формат не поддерживается)";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}