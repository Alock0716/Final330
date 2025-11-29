package com.example.shiftscheduler;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.adapters.EmployerEmployeeAdapter;
import com.example.shiftscheduler.adapters.EmployerRequestAdapter;
import com.example.shiftscheduler.model.Employee;
import com.example.shiftscheduler.model.ShiftRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployerApprovalsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewEmployees;
    private RecyclerView recyclerViewRequests;
    private TextView textSelectedEmployee;
    private TextView textNoRequests;

    private EmployerEmployeeAdapter employeeAdapter;
    private EmployerRequestAdapter requestAdapter;

    private final List<Employee> employeeList = new ArrayList<>();
    private final List<ShiftRequest> requestList = new ArrayList<>();

    private final Map<String, Integer> pendingCounts = new HashMap<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String currentTeamId;
    private Employee selectedEmployee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_approvals);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerViewEmployees = findViewById(R.id.recyclerViewEmployees);
        recyclerViewRequests = findViewById(R.id.recyclerViewRequests);
        textSelectedEmployee = findViewById(R.id.textSelectedEmployee);
        textNoRequests = findViewById(R.id.textNoRequests);

        // Employees stacked vertically
        recyclerViewEmployees.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        );

        // Requests list vertical
        recyclerViewRequests.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        );

        employeeAdapter = new EmployerEmployeeAdapter(
                employeeList,
                (employee, position) -> {
                    selectedEmployee = employee;
                    textSelectedEmployee.setText("Pending requests for: " + employee.getName());
                    loadRequestsForEmployee(employee.getUid());
                },
                pendingCounts
        );

        requestAdapter = new EmployerRequestAdapter(
                requestList,
                (request, position) -> updateRequestStatus(request, "approved"),
                (request, position) -> updateRequestStatus(request, "denied")
        );

        recyclerViewEmployees.setAdapter(employeeAdapter);
        recyclerViewRequests.setAdapter(requestAdapter);

        loadEmployerTeam();
    }

    private void loadEmployerTeam() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Employer record not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentTeamId = snapshot.getString("teamId");
                    if (currentTeamId == null) {
                        Toast.makeText(this, "No team assigned to employer.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    loadEmployees();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load employer data.", Toast.LENGTH_SHORT).show());
    }

    private void loadEmployees() {
        db.collection("users")
                .whereEqualTo("teamId", currentTeamId)
                .whereEqualTo("role", "employee")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to load employees.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    employeeList.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Log.d("EMP_DEBUG", "USER DOC: " + doc.getId());
                            Log.d("EMP_DEBUG", "teamId: '" + doc.getString("teamId") + "'");
                            Log.d("EMP_DEBUG", "role:   '" + doc.getString("role") + "'");
                            Log.d("EMP_DEBUG", "name:   '" + doc.getString("name") + "'");

                            Employee emp = doc.toObject(Employee.class);
                            emp.setUid(doc.getId());
                            employeeList.add(emp);
                        }
                    }

                    employeeAdapter.notifyDataSetChanged();
                    fetchPendingCountsForTeam();
                });
    }

    private void fetchPendingCountsForTeam() {
        // Reset counts for current employees
        pendingCounts.clear();
        for (Employee emp : employeeList) {
            if (emp.getUid() != null) {
                pendingCounts.put(emp.getUid(), 0);
            }
        }

        if (pendingCounts.isEmpty()) {
            employeeAdapter.setPendingCounts(pendingCounts);
            return;
        }

        // 1) Pending time-off requests
        db.collection("timeOffRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {
                            String empId = doc.getString("employeeId");
                            if (empId != null && pendingCounts.containsKey(empId)) {
                                int current = pendingCounts.get(empId);
                                pendingCounts.put(empId, current + 1);
                            }
                        }
                    }
                    employeeAdapter.setPendingCounts(new HashMap<>(pendingCounts));
                });

        // 2) Pending shift-change calloffs
        db.collection("shiftChangeRequests")
                .whereEqualTo("status", "pending")
                .whereEqualTo("type", "calloff")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {
                            String empId = doc.getString("employeeId");
                            if (empId != null && pendingCounts.containsKey(empId)) {
                                int current = pendingCounts.get(empId);
                                pendingCounts.put(empId, current + 1);
                            }
                        }
                    }
                    employeeAdapter.setPendingCounts(new HashMap<>(pendingCounts));
                });
    }

    private void loadRequestsForEmployee(String employeeId) {
        requestList.clear();
        requestAdapter.notifyDataSetChanged();
        textNoRequests.setVisibility(View.GONE);

        // 1) timeOffRequests
        db.collection("timeOffRequests")
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {
                            ShiftRequest req = doc.toObject(ShiftRequest.class);
                            if (req != null) {
                                req.setRequestId(doc.getId());
                                req.setType("time off");
                                requestList.add(req);
                            }
                        }
                    }
                    requestAdapter.notifyDataSetChanged();
                    textNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                });

        // 2) shiftChangeRequests (calloffs)
        db.collection("shiftChangeRequests")
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("status", "pending")
                .whereEqualTo("type", "calloff")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {
                            ShiftRequest req = doc.toObject(ShiftRequest.class);
                            if (req != null) {
                                req.setRequestId(doc.getId());
                                requestList.add(req);
                            }
                        }
                    }
                    requestAdapter.notifyDataSetChanged();
                    textNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void updateRequestStatus(ShiftRequest request, String status) {
        if (request.getRequestId() == null) {
            Toast.makeText(this, "Missing request ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String collection = "shiftChangeRequests";
        if ("time off".equals(request.getType())) {
            collection = "timeOffRequests";
        }

        db.collection(collection)
                .document(request.getRequestId())
                .update("status", status)
                .addOnSuccessListener(aVoid -> {

                    if ("denied".equals(status)) {
                        // restore shift if this was a calloff
                        handleDeniedRequest(request);
                    } else if ("approved".equals(status)) {
                        // NEW: create a notification for the employee
                        createApprovalNotification(request);
                    }

                    // refresh counts + list for selected employee
                    fetchPendingCountsForTeam();
                    if (selectedEmployee != null && selectedEmployee.getUid() != null) {
                        loadRequestsForEmployee(selectedEmployee.getUid());
                    }

                    Toast.makeText(this, "Request " + status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update.", Toast.LENGTH_SHORT).show());
    }
    private void createApprovalNotification(ShiftRequest request) {
        String employeeId = request.getEmployeeId();
        if (employeeId == null || employeeId.isEmpty()) {
            Log.w("NOTIFY", "No employeeId on request, cannot create notification.");
            return;
        }

        // Build a human-readable message
        String title;
        String message;

        if ("time off".equals(request.getType())) {
            title = "Time Off Approved";
            message = "Your time off request for " + request.getShiftDate() + " was approved.";
        } else if ("calloff".equals(request.getType())) {
            title = "Call-Off Approved";
            if (request.getStartTime() != null && request.getEndTime() != null) {
                message = "Your call-off for " + request.getShiftDate()
                        + " (" + request.getStartTime() + " - " + request.getEndTime()
                        + ") was approved.";
            } else {
                message = "Your call-off request was approved.";
            }
        } else {
            title = "Request Approved";
            message = "Your request was approved.";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", employeeId);
        data.put("requestId", request.getRequestId());
        data.put("shiftId", request.getShiftId());
        data.put("type", "approval");
        data.put("requestType", request.getType());   // "time off" or "calloff"
        data.put("title", title);
        data.put("message", message);
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        data.put("read", false);

        db.collection("notifications")
                .add(data)
                .addOnSuccessListener(docRef ->
                        Log.d("NOTIFY", "Notification created: " + docRef.getId()))
                .addOnFailureListener(e ->
                        Log.w("NOTIFY", "Failed to create notification", e));
    }

    private void handleDeniedRequest(ShiftRequest request) {
        if (!"calloff".equals(request.getType())) {
            // Only shiftChangeRequests (calloffs) have a shift to restore
            return;
        }

        String shiftId = request.getShiftId();
        if (shiftId == null || shiftId.isEmpty()) {
            Toast.makeText(this, "Shift ID missing — cannot restore shift.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("shifts")
                .document(shiftId)
                .update("status", "scheduled")
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Shift restored to schedule.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to restore shift.", Toast.LENGTH_SHORT).show());
    }
}
