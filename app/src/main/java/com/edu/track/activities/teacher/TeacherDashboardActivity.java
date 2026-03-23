package com.edu.track.activities.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.edu.track.R;
import com.edu.track.activities.SplashActivity;
import com.edu.track.fragments.teacher.TeacherHomeFragment;
import com.edu.track.fragments.teacher.TeacherProfileFragment;
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

        loadTeacherGreeting();
        setupClickListeners();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new TeacherHomeFragment(), "HOME");
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_teacher_dashboard) {
                loadFragment(new TeacherHomeFragment(), "HOME");
                return true;
            } else if (itemId == R.id.nav_teacher_classes) {
                // "History" tab — opens AttendanceHistoryActivity filtered by teacher's classes
                startActivity(new Intent(this, AttendanceHistoryActivity.class)
                        .putExtra("teacher_filter", true));
                return false; // don't change selected tab when launching activity
            } else if (itemId == R.id.nav_teacher_profile) {
                loadFragment(new TeacherProfileFragment(), "PROFILE");
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

    private void loadTeacherGreeting() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getTeachersRef().document(user.getUid())
                    .addSnapshotListener((value, error) -> {
                        if (value != null && value.exists()) {
                            String name = value.getString("name");
                            String expertise = value.getString("expertise");
                            String classTeacher = value.getString("classTeacher");

                            if (tvGreeting != null && name != null)
                                tvGreeting.setText("Hello, " + name);

                            if (tvSubtitle != null) {
                                if (classTeacher != null && !classTeacher.isEmpty()) {
                                    tvSubtitle.setText("Class Teacher · Std " + classTeacher);
                                } else if (expertise != null && !expertise.isEmpty()) {
                                    tvSubtitle.setText(expertise + " Teacher");
                                } else {
                                    tvSubtitle.setText("Teacher");
                                }
                            }
                        }
                    });
        }
    }

    private void setupClickListeners() {
        ImageView btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logout());

        View tvAvatar = findViewById(R.id.tv_avatar);
        if (tvAvatar != null) {
            tvAvatar.setOnClickListener(v -> {
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_teacher_profile);
                }
            });
        }

        View btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                // Open announcement/notification list for teacher
                Intent intent = new Intent(this, com.edu.track.activities.teacher.AnnouncementsActivity.class);
                startActivity(intent);
            });
        }

        findViewById(R.id.fab_add_announcement).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.edu.track.activities.admin.CreateAnnouncementActivity.class);
            intent.putExtra("is_teacher", true);
            startActivity(intent);
        });
    }

    private void logout() {
        FirebaseSource.getInstance().getAuth().signOut();
        getSharedPreferences("EduTrackPrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, SplashActivity.class);
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
