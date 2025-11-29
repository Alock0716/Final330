package com.example.shiftscheduler;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.adapters.PendingTradesAdapter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shows:
 *  - My Pending Changes (trade requests, call-offs, time-off)
 *  - Available Shifts (other employees' posted trades)
 */
public class PendingTradesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPendingTrades;
    private PendingTradesAdapter pendingTradesAdapter;
    private final List<ShiftItem> items = new ArrayList<>();

    private MaterialButtonToggleGroup toggleGroupTradeTabs;

    private enum TabMode { PENDING, AVAILABLE }
    private TabMode currentTab = TabMode.PENDING;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_trades);

        recyclerViewPendingTrades = findViewById(R.id.recyclerViewPendingTrades);
        toggleGroupTradeTabs = findViewById(R.id.toggleGroupTradeTabs);

        recyclerViewPendingTrades.setLayoutManager(new LinearLayoutManager(this));

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Default adapter: Pending tab, cancel button visible
        pendingTradesAdapter = new PendingTradesAdapter(
                items,
                true,   // showCancelButton
                false,  // showAcceptButton
                new PendingTradesAdapter.OnPendingActionListener() {
                    @Override
                    public void onCancelTrade(@NonNull ShiftItem item, int position) {
                        cancelRequest(item, position);
                    }

                    @Override
                    public void onAcceptTrade(@NonNull ShiftItem item, int position) {
                        // Not used in pending mode
                    }
                }
        );
        recyclerViewPendingTrades.setAdapter(pendingTradesAdapter);

        // Default tab
        currentTab = TabMode.PENDING;
        toggleGroupTradeTabs.check(R.id.buttonTabPending);
        loadPendingDataFromFirestore();

        toggleGroupTradeTabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.buttonTabPending) {
                currentTab = TabMode.PENDING;
                switchToPendingMode();
            } else if (checkedId == R.id.buttonTabAvailable) {
                currentTab = TabMode.AVAILABLE;
                switchToAvailableMode();
            }
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

    private void switchToPendingMode() {
        pendingTradesAdapter = new PendingTradesAdapter(
                items,
                true,   // showCancelButton
                false,  // showAcceptButton
                new PendingTradesAdapter.OnPendingActionListener() {
                    @Override
                    public void onCancelTrade(@NonNull ShiftItem item, int position) {
                        cancelRequest(item, position);
                    }

                    @Override
                    public void onAcceptTrade(@NonNull ShiftItem item, int position) {
                        // no-op here
                    }
                }
        );
        recyclerViewPendingTrades.setAdapter(pendingTradesAdapter);
        loadPendingDataFromFirestore();
    }

    private void switchToAvailableMode() {
        pendingTradesAdapter = new PendingTradesAdapter(
                items,
                false,  // showCancelButton
                true,   // showAcceptButton
                new PendingTradesAdapter.OnPendingActionListener() {
                    @Override
                    public void onCancelTrade(@NonNull ShiftItem item, int position) {
                        // no-op here
                    }

                    @Override
                    public void onAcceptTrade(@NonNull ShiftItem item, int position) {
                        acceptTrade(item, position);
                    }
                }
        );
        recyclerViewPendingTrades.setAdapter(pendingTradesAdapter);
        loadAvailableDataFromFirestore();
    }

    // ===== Load My Pending Changes =====
    private void loadPendingDataFromFirestore() {
        String uid = getCurrentUserId();
        if (uid == null) return;

        items.clear();
        pendingTradesAdapter.notifyDataSetChanged();

        // 1) Pending trade/calloff requests
        db.collection("shiftChangeRequests")
                .whereEqualTo("employeeId", uid)
                .whereIn("status", Arrays.asList("pending", "submitted"))
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load pending changes.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        ShiftItem item = new ShiftItem();
                        item.setRequestId(doc.getId());
                        item.setType(safeString(doc.getString("type"))); // "trade" or "calloff"
                        item.setDate(safeString(doc.getString("date")));
                        item.setStartTime(safeString(doc.getString("startTime")));
                        item.setEndTime(safeString(doc.getString("endTime")));
                        item.setRole(safeString(doc.getString("position")));
                        item.setStatus(safeString(doc.getString("status")));
                        item.setEmployeeId(uid);
                        item.setShiftId(safeString(doc.getString("shiftId")));
                        items.add(item);
                    }

                    // 2) Pending time-off requests
                    db.collection("timeOffRequests")
                            .whereEqualTo("employeeId", uid)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnCompleteListener(task2 -> {
                                if (!task2.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Failed to load time-off requests.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                for (QueryDocumentSnapshot doc : task2.getResult()) {
                                    ShiftItem item = new ShiftItem();
                                    item.setRequestId(doc.getId());
                                    item.setType("timeoff");
                                    item.setDate(safeString(doc.getString("date")));
                                    item.setStartTime("");
                                    item.setEndTime("");
                                    item.setRole("Unavailable / Time Off");
                                    item.setStatus(safeString(doc.getString("status")));
                                    item.setEmployeeId(uid);
                                    // no shiftId for pure time-off
                                    items.add(item);
                                }

                                pendingTradesAdapter.notifyDataSetChanged();
                            });
                });
    }

    // ===== Load Available Shifts From Other Employees =====
    private void loadAvailableDataFromFirestore() {
        String uid = getCurrentUserId();
        if (uid == null) return;

        items.clear();
        pendingTradesAdapter.notifyDataSetChanged();

        // Show trade requests from other employees that are still pending/available
        db.collection("shiftChangeRequests")
                .whereEqualTo("type", "trade")
                .whereIn("status", Arrays.asList("pending", "available_for_trade"))
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to load available shifts.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String ownerId = safeString(doc.getString("employeeId"));
                        if (ownerId.equals(uid)) {
                            // Don't show own trades here
                            continue;
                        }

                        ShiftItem item = new ShiftItem();
                        item.setRequestId(doc.getId());
                        item.setType("trade");
                        item.setEmployeeId(ownerId);
                        item.setShiftId(safeString(doc.getString("shiftId")));
                        item.setDate(safeString(doc.getString("date")));
                        item.setStartTime(safeString(doc.getString("startTime")));
                        item.setEndTime(safeString(doc.getString("endTime")));
                        item.setRole(safeString(doc.getString("position")));
                        item.setStatus(safeString(doc.getString("status")));

                        items.add(item);
                    }

                    pendingTradesAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Cancel a pending request in Firestore and remove it from the list.
     */
    private void cancelRequest(ShiftItem item, int position) {
        String type = item.getType();
        String requestId = item.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(this,
                    "Missing request ID.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String collection;
        if ("timeoff".equals(type)) {
            collection = "timeOffRequests";
        } else {
            collection = "shiftChangeRequests";
        }

        db.collection(collection)
                .document(requestId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to cancel request.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    items.remove(position);
                    pendingTradesAdapter.notifyItemRemoved(position);
                    Toast.makeText(this,
                            "Cancelled request for " + item.getDate(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Accept a trade: assign the underlying shift to the current user
     * and mark the trade request as accepted.
     */
    private void acceptTrade(ShiftItem item, int position) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        String shiftId = item.getShiftId();
        String requestId = item.getRequestId();

        if (shiftId == null || shiftId.isEmpty()) {
            Toast.makeText(this,
                    "Missing shift ID, cannot accept trade.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(this,
                    "Missing request ID, cannot accept trade.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // 1) Update the shift to belong to the new employee
        db.collection("shifts")
                .document(shiftId)
                .update(
                        "employeeId", uid,
                        "status", "scheduled"
                )
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Failed to assign shift.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 2) Mark the trade request as accepted
                    db.collection("shiftChangeRequests")
                            .document(requestId)
                            .update("status", "accepted")
                            .addOnCompleteListener(task2 -> {
                                if (!task2.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Shift updated, but failed to update request status.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                items.remove(position);
                                pendingTradesAdapter.notifyItemRemoved(position);

                                Toast.makeText(this,
                                        "Trade accepted for " + item.getDate(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private String safeString(String v) {
        return v == null ? "" : v;
    }
}
