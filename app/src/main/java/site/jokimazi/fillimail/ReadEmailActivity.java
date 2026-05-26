package site.jokimazi.fillimail;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
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

    private TextView tvSender, tvReceiver, tvSubject, tvBodyPlain;
    private WebView wvBody;
    private ImageView ivAvatar;

    private static class ParsedBody {
        String content;
        boolean isHtml;
        ParsedBody(String content, boolean isHtml) {
            this.content = content;
            this.isHtml = isHtml;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_email);

        tvSender = findViewById(R.id.tv_read_sender);
        tvReceiver = findViewById(R.id.tv_read_receiver);
        tvSubject = findViewById(R.id.tv_read_subject);
        tvBodyPlain = findViewById(R.id.tv_read_body_plain);
        wvBody = findViewById(R.id.wv_read_body);
        ivAvatar = findViewById(R.id.iv_read_avatar);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_read);

        toolbar.setNavigationOnClickListener(v -> finish());

        wvBody.getSettings().setJavaScriptEnabled(false);
        wvBody.getSettings().setSupportZoom(true);
        wvBody.getSettings().setBuiltInZoomControls(true);
        wvBody.getSettings().setDisplayZoomControls(false);

        EmailMessage email = (EmailMessage) getIntent().getSerializableExtra("email_object");

        if (email != null) {
            String displayName = (email.senderName != null && !email.senderName.isEmpty())
                    ? email.senderName + " <" + email.senderEmail + ">"
                    : email.senderEmail;

            tvSender.setText(getString(R.string.format_from, displayName));
            tvReceiver.setText(getString(R.string.format_to, email.receiver));
            tvSubject.setText(email.subject);

            tvBodyPlain.setVisibility(View.VISIBLE);
            wvBody.setVisibility(View.GONE);
            tvBodyPlain.setText(getString(R.string.state_loading));

            Glide.with(this)
                    .load(email.getGravatarUrl())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(ivAvatar);

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
                    runOnUiThread(() -> showError(getString(R.string.toast_account_error)));
                    return;
                }

                Properties props = new Properties();
                props.setProperty("mail.store.protocol", "imaps");
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");

                store.connect(targetAccount.getImapHost(), targetAccount.getImapPort(), targetAccount.getEmail(), targetAccount.getPassword());

                Folder folder = store.getFolder(email.folderName);
                folder.open(Folder.READ_ONLY);

                if (folder instanceof UIDFolder && email.uid != -1) {
                    msg = ((UIDFolder) folder).getMessageByUID(email.uid);
                }

                if (msg != null) {
                    final ParsedBody parsed = extractBody(msg);
                    runOnUiThread(() -> {
                        if (parsed.isHtml) {
                            tvBodyPlain.setVisibility(View.GONE);
                            wvBody.setVisibility(View.VISIBLE);
                            String adaptiveHtml = prepareHtmlForMobile(parsed.content);
                            wvBody.loadDataWithBaseURL(null, adaptiveHtml, "text/html; charset=utf-8", "UTF-8", null);
                        } else {
                            wvBody.setVisibility(View.GONE);
                            tvBodyPlain.setVisibility(View.VISIBLE);
                            tvBodyPlain.setText(parsed.content);
                        }
                    });
                } else {
                    runOnUiThread(() -> showError(getString(R.string.state_email_not_found)));
                }

                folder.close(false);
                store.close();

            } catch (Exception e) {
                runOnUiThread(() -> showError(getString(R.string.state_error, e.getMessage())));
            }
        }).start();
    }

    private void showError(String errorMsg) {
        wvBody.setVisibility(View.GONE);
        tvBodyPlain.setVisibility(View.VISIBLE);
        tvBodyPlain.setText(errorMsg);
    }

    private String prepareHtmlForMobile(String html) {
        String viewportAndStyle = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=2.0\">" +
                "<style>img { max-width: 100% !important; height: auto !important; } body { word-wrap: break-word; }</style>";
        if (html.toLowerCase().contains("<head>")) {
            return html.replaceFirst("(?i)<head>", "<head>\n" + viewportAndStyle);
        } else {
            return "<html><head>" + viewportAndStyle + "</head><body>" + html + "</body></html>";
        }
    }

    private ParsedBody extractBody(Message message) throws Exception {
        if (message.isMimeType("text/html")) {
            return new ParsedBody(message.getContent().toString(), true);
        } else if (message.isMimeType("text/plain")) {
            return new ParsedBody(message.getContent().toString(), false);
        } else if (message.isMimeType("multipart/*")) {
            javax.mail.Multipart multipart = (javax.mail.Multipart) message.getContent();
            return extractFromMultipart(multipart);
        }
        return new ParsedBody(getString(R.string.state_unsupported_format), false);
    }

    private ParsedBody extractFromMultipart(javax.mail.Multipart multipart) throws Exception {
        StringBuilder plainText = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            javax.mail.BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/html")) {
                return new ParsedBody(bodyPart.getContent().toString(), true);
            } else if (bodyPart.isMimeType("text/plain")) {
                plainText.append(bodyPart.getContent().toString()).append("\n");
            } else if (bodyPart.getContent() instanceof javax.mail.Multipart) {
                ParsedBody nested = extractFromMultipart((javax.mail.Multipart) bodyPart.getContent());
                if (nested.isHtml) {
                    return nested;
                } else {
                    plainText.append(nested.content);
                }
            }
        }
        return new ParsedBody(plainText.toString(), false);
    }
}