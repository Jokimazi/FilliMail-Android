package site.jokimazi.fillimail;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import site.jokimazi.fillimail.model.EmailAccount;

public class AddAccountActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etImapHost, etImapPort, etSmtpHost, etSmtpPort;
    private CheckBox cbSsl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etImapHost = findViewById(R.id.et_imap_host);
        etImapPort = findViewById(R.id.et_imap_port);

        // Новые поля
        etSmtpHost = findViewById(R.id.et_smtp_host);
        etSmtpPort = findViewById(R.id.et_smtp_port);
        cbSsl = findViewById(R.id.cb_ssl);

        Button btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> saveAccount());
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
                EmailAccount newAccount = new EmailAccount(email, password, imapHost, imapPort, smtpHost, smtpPort, useSsl);
                App.getInstance().getDatabase().accountDao().insert(newAccount);

                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.toast_account_added), Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.toast_save_error, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}