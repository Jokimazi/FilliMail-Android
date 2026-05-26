package site.jokimazi.fillimail;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import site.jokimazi.fillimail.model.EmailAccount;

public class ComposeActivity extends AppCompatActivity {

    private AutoCompleteTextView actvFrom;
    private EditText etTo, etSubject, etBody;
    private LinearLayout layoutAttachments;
    private List<EmailAccount> accounts;
    private EmailAccount selectedAccount;
    private final List<Uri> attachedUris = new ArrayList<>();

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            addAttachment(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        addAttachment(data.getData());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        actvFrom = findViewById(R.id.actv_from);
        etTo = findViewById(R.id.et_to);
        etSubject = findViewById(R.id.et_subject);
        etBody = findViewById(R.id.et_body);
        layoutAttachments = findViewById(R.id.layout_compose_attachments);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_compose);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_send) {
                sendEmail();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_bold).setOnClickListener(v -> applyStyle(Typeface.BOLD));
        findViewById(R.id.btn_italic).setOnClickListener(v -> applyStyle(Typeface.ITALIC));

        findViewById(R.id.btn_attach).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            filePickerLauncher.launch(Intent.createChooser(intent, "Select files"));
        });

        loadAccounts();
    }

    private void applyStyle(int typeface) {
        int start = etBody.getSelectionStart();
        int end = etBody.getSelectionEnd();
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        if (start != end) {
            Spannable spannable = etBody.getText();
            spannable.setSpan(new StyleSpan(typeface), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void addAttachment(Uri uri) {
        if (!attachedUris.contains(uri)) {
            attachedUris.add(uri);
            renderAttachments();
        }
    }

    private void renderAttachments() {
        layoutAttachments.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Uri uri : attachedUris) {
            View view = inflater.inflate(R.layout.item_attachment, layoutAttachments, false);
            TextView tvName = view.findViewById(R.id.tv_attachment_name);
            TextView tvInfo = view.findViewById(R.id.tv_attachment_info);

            tvName.setText(getFileName(uri));
            tvInfo.setText("Нажмите, чтобы удалить");

            view.setOnClickListener(v -> {
                attachedUris.remove(uri);
                renderAttachments();
            });

            layoutAttachments.addView(view);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "file";
    }

    private void loadAccounts() {
        new Thread(() -> {
            accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
            if (accounts.isEmpty()) return;

            List<String> emails = new ArrayList<>();
            for (EmailAccount acc : accounts) {
                emails.add(acc.email);
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, emails);
                actvFrom.setAdapter(adapter);
                actvFrom.setText(emails.get(0), false);
                selectedAccount = accounts.get(0);

                actvFrom.setOnItemClickListener((parent, view, position, id) -> selectedAccount = accounts.get(position));
            });
        }).start();
    }

    private void sendEmail() {
        if (selectedAccount == null) {
            Toast.makeText(this, getString(R.string.toast_no_account), Toast.LENGTH_SHORT).show();
            return;
        }

        String to = etTo.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String htmlBody = Html.toHtml(etBody.getText(), 0);

        if (to.isEmpty()) return;

        Toast.makeText(this, getString(R.string.toast_sending), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", selectedAccount.smtpHost);
                props.put("mail.smtp.port", String.valueOf(selectedAccount.smtpPort));
                props.put("mail.smtp.auth", "true");

                if (selectedAccount.ssl) {
                    props.put("mail.smtp.socketFactory.port", String.valueOf(selectedAccount.smtpPort));
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                } else {
                    props.put("mail.smtp.starttls.enable", "true");
                }

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(selectedAccount.email, selectedAccount.password);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(selectedAccount.email));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setSentDate(new Date());

                Multipart multipart = new MimeMultipart();

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(htmlBody, "text/html; charset=utf-8");
                multipart.addBodyPart(textPart);

                for (Uri uri : attachedUris) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        String mimeType = getContentResolver().getType(uri);
                        if (mimeType == null) mimeType = "application/octet-stream";
                        ByteArrayDataSource source = new ByteArrayDataSource(is, mimeType);
                        attachPart.setDataHandler(new javax.activation.DataHandler(source));
                        attachPart.setFileName(getFileName(uri));
                        multipart.addBodyPart(attachPart);
                    }
                }

                message.setContent(multipart);
                Transport.send(message);

                try {
                    Properties imapProps = new Properties();
                    imapProps.setProperty("mail.store.protocol", "imaps");
                    Session imapSession = Session.getInstance(imapProps);
                    Store store = imapSession.getStore("imaps");
                    store.connect(selectedAccount.imapHost, selectedAccount.imapPort, selectedAccount.email, selectedAccount.password);

                    Folder[] folders = store.getDefaultFolder().list("*");
                    Folder sentFolder = null;
                    for (Folder f : folders) {
                        String name = f.getName().toLowerCase();
                        if (name.contains("sent") || name.contains("отправленные")) {
                            sentFolder = f;
                            break;
                        }
                    }
                    if (sentFolder != null) {
                        sentFolder.open(Folder.READ_WRITE);
                        message.setFlag(Flags.Flag.SEEN, true);
                        sentFolder.appendMessages(new Message[]{message});
                        sentFolder.close(false);
                    }
                    store.close();
                } catch (Exception ignored) {}

                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.toast_sent), Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.toast_send_error, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}