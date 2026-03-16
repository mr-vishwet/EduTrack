package com.edu.track.fragments.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.activities.teacher.AttendanceHistoryActivity;
import com.edu.track.activities.teacher.TakeAttendanceActivity;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherHomeFragment extends Fragment {

    private ShimmerFrameLayout shimmerClasses;
    private LinearLayout llClassesContainer, llRecentActivityContainer;
    private TextView tvAttendanceDate;
    private MaterialButton btnStartAttendance;
    private String primaryClass = "";
    private String primaryStd = "";
    private String primaryDiv = "";
    private final String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_home, container, false);

        shimmerClasses = view.findViewById(R.id.shimmer_classes);
        llClassesContainer = view.findViewById(R.id.ll_classes_container);
        llRecentActivityContainer = view.findViewById(R.id.ll_recent_activity_container);
        tvAttendanceDate = view.findViewById(R.id.tv_attendance_date);
        btnStartAttendance = view.findViewById(R.id.btn_start_attendance);

        if (shimmerClasses != null) shimmerClasses.startShimmer();

        updateDate();
        loadTeacherData();
        setupClickListeners(view);

        return view;
    }

    private void updateDate() {
        if (tvAttendanceDate != null) {
            String date = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date());
            tvAttendanceDate.setText(date);
        }
    }

    private void loadTeacherData() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getTeachersRef().document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || value == null || !value.exists()) return;

                    List<String> classes = (List<String>) value.get("assignedClasses");
                    populateClasses(classes);
                    if (classes != null && !classes.isEmpty()) {
                        primaryClass = classes.get(0);
                        primaryStd = primaryClass.replaceAll("[^0-9]", "");
                        primaryDiv = primaryClass.replaceAll("[0-9]", "");

                        // Check if today's attendance already marked for primary class
                        checkAttendanceMarked(primaryStd, primaryDiv);
                        fetchRecentActivity(classes);
                    }
                });
        }
    }

    /**
     * Check if attendance for today already exists — if so, update btn_start_attendance
     * to say "View Attendance" and take the teacher to the history view.
     */
    private void checkAttendanceMarked(String std, String div) {
        if (std.isEmpty() || div.isEmpty() || btnStartAttendance == null) return;

        String docId = todayDate + "_" + std + div;
        FirebaseSource.getInstance().getFirestore()
            .collection("attendance_records").document(docId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                if (doc.exists()) {
                    // Already marked — show "View Attendance" CTA
                    btnStartAttendance.setText("✓  Today's Attendance Marked — View");
                    btnStartAttendance.setOnClickListener(v -> {
                        Intent intent = new Intent(requireContext(), AttendanceHistoryActivity.class);
                        intent.putExtra("standard", std);
                        intent.putExtra("division", div);
                        startActivity(intent);
                    });
                } else {
                    btnStartAttendance.setText("Mark Attendance");
                    btnStartAttendance.setOnClickListener(v -> openTakeAttendance(std, div));
                }
            });
    }

    private void openTakeAttendance(String std, String div) {
        Intent intent = new Intent(requireContext(), TakeAttendanceActivity.class);
        intent.putExtra("standard", std);
        intent.putExtra("division", div);
        startActivity(intent);
    }

    private void populateClasses(List<String> classes) {
        if (llClassesContainer == null) return;
        llClassesContainer.removeAllViews();

        if (shimmerClasses != null) {
            shimmerClasses.stopShimmer();
            shimmerClasses.setVisibility(View.GONE);
        }

        if (classes == null || classes.isEmpty()) return;

        for (String classId : classes) {
            View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_class_card_small, llClassesContainer, false);
            TextView tvClass = card.findViewById(R.id.tv_class_name);
            tvClass.setText("Std " + classId);

            String std = classId.replaceAll("[^0-9]", "");
            String div = classId.replaceAll("[0-9]", "");

            // Check per-card if attendance is already marked today
            String docId = todayDate + "_" + classId;
            FirebaseSource.getInstance().getFirestore()
                .collection("attendance_records").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        // Already marked — show a "marked" indicator
                        TextView tvMark = card.findViewById(R.id.tv_class_name);
                        tvMark.setText("✓ Std " + classId + " · Done");
                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), AttendanceHistoryActivity.class);
                            intent.putExtra("standard", std);
                            intent.putExtra("division", div);
                            startActivity(intent);
                        });
                    } else {
                        card.setOnClickListener(v -> openTakeAttendance(std, div));
                    }
                });

            llClassesContainer.addView(card);
        }
    }

    private void fetchRecentActivity(List<String> classes) {
        if (llRecentActivityContainer == null) return;
        llRecentActivityContainer.removeAllViews();

        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String std = doc.getString("standard");
                    String div = doc.getString("division");
                    if (std == null || div == null) continue;
                    String combined = std + div;
                    if (classes.contains(combined)) {
                        addActivityItem(doc);
                    }
                }
            });
    }

    private void addActivityItem(DocumentSnapshot doc) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_teacher_activity, llRecentActivityContainer, false);
        TextView tvTitle = view.findViewById(R.id.tv_activity_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_activity_subtitle);

        String std = doc.getString("standard");
        String div = doc.getString("division");
        String date = doc.getString("date");

        java.util.Map<String, Boolean> statuses = (java.util.Map<String, Boolean>) doc.get("statuses");
        int present = 0;
        if (statuses != null) for (Boolean p : statuses.values()) if (p) present++;
        int total = statuses != null ? statuses.size() : 0;

        tvTitle.setText("Std " + std + div + " · Attendance Marked");
        tvSubtitle.setText(date + " | " + present + "/" + total + " Present");
        llRecentActivityContainer.addView(view);
    }

    private void setupClickListeners(View view) {
        // btn_start_attendance is wired dynamically in checkAttendanceMarked/loadTeacherData
        // Default handler until class data loads
        if (btnStartAttendance != null) {
            btnStartAttendance.setOnClickListener(v -> {
                if (!primaryStd.isEmpty()) {
                    openTakeAttendance(primaryStd, primaryDiv);
                } else {
                    Toast.makeText(requireContext(), "No classes assigned yet", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View tvViewAll = view.findViewById(R.id.tv_view_all_classes);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AttendanceHistoryActivity.class);
                intent.putExtra("teacher_filter", true);
                startActivity(intent);
            });
        }
    }
}
