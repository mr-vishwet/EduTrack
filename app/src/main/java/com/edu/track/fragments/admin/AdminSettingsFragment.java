package com.edu.track.fragments.admin;

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
import com.edu.track.activities.SplashActivity;
import com.edu.track.utils.FirebaseSource;
import com.google.firebase.auth.FirebaseUser;

public class AdminSettingsFragment extends Fragment {

    private TextView tvName, tvEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_settings, container, false);
        tvName = view.findViewById(R.id.tv_admin_name);
        tvEmail = view.findViewById(R.id.tv_admin_email);
        
        loadProfile();
        
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseSource.getInstance().getAuth().signOut();
            if (getActivity() != null) {
                getActivity().getSharedPreferences("EduTrackPrefs", android.content.Context.MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });
        return view;
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());
            FirebaseSource.getInstance().getUsersRef().document(user.getUid())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists() && tvName != null) {
                            tvName.setText(doc.getString("name"));
                        }
                    });
        }
    }
}
