package com.example.shiftscheduler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private RadioGroup radioGroupRole;
    private RadioButton radioEmployee;
    private RadioButton radioEmployer;
    private MaterialButton buttonCreateAccount;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioEmployee = findViewById(R.id.radioEmployee);
        radioEmployer = findViewById(R.id.radioEmployer);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        buttonCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAccount();
            }
        });
    }

    private void createAccount() {
        String name = getText(editTextName);
        String email = getText(editTextEmail);
        String password = getText(editTextPassword);
        String confirmPassword = getText(editTextConfirmPassword);

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return;
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
        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            editTextConfirmPassword.requestFocus();
            return;
        }

        final String role;
        int checkedId = radioGroupRole.getCheckedRadioButtonId();
        if (checkedId == R.id.radioEmployer) {
            role = "employer";
        } else {
            role = "employee";
        }

        buttonCreateAccount.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                buttonCreateAccount.setEnabled(true);
                                if (task.isSuccessful()) {
                                    String uid = mAuth.getCurrentUser() != null
                                            ? mAuth.getCurrentUser().getUid()
                                            : null;

                                    if (uid == null) {
                                        Toast.makeText(RegisterActivity.this,
                                                "User created but UID missing",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    // Save user profile in Firestore
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("uid", uid);
                                    userData.put("name", name);
                                    userData.put("email", email);
                                    userData.put("role", role);
                                    userData.put("teamId", "default_team"); // placeholder for now

                                    db.collection("users")
                                            .document(uid)
                                            .set(userData)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Account created!",
                                                            Toast.LENGTH_SHORT).show();
                                                    // After registration, just finish and go back to Login
                                                    finish();
                                                } else {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Failed to save profile: "
                                                                    + task1.getException().getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });

                                } else {
                                    String message = "Registration failed";
                                    if (task.getException() != null) {
                                        message = "Registration failed: "
                                                + task.getException().getMessage();
                                    }
                                    Toast.makeText(RegisterActivity.this,
                                            message,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }
}
