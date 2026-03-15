package com.edu.track.activities.parent;

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
import com.edu.track.activities.LoginActivity;
import com.edu.track.fragments.parent.ParentHomeFragment;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class ParentDashboardActivity extends AppCompatActivity {

    private TextView tvGreeting, tvSubtitle;
    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        bottomNav = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        loadParentProfile();
        setupClickListeners();
        setupBottomNavigation();

        // Load Default Fragment
        if (savedInstanceState == null) {
            loadFragment(new ParentHomeFragment(), "HOME");
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment fragment = null;
            String tag = "";

            if (itemId == R.id.nav_parent_home) {
                fragment = new ParentHomeFragment();
                tag = "HOME";
            } else if (itemId == R.id.nav_parent_performance) {
                // Performance fragment
                return true;
            } else if (itemId == R.id.nav_parent_notices) {
                startActivity(new Intent(this, com.edu.track.activities.AnnouncementsFeedActivity.class));
                return false;
            } else if (itemId == R.id.nav_parent_profile) {
                // Profile fragment
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

    private void loadParentProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getParentsRef().document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        String parentName = value.getString("name");
                        if (tvSubtitle != null && parentName != null) {
                            tvSubtitle.setText("Welcome, " + parentName);
                        }
                    }
                });
        }
    }

    private void setupClickListeners() {
        ImageView btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logout());

        View btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(this, com.edu.track.activities.AnnouncementsFeedActivity.class));
            });
        }
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
        if (bottomNav.getSelectedItemId() != R.id.nav_parent_home) {
            bottomNav.setSelectedItemId(R.id.nav_parent_home);
        } else {
            super.onBackPressed();
            moveTaskToBack(true);
        }
    }
}
