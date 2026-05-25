package site.jokimazi.fillimail;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import site.jokimazi.fillimail.databinding.ActivityMainBinding;
import site.jokimazi.fillimail.model.EmailAccount;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean isAdvancedVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Проверяем, есть ли аккаунт в базе (если да — сразу на почту)
        new Thread(() -> {
            List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
            if (!accounts.isEmpty()) {
                runOnUiThread(() -> {
                    startActivity(new Intent(MainActivity.this, MailboxActivity.class));
                    finish();
                });
            }
        }).start();

        // Слушатель для золотой звёздочки (смена языка)
        binding.btnLangStar.setOnClickListener(v -> toggleLanguage());

        // Автоопределение настроек при потере фокуса с поля Email
        binding.etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                autoDetectSettings();
            }
        });

        // Логика нажатия кнопки Войти
        binding.btnLogin.setOnClickListener(v -> loginAndSave());
    }

    private void autoDetectSettings() {
        String email = String.valueOf(binding.etEmail.getText()).trim();
        if (!email.contains("@")) return;

        Toast.makeText(this, R.string.toast_searching_settings, Toast.LENGTH_SHORT).show();
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        // Логика базы данных популярных доменов
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
                // Если личный сервер почты — оставляем пустым, чтобы пользователь ввёл сам
                found = false;
                imapHost = "mail." + domain;
                smtpHost = "mail." + domain;
                break;
        }

        // Заполняем поля формы
        binding.etImapHost.setText(imapHost);
        binding.etImapPort.setText(String.valueOf(imapPort));
        binding.etSmtpHost.setText(smtpHost);
        binding.etSmtpPort.setText(String.valueOf(smtpPort));
        binding.cbSsl.setChecked(true);

        // Плавно показываем блок ручной настройки
        if (!isAdvancedVisible) {
            binding.layoutAdvancedSettings.setVisibility(View.VISIBLE);
            isAdvancedVisible = true;
        }

        if (found) {
            Toast.makeText(this, R.string.toast_settings_found, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.toast_settings_not_found, Toast.LENGTH_LONG).show();
        }
    }

    private void loginAndSave() {
        String email = String.valueOf(binding.etEmail.getText()).trim();
        String password = String.valueOf(binding.etPassword.getText()).trim();

        String imapHost = String.valueOf(binding.etImapHost.getText()).trim();
        String imapPortStr = String.valueOf(binding.etImapPort.getText()).trim();
        String smtpHost = String.valueOf(binding.etSmtpHost.getText()).trim();
        String smtpPortStr = String.valueOf(binding.etSmtpPort.getText()).trim();

        if (email.isEmpty() || password.isEmpty() || imapHost.isEmpty() || imapPortStr.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем в Room
        new Thread(() -> {
            // Передаем все 7 параметров прямо в конструктор в нужном порядке
            EmailAccount account = new EmailAccount(
                    email,
                    password,
                    imapHost,
                    Integer.parseInt(imapPortStr),
                    smtpHost,
                    Integer.parseInt(smtpPortStr),
                    binding.cbSsl.isChecked()
            );

            App.getInstance().getDatabase().accountDao().insert(account);

            runOnUiThread(() -> {
                startActivity(new Intent(MainActivity.this, MailboxActivity.class));
                finish();
            });
        }).start();
    }

    // Принудительное динамическое переключение языка через звёздочку
    private void toggleLanguage() {
        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        Locale currentLocale = conf.locale;

        String nextLang = currentLocale.getLanguage().equals("en") ? "ru" : "en";
        Locale locale = new Locale(nextLang);
        Locale.setDefault(locale);

        conf.setLocale(locale);
        res.updateConfiguration(conf, res.getDisplayMetrics());

        // Перезапускаем активити, чтобы применить новый язык интерфейса
        Intent intent = getIntent();
        finish();
        startActivity(intent);

        Toast.makeText(this, nextLang.equals("ru") ? "Язык изменен на русский" : "Language changed to English", Toast.LENGTH_SHORT).show();
    }
}