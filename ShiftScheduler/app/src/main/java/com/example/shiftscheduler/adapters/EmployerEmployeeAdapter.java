package com.example.shiftscheduler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.R;
import com.example.shiftscheduler.model.Employee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployerEmployeeAdapter extends RecyclerView.Adapter<EmployerEmployeeAdapter.EmployeeViewHolder> {

    public interface OnEmployeeClickListener {
        void onEmployeeClick(@NonNull Employee employee, int position);
    }

    private List<Employee> employeeList;
    private OnEmployeeClickListener listener;

    // NEW: map of employeeId -> pending request count
    private Map<String, Integer> pendingCounts = new HashMap<>();

    public EmployerEmployeeAdapter(List<Employee> employeeList,
                                   OnEmployeeClickListener listener,
                                   Map<String, Integer> pendingCounts) {
        this.employeeList = employeeList;
        this.listener = listener;
        if (pendingCounts != null) {
            this.pendingCounts = pendingCounts;
        }
    }

    // Allow updating the counts from the activity
    public void setPendingCounts(Map<String, Integer> counts) {
        this.pendingCounts = counts != null ? counts : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_approval, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeList.get(position);

        holder.textEmployeeName.setText(employee.getName());

        // Look up pending count by employee UID
        int count = 0;
        if (pendingCounts != null && employee.getUid() != null) {
            Integer value = pendingCounts.get(employee.getUid());
            if (value != null) {
                count = value;
            }
        }

        holder.textEmployeeRequestsCount.setText("Pending requests: " + count);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeClick(employee, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList != null ? employeeList.size() : 0;
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView textEmployeeName;
        TextView textEmployeeRequestsCount;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            textEmployeeName = itemView.findViewById(R.id.textEmployeeName);
            textEmployeeRequestsCount = itemView.findViewById(R.id.textEmployeeRequestsCount);
        }
    }
}
