package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.edu.track.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateAnnouncementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_announcement);

        // Show today's date in posting date row
        TextView tvDate = findViewById(R.id.tv_posting_date);
        if (tvDate != null) {
            String date = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date());
            tvDate.setText(date);
        }

        setupAudienceSpinner();
        setupClickListeners();
    }

    private void setupAudienceSpinner() {
        String[] audiences = {"All", "Teachers Only", "Parents Only"};
        Spinner spinner = findViewById(R.id.spinner_audience);
        if (spinner != null) {
            spinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, audiences));
        }
    }

    private void setupClickListeners() {
        ImageView btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> onBackPressed());

        TextView btnPostHeader = findViewById(R.id.btn_post_header);
        if (btnPostHeader != null) btnPostHeader.setOnClickListener(v -> postAnnouncement());

        MaterialButton btnPost = findViewById(R.id.btn_post_announcement);
        if (btnPost != null) btnPost.setOnClickListener(v -> postAnnouncement());
    }

    private void postAnnouncement() {
        android.widget.EditText etTitle   = findViewById(R.id.et_title);
        android.widget.EditText etMessage = findViewById(R.id.et_message);
        SwitchCompat switchPin            = findViewById(R.id.switch_pin);
        Spinner spinnerAudience           = findViewById(R.id.spinner_audience);

        String title   = etTitle   != null ? etTitle.getText().toString().trim() : "";
        String message = etMessage != null ? etMessage.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (etTitle != null) etTitle.setError("Title is required");
            return;
        }
        if (message.isEmpty()) {
            if (etMessage != null) etMessage.setError("Message cannot be empty");
            return;
        }

        String audience = spinnerAudience != null
                ? spinnerAudience.getSelectedItem().toString() : "All";
        boolean pinned  = switchPin != null && switchPin.isChecked();

        java.util.Map<String, Object> announcement = new java.util.HashMap<>();
        announcement.put("title", title);
        announcement.put("content", message);
        announcement.put("audience", audience);
        announcement.put("isPinned", pinned);
        announcement.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        announcement.put("author", "Admin");

        com.edu.track.utils.FirebaseSource.getInstance().getFirestore()
                .collection("announcements")
                .add(announcement)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Announcement posted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show());
    }
}
