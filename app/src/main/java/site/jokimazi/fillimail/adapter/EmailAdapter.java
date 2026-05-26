package site.jokimazi.fillimail.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import site.jokimazi.fillimail.R;
import site.jokimazi.fillimail.model.EmailMessage;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private final List<EmailMessage> emails = new ArrayList<>();
    private final OnEmailClickListener listener;

    public interface OnEmailClickListener {
        void onEmailClick(EmailMessage email);
    }

    public EmailAdapter(OnEmailClickListener listener) {
        this.listener = listener;
    }

    public void setEmails(List<EmailMessage> newEmails) {
        emails.clear();
        emails.addAll(newEmails);
        notifyDataSetChanged();
    }

    public void addEmail(EmailMessage email) {
        emails.add(email);
        notifyItemInserted(emails.size() - 1);
    }

    public void clear() {
        int size = emails.size();
        emails.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_email, parent, false);
        return new EmailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
        EmailMessage email = emails.get(position);
        holder.tvSender.setText(email.sender);

        String subject = (email.subject != null && !email.subject.isEmpty()) ? email.subject : "(Без темы)";
        holder.tvSubject.setText(subject);

        holder.itemView.setOnClickListener(v -> listener.onEmailClick(email));
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvSubject;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tv_sender);
            tvSubject = itemView.findViewById(R.id.tv_subject);
        }
    }
}