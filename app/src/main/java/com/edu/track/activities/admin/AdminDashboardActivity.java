package com.edu.track.activities.admin;

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
import com.edu.track.activities.AnnouncementsFeedActivity;
import com.edu.track.activities.LoginActivity;
import com.edu.track.activities.SplashActivity;
import com.edu.track.fragments.admin.AdminHomeFragment;
import com.edu.track.fragments.admin.AdminManageFragment;
import com.edu.track.fragments.admin.AdminReportsFragment;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvGreeting, tvDate;
    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvDate = findViewById(R.id.tv_date);
        bottomNav = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        setCurrentDate();
        loadAdminName();
        setupClickListeners();
        setupBottomNavigation();

        // Load Default Fragment
        if (savedInstanceState == null) {
            loadFragment(new AdminHomeFragment(), "HOME");
        }
        // Added: dummy data population for announcements
        populateDummyAnnouncements();
    }

    private void populateDummyAnnouncements() {
        com.google.firebase.firestore.FirebaseFirestore db = com.edu.track.utils.FirebaseSource.getInstance().getFirestore();
        db.collection("announcements").limit(1).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                
                java.util.Map<String, Object> a1 = new java.util.HashMap<>();
                a1.put("title", "Annual Sports Day 2026");
                a1.put("content", "Dear Parents and Students, the Annual Sports Day will be held next Friday. Please come in your sports uniform.");
                a1.put("audience", "All");
                a1.put("timestamp", new java.util.Date(System.currentTimeMillis() + 86400000L * 7));
                a1.put("author", "Admin");
                batch.set(db.collection("announcements").document("ANN_1"), a1);

                java.util.Map<String, Object> a2 = new java.util.HashMap<>();
                a2.put("title", "Half-Yearly Examination Schedule");
                a2.put("content", "The half-yearly examinations are scheduled to begin from the 15th of next month. Timetable will be shared shortly.");
                a2.put("audience", "Students & Parents");
                a2.put("timestamp", new java.util.Date(System.currentTimeMillis() - 86400000L * 2));
                a2.put("author", "Admin");
                batch.set(db.collection("announcements").document("ANN_2"), a2);

                java.util.Map<String, Object> a3 = new java.util.HashMap<>();
                a3.put("title", "Staff Meeting Notice");
                a3.put("content", "There will be a mandatory staff meeting this Saturday at 10:00 AM in the main auditorium.");
                a3.put("audience", "Teachers Only");
                a3.put("timestamp", new java.util.Date(System.currentTimeMillis() - 86400000L * 5));
                a3.put("author", "Admin");
                batch.set(db.collection("announcements").document("ANN_3"), a3);

                batch.commit();
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment fragment = null;
            String tag = "";

            if (itemId == R.id.nav_admin_dashboard) {
                fragment = new AdminHomeFragment();
                tag = "HOME";
            } else if (itemId == R.id.nav_admin_manage) {
                fragment = new AdminManageFragment();
                tag = "MANAGE";
            } else if (itemId == R.id.nav_admin_reports) {
                fragment = new AdminReportsFragment();
                tag = "REPORTS";
            } else if (itemId == R.id.nav_admin_settings) {
                fragment = new com.edu.track.fragments.admin.AdminSettingsFragment();
                tag = "SETTINGS";
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

    private void setCurrentDate() {
        if (tvDate != null) {
            String date = new java.text.SimpleDateFormat("EEE, dd MMM yyyy", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            tvDate.setText(date);
        }
    }

    private void loadAdminName() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getUsersRef().document(user.getUid())
                    .addSnapshotListener((value, error) -> {
                        if (value != null && value.exists()) {
                            String name = value.getString("name");
                            if (tvGreeting != null && name != null)
                                tvGreeting.setText("Hello, " + name);
                        }
                    });
        }
    }

    private void setupClickListeners() {
        ImageView btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> logout());

        findViewById(R.id.btn_notifications)
                .setOnClickListener(v -> startActivity(new Intent(this, AnnouncementsFeedActivity.class)));

        View tvAvatar = findViewById(R.id.tv_avatar);
        if (tvAvatar != null) {
            tvAvatar.setOnClickListener(v -> {
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_admin_settings);
                }
            });
        }
    }

    private void logout() {
        FirebaseSource.getInstance().getAuth().signOut();
        // Clear all saved preferences so SplashActivity knows no session exists
        getSharedPreferences("EduTrackPrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (bottomNav.getSelectedItemId() != R.id.nav_admin_dashboard) {
            bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
        } else {
            super.onBackPressed();
            moveTaskToBack(true);
        }
    }
}
