package site.jokimazi.fillimail.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import site.jokimazi.fillimail.R;
import site.jokimazi.fillimail.model.EmailMessage;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private final List<EmailMessage> emails = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();
    private final OnEmailClickListener listener;
    private boolean showReceiver = false;
    private boolean isSelectionMode = false;

    public interface OnEmailClickListener {
        void onEmailClick(EmailMessage email);
        void onSelectionChanged(int count);
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

    public boolean hasEmail(long uid, int accountId) {
        for (EmailMessage m : emails) {
            if (m.uid == uid && m.accountId == accountId) {
                return true;
            }
        }
        return false;
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

    public void toggleSelection(String uniqueKey) {
        if (selectedKeys.contains(uniqueKey)) {
            selectedKeys.remove(uniqueKey);
        } else {
            selectedKeys.add(uniqueKey);
        }
        isSelectionMode = !selectedKeys.isEmpty();
        listener.onSelectionChanged(selectedKeys.size());
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedKeys.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    public List<EmailMessage> getSelectedEmails() {
        List<EmailMessage> selected = new ArrayList<>();
        for (EmailMessage m : emails) {
            if (selectedKeys.contains(m.getUniqueKey())) {
                selected.add(m);
            }
        }
        return selected;
    }

    public void removeEmails(List<EmailMessage> toRemove) {
        emails.removeAll(toRemove);
        notifyDataSetChanged();
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

        boolean isSentFolder = email.folderName != null && (email.folderName.equalsIgnoreCase("Sent") || email.folderName.equalsIgnoreCase("Отправленные"));
        String displayName = (email.senderName != null && !email.senderName.isEmpty()) ? email.senderName : email.senderEmail;

        if (isSentFolder) {
            holder.tvSender.setText(context.getString(R.string.format_to, displayName));
            if (showReceiver && email.receiver != null) {
                holder.tvReceiver.setVisibility(View.VISIBLE);
                holder.tvReceiver.setText(context.getString(R.string.format_from, email.receiver));
            } else {
                holder.tvReceiver.setVisibility(View.GONE);
            }
        } else {
            holder.tvSender.setText(context.getString(R.string.format_from, displayName));
            if (showReceiver && email.receiver != null) {
                holder.tvReceiver.setVisibility(View.VISIBLE);
                holder.tvReceiver.setText(context.getString(R.string.format_to, email.receiver));
            } else {
                holder.tvReceiver.setVisibility(View.GONE);
            }
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

        Glide.with(context)
                .load(email.getGravatarUrl())
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .into(holder.ivAvatar);

        boolean isSelected = selectedKeys.contains(email.getUniqueKey());
        if (isSelected) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorControlHighlight, typedValue, true);
            holder.itemView.setBackgroundColor(typedValue.data);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(email.getUniqueKey());
            } else {
                listener.onEmailClick(email);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(email.getUniqueKey());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvSubject, tvReceiver, tvDate;
        ImageView ivAvatar;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tv_sender);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvReceiver = itemView.findViewById(R.id.tv_receiver);
            tvDate = itemView.findViewById(R.id.tv_date);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }
    }
}