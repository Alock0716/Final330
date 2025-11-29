package com.example.shiftscheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import com.example.shiftscheduler.model.Employee;
import com.example.shiftscheduler.R;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    public interface OnEmployeeActionListener {
        void onToggleActive(@NonNull Employee employee, int position, boolean isActive);
        void onToggleManager(@NonNull Employee employee, int position);
    }

    private List<Employee> employeeList;
    private OnEmployeeActionListener listener;

    public EmployeeAdapter(List<Employee> employeeList, OnEmployeeActionListener listener) {
        this.employeeList = employeeList;
        this.listener = listener;
    }

    public void updateData(List<Employee> newList) {
        this.employeeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeList.get(position);

        holder.textEmployeeName.setText(employee.getName());
        holder.textEmployeeEmail.setText(employee.getEmail());
        holder.textEmployeeRole.setText("Role: " + employee.getRole());

        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(employee.isActive());

        // Active toggle
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleActive(employee, holder.getBindingAdapterPosition(), isChecked);
            }
        });

        // Manager toggle button
        String role = employee.getRole();
        if ("manager".equalsIgnoreCase(role)) {
            holder.buttonToggleManager.setText("Make Employee");
        } else {
            holder.buttonToggleManager.setText("Make Manager");
        }

        holder.buttonToggleManager.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleManager(employee, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList != null ? employeeList.size() : 0;
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView textEmployeeName;
        TextView textEmployeeEmail;
        TextView textEmployeeRole;
        Switch switchActive;
        Button buttonToggleManager;

        EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            textEmployeeName = itemView.findViewById(R.id.textEmployeeName);
            textEmployeeEmail = itemView.findViewById(R.id.textEmployeeEmail);
            textEmployeeRole = itemView.findViewById(R.id.textEmployeeRole);
            switchActive = itemView.findViewById(R.id.switchActive);
            buttonToggleManager = itemView.findViewById(R.id.buttonToggleManager);
        }
    }
}
