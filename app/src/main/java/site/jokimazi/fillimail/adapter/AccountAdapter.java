package site.jokimazi.fillimail.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import site.jokimazi.fillimail.R;
import site.jokimazi.fillimail.model.EmailAccount;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {
    private List<EmailAccount> list;
    private OnAccountClickListener deleteListener, editListener;

    public interface OnAccountClickListener { void onClick(EmailAccount account); }

    public AccountAdapter(List<EmailAccount> list, OnAccountClickListener delete, OnAccountClickListener edit) {
        this.list = list; this.deleteListener = delete; this.editListener = edit;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmailAccount acc = list.get(position);
        holder.tvEmail.setText(acc.getEmail());
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(acc));
        holder.btnEdit.setOnClickListener(v -> editListener.onClick(acc));
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail; ImageButton btnDelete, btnEdit;
        ViewHolder(View v) { super(v); tvEmail = v.findViewById(R.id.tv_account_email); btnDelete = v.findViewById(R.id.btn_delete); btnEdit = v.findViewById(R.id.btn_edit); }
    }
}