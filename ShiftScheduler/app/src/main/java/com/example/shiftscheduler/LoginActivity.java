package com.example.shiftscheduler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private MaterialButton buttonLogin;
    private MaterialButton buttonRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Link views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);

        // Login click
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Go to Register screen
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = "";
        String password = "";

        if (editTextEmail.getText() != null) {
            email = editTextEmail.getText().toString().trim();
        }
        if (editTextPassword.getText() != null) {
            password = editTextPassword.getText().toString().trim();
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        // Optional: enforce 6+ chars
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        buttonLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                buttonLogin.setEnabled(true);

                                if (!task.isSuccessful()) {
                                    String message = "Login failed";
                                    if (task.getException() != null) {
                                        message = "Login failed: "
                                                + task.getException().getMessage();
                                    }
                                    Toast.makeText(LoginActivity.this,
                                            message,
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                if (mAuth.getCurrentUser() == null) {
                                    Toast.makeText(LoginActivity.this,
                                            "Login successful, but no user info found.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                String uid = mAuth.getCurrentUser().getUid();

                                // Look up user profile to get role
                                db.collection("users")
                                        .document(uid)
                                        .get()
                                        .addOnCompleteListener(task1 -> {
                                            if (!task1.isSuccessful()) {
                                                Toast.makeText(LoginActivity.this,
                                                        "Login ok, but failed to load profile.",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            DocumentSnapshot doc = task1.getResult();
                                            if (doc == null || !doc.exists()) {
                                                Toast.makeText(LoginActivity.this,
                                                        "Login ok, but user profile not found.",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            String role = doc.getString("role");
                                            if (role == null) {
                                                role = "employee";
                                            }

                                            Toast.makeText(LoginActivity.this,
                                                    "Login successful",
                                                    Toast.LENGTH_SHORT).show();

                                            Intent intent;
                                            if ("employer".equals(role)) {
                                                intent = new Intent(LoginActivity.this,
                                                        EmployerHomeActivity.class);
                                            } else {
                                                intent = new Intent(LoginActivity.this,
                                                        EmployeeHomeActivity.class);
                                            }

                                            startActivity(intent);
                                            finish();
                                        });
                            }
                        });
    }
}
