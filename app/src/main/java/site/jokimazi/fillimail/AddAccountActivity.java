package site.jokimazi.fillimail;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import site.jokimazi.fillimail.model.EmailAccount;
import java.util.List;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.Store;

public class AddAccountActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etImapHost, etImapPort, etSmtpHost, etSmtpPort;
    private CheckBox cbSsl;
    private Button btnSave;
    private int editAccountId = -1;

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

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                autoDetectSettings();
            }
        });

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

        // Блокируем кнопку
        btnSave.setEnabled(false);
        Toast.makeText(this, getString(R.string.toast_checking_credentials), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // ИСПРАВЛЕНИЕ: Тестируем подключение
                Properties props = new Properties();
                props.setProperty("mail.store.protocol", "imaps");
                props.setProperty("mail.imaps.timeout", "10000"); // 10 секунд
                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect(imapHost, imapPort, email, password);
                store.close();

                // Авторизация прошла успешно — сохраняем/обновляем
                EmailAccount account = new EmailAccount(email, password, imapHost, imapPort, smtpHost, smtpPort, useSsl);

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
            } catch (javax.mail.AuthenticationFailedException authEx) {
                runOnUiThread(() -> {
                    Toast.makeText(AddAccountActivity.this, getString(R.string.toast_auth_failed), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddAccountActivity.this, getString(R.string.toast_connection_error, e.getMessage()), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                });
            }
        }).start();
    }
}