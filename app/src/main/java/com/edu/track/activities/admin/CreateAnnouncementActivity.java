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

    private boolean isTeacher = false;
    private String teacherUid = "";
    private android.widget.LinearLayout layoutClass, layoutSubject;
    private android.widget.Spinner spinnerClass, spinnerSubject, spinnerAudience, spinnerCategory;
    private android.widget.TextView tvAudienceLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_announcement);

        isTeacher = getIntent().getBooleanExtra("is_teacher", false);
        teacherUid = com.edu.track.utils.FirebaseSource.getInstance().getAuth().getUid();

        layoutClass = findViewById(R.id.layout_target_class);
        layoutSubject = findViewById(R.id.layout_target_subject);
        spinnerClass = findViewById(R.id.spinner_class);
        spinnerSubject = findViewById(R.id.spinner_subject);
        spinnerAudience = findViewById(R.id.spinner_audience);
        spinnerCategory = findViewById(R.id.spinner_category);
        tvAudienceLabel = findViewById(R.id.tv_audience_label);

        // Show today's date in posting date row
        TextView tvDate = findViewById(R.id.tv_posting_date);
        if (tvDate != null) {
            String date = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date());
            tvDate.setText(date);
        }

        setupCategorySpinner();
        if (isTeacher) {
            setupTeacherUI();
        } else {
            setupAudienceSpinner();
        }
        setupClickListeners();
    }

    private void setupTeacherUI() {
        if (layoutClass != null) layoutClass.setVisibility(android.view.View.VISIBLE);
        if (layoutSubject != null) layoutSubject.setVisibility(android.view.View.VISIBLE);
        if (spinnerAudience != null) spinnerAudience.setVisibility(android.view.View.GONE);
        if (tvAudienceLabel != null) tvAudienceLabel.setVisibility(android.view.View.GONE);

        if (teacherUid == null) return;
        com.edu.track.utils.FirebaseSource.getInstance().getTeachersRef().document(teacherUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                java.util.List<String> classes = (java.util.List<String>) doc.get("assignedClasses");
                java.util.List<String> subjects = (java.util.List<String>) doc.get("subjects");
                
                if (classes == null) classes = new java.util.ArrayList<>();
                if (subjects == null) subjects = new java.util.ArrayList<>();
                
                if (spinnerClass != null) {
                    spinnerClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classes));
                }
                if (spinnerSubject != null) {
                    spinnerSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
                }
            }
        });
    }

    private void setupAudienceSpinner() {
        String[] audiences = {"All", "Teachers Only", "Parents Only"};
        Spinner spinner = findViewById(R.id.spinner_audience);
        if (spinner != null) {
            spinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, audiences));
        }
    }

    private void setupCategorySpinner() {
        String[] categories = {"All", "Academic", "Events", "Sports"};
        if (spinnerCategory != null) {
            spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, categories));
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

        boolean pinned  = switchPin != null && switchPin.isChecked();

        java.util.Map<String, Object> announcement = new java.util.HashMap<>();
        announcement.put("title", title);
        announcement.put("content", message);
        announcement.put("isPinned", pinned);
        announcement.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        String category = spinnerCategory != null ? spinnerCategory.getSelectedItem().toString() : "All";
        announcement.put("category", category);

        if (isTeacher) {
            announcement.put("author", teacherUid);
            announcement.put("audience", "Class"); // Simplified internal audience tag
            announcement.put("isSubjectLevel", true);
            if (spinnerClass != null && spinnerClass.getSelectedItem() != null) {
                announcement.put("classId", spinnerClass.getSelectedItem().toString());
            }
            if (spinnerSubject != null && spinnerSubject.getSelectedItem() != null) {
                announcement.put("subjectId", spinnerSubject.getSelectedItem().toString());
            }
        } else {
            announcement.put("author", "ADMIN");
            String audience = spinnerAudience != null ? spinnerAudience.getSelectedItem().toString() : "All";
            announcement.put("audience", audience);
            announcement.put("isSubjectLevel", false);
        }

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
