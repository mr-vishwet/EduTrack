package com.edu.track.fragments.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.activities.admin.AdminReportsActivity;
import com.edu.track.activities.admin.ManageStudentsActivity;
import com.edu.track.activities.admin.ManageTeachersActivity;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;

public class AdminHomeFragment extends Fragment {

    private ShimmerFrameLayout shimmerStats;
    private int statsLoadedCount = 0;
    private TextView tvStatStudents, tvStatClasses, tvStatTeachers, tvStatAttendance;
    private TextView tvHighlight1, tvHighlight2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        shimmerStats = view.findViewById(R.id.shimmer_stats);
        tvStatStudents = view.findViewById(R.id.tv_stat_students);
        tvStatClasses = view.findViewById(R.id.tv_stat_classes);
        tvStatTeachers = view.findViewById(R.id.tv_stat_teachers);
        tvStatAttendance = view.findViewById(R.id.tv_stat_attendance);
        tvHighlight1 = view.findViewById(R.id.tv_highlight_1);
        tvHighlight2 = view.findViewById(R.id.tv_highlight_2);

        if (shimmerStats != null) shimmerStats.startShimmer();

        setupClickListeners(view);
        fetchDashboardStats();
        fetchHighlights();

        return view;
    }

    private void checkAllStatsLoaded() {
        statsLoadedCount++;
        if (statsLoadedCount >= 4 && shimmerStats != null) {
            shimmerStats.stopShimmer();
            shimmerStats.setShimmer(null);
        }
    }

    private void fetchDashboardStats() {
        FirebaseSource.getInstance().getStudentsRef().whereEqualTo("isActive", true)
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    if (tvStatStudents != null) tvStatStudents.setText(String.valueOf(value.size()));
                }
                checkAllStatsLoaded();
            });

        FirebaseSource.getInstance().getClassesRef()
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    if (tvStatClasses != null) tvStatClasses.setText(String.valueOf(value.size()));
                }
                checkAllStatsLoaded();
            });

        FirebaseSource.getInstance().getTeachersRef()
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    if (tvStatTeachers != null) tvStatTeachers.setText(String.valueOf(value.size()));
                }
                checkAllStatsLoaded();
            });

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
            .whereEqualTo("date", today)
            .addSnapshotListener((value, error) -> {
                if (value != null && !value.isEmpty()) {
                    // This counts classes marked today
                    if (tvStatAttendance != null) tvStatAttendance.setText(value.size() + " classes");
                } else {
                    if (tvStatAttendance != null) tvStatAttendance.setText("0 marked");
                }
                checkAllStatsLoaded();
            });
    }

    private void fetchHighlights() {
        FirebaseSource.getInstance().getFirestore().collection("announcements")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(2)
            .addSnapshotListener((value, error) -> {
                if (value != null && !value.isEmpty()) {
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = value.getDocuments();
                    if (docs.size() > 0 && tvHighlight1 != null) tvHighlight1.setText(docs.get(0).getString("title"));
                    if (docs.size() > 1 && tvHighlight2 != null) tvHighlight2.setText(docs.get(1).getString("title"));
                }
            });
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btn_action_students).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageStudentsActivity.class)));

        view.findViewById(R.id.btn_action_teachers).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageTeachersActivity.class)));

        view.findViewById(R.id.btn_action_reports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminReportsActivity.class)));

        view.findViewById(R.id.btn_action_announcements).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.edu.track.activities.AnnouncementsFeedActivity.class)));

        View btnPostNew = view.findViewById(R.id.btn_post_new);
        if (btnPostNew != null) {
            btnPostNew.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), com.edu.track.activities.admin.CreateAnnouncementActivity.class)));
        }
    }
}
