package site.jokimazi.fillimail;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import site.jokimazi.fillimail.model.EmailAccount;
import java.util.List;

public class AddAccountActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etImapHost, etImapPort, etSmtpHost, etSmtpPort;
    private CheckBox cbSsl;
    private Button btnSave;
    private int editAccountId = -1; // -1 означает создание нового, иначе — редактирование

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etImapHost = findViewById(R.id.et_imap_host);
        etImapPort = findViewById(R.id.et_imap_port);
        etSmtpHost = findViewById(R.id.et_smtp_host);
        etSmtpPort = findViewById(R.id.et_smtp_port);
        cbSsl = findViewById(R.id.cb_ssl);
        btnSave = findViewById(R.id.btn_save);

        // ИСПРАВЛЕНИЕ: Автоопределение настроек сервера при потере фокуса поля Email
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                autoDetectSettings();
            }
        });

        // ИСПРАВЛЕНИЕ: Проверяем, зашли ли мы в режиме редактирования
        editAccountId = getIntent().getIntExtra("edit_account_id", -1);
        if (editAccountId != -1) {
            btnSave.setText("Сохранить изменения");
            loadAccountDataForEditing();
        }

        btnSave.setOnClickListener(v -> saveAccount());
    }

    private void loadAccountDataForEditing() {
        new Thread(() -> {
            List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
            EmailAccount target = null;
            for (EmailAccount acc : accounts) {
                if (acc.getId() == editAccountId) {
                    target = acc;
                    break;
                }
            }
            if (target != null) {
                final EmailAccount finalTarget = target;
                runOnUiThread(() -> {
                    etEmail.setText(finalTarget.getEmail());
                    etPassword.setText(finalTarget.getPassword());
                    etImapHost.setText(finalTarget.getImapHost());
                    etImapPort.setText(String.valueOf(finalTarget.getImapPort()));
                    etSmtpHost.setText(finalTarget.getSmtpHost());
                    etSmtpPort.setText(String.valueOf(finalTarget.getSmtpPort()));
                    cbSsl.setChecked(finalTarget.isUseSSL());
                });
            }
        }).start();
    }

    private void autoDetectSettings() {
        String email = etEmail.getText().toString().trim();
        if (!email.contains("@")) return;

        Toast.makeText(this, getString(R.string.toast_searching_settings), Toast.LENGTH_SHORT).show();
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        String imapHost = "";
        String smtpHost = "";
        int imapPort = 993;
        int smtpPort = 465;
        boolean found = true;

        switch (domain) {
            case "yandex.ru":
            case "yandex.com":
                imapHost = "imap.yandex.ru";
                smtpHost = "smtp.yandex.ru";
                break;
            case "gmail.com":
                imapHost = "imap.gmail.com";
                smtpHost = "smtp.gmail.com";
                break;
            case "mail.ru":
            case "bk.ru":
            case "inbox.ru":
            case "list.ru":
                imapHost = "imap.mail.ru";
                smtpHost = "smtp.mail.ru";
                break;
            default:
                found = false;
                imapHost = "mail." + domain;
                smtpHost = "mail." + domain;
                break;
        }

        etImapHost.setText(imapHost);
        etImapPort.setText(String.valueOf(imapPort));
        etSmtpHost.setText(smtpHost);
        etSmtpPort.setText(String.valueOf(smtpPort));
        cbSsl.setChecked(true);

        if (found) {
            Toast.makeText(this, getString(R.string.toast_settings_found), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.toast_settings_not_found), Toast.LENGTH_LONG).show();
        }
    }

    private void saveAccount() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String imapHost = etImapHost.getText().toString().trim();
        String imapPortStr = etImapPort.getText().toString().trim();
        String smtpHost = etSmtpHost.getText().toString().trim();
        String smtpPortStr = etSmtpPort.getText().toString().trim();
        boolean useSsl = cbSsl.isChecked();

        if (email.isEmpty() || password.isEmpty() || imapHost.isEmpty() || imapPortStr.isEmpty() || smtpHost.isEmpty() || smtpPortStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        int imapPort, smtpPort;
        try {
            imapPort = Integer.parseInt(imapPortStr);
            smtpPort = Integer.parseInt(smtpPortStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.toast_invalid_port), Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                EmailAccount account = new EmailAccount(email, password, imapHost, imapPort, smtpHost, smtpPort, useSsl);

                // ИСПРАВЛЕНИЕ: Если редактируем — обновляем, если нет — создаем новый
                if (editAccountId != -1) {
                    account.setId(editAccountId);
                    App.getInstance().getDatabase().accountDao().update(account);
                } else {
                    App.getInstance().getDatabase().accountDao().insert(account);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, editAccountId != -1 ? "Изменения сохранены!" : getString(R.string.toast_account_added), Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.toast_save_error, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}