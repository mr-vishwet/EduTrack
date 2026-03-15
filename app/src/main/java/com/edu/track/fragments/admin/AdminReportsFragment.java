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

public class AdminReportsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        view.findViewById(R.id.btn_academic_reports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminReportsActivity.class)));

        // Other report types can be added here
        
        return view;
    }
}
