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
import com.edu.track.activities.admin.ManageClassesActivity;
import com.edu.track.activities.admin.ManageStudentsActivity;
import com.edu.track.activities.admin.ManageTeachersActivity;

public class AdminManageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_manage, container, false);

        view.findViewById(R.id.btn_manage_students).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageStudentsActivity.class)));

        view.findViewById(R.id.btn_manage_teachers).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageTeachersActivity.class)));

        view.findViewById(R.id.btn_manage_classes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageClassesActivity.class)));

        return view;
    }
}
