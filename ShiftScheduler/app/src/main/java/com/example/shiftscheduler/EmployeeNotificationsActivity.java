package com.example.shiftscheduler;

import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftscheduler.adapters.NotificationsAdapter;
import com.example.shiftscheduler.models.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class EmployeeNotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewNotifications;
    private NotificationsAdapter adapter;
    private final List<NotificationItem> notificationList = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_notifications);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        recyclerViewNotifications.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        adapter = new NotificationsAdapter(notificationList);
        recyclerViewNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("notifications")
                .whereEqualTo("employeeId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("NOTIFY_DEBUG", "Error loading notifications", e);
                        Toast.makeText(this, "Failed to load notifications.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationList.clear();
                    if (snapshots != null) {

                        List<DocumentReference> toMarkRead = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : snapshots) {
                            NotificationItem item = doc.toObject(NotificationItem.class);
                            item.setId(doc.getId());
                            notificationList.add(item);

                            Boolean read = doc.getBoolean("read");
                            if (read == null || !read) {
                                toMarkRead.add(doc.getReference());
                            }
                        }

                        adapter.notifyDataSetChanged();

                        // Mark all displayed unread notifications as read = true
                        if (!toMarkRead.isEmpty()) {
                            WriteBatch batch = db.batch();
                            for (DocumentReference ref : toMarkRead) {
                                batch.update(ref, "read", true);
                            }
                            batch.commit()
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("NOTIFY_DEBUG", "Marked " + toMarkRead.size() + " notifications as read."))
                                    .addOnFailureListener(ex ->
                                            Log.w("NOTIFY_DEBUG", "Failed to mark notifications as read", ex));
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
