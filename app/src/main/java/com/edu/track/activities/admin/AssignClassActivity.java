package com.edu.track.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignClassActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String teacherUid, teacherName;
    
    // Class selection
    private ChipGroup chipGroupClasses;
    private final List<String> selectedClasses = new ArrayList<>();
    private final List<String> allClassIds = new ArrayList<>();
    
    // Subject selection
    private ChipGroup chipGroupSubjects;
    private final List<String> selectedSubjects = new ArrayList<>();
    private final List<String[]> allSubjects = new ArrayList<>(); // [id, name]

    private TextView tvTeacherName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_class);

        db = FirebaseSource.getInstance().getFirestore();
        teacherUid = getIntent().getStringExtra("teacher_uid");
        teacherName = getIntent().getStringExtra("teacher_name");

        tvTeacherName = findViewById(R.id.tv_teacher_name);
        chipGroupClasses = findViewById(R.id.chip_group_classes);
        chipGroupSubjects = findViewById(R.id.chip_group_subjects);

        if (tvTeacherName != null) tvTeacherName.setText(teacherName != null ? teacherName : "Teacher");

        setupClickListeners();
        loadExistingAssignment();
        loadClasses();
        loadSubjects();
    }

    private void loadExistingAssignment() {
        if (teacherUid == null) return;
        db.collection("teachers").document(teacherUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Load Classes
                        List<String> existingClasses = (List<String>) doc.get("assignedClasses");
                        if (existingClasses != null) {
                            selectedClasses.clear();
                            selectedClasses.addAll(existingClasses);
                        }
                        // Load Subjects
                        List<String> existingSubjects = (List<String>) doc.get("subjectIds");
                        if (existingSubjects != null) {
                            selectedSubjects.clear();
                            selectedSubjects.addAll(existingSubjects);
                        }
                        
                        renderClassChips();
                        renderSubjectChips();
                    }
                });
    }

    private void loadClasses() {
        db.collection("classes")
                .orderBy("standard")
                .get()
                .addOnSuccessListener(snap -> {
                    allClassIds.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        String std = doc.getString("standard");
                        String div = doc.getString("division");
                        if (std != null && div != null) {
                            allClassIds.add(std + div);
                        }
                    }
                    renderClassChips();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load classes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadSubjects() {
        db.collection("subjects")
                .orderBy("name")
                .get()
                .addOnSuccessListener(snap -> {
                    allSubjects.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        if (name != null) {
                            allSubjects.add(new String[]{id, name});
                        }
                    }
                    renderSubjectChips();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void renderClassChips() {
        if (chipGroupClasses == null) return;
        chipGroupClasses.removeAllViews();

        for (String classId : allClassIds) {
            Chip chip = new Chip(this);
            chip.setText("Std " + classId.replaceAll("[A-Za-z]", "") + " - " + classId.replaceAll("[0-9]", ""));
            chip.setCheckable(true);
            chip.setChecked(selectedClasses.contains(classId));
            chip.setTag(classId);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String id = (String) buttonView.getTag();
                if (isChecked) {
                    if (!selectedClasses.contains(id)) selectedClasses.add(id);
                } else {
                    selectedClasses.remove(id);
                }
            });

            chipGroupClasses.addView(chip);
        }
    }

    private void renderSubjectChips() {
        if (chipGroupSubjects == null) return;
        chipGroupSubjects.removeAllViews();

        for (String[] subject : allSubjects) {
            String id = subject[0];
            String name = subject[1];
            
            Chip chip = new Chip(this);
            chip.setText(name);
            chip.setCheckable(true);
            chip.setChecked(selectedSubjects.contains(id));
            chip.setTag(id);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String subjId = (String) buttonView.getTag();
                if (isChecked) {
                    if (!selectedSubjects.contains(subjId)) selectedSubjects.add(subjId);
                } else {
                    selectedSubjects.remove(subjId);
                }
            });

            chipGroupSubjects.addView(chip);
        }
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        MaterialButton btnSave = findViewById(R.id.btn_save);
        if (btnSave != null) btnSave.setOnClickListener(v -> saveAssignment());
    }

    private void saveAssignment() {
        if (teacherUid == null) {
            Toast.makeText(this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("assignedClasses", selectedClasses);
        update.put("subjectIds", selectedSubjects);

        db.collection("teachers").document(teacherUid)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Classes & Subjects assigned successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
