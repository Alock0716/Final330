package com.example.shiftscheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.R;
import com.example.shiftscheduler.ShiftItem;

import java.util.List;

/**
 * List of pending trades / schedule changes OR available shifts.
 * Controls visibility of Cancel / Accept buttons based on flags.
 */
public class PendingTradesAdapter extends RecyclerView.Adapter<PendingTradesAdapter.PendingViewHolder> {

    public interface OnPendingActionListener {
        void onCancelTrade(@NonNull ShiftItem item, int position);
        void onAcceptTrade(@NonNull ShiftItem item, int position);
    }

    private final List<ShiftItem> items;
    private final boolean showCancelButton;
    private final boolean showAcceptButton;
    private final OnPendingActionListener listener;

    public PendingTradesAdapter(@NonNull List<ShiftItem> items,
                                boolean showCancelButton,
                                boolean showAcceptButton,
                                OnPendingActionListener listener) {
        this.items = items;
        this.showCancelButton = showCancelButton;
        this.showAcceptButton = showAcceptButton;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_trade, parent, false);
        return new PendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingViewHolder holder, int position) {
        ShiftItem item = items.get(position);

        holder.textDate.setText(item.getDate());

        String timeText;
        if (item.getStartTime() != null && !item.getStartTime().isEmpty()) {
            timeText = item.getStartTime() + " - " + item.getEndTime();
        } else {
            timeText = "All Day / Blocked";
        }
        holder.textTime.setText(timeText);

        holder.textRole.setText(item.getRole() != null ? item.getRole() : "");

        String status = item.getStatus() != null ? item.getStatus() : "";
        holder.textStatus.setText(status.isEmpty() ? "Pending" : status);

        // Cancel button
        if (showCancelButton) {
            holder.buttonCancel.setVisibility(View.VISIBLE);
            holder.buttonCancel.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onCancelTrade(items.get(pos), pos);
                    }
                }
            });
        } else {
            holder.buttonCancel.setVisibility(View.GONE);
            holder.buttonCancel.setOnClickListener(null);
        }

        // Accept button
        if (showAcceptButton) {
            holder.buttonAccept.setVisibility(View.VISIBLE);
            holder.buttonAccept.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onAcceptTrade(items.get(pos), pos);
                    }
                }
            });
        } else {
            holder.buttonAccept.setVisibility(View.GONE);
            holder.buttonAccept.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class PendingViewHolder extends RecyclerView.ViewHolder {

        TextView textDate, textTime, textRole, textStatus;
        Button buttonCancel, buttonAccept;

        public PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textPendingDate);
            textTime = itemView.findViewById(R.id.textPendingTime);
            textRole = itemView.findViewById(R.id.textPendingRole);
            textStatus = itemView.findViewById(R.id.textPendingStatus);
            buttonCancel = itemView.findViewById(R.id.buttonCancelRequest);
            buttonAccept = itemView.findViewById(R.id.buttonAcceptTrade);
        }
    }
}
