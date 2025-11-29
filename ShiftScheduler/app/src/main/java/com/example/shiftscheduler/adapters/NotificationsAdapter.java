package com.example.shiftscheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.R;
import com.example.shiftscheduler.models.NotificationItem;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<NotificationItem> notifications;

    public NotificationsAdapter(List<NotificationItem> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);

        holder.textNotificationTitle.setText(item.getTitle());
        holder.textNotificationMessage.setText(item.getMessage());

        Timestamp ts = item.getCreatedAt();
        String timeText = "Just now";
        if (ts != null) {
            timeText = DateFormat.getDateTimeInstance().format(ts.toDate());
        }
        holder.textNotificationTime.setText(timeText);
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView textNotificationTitle;
        TextView textNotificationMessage;
        TextView textNotificationTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textNotificationTitle = itemView.findViewById(R.id.textNotificationTitle);
            textNotificationMessage = itemView.findViewById(R.id.textNotificationMessage);
            textNotificationTime = itemView.findViewById(R.id.textNotificationTime);
        }
    }
}
