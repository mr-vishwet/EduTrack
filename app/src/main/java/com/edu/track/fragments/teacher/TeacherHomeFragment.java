package com.edu.track.fragments.teacher;

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
import com.edu.track.activities.teacher.TakeAttendanceActivity;
import com.edu.track.utils.FirebaseSource;
import com.google.firebase.auth.FirebaseUser;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TeacherHomeFragment extends Fragment {

    private ShimmerFrameLayout shimmerClasses;
    private TextView tvAttendanceDate;
    private String assignedClassStr = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_home, container, false);

        shimmerClasses = view.findViewById(R.id.shimmer_classes);
        tvAttendanceDate = view.findViewById(R.id.tv_attendance_date);

        if (shimmerClasses != null) shimmerClasses.startShimmer();

        updateDate();
        loadTeacherProfile();
        setupClickListeners(view);

        return view;
    }

    private void updateDate() {
        if (tvAttendanceDate != null) {
            String date = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date());
            tvAttendanceDate.setText(date);
        }
    }

    private void loadTeacherProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            FirebaseSource.getInstance().getTeachersRef().document(user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (value != null && value.exists()) {
                        Object assignedClasses = value.get("metadata.assignedClasses");
                        if (assignedClasses != null) {
                            assignedClassStr = assignedClasses.toString();
                            if (shimmerClasses != null) {
                                shimmerClasses.stopShimmer();
                                shimmerClasses.setVisibility(View.GONE);
                            }
                        }
                    }
                });
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btn_start_attendance).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TakeAttendanceActivity.class);
            if (!assignedClassStr.isEmpty()) {
                String std = assignedClassStr.replaceAll("[^0-9]", "");
                String div = assignedClassStr.replaceAll("[0-9]", "");
                intent.putExtra("standard", std);
                intent.putExtra("division", div);
            }
            startActivity(intent);
        });
    }
}
