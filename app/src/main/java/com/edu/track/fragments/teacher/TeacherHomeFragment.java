package com.edu.track.fragments.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.activities.teacher.AttendanceHistoryActivity;
import com.edu.track.activities.teacher.TakeAttendanceActivity;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherHomeFragment extends Fragment {

    private ShimmerFrameLayout shimmerClasses;
    private LinearLayout llClassesContainer, llRecentActivityContainer;
    private TextView tvAttendanceDate, tvAttendanceCardTitle;
    private MaterialButton btnStartAttendance;
    private ImageView ivAttendanceIcon;
    private android.widget.FrameLayout flIconBg;

    private String primaryClass = "";
    private String primaryStd = "";
    private String primaryDiv = "";
    private String classTeacher = "";
    private final String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    /** Realtime listener for recent activity */
    private ListenerRegistration recentActivityListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_home, container, false);

        shimmerClasses = view.findViewById(R.id.shimmer_classes);
        llClassesContainer = view.findViewById(R.id.ll_classes_container);
        llRecentActivityContainer = view.findViewById(R.id.ll_recent_activity_container);
        tvAttendanceDate = view.findViewById(R.id.tv_attendance_date);
        tvAttendanceCardTitle = view.findViewById(R.id.tv_attendance_card_title);
        btnStartAttendance = view.findViewById(R.id.btn_start_attendance);
        ivAttendanceIcon = view.findViewById(R.id.iv_attendance_icon);
        flIconBg = view.findViewById(R.id.fl_attendance_icon_bg);

        if (shimmerClasses != null) shimmerClasses.startShimmer();

        updateDate();
        loadTeacherData();
        setupClickListeners(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh attendance card state every time the fragment becomes visible
        // (e.g. after returning from TakeAttendanceActivity)
        if (!primaryStd.isEmpty() && !primaryDiv.isEmpty()) {
            checkAttendanceMarked(primaryStd, primaryDiv);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recentActivityListener != null) {
            recentActivityListener.remove();
            recentActivityListener = null;
        }
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
                    classTeacher = value.getString("classTeacher");
                    if (classTeacher == null) classTeacher = "";

                    populateClasses(classes);

                    // Use classTeacher as the primary class for the main attendance card
                    if (!classTeacher.isEmpty()) {
                        primaryClass = classTeacher;
                        primaryStd = classTeacher.replaceAll("[^0-9]", "");
                        primaryDiv = classTeacher.replaceAll("[0-9]", "");
                        checkAttendanceMarked(primaryStd, primaryDiv);
                    } else if (classes != null && !classes.isEmpty()) {
                        primaryClass = classes.get(0);
                        primaryStd = primaryClass.replaceAll("[^0-9]", "");
                        primaryDiv = primaryClass.replaceAll("[0-9]", "");
                    }

                    if (classes != null) subscribeRecentActivity(classes);
                });
        }
    }

    /**
     * Check if attendance for today already exists.
     * Updates the card to DONE state (green) if marked, else PENDING state (teal).
     */
    private void checkAttendanceMarked(String std, String div) {
        if (std.isEmpty() || div.isEmpty()) return;

        String docId = todayDate + "_" + std + div;
        FirebaseSource.getInstance().getFirestore()
            .collection("attendance_records").document(docId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                if (doc.exists()) {
                    setAttendanceCardDone(std, div);
                } else {
                    setAttendanceCardPending(std, div);
                }
            });
    }

    private void setAttendanceCardDone(String std, String div) {
        if (!isAdded()) return;
        // Green check icon & label
        if (ivAttendanceIcon != null)
            ivAttendanceIcon.setImageResource(R.drawable.ic_check_circle);
        if (ivAttendanceIcon != null)
            ivAttendanceIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.success_green)));
        if (flIconBg != null)
            flIconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9)); // light green

        if (tvAttendanceCardTitle != null)
            tvAttendanceCardTitle.setText("✓ Attendance Marked");
        if (tvAttendanceDate != null)
            tvAttendanceDate.setText("Today's attendance submitted. Tap to view.");

        if (btnStartAttendance != null) {
            btnStartAttendance.setText("View Attendance");
            btnStartAttendance.setIconResource(R.drawable.ic_check_circle);
            btnStartAttendance.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.success_green)));
            btnStartAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AttendanceHistoryActivity.class);
                intent.putExtra("standard", std);
                intent.putExtra("division", div);
                intent.putExtra("teacher_filter", true);
                startActivity(intent);
            });
        }
    }

    private void setAttendanceCardPending(String std, String div) {
        if (!isAdded()) return;
        if (ivAttendanceIcon != null) {
            ivAttendanceIcon.setImageResource(R.drawable.ic_calendar_today);
            ivAttendanceIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.teal)));
        }
        if (flIconBg != null)
            flIconBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0F7FA)); // light teal

        if (tvAttendanceCardTitle != null)
            tvAttendanceCardTitle.setText("Take Today's Attendance");
        if (tvAttendanceDate != null) {
            String date = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date());
            tvAttendanceDate.setText(date);
        }

        if (btnStartAttendance != null) {
            btnStartAttendance.setText("Mark Attendance");
            btnStartAttendance.setIconResource(R.drawable.ic_arrow_forward);
            btnStartAttendance.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.teal)));
            btnStartAttendance.setOnClickListener(v -> openTakeAttendance(std, div));
        }
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

            String docId = todayDate + "_" + classId;
            boolean isClassTeacherForThis = classId.equalsIgnoreCase(classTeacher);

            FirebaseSource.getInstance().getFirestore()
                .collection("attendance_records").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        tvClass.setText("✓ Std " + classId + " · Done");
                        // Use green tint for done cards
                        card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), AttendanceHistoryActivity.class);
                            intent.putExtra("standard", std);
                            intent.putExtra("division", div);
                            intent.putExtra("teacher_filter", true);
                            startActivity(intent);
                        });
                    } else if (isClassTeacherForThis) {
                        card.setOnClickListener(v -> openTakeAttendance(std, div));
                    } else {
                        tvClass.setText("Std " + classId + " · View");
                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), AttendanceHistoryActivity.class);
                            intent.putExtra("standard", std);
                            intent.putExtra("division", div);
                            intent.putExtra("teacher_filter", true);
                            startActivity(intent);
                        });
                    }
                });

            llClassesContainer.addView(card);
        }
    }

    /** Subscribe to real-time updates for recent activity — only this teacher's classes. */
    private void subscribeRecentActivity(List<String> classes) {
        if (llRecentActivityContainer == null || classes == null || classes.isEmpty()) return;

        if (recentActivityListener != null) {
            recentActivityListener.remove();
        }

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -14);
        String fromDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        recentActivityListener = FirebaseSource.getInstance().getFirestore()
            .collection("attendance_records")
            .whereGreaterThanOrEqualTo("date", fromDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener((querySnap, error) -> {
                if (!isAdded() || querySnap == null) return;

                llRecentActivityContainer.removeAllViews();
                int count = 0;
                for (DocumentSnapshot doc : querySnap.getDocuments()) {
                    String std = doc.getString("standard");
                    String div = doc.getString("division");
                    if (std == null || div == null) continue;
                    String combined = std + div;
                    if (classes.contains(combined)) {
                        addActivityItem(doc);
                        count++;
                        if (count >= 5) break;
                    }
                }

                if (count == 0) {
                    View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_teacher_activity, llRecentActivityContainer, false);
                    TextView tv = view.findViewById(R.id.tv_activity_title);
                    TextView tvSub = view.findViewById(R.id.tv_activity_subtitle);
                    if (tv != null) tv.setText("No recent activity");
                    if (tvSub != null) tvSub.setText("Attendance records will appear here once marked");
                    llRecentActivityContainer.addView(view);
                }
            });
    }

    private void addActivityItem(DocumentSnapshot doc) {
        if (!isAdded()) return;
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_teacher_activity, llRecentActivityContainer, false);
        TextView tvTitle = view.findViewById(R.id.tv_activity_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_activity_subtitle);
        TextView tvMyClassBadge = view.findViewById(R.id.tv_my_class_badge);

        String std = doc.getString("standard");
        String div = doc.getString("division");
        String date = doc.getString("date");

        java.util.Map<String, Boolean> statuses = (java.util.Map<String, Boolean>) doc.get("statuses");
        int present = 0;
        if (statuses != null) for (Boolean p : statuses.values()) if (Boolean.TRUE.equals(p)) present++;
        int total = statuses != null ? statuses.size() : 0;

        boolean isToday = todayDate.equals(date);
        
        String combined = (std != null ? std : "") + (div != null ? div : "");
        if (tvMyClassBadge != null) {
            if (!classTeacher.isEmpty() && classTeacher.equalsIgnoreCase(combined)) {
                tvMyClassBadge.setVisibility(View.VISIBLE);
            } else {
                tvMyClassBadge.setVisibility(View.GONE);
            }
        }

        if (tvTitle != null)
            tvTitle.setText("Std " + std + div + " · Attendance" + (isToday ? " (Today)" : ""));
        if (tvSubtitle != null)
            tvSubtitle.setText(date + " | " + present + "/" + total + " Present");

        llRecentActivityContainer.addView(view);
    }

    private void setupClickListeners(View view) {
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
