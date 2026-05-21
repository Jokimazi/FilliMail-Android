package site.jokimazi.fillimail;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import site.jokimazi.fillimail.databinding.ActivityComposeBinding;
import site.jokimazi.fillimail.model.EmailAccount;

public class ComposeActivity extends AppCompatActivity {

    private ActivityComposeBinding binding;
    private EmailAccount currentAccount;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Получаем наш аккаунт из базы
        List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
        if (!accounts.isEmpty()) {
            currentAccount = accounts.get(0);
        } else {
            Toast.makeText(this, "Ошибка аккаунта", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.toolbarCompose.setNavigationOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> sendEmail());
    }

    private void sendEmail() {
        String to = String.valueOf(binding.etTo.getText()).trim();
        String subject = String.valueOf(binding.etSubject.getText()).trim();
        String body = String.valueOf(binding.etBody.getText()).trim();

        if (to.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Заполните получателя и текст", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Отправка...", Toast.LENGTH_SHORT).show();
        binding.btnSend.setEnabled(false);

        executor.execute(() -> {
            try {
                Properties props = new Properties();
                // Всегда используем префикс smtp, но включаем SSL отдельным флагом
                props.put("mail.smtp.host", currentAccount.getSmtpHost());
                props.put("mail.smtp.port", String.valueOf(currentAccount.getSmtpPort()));
                props.put("mail.smtp.auth", "true");

                if (currentAccount.isUseSSL()) {
                    props.put("mail.smtp.ssl.enable", "true");
                    // Доверяем любому сертификату (важно для личных серверов)
                    props.put("mail.smtp.ssl.trust", "*");
                }

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(currentAccount.getEmail(), currentAccount.getPassword());
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(currentAccount.getEmail()));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Письмо отправлено!", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    android.util.Log.e("FilliMailError", "ОШИБКА SMTP", e);
                    Toast.makeText(this, "Ошибка отправки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.btnSend.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}