package com.edu.track.activities.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.edu.track.R;
import com.edu.track.activities.LoginActivity;
import com.edu.track.fragments.teacher.TeacherHomeFragment;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class TeacherDashboardActivity extends AppCompatActivity {

    private TextView tvGreeting, tvSubtitle;
    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        bottomNav = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        loadTeacherProfile();
        setupClickListeners();
        setupBottomNavigation();

        // Load Default Fragment
        if (savedInstanceState == null) {
            loadFragment(new TeacherHomeFragment(), "HOME");
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment fragment = null;
            String tag = "";

            if (itemId == R.id.nav_teacher_dashboard) {
                fragment = new TeacherHomeFragment();
                tag = "HOME";
            } else if (itemId == R.id.nav_teacher_attendance) {
                startActivity(new Intent(this, TakeAttendanceActivity.class));
                return false;
            } else if (itemId == R.id.nav_teacher_classes) {
                // Classes fragment can be added here
                return true;
            } else if (itemId == R.id.nav_teacher_profile) {
                // Profile fragment can be added here
                return true;
            }

            if (fragment != null) {
                loadFragment(fragment, tag);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }

    private void loadTeacherProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getTeachersRef().document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        String name = value.getString("name");
                        Object assignedClasses = value.get("metadata.assignedClasses");
                        
                        if (tvGreeting != null && name != null) tvGreeting.setText("Hello, " + name);
                        if (tvSubtitle != null && assignedClasses != null) {
                            tvSubtitle.setText("Class Teacher · " + assignedClasses.toString());
                        }
                    }
                });
        }
    }

    private void setupClickListeners() {
        ImageView btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        FirebaseSource.getInstance().getAuth().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (bottomNav.getSelectedItemId() != R.id.nav_teacher_dashboard) {
            bottomNav.setSelectedItemId(R.id.nav_teacher_dashboard);
        } else {
            super.onBackPressed();
            moveTaskToBack(true);
        }
    }
}
