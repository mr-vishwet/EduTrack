package com.edu.track.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.edu.track.R;
import com.edu.track.adapters.AnnouncementAdapter;
import com.edu.track.models.Announcement;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementsFeedActivity extends AppCompatActivity {

    private RecyclerView rvAnnouncements;
    private AnnouncementAdapter adapter;
    private ShimmerFrameLayout shimmerView;
    private List<Announcement> announcementList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements_feed);

        rvAnnouncements = findViewById(R.id.rv_announcements);
        shimmerView = findViewById(R.id.shimmer_announcements);

        setupRecyclerView();
        setupClickListeners();
        fetchAnnouncements();
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter();
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        rvAnnouncements.setAdapter(adapter);
    }

    private void fetchAnnouncements() {
        if (shimmerView != null) {
            shimmerView.startShimmer();
            shimmerView.setVisibility(View.VISIBLE);
        }
        rvAnnouncements.setVisibility(View.GONE);

        FirebaseSource.getInstance().getFirestore().collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    if (shimmerView != null) shimmerView.stopShimmer();
                    return;
                }

                if (value != null) {
                    announcementList = value.toObjects(Announcement.class);
                    adapter.setAnnouncements(announcementList);

                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(View.GONE);
                    }
                    rvAnnouncements.setVisibility(View.VISIBLE);
                }
            });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        ImageView btnCreate = findViewById(R.id.btn_create_announcement);
        if (btnCreate != null) {
            String role = getSharedPreferences("EduTrackPrefs", MODE_PRIVATE).getString("USER_ROLE", "");
            if ("ADMIN".equals(role)) {
                btnCreate.setVisibility(View.VISIBLE);
                btnCreate.setOnClickListener(v -> {
                    startActivity(new android.content.Intent(this, com.edu.track.activities.admin.CreateAnnouncementActivity.class));
                });
            } else {
                btnCreate.setVisibility(View.GONE);
            }
        }
    }
}
