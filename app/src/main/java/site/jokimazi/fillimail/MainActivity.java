package site.jokimazi.fillimail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import site.jokimazi.fillimail.databinding.ActivityMainBinding;
import site.jokimazi.fillimail.model.EmailAccount;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Проверяем, есть ли уже аккаунт в базе. Если есть - сразу идем в почту.
        List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
        if (!accounts.isEmpty()) {
            openMailbox();
            return; // Останавливаем выполнение onCreate
        }

        // 2. Если аккаунтов нет, показываем экран входа
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnAddAccount.setOnClickListener(v -> saveAccount());
    }

    private void saveAccount() {
        String email = String.valueOf(binding.etEmail.getText()).trim();
        String password = String.valueOf(binding.etPassword.getText()).trim();
        String imapHost = String.valueOf(binding.etImapHost.getText()).trim();
        String portStr = String.valueOf(binding.etImapPort.getText()).trim();
        boolean useSsl = binding.cbUseSsl.isChecked();

        if (email.isEmpty() || password.isEmpty() || imapHost.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        int imapPort = Integer.parseInt(portStr);
        String smtpHost = imapHost.replace("imap", "smtp");
        int smtpPort = 465;

        EmailAccount account = new EmailAccount(
                email, password, imapHost, imapPort, smtpHost, smtpPort, useSsl
        );

        try {
            App.getInstance().getDatabase().accountDao().insert(account);
            openMailbox(); // Сразу переходим к почте после сохранения
        } catch (Exception e) {
            android.util.Log.e("FilliMailError", "КРИТИЧЕСКАЯ ОШИБКА БД", e);
            Toast.makeText(this, "Смотри лог!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMailbox() {
        Intent intent = new Intent(this, MailboxActivity.class);
        startActivity(intent);
        finish(); // Закрываем экран входа, чтобы нельзя было вернуться кнопкой "Назад"
    }
}