package com.example.shiftscheduler;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.adapters.ShiftAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Employee home screen where a logged-in employee
 * can see upcoming shifts, view pending changes,
 * request future days off / unavailable dates,
 * and view notifications about approved requests.
 */
public class EmployeeHomeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewShifts;
    private ShiftAdapter shiftAdapter;
    private final List<ShiftItem> shiftItems = new ArrayList<>();

    private MaterialButton buttonPendingChanges;
    private MaterialButton buttonRequestTimeOff;
    private MaterialButton buttonNotifications;   // NEW: open notification history

    private final Calendar calendar = Calendar.getInstance();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        recyclerViewShifts = findViewById(R.id.recyclerViewShifts);
        buttonPendingChanges = findViewById(R.id.buttonPendingChanges);
        buttonRequestTimeOff = findViewById(R.id.buttonRequestTimeOff);
        buttonNotifications = findViewById(R.id.buttonNotifications); // make sure this exists in XML

        // Layout manager for vertical list
        recyclerViewShifts.setLayoutManager(new LinearLayoutManager(this));

        // Create adapter with listener for per-shift actions
        shiftAdapter = new ShiftAdapter(shiftItems, new ShiftAdapter.OnShiftActionListener() {
            @Override
            public void onTradeClick(@NonNull ShiftItem shift) {
                sendTradeOrCallOffRequest(shift, "trade");
            }

            @Override
            public void onCancelClick(@NonNull ShiftItem shift) {
                // Not used for employees
            }

            @Override
            public void onCallOffClick(@NonNull ShiftItem shift) {
                sendTradeOrCallOffRequest(shift, "calloff");
            }
        });

        recyclerViewShifts.setAdapter(shiftAdapter);

        // Load real shifts from Firestore
        loadUpcomingShiftsFromFirestore();

        // Pending schedule changes button
        buttonPendingChanges.setOnClickListener(v -> {
            startActivity(new android.content.Intent(
                    EmployeeHomeActivity.this,
                    PendingTradesActivity.class
            ));
        });

        // Global time-off / unavailability button
        buttonRequestTimeOff.setOnClickListener(v -> showTimeOffDatePicker());

        // NEW: Notifications / Updates button
        buttonNotifications.setOnClickListener(v -> {
            startActivity(new android.content.Intent(
                    EmployeeHomeActivity.this,
                    EmployeeNotificationsActivity.class
            ));
        });
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "Not logged in.",
                    Toast.LENGTH_LONG).show();
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    /**
     * Load all upcoming shifts for this employee from /shifts.
     */
    private void loadUpcomingShiftsFromFirestore() {
        String uid = getCurrentUserId();
        if (uid == null) return;

        shiftItems.clear();
        shiftAdapter.notifyDataSetChanged();

        // For now: all shifts with employeeId == uid.
        // You can add date filters later.
        db.collection("shifts")
                .whereEqualTo("employeeId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load shifts.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        ShiftItem item = new ShiftItem();
                        item.setShiftId(doc.getId());
                        item.setEmployeeId(uid);
                        item.setDate(safeString(doc.getString("date")));
                        item.setStartTime(safeString(doc.getString("startTime")));
                        item.setEndTime(safeString(doc.getString("endTime")));
                        item.setRole(safeString(doc.getString("position")));
                        item.setStatus(safeString(doc.getString("status")));
                        // You could resolve employeeName from a users collection if needed
                        shiftItems.add(item);
                    }

                    shiftAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Creates a trade or call-off request document in /shiftChangeRequests.
     */
    private void sendTradeOrCallOffRequest(@NonNull ShiftItem shift, @NonNull String type) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("type", type); // "trade" or "calloff"
        data.put("employeeId", uid);
        data.put("shiftId", shift.getShiftId());
        data.put("date", shift.getDate());
        data.put("startTime", shift.getStartTime());
        data.put("endTime", shift.getEndTime());
        data.put("position", shift.getRole());
        data.put("status", "pending");
        data.put("createdAt", Timestamp.now());

        db.collection("shiftChangeRequests")
                .add(data)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to submit " + type + " request.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this,
                            "Submitted " + type + " request for " + shift.getDate(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Opens a DatePicker for the employee to select a day in the future
     * when they are not available (vacation, appointment, etc.),
     * then writes a document to /timeOffRequests.
     */
    private void showTimeOffDatePicker() {
        final Calendar today = Calendar.getInstance();

        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(selectedYear, selectedMonth, selectedDayOfMonth, 0, 0, 0);

                    if (!selected.after(today)) {
                        Toast.makeText(this,
                                "Please select a future date for time off.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String dateStr = String.format(
                            "%04d-%02d-%02d",
                            selectedYear,
                            (selectedMonth + 1),
                            selectedDayOfMonth
                    );

                    sendTimeOffRequest(dateStr);
                },
                year, month, day
        );

        // Prevent picking past dates
        datePickerDialog.getDatePicker().setMinDate(today.getTimeInMillis());
        datePickerDialog.show();
    }

    /**
     * Writes a /timeOffRequests document.
     */
    private void sendTimeOffRequest(String dateStr) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", uid);
        data.put("date", dateStr);
        data.put("reason", "Time off"); // you can extend with a dialog later
        data.put("status", "pending");
        data.put("createdAt", Timestamp.now());

        db.collection("timeOffRequests")
                .add(data)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to request time off.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this,
                            "Requested time off for " + dateStr,
                            Toast.LENGTH_LONG).show();
                });
    }

    @NonNull
    private String safeString(String v) {
        return v == null ? "" : v;
    }
}
