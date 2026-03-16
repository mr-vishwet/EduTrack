package com.edu.track.fragments.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.activities.admin.AdminReportsActivity;
import com.edu.track.activities.admin.TeacherPerformanceActivity;

public class AdminReportsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        // Academic / Attendance Reports
        View btnAcademic = view.findViewById(R.id.btn_academic_reports);
        if (btnAcademic != null) {
            btnAcademic.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AdminReportsActivity.class)));
        }

        // Faculty / Teacher Performance Reports
        View btnFaculty = view.findViewById(R.id.btn_teacher_performance);
        if (btnFaculty != null) {
            btnFaculty.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), TeacherPerformanceActivity.class)));
        }

        return view;
    }
}

