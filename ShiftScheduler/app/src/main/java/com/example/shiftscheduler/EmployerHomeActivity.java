package com.example.shiftscheduler;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

public class EmployerHomeActivity extends AppCompatActivity {

    private MaterialButton buttonManageSchedule;
    private MaterialButton buttonReviewRequests;

    private MaterialButton buttonApprovals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_home);

        // Match these IDs to your activity_employer_home.xml
        buttonManageSchedule = findViewById(R.id.buttonManageSchedule);
        buttonReviewRequests = findViewById(R.id.buttonReviewRequests);


        // Open the new calendar-based schedule screen
        buttonManageSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(
                    EmployerHomeActivity.this,
                    EmployerScheduleActivity.class
            );
            startActivity(intent);
        });

        // Placeholders for future screens
        buttonReviewRequests.setOnClickListener(v -> {
            Intent intent = new Intent(
                    EmployerHomeActivity.this,
                    EmployerApprovalsActivity.class
            );
            startActivity(intent);
        });

    }
}
