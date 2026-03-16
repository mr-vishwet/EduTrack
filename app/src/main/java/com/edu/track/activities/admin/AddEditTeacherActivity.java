package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class AddEditTeacherActivity extends AppCompatActivity {

    private boolean isEditMode = false;
    private String teacherUid;
    private EditText etName, etEmail, etExpertise;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_teacher);

        db = FirebaseSource.getInstance().getFirestore();
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);
        teacherUid = getIntent().getStringExtra("teacher_uid");

        initViews();
        setupClickListeners();

        if (isEditMode && teacherUid != null) {
            loadTeacherData();
        }
    }

    private void initViews() {
        etName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etExpertise = findViewById(R.id.et_expertise);

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) tvTitle.setText(isEditMode ? "Edit Teacher" : "Add Teacher");

        MaterialButton btnAction = findViewById(R.id.btn_action);
        if (btnAction != null) btnAction.setText(isEditMode ? "UPDATE TEACHER" : "ADD TEACHER");
    }

    private void loadTeacherData() {
        FirebaseSource.getInstance().getTeachersRef().document(teacherUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etName.setText(doc.getString("name"));
                etEmail.setText(doc.getString("email"));
                etExpertise.setText(doc.getString("expertise"));
                etEmail.setEnabled(false); // Can't change email usually
            }
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_close).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_save_header).setOnClickListener(v -> saveTeacher());
        findViewById(R.id.btn_action).setOnClickListener(v -> saveTeacher());

        etName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (!isEditMode && s.length() > 0) {
                    String[] parts = s.toString().trim().split("\\s+");
                    if (parts.length >= 2) {
                        String suggested = parts[0].toLowerCase() + "." + parts[1].toLowerCase() + "@edutrack.com";
                        etEmail.setText(suggested);
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void saveTeacher() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String expertise = etExpertise.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.endsWith("@edutrack.com")) {
            etEmail.setError("Email must end with @edutrack.com");
            return;
        }

        Map<String, Object> teacherMap = new HashMap<>();
        teacherMap.put("name", name);
        teacherMap.put("email", email);
        teacherMap.put("expertise", expertise);
        teacherMap.put("role", "TEACHER");
        teacherMap.put("isActive", true);

        if (!isEditMode) {
            teacherUid = "T_" + System.currentTimeMillis();
            teacherMap.put("uid", teacherUid);
        }

        db.collection("teachers").document(teacherUid)
                .set(teacherMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Sync with users collection for login routing
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", teacherUid);
                    userMap.put("role", "TEACHER");
                    userMap.put("email", email);
                    
                    db.collection("users").document(teacherUid).set(userMap, com.google.firebase.firestore.SetOptions.merge());

                    Toast.makeText(this, isEditMode ? "Teacher updated" : "Teacher added", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }
}
