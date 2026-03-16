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

public class AddEditClassActivity extends AppCompatActivity {

    private boolean isEditMode = false;
    private String classId;
    private EditText etStandard, etDivision;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_class);

        db = FirebaseSource.getInstance().getFirestore();
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);
        classId = getIntent().getStringExtra("class_id");

        etStandard = findViewById(R.id.et_standard);
        etDivision = findViewById(R.id.et_division);

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) tvTitle.setText(isEditMode ? "Edit Class" : "Add Class");

        MaterialButton btnAction = findViewById(R.id.btn_action);
        if (btnAction != null) btnAction.setText(isEditMode ? "UPDATE CLASS" : "ADD CLASS");

        if (isEditMode && classId != null) {
            loadClassData();
        }

        findViewById(R.id.btn_close).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_save_header).setOnClickListener(v -> saveClass());
        findViewById(R.id.btn_action).setOnClickListener(v -> saveClass());
    }

    private void loadClassData() {
        db.collection("classes").document(classId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etStandard.setText(doc.getString("standard"));
                etDivision.setText(doc.getString("division"));
                etStandard.setEnabled(false);
                etDivision.setEnabled(false);
            }
        });
    }

    private void saveClass() {
        String std = etStandard.getText().toString().trim();
        String div = etDivision.getText().toString().trim();

        if (std.isEmpty() || div.isEmpty()) {
            Toast.makeText(this, "Standard and Division are required", Toast.LENGTH_SHORT).show();
            return;
        }

        classId = std + div;
        Map<String, Object> classMap = new HashMap<>();
        classMap.put("standard", std);
        classMap.put("division", div);
        classMap.put("studentCount", 0); // Initial

        db.collection("classes").document(classId)
                .set(classMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isEditMode ? "Class updated" : "Class created! Add students now.", Toast.LENGTH_SHORT).show();
                    if (!isEditMode) {
                        // Navigate straight to ManageStudentsActivity for this new class
                        android.content.Intent intent = new android.content.Intent(this, ManageStudentsActivity.class);
                        intent.putExtra("standard", std);
                        intent.putExtra("division", div);
                        startActivity(intent);
                    }
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
