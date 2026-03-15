package com.edu.track.fragments.parent;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ParentHomeFragment extends Fragment {

    private ShimmerFrameLayout shimmerContent;
    private TextView tvChildName, tvChildDetails, tvStatOverall, tvStatChange, tvStatMonth;
    private ProgressBar pbOverall, pbMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_home, container, false);

        shimmerContent = view.findViewById(R.id.shimmer_parent_content);
        tvChildName = view.findViewById(R.id.tv_child_name);
        tvChildDetails = view.findViewById(R.id.tv_child_details);
        tvStatOverall = view.findViewById(R.id.tv_stat_overall);
        tvStatChange = view.findViewById(R.id.tv_stat_change);
        tvStatMonth = view.findViewById(R.id.tv_stat_month);
        pbOverall = view.findViewById(R.id.pb_overall);
        pbMonth = view.findViewById(R.id.pb_month);

        if (shimmerContent != null) shimmerContent.startShimmer();

        loadParentProfile();

        view.findViewById(R.id.tv_view_all_announcements).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.edu.track.activities.AnnouncementsFeedActivity.class));
        });

        return view;
    }

    private void loadParentProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getParentsRef().document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (value != null && value.exists()) {
                        String parentEmail = value.getString("email");
                        if (parentEmail != null) {
                            fetchChildData(parentEmail);
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
                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    String name = doc.getString("name");
                    String roll = String.valueOf(doc.get("rollNumber"));
                    String std = doc.getString("standard");
                    String div = doc.getString("division");

                    if (tvChildName != null) tvChildName.setText(name);
                    if (tvChildDetails != null) tvChildDetails.setText("Roll No. " + roll + " · Class " + std + div);
                    
                    fetchAttendanceStats(doc.getId());
                } else {
                    if (shimmerContent != null) shimmerContent.stopShimmer();
                    Toast.makeText(requireContext(), "No child data found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (shimmerContent != null) shimmerContent.stopShimmer();
            });
    }

    private void fetchAttendanceStats(String studentId) {
        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener(attendanceDocs -> {
                if (!isAdded()) return;
                int total = attendanceDocs.size();
                int present = 0;
                for (DocumentSnapshot d : attendanceDocs) {
                    if (Boolean.TRUE.equals(d.getBoolean("present"))) {
                        present++;
                    }
                }
                
                int percent = total > 0 ? (present * 100 / total) : 0;
                if (tvStatOverall != null) tvStatOverall.setText(percent + "%");
                if (tvStatMonth != null) tvStatMonth.setText(present + "/" + total);
                if (pbOverall != null) pbOverall.setProgress(percent);
                if (pbMonth != null) pbMonth.setProgress(percent);
                
                if (shimmerContent != null) {
                    shimmerContent.stopShimmer();
                    shimmerContent.setVisibility(View.GONE);
                }
            });
    }
}
