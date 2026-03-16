package com.edu.track.fragments.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.activities.SplashActivity;
import com.edu.track.utils.FirebaseSource;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvExpertise, tvClassTeacher, tvInitials, tvJoined;
    private LinearLayout llAssignedClasses;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_profile, container, false);

        tvName = view.findViewById(R.id.tv_teacher_name_profile);
        tvEmail = view.findViewById(R.id.tv_teacher_email);
        tvExpertise = view.findViewById(R.id.tv_teacher_expertise);
        tvClassTeacher = view.findViewById(R.id.tv_class_teacher_label);
        tvInitials = view.findViewById(R.id.tv_profile_initials);
        tvJoined = view.findViewById(R.id.tv_joined_date);
        llAssignedClasses = view.findViewById(R.id.ll_assigned_classes);

        loadProfile();
        setupLogoutButton(view);

        return view;
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user == null) return;

        FirebaseSource.getInstance().getTeachersRef().document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded() || !doc.exists()) return;

                String name = doc.getString("name");
                String email = doc.getString("email");
                String expertise = doc.getString("expertise");
                String classTeacher = doc.getString("classTeacher");
                Long createdAt = doc.getLong("createdAt");
                List<String> assigned = (List<String>) doc.get("assignedClasses");

                if (tvName != null) tvName.setText(name != null ? name : "—");
                if (tvEmail != null) tvEmail.setText(email != null ? email : "—");
                if (tvExpertise != null) tvExpertise.setText("Subject: " + (expertise != null ? expertise : "—"));

                if (tvClassTeacher != null) {
                    if (classTeacher != null && !classTeacher.isEmpty()) {
                        tvClassTeacher.setText("Class Teacher of: Std " + classTeacher);
                        tvClassTeacher.setVisibility(View.VISIBLE);
                    } else {
                        tvClassTeacher.setText("Subject Teacher");
                        tvClassTeacher.setVisibility(View.VISIBLE);
                    }
                }

                if (tvInitials != null && name != null && !name.isEmpty()) {
                    String[] words = name.split(" ");
                    String initials = words.length > 1
                            ? (words[0].charAt(0) + "" + words[1].charAt(0)).toUpperCase()
                            : ("" + words[0].charAt(0)).toUpperCase();
                    tvInitials.setText(initials);
                }

                if (tvJoined != null && createdAt != null) {
                    String joined = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(createdAt));
                    tvJoined.setText("Joined: " + joined);
                }

                if (llAssignedClasses != null && assigned != null) {
                    llAssignedClasses.removeAllViews();
                    for (String cls : assigned) {
                        TextView chip = new TextView(requireContext());
                        chip.setText("Std " + cls);
                        chip.setBackgroundResource(R.drawable.bg_badge_saved);
                        chip.setPadding(36, 12, 36, 12);
                        chip.setTextSize(13f);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMarginEnd(10);
                        chip.setLayoutParams(params);
                        llAssignedClasses.addView(chip);
                    }
                }
            });
    }

    private void setupLogoutButton(View view) {
        View btnLogout = view.findViewById(R.id.btn_logout_profile);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseSource.getInstance().getAuth().signOut();
                requireActivity().getSharedPreferences("EduTrackPrefs",
                        requireActivity().MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(requireContext(), SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }
    }
}
