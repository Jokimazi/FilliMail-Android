package site.jokimazi.fillimail;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
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
            tvBody.setText(getString(R.string.state_loading));
            loadEmailBody(email);
        }
    }

    private void loadEmailBody(EmailMessage email) {
        new Thread(() -> {
            try {
                List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
                Message msg = null;
                EmailAccount targetAccount = null;

                for (EmailAccount acc : accounts) {
                    if (acc.getId() == email.accountId) {
                        targetAccount = acc;
                        break;
                    }
                }

                if (targetAccount == null) {
                    runOnUiThread(() -> tvBody.setText(getString(R.string.toast_account_error)));
                    return;
                }

                Properties props = new Properties();
                props.setProperty("mail.store.protocol", "imaps");
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");

                store.connect(targetAccount.getImapHost(), targetAccount.getImapPort(), targetAccount.getEmail(), targetAccount.getPassword());

                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);

                if (folder instanceof UIDFolder && email.uid != -1) {
                    msg = ((UIDFolder) folder).getMessageByUID(email.uid);
                }

                if (msg != null) {
                    final String contentStr = getTextFromMessage(msg);
                    runOnUiThread(() -> tvBody.setText(contentStr));
                } else {
                    runOnUiThread(() -> tvBody.setText(getString(R.string.state_email_not_found)));
                }

                folder.close(false);
                store.close();

            } catch (Exception e) {
                runOnUiThread(() -> tvBody.setText(getString(R.string.state_error, e.getMessage())));
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
        return getString(R.string.state_unsupported_format);
    }

    private String getTextFromMultipart(javax.mail.Multipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            javax.mail.BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                result.append(bodyPart.getContent().toString().replaceAll("<[^>]*>", ""));
            } else if (bodyPart.getContent() instanceof javax.mail.Multipart) {
                result.append(getTextFromMultipart((javax.mail.Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}