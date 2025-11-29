package com.example.shiftscheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.R;
import com.example.shiftscheduler.model.ShiftRequest;

import java.util.List;

public class EmployerRequestAdapter extends RecyclerView.Adapter<EmployerRequestAdapter.RequestViewHolder> {

    public interface OnApproveClickListener {
        void onApprove(@NonNull ShiftRequest request, int position);
    }

    public interface OnDenyClickListener {
        void onDeny(@NonNull ShiftRequest request, int position);
    }

    private List<ShiftRequest> requestList;
    private OnApproveClickListener approveListener;
    private OnDenyClickListener denyListener;

    public EmployerRequestAdapter(List<ShiftRequest> requestList,
                                  OnApproveClickListener approveListener,
                                  OnDenyClickListener denyListener) {
        this.requestList = requestList;
        this.approveListener = approveListener;
        this.denyListener = denyListener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        ShiftRequest request = requestList.get(position);

        // TYPE DISPLAY
        holder.textRequestType.setText(request.getType());

        // SHIFT INFO DISPLAY
        String info;
        if (request.getStartTime() != null && request.getEndTime() != null) {
            // Scheduled shift call-off
            info = request.getShiftDate() + " • " +
                    request.getStartTime() + " - " + request.getEndTime();
        } else {
            // Full day time-off request
            info = request.getShiftDate() + " • Full Day";
        }
        holder.textRequestShiftInfo.setText(info);

        // STATUS
        holder.textRequestStatus.setText("Status: " + request.getStatus());

        // BUTTON HANDLERS
        holder.buttonApprove.setOnClickListener(v -> {
            if (approveListener != null) approveListener.onApprove(request, position);
        });

        holder.buttonDeny.setOnClickListener(v -> {
            if (denyListener != null) denyListener.onDeny(request, position);
        });
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView textRequestType, textRequestShiftInfo, textRequestStatus;
        Button buttonApprove, buttonDeny;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textRequestType = itemView.findViewById(R.id.textRequestType);
            textRequestShiftInfo = itemView.findViewById(R.id.textRequestShiftInfo);
            textRequestStatus = itemView.findViewById(R.id.textRequestStatus);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonDeny = itemView.findViewById(R.id.buttonDeny);
        }
    }
}
