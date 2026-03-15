package com.edu.track.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.edu.track.R;
import com.edu.track.models.User;
import com.edu.track.utils.FirebaseSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectedRole = getIntent().getStringExtra("ROLE");
        if (selectedRole == null) selectedRole = "PARENT"; 

        if ("ADMIN".equals(selectedRole)) {
            setContentView(R.layout.activity_login_admin);
        } else if ("TEACHER".equals(selectedRole)) {
            setContentView(R.layout.activity_login_teacher);
        } else {
            setContentView(R.layout.activity_login_parent);
        }

        mAuth = FirebaseSource.getInstance().getAuth();

        if ("PARENT".equals(selectedRole)) {
            etEmail = findViewById(R.id.et_roll);
        } else {
            etEmail = findViewById(R.id.et_email);
        }
        
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> attemptLogin());
        }
    }

    private void attemptLogin() {
        String loginId = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(loginId)) {
            if ("PARENT".equals(selectedRole)) {
                etEmail.setError("Roll number/Login ID is required");
            } else {
                etEmail.setError("Email is required");
            }
            return;
        }
        
        // If Parent, format the login ID to an internal email if it follows the pattern
        String finalEmail = loginId;
        if ("PARENT".equals(selectedRole)) {
            if (!loginId.contains("@")) {
                finalEmail = loginId + "@edutrack.com";
            }
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(finalEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRole(user.getUid());
                        }
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        android.util.Log.e("FirebaseLogin", "Auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole(String uid) {
        FirebaseSource.getInstance().getUsersRef().document(uid).get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            User userData = document.toObject(User.class);
                            if (userData != null) {
                                if (userData.getRole().equals(selectedRole)) {
                                    navigateToDashboard(userData.getRole());
                                } else {
                                    mAuth.signOut();
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(this, "Unauthorized: Role mismatch", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            mAuth.signOut();
                            btnLogin.setEnabled(true);
                            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Error fetching profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent nextIntent;
        if ("ADMIN".equals(role)) {
            nextIntent = new Intent(LoginActivity.this, com.edu.track.activities.admin.AdminDashboardActivity.class);
        } else if ("TEACHER".equals(role)) {
            nextIntent = new Intent(LoginActivity.this, com.edu.track.activities.teacher.TeacherDashboardActivity.class);
        } else {
            nextIntent = new Intent(LoginActivity.this, com.edu.track.activities.parent.ParentDashboardActivity.class);
        }
        startActivity(nextIntent);
        finish();
    }
}
