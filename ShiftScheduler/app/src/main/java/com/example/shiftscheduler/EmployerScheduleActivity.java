package com.example.shiftscheduler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shiftscheduler.adapters.ShiftAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployerScheduleActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup toggleGroupViewMode;
    private MaterialButton buttonModeMonth, buttonModeWeek, buttonModeDay;
    private CalendarView calendarView;
    private TextView textSelectedDate, textViewModeHeader;
    private RecyclerView recyclerViewShifts;
    private ProgressBar progressBarSchedule;
    private FloatingActionButton fabAddShift;

    // Visual schedule
    private ScrollView scrollVisualSchedule;
    private LinearLayout visualScheduleContainer;

    private FirebaseFirestore db;

    private ShiftAdapter shiftAdapter;
    private final List<ShiftItem> shiftItems = new ArrayList<>();

    // Employee dropdown data
    private final List<String> employeeNames = new ArrayList<>();
    private final List<String> employeeIds = new ArrayList<>();
    private final Map<String, String> employeeIdToName = new LinkedHashMap<>();

    // Employees unavailable for selected date (time off)
    private final List<String> blockedEmployeeIds = new ArrayList<>();

    // Color map for visual schedule
    private final Map<String, Integer> employeeColorMap = new HashMap<>();

    private final Calendar selectedCal = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm", Locale.US);
    private final SimpleDateFormat timeFormat12 = new SimpleDateFormat("h:mm a", Locale.US);

    private enum ViewMode { MONTH, WEEK, DAY }
    private ViewMode currentMode = ViewMode.DAY;

    private void cancelShift(ShiftItem shift) {
        if (shift.getShiftId() == null || shift.getShiftId().isEmpty()) {
            Toast.makeText(this, "Shift ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("shifts")
                .document(shift.getShiftId())
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Shift cancelled", Toast.LENGTH_SHORT).show();
                    loadShiftsForCurrentMode();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel shift", Toast.LENGTH_SHORT).show());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_schedule);

        db = FirebaseFirestore.getInstance();

        toggleGroupViewMode = findViewById(R.id.toggleGroupViewMode);
        buttonModeMonth = findViewById(R.id.buttonModeMonth);
        buttonModeWeek = findViewById(R.id.buttonModeWeek);
        buttonModeDay = findViewById(R.id.buttonModeDay);
        calendarView = findViewById(R.id.calendarView);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textViewModeHeader = findViewById(R.id.textViewModeHeader);
        recyclerViewShifts = findViewById(R.id.recyclerViewShifts);
        progressBarSchedule = findViewById(R.id.progressBarSchedule);
        fabAddShift = findViewById(R.id.fabAddShift);

        scrollVisualSchedule = findViewById(R.id.scrollVisualSchedule);
        visualScheduleContainer = findViewById(R.id.visualScheduleContainer);

        recyclerViewShifts.setLayoutManager(new LinearLayoutManager(this));
        shiftAdapter = new ShiftAdapter(shiftItems, new ShiftAdapter.OnShiftActionListener() {
            @Override
            public void onTradeClick(@NonNull ShiftItem shift) {
                // Employers don't trade here
            }

            @Override
            public void onCallOffClick(@NonNull ShiftItem shift) {
                // Employers don't call off from this screen
            }

            @Override
            public void onCancelClick(@NonNull ShiftItem shift) {
                // ⭐ Handle Month-Mode Cancel Here
                cancelShift(shift);
            }
        });




        recyclerViewShifts.setAdapter(shiftAdapter);

        // Default selected date is today
        selectedCal.setTimeInMillis(calendarView.getDate());
        updateSelectedDateLabel();

        // Calendar change
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCal.set(Calendar.YEAR, year);
            selectedCal.set(Calendar.MONTH, month);
            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateSelectedDateLabel();
            loadTimeOffForSelectedDate();   // refresh blocked employees
            loadShiftsForCurrentMode();     // refresh schedule
        });

        // Default mode = DAY
        currentMode = ViewMode.DAY;
        toggleGroupViewMode.check(R.id.buttonModeDay);
        updateModeHeader();

        toggleGroupViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.buttonModeMonth) {
                currentMode = ViewMode.MONTH;
            } else if (checkedId == R.id.buttonModeWeek) {
                currentMode = ViewMode.WEEK;
            } else {
                currentMode = ViewMode.DAY;
            }

            updateModeHeader();
            loadShiftsForCurrentMode();
        });

        fabAddShift.setOnClickListener(v -> showAddShiftDialog());

        // Load employees & time off
        loadEmployeesForDropdown();
        loadTimeOffForSelectedDate();
        loadShiftsForCurrentMode();
    }

    // ============================================================
    // TIME-OFF LOOKUP FOR SELECTED DATE (timeOffRequests only)
    // ============================================================
    private void loadTimeOffForSelectedDate() {
        blockedEmployeeIds.clear();
        String selectedDate = dateFormat.format(selectedCal.getTime());

        db.collection("timeOffRequests")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap) {
                        String empId = doc.getString("employeeId");
                        String shiftDate = doc.getString("shiftDate");
                        String dateField = doc.getString("date");

                        // Accept either "shiftDate" or "date" as the date field
                        boolean matchesDate = selectedDate.equals(shiftDate) || selectedDate.equals(dateField);
                        if (matchesDate && empId != null) {
                            blockedEmployeeIds.add(empId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // If this fails, we just don't block anyone for this date (better than crashing)
                });
    }

    // ============================================================
    // LABELS & MODE
    // ============================================================
    private void updateSelectedDateLabel() {
        String dateStr = dateFormat.format(selectedCal.getTime());
        textSelectedDate.setText("Schedule for: " + dateStr);
    }

    private void updateModeHeader() {
        if (currentMode == ViewMode.DAY) {
            String dateStr = dateFormat.format(selectedCal.getTime());
            textViewModeHeader.setText("Day view – " + dateStr);
        } else if (currentMode == ViewMode.WEEK) {
            Calendar start = (Calendar) selectedCal.clone();
            int dow = start.get(Calendar.DAY_OF_WEEK); // Sunday = 1
            start.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY));

            Calendar end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_MONTH, 6);

            String startStr = dateFormat.format(start.getTime());
            String endStr = dateFormat.format(end.getTime());
            textViewModeHeader.setText("Week view – " + startStr + " to " + endStr);
        } else {
            Calendar start = (Calendar) selectedCal.clone();
            start.set(Calendar.DAY_OF_MONTH, 1);

            Calendar end = (Calendar) start.clone();
            end.add(Calendar.MONTH, 1);
            end.add(Calendar.DAY_OF_MONTH, -1);

            String startStr = dateFormat.format(start.getTime());
            String endStr = dateFormat.format(end.getTime());
            textViewModeHeader.setText("Month view – " + startStr + " to " + endStr);
        }
    }

    private void loadShiftsForCurrentMode() {
        if (currentMode == ViewMode.DAY) {
            loadShiftsForDay();
            shiftAdapter.setMonthMode(false);
        } else if (currentMode == ViewMode.WEEK) {
            loadShiftsForWeek();
            shiftAdapter.setMonthMode(false);
        } else {
            loadShiftsForMonth();
            shiftAdapter.setMonthMode(true);

        }
    }

    // ============================================================
    // DAY VIEW (now hides non-scheduled shifts)
    // ============================================================
    private void loadShiftsForDay() {
        progressBarSchedule.setVisibility(View.VISIBLE);
        shiftItems.clear();
        shiftAdapter.notifyDataSetChanged();

        String dateStr = dateFormat.format(selectedCal.getTime());

        db.collection("shifts")
                .whereEqualTo("date", dateStr)
                .get()
                .addOnCompleteListener(task -> {
                    progressBarSchedule.setVisibility(View.GONE);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load day shifts",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String status = safeString(doc.getString("status"));
                        // Only show active scheduled shifts
                        if (!status.isEmpty() && !status.equalsIgnoreCase("scheduled")) {
                            continue;
                        }

                        ShiftItem item = new ShiftItem();
                        item.setShiftId(doc.getId());
                        item.setDate(dateStr);
                        item.setStartTime(safeString(doc.getString("startTime")));
                        item.setEndTime(safeString(doc.getString("endTime")));
                        item.setRole(safeString(doc.getString("position")));
                        item.setStatus(status);

                        String employeeId = safeString(doc.getString("employeeId"));
                        item.setEmployeeId(employeeId);
                        item.setEmployeeName(employeeIdToName.get(employeeId));

                        shiftItems.add(item);
                    }

                    // Day view uses visual timeline
                    recyclerViewShifts.setVisibility(View.GONE);
                    scrollVisualSchedule.setVisibility(View.VISIBLE);
                    buildDayTimeline(shiftItems);
                });
    }

    // ============================================================
    // WEEK VIEW (now hides non-scheduled shifts)
    // ============================================================
    private void loadShiftsForWeek() {
        progressBarSchedule.setVisibility(View.VISIBLE);
        shiftItems.clear();
        shiftAdapter.notifyDataSetChanged();

        Calendar start = (Calendar) selectedCal.clone();
        int dayOfWeek = start.get(Calendar.DAY_OF_WEEK); // Sunday = 1
        start.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.SUNDAY));

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        String startStr = dateFormat.format(start.getTime());
        String endStr = dateFormat.format(end.getTime());

        db.collection("shifts")
                .whereGreaterThanOrEqualTo("date", startStr)
                .whereLessThanOrEqualTo("date", endStr)
                .get()
                .addOnCompleteListener(task -> {
                    progressBarSchedule.setVisibility(View.GONE);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load week shifts",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String status = safeString(doc.getString("status"));
                        if (!status.isEmpty() && !status.equalsIgnoreCase("scheduled")) {
                            continue;
                        }

                        ShiftItem item = new ShiftItem();
                        item.setShiftId(doc.getId());
                        item.setDate(safeString(doc.getString("date")));
                        item.setStartTime(safeString(doc.getString("startTime")));
                        item.setEndTime(safeString(doc.getString("endTime")));
                        item.setRole(safeString(doc.getString("position")));
                        item.setStatus(status);

                        String employeeId = safeString(doc.getString("employeeId"));
                        item.setEmployeeId(employeeId);
                        item.setEmployeeName(employeeIdToName.get(employeeId));

                        shiftItems.add(item);
                    }

                    recyclerViewShifts.setVisibility(View.GONE);
                    scrollVisualSchedule.setVisibility(View.VISIBLE);
                    buildWeekTimeline(shiftItems, start);
                });
    }

    // ============================================================
    // MONTH VIEW (now hides non-scheduled shifts)
    // ============================================================
    private void loadShiftsForMonth() {
        progressBarSchedule.setVisibility(View.VISIBLE);
        shiftItems.clear();
        shiftAdapter.notifyDataSetChanged();

        Calendar start = (Calendar) selectedCal.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.DAY_OF_MONTH, -1);

        String startStr = dateFormat.format(start.getTime());
        String endStr = dateFormat.format(end.getTime());

        db.collection("shifts")
                .whereGreaterThanOrEqualTo("date", startStr)
                .whereLessThanOrEqualTo("date", endStr)
                .get()
                .addOnCompleteListener(task -> {
                    progressBarSchedule.setVisibility(View.GONE);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load month shifts",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String status = safeString(doc.getString("status"));
                        if (!status.isEmpty() && !status.equalsIgnoreCase("scheduled")) {
                            continue;
                        }

                        String dateStr = safeString(doc.getString("date"));
                        if (dateStr.isEmpty()) continue;

                        ShiftItem item = new ShiftItem();
                        item.setShiftId(doc.getId());
                        item.setDate(dateStr);

                        String employeeId = safeString(doc.getString("employeeId"));
                        item.setEmployeeId(employeeId);
                        item.setStartTime(safeString(doc.getString("startTime")));
                        item.setEndTime(safeString(doc.getString("endTime")));
                        item.setRole(safeString(doc.getString("position")));
                        item.setStatus(status);
                        item.setEmployeeName(employeeIdToName.get(employeeId));

                        shiftItems.add(item);
                    }

                    scrollVisualSchedule.setVisibility(View.GONE);
                    recyclerViewShifts.setVisibility(View.VISIBLE);
                    shiftAdapter.notifyDataSetChanged();
                });
    }

    // ============================================================
    // EMPLOYEES FOR DROPDOWN
    // ============================================================
    private void loadEmployeesForDropdown() {
        db.collection("users")
                .whereEqualTo("role", "employee")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load employees",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    employeeNames.clear();
                    employeeIds.clear();
                    employeeIdToName.clear();
                    employeeColorMap.clear();

                    int[] palette = {
                            Color.parseColor("#FF8A80"),
                            Color.parseColor("#FFCC80"),
                            Color.parseColor("#FFE082"),
                            Color.parseColor("#C5E1A5"),
                            Color.parseColor("#80DEEA"),
                            Color.parseColor("#B39DDB"),
                            Color.parseColor("#F48FB1"),
                            Color.parseColor("#A5D6A7")
                    };
                    int colorIndex = 0;

                    for (DocumentSnapshot doc : task.getResult()) {
                        String uid = doc.getId();
                        String name = safeString(doc.getString("name"));
                        String email = safeString(doc.getString("email"));

                        if (name.isEmpty()) name = email;
                        if (name.isEmpty()) name = uid;

                        employeeIds.add(uid);
                        employeeNames.add(name);
                        employeeIdToName.put(uid, name);

                        employeeColorMap.put(uid, palette[colorIndex % palette.length]);
                        colorIndex++;
                    }
                });
    }

    // ============================================================
    // ADD SHIFT DIALOG (time-off aware)
    // ============================================================
    private void showAddShiftDialog() {
        if (employeeIds.isEmpty()) {
            Toast.makeText(this,
                    "No employees found. Add employee users first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_shift, null, false);

        TextView textDialogDate = dialogView.findViewById(R.id.textDialogDate);
        Spinner spinnerEmployees = dialogView.findViewById(R.id.spinnerEmployees);
        EditText editTextStartTime = dialogView.findViewById(R.id.editTextStartTime);
        EditText editTextEndTime = dialogView.findViewById(R.id.editTextEndTime);
        EditText editTextPosition = dialogView.findViewById(R.id.editTextPosition);
        EditText editTextStatus = dialogView.findViewById(R.id.editTextStatus);

        String dateStr = dateFormat.format(selectedCal.getTime());
        textDialogDate.setText("Date: " + dateStr);

        // Custom adapter: gray out time-off employees in the dropdown
        ArrayAdapter<String> empAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                employeeNames
        ) {
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v;

                String uid = employeeIds.get(position);
                if (blockedEmployeeIds.contains(uid)) {
                    tv.setTextColor(Color.GRAY);
                    tv.setText(employeeNames.get(position) + " (Time Off)");
                } else {
                    tv.setTextColor(Color.BLACK);
                }

                return v;
            }

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v;

                String uid = employeeIds.get(position);
                if (blockedEmployeeIds.contains(uid)) {
                    tv.setTextColor(Color.GRAY);
                    tv.setText(employeeNames.get(position) + " (Time Off)");
                } else {
                    tv.setTextColor(Color.BLACK);
                }

                return v;
            }
        };
        empAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmployees.setAdapter(empAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Shift")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int selectedIndex = spinnerEmployees.getSelectedItemPosition();
                if (selectedIndex < 0 || selectedIndex >= employeeIds.size()) {
                    Toast.makeText(this,
                            "Select an employee",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String employeeId = employeeIds.get(selectedIndex);

                // Final protection: block employees with time off
                if (blockedEmployeeIds.contains(employeeId)) {
                    Toast.makeText(this,
                            "This employee has approved time off for this date.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String startTime = editTextStartTime.getText().toString().trim();
                String endTime = editTextEndTime.getText().toString().trim();
                String position = editTextPosition.getText().toString().trim();
                String status = editTextStatus.getText().toString().trim();

                if (startTime.isEmpty() || endTime.isEmpty()) {
                    Toast.makeText(this,
                            "Start and end time are required",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (status.isEmpty()) status = "scheduled";

                Map<String, Object> shiftData = new HashMap<>();
                shiftData.put("employeeId", employeeId);
                shiftData.put("teamId", "default_team");
                shiftData.put("date", dateStr);
                shiftData.put("startTime", startTime);
                shiftData.put("endTime", endTime);
                shiftData.put("position", position);
                shiftData.put("status", status);

                progressBarSchedule.setVisibility(View.VISIBLE);

                db.collection("shifts")
                        .add(shiftData)
                        .addOnCompleteListener(task -> {
                            progressBarSchedule.setVisibility(View.GONE);
                            if (!task.isSuccessful()) {
                                Toast.makeText(this,
                                        "Failed to save shift",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            Toast.makeText(this,
                                    "Shift saved",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadShiftsForCurrentMode();
                        });
            });
        });

        dialog.show();
    }

    // ============================================================
    // VISUAL TIMELINE HELPERS (unchanged)
    // ============================================================
    private void buildDayTimeline(List<ShiftItem> shifts) {
        visualScheduleContainer.removeAllViews();

        TextView header = new TextView(this);
        header.setText("Day Schedule");
        header.setTextSize(16f);
        header.setTextColor(Color.DKGRAY);
        header.setPadding(0, 0, 0, 16);
        visualScheduleContainer.addView(header);

        int minutesPerDp = 10; // 10 minutes = 1dp height
        int currentMinute = 0;

        shifts.sort((a, b) ->
                Integer.compare(parseMinutes(a.getStartTime()), parseMinutes(b.getStartTime())));

        for (ShiftItem item : shifts) {
            int start = parseMinutes(item.getStartTime());
            int end = parseMinutes(item.getEndTime());
            if (end <= start) end = start + 60; // minimum 1h if bad data

            int gap = Math.max(0, start - currentMinute);
            if (gap > 0) {
                View gapView = new View(this);
                LinearLayout.LayoutParams gapParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        gap / minutesPerDp
                );
                gapView.setLayoutParams(gapParams);
                visualScheduleContainer.addView(gapView);
            }

            int duration = end - start;
            View block = createShiftBlockView(item, duration / minutesPerDp);
            visualScheduleContainer.addView(block);

            currentMinute = end;
        }
    }

    private void buildWeekTimeline(List<ShiftItem> shifts, Calendar weekStart) {
        visualScheduleContainer.removeAllViews();

        TextView header = new TextView(this);
        header.setText("Week Schedule");
        header.setTextSize(16f);
        header.setTextColor(Color.DKGRAY);
        header.setPadding(0, 0, 0, 16);
        visualScheduleContainer.addView(header);

        Calendar dayCal = (Calendar) weekStart.clone();
        for (int i = 0; i < 7; i++) {
            final String dateStr = dateFormat.format(dayCal.getTime());

            TextView dayLabel = new TextView(this);
            dayLabel.setText(dateStr);
            dayLabel.setTextSize(14f);
            dayLabel.setTextColor(Color.BLACK);
            dayLabel.setPadding(0, 8, 0, 4);
            visualScheduleContainer.addView(dayLabel);

            LinearLayout dayTimeline = new LinearLayout(this);
            dayTimeline.setOrientation(LinearLayout.VERTICAL);
            dayTimeline.setPadding(8, 8, 8, 24);
            dayTimeline.setBackgroundColor(Color.parseColor("#FFF5F5F5"));
            visualScheduleContainer.addView(dayTimeline);

            List<ShiftItem> dayShifts = new ArrayList<>();
            for (ShiftItem item : shifts) {
                if (dateStr.equals(item.getDate())) {
                    dayShifts.add(item);
                }
            }

            if (dayShifts.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No shifts");
                empty.setTextSize(12f);
                empty.setTextColor(Color.GRAY);
                dayTimeline.addView(empty);
            } else {
                int minutesPerDp = 10;
                int currentMinute = 0;

                dayShifts.sort((a, b) ->
                        Integer.compare(parseMinutes(a.getStartTime()), parseMinutes(b.getStartTime())));

                for (ShiftItem item : dayShifts) {
                    int start = parseMinutes(item.getStartTime());
                    int end = parseMinutes(item.getEndTime());
                    if (end <= start) end = start + 60;

                    int gap = Math.max(0, start - currentMinute);
                    if (gap > 0) {
                        View gapView = new View(this);
                        LinearLayout.LayoutParams gapParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                gap / minutesPerDp
                        );
                        gapView.setLayoutParams(gapParams);
                        dayTimeline.addView(gapView);
                    }

                    int duration = end - start;
                    View block = createShiftBlockView(item, duration / minutesPerDp);
                    dayTimeline.addView(block);

                    currentMinute = end;
                }
            }

            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View createShiftBlockView(ShiftItem item, int heightDp) {
        if (heightDp < 40) heightDp = 40; // minimum visible block

        TextView block = new TextView(this);
        String label = (item.getEmployeeName() != null ? item.getEmployeeName() : "Employee")
                + "  " + item.getStartTime() + " - " + item.getEndTime();
        String role = item.getRole();
        if (role != null && !role.isEmpty()) {
            label += "  (" + role + ")";
        }
        block.setText(label);
        block.setTextSize(12f);
        block.setTextColor(Color.BLACK);
        block.setPadding(16, 8, 16, 8);

        String empId = item.getEmployeeId();
        int bgColor = employeeColorMap.containsKey(empId)
                ? employeeColorMap.get(empId)
                : Color.parseColor("#FFCC80");

        block.setBackgroundColor(bgColor);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (heightDp * getResources().getDisplayMetrics().density)
        );
        params.bottomMargin = 8;
        block.setLayoutParams(params);

        return block;
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private int parseMinutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 9 * 60; // default 9:00
        try {
            return (int) (timeFormat24.parse(timeStr).getTime() % (24 * 60 * 60 * 1000)) / (60 * 1000);
        } catch (ParseException ignored) { }
        try {
            return (int) (timeFormat12.parse(timeStr).getTime() % (24 * 60 * 60 * 1000)) / (60 * 1000);
        } catch (ParseException ignored) { }
        return 9 * 60;
    }

    @NonNull
    private String safeString(String v) {
        return v == null ? "" : v;
    }
}
