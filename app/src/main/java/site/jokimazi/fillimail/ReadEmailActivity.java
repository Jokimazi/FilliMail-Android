package site.jokimazi.fillimail;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeUtility;
import site.jokimazi.fillimail.model.EmailAccount;
import site.jokimazi.fillimail.model.EmailMessage;

public class ReadEmailActivity extends AppCompatActivity {

    private TextView tvSender, tvReceiver, tvSubject, tvBodyPlain;
    private WebView wvBody;
    private ImageView ivAvatar;
    private LinearLayout layoutAttachments;
    private EmailMessage currentEmail;

    private static class ParsedData {
        String plainContent = "";
        String htmlContent = "";
        boolean isHtml = false;
        List<AttachmentInfo> attachments = new ArrayList<>();
    }

    private static class AttachmentInfo {
        String fileName;
        String mimeType;
        int size;

        AttachmentInfo(String fileName, String mimeType, int size) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.size = size;
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
        layoutAttachments = findViewById(R.id.layout_attachments);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_read);

        toolbar.setNavigationOnClickListener(v -> finish());

        wvBody.getSettings().setJavaScriptEnabled(false);
        wvBody.getSettings().setSupportZoom(true);
        wvBody.getSettings().setBuiltInZoomControls(true);
        wvBody.getSettings().setDisplayZoomControls(false);

        currentEmail = (EmailMessage) getIntent().getSerializableExtra("email_object");

        if (currentEmail != null) {
            boolean isSentFolder = currentEmail.folderName != null && (currentEmail.folderName.equalsIgnoreCase("Sent") || currentEmail.folderName.equalsIgnoreCase("Отправленные"));

            String displayName = (currentEmail.senderName != null && !currentEmail.senderName.isEmpty())
                    ? currentEmail.senderName + " <" + currentEmail.senderEmail + ">"
                    : currentEmail.senderEmail;

            if (isSentFolder) {
                tvSender.setText(getString(R.string.format_from, currentEmail.receiver));
                tvReceiver.setText(getString(R.string.format_to, displayName));
            } else {
                tvSender.setText(getString(R.string.format_from, displayName));
                tvReceiver.setText(getString(R.string.format_to, currentEmail.receiver));
            }

            tvSubject.setText(currentEmail.subject);

            tvBodyPlain.setVisibility(View.VISIBLE);
            wvBody.setVisibility(View.GONE);
            tvBodyPlain.setText(getString(R.string.state_loading));

            Glide.with(this)
                    .load(currentEmail.getGravatarUrl())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(ivAvatar);

            loadEmailBody(currentEmail);
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
                    final ParsedData parsed = new ParsedData();
                    extractParts(msg, parsed);

                    runOnUiThread(() -> {
                        if (parsed.isHtml) {
                            tvBodyPlain.setVisibility(View.GONE);
                            wvBody.setVisibility(View.VISIBLE);
                            String adaptiveHtml = prepareHtmlForMobile(parsed.htmlContent);
                            wvBody.loadDataWithBaseURL(null, adaptiveHtml, "text/html; charset=utf-8", "UTF-8", null);
                        } else {
                            wvBody.setVisibility(View.GONE);
                            tvBodyPlain.setVisibility(View.VISIBLE);
                            tvBodyPlain.setText(parsed.plainContent.trim());
                        }

                        renderAttachments(parsed.attachments);
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

    private void renderAttachments(List<AttachmentInfo> attachments) {
        layoutAttachments.removeAllViews();
        if (attachments.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (AttachmentInfo att : attachments) {
            View view = inflater.inflate(R.layout.item_attachment, layoutAttachments, false);
            TextView tvName = view.findViewById(R.id.tv_attachment_name);
            TextView tvInfo = view.findViewById(R.id.tv_attachment_info);

            tvName.setText(att.fileName);

            String sizeStr = att.size > 0 ? (att.size / 1024) + " KB" : "Unknown size";
            tvInfo.setText(sizeStr);

            view.setOnClickListener(v -> startDownload(att.fileName));

            layoutAttachments.addView(view);
        }
    }

    private void startDownload(String targetFileName) {
        Toast.makeText(this, "Downloading " + targetFileName + "...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                EmailAccount targetAccount = App.getInstance().getDatabase().accountDao().getAccountById(currentEmail.accountId);
                if (targetAccount == null) return;

                Properties props = new Properties();
                props.setProperty("mail.store.protocol", "imaps");
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect(targetAccount.getImapHost(), targetAccount.getImapPort(), targetAccount.getEmail(), targetAccount.getPassword());

                Folder folder = store.getFolder(currentEmail.folderName);
                folder.open(Folder.READ_ONLY);

                Message msg = ((UIDFolder) folder).getMessageByUID(currentEmail.uid);
                if (msg != null) {
                    Part targetPart = findPartByName(msg, targetFileName);
                    if (targetPart != null) {
                        saveAndOpenFile(targetPart, targetFileName);
                    }
                }

                folder.close(false);
                store.close();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private Part findPartByName(Part part, String targetName) throws Exception {
        String fileName = part.getFileName();
        if (fileName != null) {
            String decodedName = MimeUtility.decodeText(fileName);
            if (decodedName.equals(targetName)) {
                return part;
            }
        }
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part found = findPartByName(mp.getBodyPart(i), targetName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void saveAndOpenFile(Part part, String fileName) throws Exception {
        String mimeType = part.getContentType();
        if (mimeType != null && mimeType.contains(";")) {
            mimeType = mimeType.split(";")[0].trim();
        } else {
            mimeType = "application/octet-stream";
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (InputStream is = part.getInputStream();
                 OutputStream os = getContentResolver().openOutputStream(uri)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            runOnUiThread(() -> Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void extractParts(Part part, ParsedData parsed) throws Exception {
        String disposition = part.getDisposition();
        String fileName = part.getFileName();

        if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || fileName != null) {
            String decodedName = fileName != null ? MimeUtility.decodeText(fileName) : "Unnamed File";
            String mime = part.getContentType() != null ? part.getContentType().split(";")[0] : "";
            parsed.attachments.add(new AttachmentInfo(decodedName, mime, part.getSize()));
        } else if (part.isMimeType("text/html")) {
            parsed.htmlContent = part.getContent().toString();
            parsed.isHtml = true;
        } else if (part.isMimeType("text/plain")) {
            parsed.plainContent += part.getContent().toString() + "\n";
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                extractParts(mp.getBodyPart(i), parsed);
            }
        }
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
}