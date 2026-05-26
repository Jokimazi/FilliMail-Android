package site.jokimazi.fillimail;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import site.jokimazi.fillimail.model.EmailAccount;
import site.jokimazi.fillimail.model.EmailMessage;
import java.util.List;
import java.util.Properties;

public class ReadEmailActivity extends AppCompatActivity {

    private static final String TAG = "ReadEmailActivity";
    private TextView tvSender, tvSubject, tvBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_email);

        tvSender = findViewById(R.id.tv_read_sender);
        tvSubject = findViewById(R.id.tv_read_subject);
        tvBody = findViewById(R.id.tv_read_body);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_read);

        toolbar.setNavigationOnClickListener(v -> finish());

        EmailMessage email = (EmailMessage) getIntent().getSerializableExtra("email_object");

        if (email != null) {
            tvSender.setText(email.sender);
            tvSubject.setText(email.subject);
            tvBody.setText("Загрузка...");
            loadEmailBody(email);
        }
    }

    private void loadEmailBody(EmailMessage email) {
        new Thread(() -> {
            try {
                // 1. Берем все наши аккаунты
                List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
                Message msg = null;
                EmailAccount targetAccount = null;

                // 2. Ищем письмо, перебирая все наши ящики
                for (EmailAccount acc : accounts) {
                    Properties props = new Properties();
                    props.setProperty("mail.store.protocol", "imaps");
                    Session session = Session.getInstance(props);
                    Store store = session.getStore("imaps");
                    try {
                        store.connect(acc.getImapHost(), acc.getImapPort(), acc.getEmail(), acc.getPassword());
                        Folder folder = store.getFolder("INBOX");
                        folder.open(Folder.READ_ONLY);

                        // Ищем письмо в этом ящике
                        for (Message m : folder.getMessages()) {
                            if (m.getSubject() != null && m.getSubject().equals(email.subject)) {
                                msg = m;
                                targetAccount = acc;
                                break;
                            }
                        }
                        if (msg != null) break; // Нашли!
                        folder.close(false);
                        store.close();
                    } catch (Exception e) { continue; } // Если не этот ящик, пробуем следующий
                }

                if (msg != null) {
                    final String contentStr = getTextFromMessage(msg);
                    runOnUiThread(() -> tvBody.setText(contentStr));
                } else {
                    runOnUiThread(() -> tvBody.setText("Письмо не найдено."));
                }

            } catch (Exception e) {
                runOnUiThread(() -> tvBody.setText("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private String getTextFromMessage(javax.mail.Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            javax.mail.Multipart multipart = (javax.mail.Multipart) message.getContent();
            return getTextFromMultipart(multipart);
        }
        return "Неподдерживаемый формат письма";
    }

    private String getTextFromMultipart(javax.mail.Multipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            javax.mail.BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                // Если HTML - пока просто берем текст, позже можно подключить парсер
                result.append(bodyPart.getContent().toString().replaceAll("<[^>]*>", ""));
            } else if (bodyPart.getContent() instanceof javax.mail.Multipart) {
                result.append(getTextFromMultipart((javax.mail.Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}