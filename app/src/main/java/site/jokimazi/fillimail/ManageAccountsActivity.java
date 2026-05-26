package site.jokimazi.fillimail;

import android.content.Intent;
import android.content.res.TypedArray;
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
            startActivity(new Intent(this, AddAccountActivity.class));
        });

        loadAccounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.toast_cannot_delete_last), Toast.LENGTH_SHORT).show());
            } else {
                runOnUiThread(() -> {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_delete_title))
                            .setMessage(getString(R.string.dialog_delete_message, account.getEmail()))
                            .setPositiveButton(getString(R.string.dialog_yes), (d, w) -> performDelete(account))
                            .setNegativeButton(getString(R.string.dialog_no), null)
                            .create();

                    dialog.show();

                    TypedArray a = obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
                    int textColor = a.getColor(0, 0);
                    a.recycle();

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
                });
            }
        }).start();
    }

    private void performDelete(EmailAccount account) {
        new Thread(() -> {
            App.getInstance().getDatabase().accountDao().delete(account);
            runOnUiThread(this::loadAccounts);
        }).start();
    }

    private void onEditClick(EmailAccount account) {
        Intent intent = new Intent(this, AddAccountActivity.class);
        intent.putExtra("edit_account_id", account.getId());
        startActivity(intent);
    }
}