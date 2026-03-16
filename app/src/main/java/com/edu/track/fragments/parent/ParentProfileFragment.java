package com.edu.track.fragments.parent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.edu.track.R;
import com.edu.track.activities.SplashActivity;
import com.edu.track.utils.FirebaseSource;
import com.google.firebase.auth.FirebaseUser;

public class ParentProfileFragment extends Fragment {

    private TextView tvParentName, tvParentEmail, tvLinkedChild;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_profile, container, false);

        tvParentName = view.findViewById(R.id.tv_parent_name);
        tvParentEmail = view.findViewById(R.id.tv_parent_email);
        tvLinkedChild = view.findViewById(R.id.tv_linked_child);

        loadParentProfile();

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> logout());

        return view;
    }

    private void loadParentProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String email = user.getEmail();

            if (tvParentEmail != null && email != null) {
                tvParentEmail.setText(email);
            }

            FirebaseSource.getInstance().getParentsRef().document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        String name = doc.getString("name");
                        if (tvParentName != null && name != null) {
                            tvParentName.setText(name);
                        }
                    }
                });

            // Fetch linked child
            FirebaseSource.getInstance().getFirestore().collection("students")
                .whereEqualTo("parentEmail", email).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (isAdded() && tvLinkedChild != null) {
                        if (!snap.isEmpty()) {
                            com.google.firebase.firestore.QueryDocumentSnapshot doc = snap.iterator().next();
                            String name = doc.getString("name");
                            String std = doc.getString("standard");
                            String div = doc.getString("division");
                            tvLinkedChild.setText(name + " (Std " + std + div + ")");
                        } else {
                            FirebaseSource.getInstance().getFirestore().collection("students")
                                .whereEqualTo("parentUid", email).limit(1).get()
                                .addOnSuccessListener(snap2 -> {
                                    if (isAdded() && tvLinkedChild != null) {
                                        if (!snap2.isEmpty()) {
                                            com.google.firebase.firestore.QueryDocumentSnapshot doc = snap2.iterator().next();
                                            String name = doc.getString("name");
                                            String std = doc.getString("standard");
                                            String div = doc.getString("division");
                                            tvLinkedChild.setText(name + " (Std " + std + div + ")");
                                        } else {
                                            tvLinkedChild.setText("No child linked");
                                        }
                                    }
                                });
                        }
                    }
                });
        }
    }

    private void logout() {
        if (getContext() == null) return;
        FirebaseSource.getInstance().getAuth().signOut();
        getContext().getSharedPreferences("EduTrackPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(getContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
