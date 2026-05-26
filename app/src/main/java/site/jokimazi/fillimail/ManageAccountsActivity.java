package site.jokimazi.fillimail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import site.jokimazi.fillimail.adapter.AccountAdapter;
import site.jokimazi.fillimail.model.EmailAccount;

public class ManageAccountsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AccountAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_accounts);

        recyclerView = findViewById(R.id.recycler_accounts);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add_account);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(v -> {
        });

        loadAccounts();
    }

    private void loadAccounts() {
        new Thread(() -> {
            List<EmailAccount> accounts = App.getInstance().getDatabase().accountDao().getAllAccounts();
            runOnUiThread(() -> {
                adapter = new AccountAdapter(accounts, this::onDeleteClick, this::onEditClick);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    private void onDeleteClick(EmailAccount account) {
        new Thread(() -> {
            List<EmailAccount> list = App.getInstance().getDatabase().accountDao().getAllAccounts();
            if (list.size() <= 1) {
                runOnUiThread(() -> Toast.makeText(this, "Нельзя удалить последний аккаунт", Toast.LENGTH_SHORT).show());
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Удаление")
                        .setMessage("Удалить " + account.getEmail() + "?")
                        .setPositiveButton("Да", (d, w) -> {
                            App.getInstance().getDatabase().accountDao().delete(account);
                            loadAccounts();
                        })
                        .setNegativeButton("Нет", null)
                        .show();
            }
        }).start();
    }

    private void onEditClick(EmailAccount account) {
        Intent intent = new Intent(this, AddAccountActivity.class);
        intent.putExtra("edit_account_id", account.getId()); // Убедись, что ID передается
        startActivity(intent);
    }

    private void performDelete(EmailAccount account) {
        new Thread(() -> {
            List<EmailAccount> all = App.getInstance().getDatabase().accountDao().getAllAccounts();
            if (all.size() > 1) {
                App.getInstance().getDatabase().accountDao().delete(account);
                runOnUiThread(this::loadAccounts);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Нельзя удалить последний аккаунт", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}