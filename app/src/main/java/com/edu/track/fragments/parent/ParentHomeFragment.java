package com.edu.track.fragments.parent;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Map;

public class ParentHomeFragment extends Fragment {

    private ShimmerFrameLayout shimmerContent;
    private TextView tvChildName, tvChildDetails, tvStatOverall, tvStatMonth;
    private LinearLayout llAnnouncementsContainer;
    private ProgressBar pbOverall, pbMonth;
    private String currentStudentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_home, container, false);

        shimmerContent = view.findViewById(R.id.shimmer_parent_content);
        tvChildName = view.findViewById(R.id.tv_child_name);
        tvChildDetails = view.findViewById(R.id.tv_child_details);
        tvStatOverall = view.findViewById(R.id.tv_stat_overall);
        tvStatMonth = view.findViewById(R.id.tv_stat_month);
        pbOverall = view.findViewById(R.id.pb_overall);
        pbMonth = view.findViewById(R.id.pb_month);
        llAnnouncementsContainer = view.findViewById(R.id.ll_announcements_container);

        if (shimmerContent != null) shimmerContent.startShimmer();

        loadParentProfile();
        fetchAnnouncements();

        view.findViewById(R.id.tv_view_all_announcements).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.edu.track.activities.AnnouncementsFeedActivity.class));
        });

        view.findViewById(R.id.card_attendance_stat).setOnClickListener(v -> {
            if (currentStudentId != null) {
                Intent intent = new Intent(requireContext(), com.edu.track.activities.parent.ChildAttendanceDetailActivity.class);
                intent.putExtra("student_id", currentStudentId);
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadParentProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            String authEmail = user.getEmail();
            FirebaseSource.getInstance().getParentsRef().document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (value != null && value.exists()) {
                        String parentEmail = value.getString("email");
                        String lookUpEmail = (parentEmail != null && !parentEmail.isEmpty()) ? parentEmail : authEmail;
                        if (lookUpEmail != null) {
                            fetchChildData(lookUpEmail);
                        }
                    }
                });
        }
    }

    private void fetchChildData(String parentEmail) {
        FirebaseSource.getInstance().getFirestore().collection("students")
            .whereEqualTo("parentEmail", parentEmail)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                if (!queryDocumentSnapshots.isEmpty()) {
                    processStudentDoc(queryDocumentSnapshots.getDocuments().get(0));
                } else {
                    FirebaseSource.getInstance().getFirestore().collection("students")
                        .whereEqualTo("parentUid", parentEmail)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(snap2 -> {
                            if (!isAdded()) return;
                            if (!snap2.isEmpty()) {
                                processStudentDoc(snap2.getDocuments().get(0));
                            } else {
                                if (shimmerContent != null) shimmerContent.stopShimmer();
                                Toast.makeText(requireContext(), "No child data found", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (shimmerContent != null) shimmerContent.stopShimmer();
            });
    }

    private void processStudentDoc(DocumentSnapshot doc) {
        String name = doc.getString("name");
        long roll = doc.getLong("rollNumber") != null ? doc.getLong("rollNumber") : 0;
        String std = doc.getString("standard");
        String div = doc.getString("division");

        if (tvChildName != null) tvChildName.setText(name);
        if (tvChildDetails != null) tvChildDetails.setText("Roll No. " + roll + " · Class " + std + div);
        
        fetchAttendanceStats(doc.getId());
    }

    private void fetchAttendanceStats(String studentId) {
        this.currentStudentId = studentId;
        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
            .get()
            .addOnSuccessListener(attendanceDocs -> {
                if (!isAdded()) return;
                int total = 0;
                int present = 0;
                for (DocumentSnapshot d : attendanceDocs) {
                    Map<String, Boolean> statuses = (Map<String, Boolean>) d.get("statuses");
                    if (statuses != null && statuses.containsKey(studentId)) {
                        total++;
                        if (Boolean.TRUE.equals(statuses.get(studentId))) {
                            present++;
                        }
                    }
                }
                
                int percent = total > 0 ? (present * 100 / total) : 0;
                if (tvStatOverall != null) tvStatOverall.setText(percent + "%");
                if (tvStatMonth != null) tvStatMonth.setText(present + "/" + total + " Days");
                if (pbOverall != null) pbOverall.setProgress(percent);
                if (pbMonth != null) pbMonth.setProgress(percent);
                
                if (shimmerContent != null) {
                    shimmerContent.stopShimmer();
                    shimmerContent.setVisibility(View.GONE);
                }
                
                View actualContent = getView() != null ? getView().findViewById(R.id.layout_actual_content) : null;
                if (actualContent != null) {
                    actualContent.setVisibility(View.VISIBLE);
                }
            });
    }

    private void fetchAnnouncements() {
        FirebaseSource.getInstance().getFirestore().collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener((value, error) -> {
                if (!isAdded() || value == null) return;
                if (llAnnouncementsContainer != null) {
                    llAnnouncementsContainer.removeAllViews();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        addAnnouncementItem(doc);
                    }
                }
            });
    }

    private void addAnnouncementItem(DocumentSnapshot doc) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_announcement_row, llAnnouncementsContainer, false);
        TextView tvTitle = view.findViewById(R.id.tv_announcement_title);
        TextView tvContent = view.findViewById(R.id.tv_announcement_content);
        TextView tvDate = view.findViewById(R.id.tv_announcement_date);

        if (tvTitle != null) tvTitle.setText(doc.getString("title"));
        if (tvContent != null) tvContent.setText(doc.getString("content"));
        
        java.util.Date ts = doc.getDate("timestamp");
        if (tvDate != null) {
            tvDate.setText(ts != null ? new java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(ts) : "N/A");
        }
        
        TextView tvCategory = view.findViewById(R.id.tv_announcement_category);
        if (tvCategory != null) {
            String audience = doc.getString("audience");
            tvCategory.setText(audience != null ? audience : "General");
        }

        llAnnouncementsContainer.addView(view);
    }
}
