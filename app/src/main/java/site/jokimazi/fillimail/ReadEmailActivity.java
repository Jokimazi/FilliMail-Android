package site.jokimazi.fillimail;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import site.jokimazi.fillimail.databinding.ActivityReadEmailBinding;
import site.jokimazi.fillimail.model.EmailMessage;

public class ReadEmailActivity extends AppCompatActivity {

    private ActivityReadEmailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReadEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Кнопка "Назад"
        binding.toolbarRead.setNavigationOnClickListener(v -> finish());

        // Получаем переданный объект письма
        EmailMessage email = (EmailMessage) getIntent().getSerializableExtra("email_data");

        if (email != null) {
            binding.tvReadSender.setText("От: " + email.sender);
            binding.tvReadSubject.setText(email.subject != null ? email.subject : "Без темы");
            binding.tvReadBody.setText(email.body);
        }
    }
}