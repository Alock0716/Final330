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

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {

    // Listener interface for shift item actions
    public interface OnShiftActionListener {
        void onTradeClick(@NonNull ShiftItem shift);
        void onCallOffClick(@NonNull ShiftItem shift);
        void onCancelClick(@NonNull ShiftItem shift);   // Used only in MONTH mode
    }

    private final List<ShiftItem> shiftList;
    private final OnShiftActionListener listener;

    // Month mode flag
    private boolean isMonthMode = false;

    public ShiftAdapter(List<ShiftItem> shiftList, OnShiftActionListener listener) {
        this.shiftList = shiftList;
        this.listener = listener;
    }

    // Allows EmployerScheduleActivity to enable/disable month mode dynamically
    public void setMonthMode(boolean monthMode) {
        this.isMonthMode = monthMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift, parent, false);
        return new ShiftViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {

        ShiftItem shift = shiftList.get(position);

        // Basic Fields
        holder.textShiftDate.setText(shift.getDate());
        holder.textShiftTime.setText(shift.getStartTime() + " - " + shift.getEndTime());
        holder.textShiftRole.setText(shift.getRole());

        String status = shift.getStatus() != null ? shift.getStatus() : "scheduled";
        holder.textShiftStatus.setText(status);

        // Color coding for status label
        switch (status.toLowerCase()) {
            case "pending":
                holder.textShiftStatus.setTextColor(0xFFE67E22); // Orange
                break;
            case "cancelled":
                holder.textShiftStatus.setTextColor(0xFFC62828); // Red
                break;
            default:
            case "scheduled":
                holder.textShiftStatus.setTextColor(0xFF2E7D32); // Green
                break;
        }

        // ========== MONTH VIEW MODE ==========
        if (isMonthMode) {

            holder.buttonTrade.setVisibility(View.GONE);
            holder.buttonCallOff.setVisibility(View.GONE);

            // Show Cancel Button
            holder.buttonCancel.setVisibility(View.VISIBLE);
            holder.buttonCancel.setOnClickListener(v ->
                    listener.onCancelClick(shift)
            );

        } else {

            // ========== DAY / WEEK VIEW ==========
            holder.buttonCancel.setVisibility(View.GONE);

            // Show standard buttons
            holder.buttonTrade.setVisibility(View.VISIBLE);
            holder.buttonCallOff.setVisibility(View.VISIBLE);

            holder.buttonTrade.setOnClickListener(v ->
                    listener.onTradeClick(shift)
            );

            holder.buttonCallOff.setOnClickListener(v ->
                    listener.onCallOffClick(shift)
            );
        }
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    // ------------------ ViewHolder ------------------
    public static class ShiftViewHolder extends RecyclerView.ViewHolder {

        TextView textShiftDate, textShiftTime, textShiftRole, textShiftStatus;
        Button buttonTrade, buttonCallOff, buttonCancel;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);

            textShiftDate = itemView.findViewById(R.id.textShiftDate);
            textShiftTime = itemView.findViewById(R.id.textShiftTime);
            textShiftRole = itemView.findViewById(R.id.textShiftRole);
            textShiftStatus = itemView.findViewById(R.id.textShiftStatus);

            buttonTrade = itemView.findViewById(R.id.buttonTrade);
            buttonCallOff = itemView.findViewById(R.id.buttonCallOff);
            buttonCancel = itemView.findViewById(R.id.buttonCancel);
        }
    }
}
