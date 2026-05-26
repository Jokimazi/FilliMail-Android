package site.jokimazi.fillimail.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import site.jokimazi.fillimail.R;
import site.jokimazi.fillimail.model.EmailMessage;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private final List<EmailMessage> emails = new ArrayList<>();
    private final OnEmailClickListener listener;
    private boolean showReceiver = false;

    public interface OnEmailClickListener {
        void onEmailClick(EmailMessage email);
    }

    public EmailAdapter(OnEmailClickListener listener) {
        this.listener = listener;
    }

    public void setShowReceiver(boolean showReceiver) {
        this.showReceiver = showReceiver;
    }

    public void setEmails(List<EmailMessage> newEmails) {
        emails.clear();
        emails.addAll(newEmails);
        notifyDataSetChanged();
    }

    public void addEmailSorted(EmailMessage email) {
        int index = 0;
        for (int i = 0; i < emails.size(); i++) {
            Date existingDate = emails.get(i).date;
            if (email.date != null && existingDate != null) {
                if (email.date.after(existingDate)) {
                    break;
                }
            }
            index++;
        }
        emails.add(index, email);
        notifyItemInserted(index);
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

        Context context = holder.itemView.getContext();

        holder.tvSender.setText(context.getString(R.string.format_from, email.sender));

        if (showReceiver && email.receiver != null) {
            holder.tvReceiver.setVisibility(View.VISIBLE);
            holder.tvReceiver.setText(context.getString(R.string.format_to, email.receiver));
        } else {
            holder.tvReceiver.setVisibility(View.GONE);
        }

        if (email.date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(email.date));
        } else {
            holder.tvDate.setText("");
        }

        String subject = (email.subject != null && !email.subject.trim().isEmpty())
                ? email.subject
                : context.getString(R.string.no_subject);
        holder.tvSubject.setText(subject);

        holder.itemView.setOnClickListener(v -> listener.onEmailClick(email));
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvSubject, tvReceiver, tvDate;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tv_sender);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvReceiver = itemView.findViewById(R.id.tv_receiver);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}