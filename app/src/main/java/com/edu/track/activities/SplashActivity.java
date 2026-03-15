package com.edu.track.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.edu.track.R;
import com.edu.track.models.User;
import com.edu.track.utils.FirebaseSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserSession();
        }, 2000);
    }

    private void checkUserSession() {
        FirebaseUser currentUser = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (currentUser != null) {
            // Fetch role and redirect
            FirebaseSource.getInstance().getUsersRef().document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            navigateToDashboard(user.getRole());
                        } else {
                            goToRoleSelection();
                        }
                    } else {
                        goToRoleSelection();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Session error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    goToRoleSelection();
                });
        } else {
            goToRoleSelection();
        }
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("ADMIN".equals(role)) {
            intent = new Intent(this, com.edu.track.activities.admin.AdminDashboardActivity.class);
        } else if ("TEACHER".equals(role)) {
            intent = new Intent(this, com.edu.track.activities.teacher.TeacherDashboardActivity.class);
        } else {
            intent = new Intent(this, com.edu.track.activities.parent.ParentDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void goToRoleSelection() {
        startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
        finish();
    }
}
