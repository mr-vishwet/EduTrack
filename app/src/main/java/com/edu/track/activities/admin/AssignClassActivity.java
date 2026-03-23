package com.edu.track.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.GlobalDialog;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
    private android.widget.Spinner spinnerClassTeacher;
    private android.widget.ProgressBar progressBar;
    private String initialClassTeacher = null;

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
        spinnerClassTeacher = findViewById(R.id.spinner_class_teacher);
        progressBar = findViewById(R.id.progress_bar);

        if (tvTeacherName != null) tvTeacherName.setText(teacherName != null ? teacherName : "Teacher");

        setupClickListeners();
        loadAllData();
    }

    private void loadAllData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        Task<com.google.firebase.firestore.QuerySnapshot> classesTask = db.collection("classes").orderBy("standard").get();
        Task<com.google.firebase.firestore.QuerySnapshot> subjectsTask = db.collection("subjects").orderBy("name").get();
        Task<com.google.firebase.firestore.DocumentSnapshot> existingTask = db.collection("teachers").document(teacherUid).get();

        Tasks.whenAllSuccess(classesTask, subjectsTask, existingTask).addOnSuccessListener(results -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            
            com.google.firebase.firestore.QuerySnapshot classSnap = (com.google.firebase.firestore.QuerySnapshot) results.get(0);
            com.google.firebase.firestore.QuerySnapshot subjectSnap = (com.google.firebase.firestore.QuerySnapshot) results.get(1);
            com.google.firebase.firestore.DocumentSnapshot teacherDoc = (com.google.firebase.firestore.DocumentSnapshot) results.get(2);

            // 1. Process Classes
            allClassIds.clear();
            for (QueryDocumentSnapshot doc : classSnap) {
                String std = doc.getString("standard");
                String div = doc.getString("division");
                if (std != null && div != null) {
                    allClassIds.add(std + div);
                }
            }

            // 2. Process Subjects
            allSubjects.clear();
            for (QueryDocumentSnapshot doc : subjectSnap) {
                String id = doc.getId();
                String name = doc.getString("name");
                if (name != null) {
                    allSubjects.add(new String[]{id, name});
                }
            }

            // 3. Process Existing Assignment
            if (teacherDoc.exists()) {
                List<String> existingClasses = (List<String>) teacherDoc.get("assignedClasses");
                if (existingClasses != null) {
                    selectedClasses.clear();
                    selectedClasses.addAll(existingClasses);
                }
                
                // Check both "subjectIds", "subjects", and "expertise" for robust pre-selection
                List<String> sIds = (List<String>) teacherDoc.get("subjectIds");
                List<String> sNames = (List<String>) teacherDoc.get("subjects");
                String expertise = teacherDoc.getString("expertise");
                
                selectedSubjects.clear();
                // 1. Add direct IDs
                if (sIds != null) {
                    for (String id : sIds) if (!selectedSubjects.contains(id)) selectedSubjects.add(id);
                }
                
                // 2. Add by matching names (from 'subjects' array or 'expertise' field)
                List<String> namesToMatch = new ArrayList<>();
                if (sNames != null) namesToMatch.addAll(sNames);
                if (expertise != null) namesToMatch.add(expertise);
                
                for (String nameOrId : namesToMatch) {
                    for (String[] s : allSubjects) {
                        String id = s[0];
                        String name = s[1];
                        if (id.equalsIgnoreCase(nameOrId) || name.equalsIgnoreCase(nameOrId)) {
                            if (!selectedSubjects.contains(id)) selectedSubjects.add(id);
                            break;
                        }
                    }
                }
                initialClassTeacher = teacherDoc.getString("classTeacher");
            }

            renderClassChips();
            renderSubjectChips();
            updateClassTeacherSpinner();
        }).addOnFailureListener(e -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void renderClassChips() {
        if (chipGroupClasses == null) return;
        chipGroupClasses.removeAllViews();

        for (String classId : allClassIds) {
            Chip chip = new Chip(this);
            String std = classId.replaceAll("[^0-9]", "");
            String div = classId.replaceAll("[^A-Za-z]", "");
            chip.setText("Std " + std + " - " + div);
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
                updateClassTeacherSpinner();
            });

            chipGroupClasses.addView(chip);
        }
        updateClassTeacherSpinner();
    }

    private void updateClassTeacherSpinner() {
        if (spinnerClassTeacher == null) return;
        
        List<String> options = new ArrayList<>();
        options.add("None");
        for (String c : selectedClasses) {
            String std = c.replaceAll("[^0-9]", "");
            String div = c.replaceAll("[^A-Za-z]", "");
            options.add("Std " + std + " - " + div);
        }
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options);
        spinnerClassTeacher.setAdapter(adapter);
        
        if (initialClassTeacher != null) {
            String std = initialClassTeacher.replaceAll("[^0-9]", "");
            String div = initialClassTeacher.replaceAll("[^A-Za-z]", "");
            String target = "Std " + std + " - " + div;
            int pos = options.indexOf(target);
            if (pos >= 0) spinnerClassTeacher.setSelection(pos);
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
        if (btnSave != null) btnSave.setOnClickListener(v -> checkClashesAndSave());
    }

    private void checkClashesAndSave() {
        if (teacherUid == null) {
            Toast.makeText(this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedClasses.isEmpty() || selectedSubjects.isEmpty()) {
            Toast.makeText(this, "Please select at least one class and one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clash Detection: Check if any other teacher has any of these classes AND subjects
        db.collection("teachers")
                .whereArrayContainsAny("assignedClasses", selectedClasses)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> clashedDetails = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getId().equals(teacherUid)) continue;
                        
                        List<String> otherSubjects = (List<String>) doc.get("subjectIds");
                        List<String> otherClasses = (List<String>) doc.get("assignedClasses");
                        
                        if (otherSubjects != null && otherClasses != null) {
                            for (String subj : selectedSubjects) {
                                if (otherSubjects.contains(subj)) {
                                    // Potential clash, check which classes overlap
                                    for (String cls : selectedClasses) {
                                        if (otherClasses.contains(cls)) {
                                            String subjName = getSubjectName(subj);
                                            clashedDetails.add(doc.getString("name") + " (Std " + cls + " - " + subjName + ")");
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!clashedDetails.isEmpty()) {
                        StringBuilder sb = new StringBuilder("The following assignments already exist:\n");
                        for (String d : clashedDetails) sb.append("• ").append(d).append("\n");
                        sb.append("\nDo you want to reassign them to " + teacherName + "?");

                        GlobalDialog.showWarning(this, "Assignment Clash", sb.toString(), new GlobalDialog.DialogCallback() {
                            @Override
                            public void onConfirm() {
                                checkClassTeacherClashAndSave();
                            }
                        });
                    } else {
                        checkClassTeacherClashAndSave();
                    }
                });
    }

    private void checkClassTeacherClashAndSave() {
        String classTeacherSelection = spinnerClassTeacher.getSelectedItem().toString();
        if ("None".equals(classTeacherSelection)) {
            saveAssignment(null);
            return;
        }

        // e.g., "Std 8 - A" -> "8A"
        String clean = classTeacherSelection.replace("Std ", "");
        String[] parts = clean.split(" - ");
        if (parts.length != 2) {
            saveAssignment(null);
            return;
        }
        
        String targetClassId = parts[0] + parts[1];

        db.collection("teachers")
                .whereEqualTo("classTeacher", targetClassId)
                .get()
                .addOnSuccessListener(snap -> {
                    String otherUid = null;
                    String otherName = null;
                    for (QueryDocumentSnapshot doc : snap) {
                        if (!doc.getId().equals(teacherUid)) {
                            otherUid = doc.getId();
                            otherName = doc.getString("name");
                            break;
                        }
                    }

                    if (otherUid != null) {
                        final String finalOtherUid = otherUid;
                        GlobalDialog.showWarning(this, "Class Teacher Clash", 
                            otherName + " is already the Class Teacher for Std " + targetClassId + ".\n\n" +
                            "Do you want to transfer this role to " + teacherName + "?", new GlobalDialog.DialogCallback() {
                                @Override
                                public void onConfirm() {
                                    saveAssignment(finalOtherUid);
                                }
                            });
                    } else {
                        saveAssignment(null);
                    }
                });
    }

    private String getSubjectName(String id) {
        for (String[] s : allSubjects) if (s[0].equals(id)) return s[1];
        return id;
    }

    private void saveAssignment(String previousTeacherUidToUnset) {
        Map<String, Object> update = new HashMap<>();
        update.put("assignedClasses", selectedClasses);
        update.put("subjectIds", selectedSubjects);

        String classTeacherSelection = spinnerClassTeacher.getSelectedItem().toString();
        if ("None".equals(classTeacherSelection)) {
            update.put("classTeacher", "");
        } else {
            String clean = classTeacherSelection.replace("Std ", "");
            String[] parts = clean.split(" - ");
            if (parts.length == 2) {
                update.put("classTeacher", parts[0] + parts[1]);
            }
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Task<Void> mainSave = db.collection("teachers").document(teacherUid).update(update);
        
        if (previousTeacherUidToUnset != null) {
            Task<Void> unsetPrevious = db.collection("teachers").document(previousTeacherUidToUnset).update("classTeacher", "");
            Tasks.whenAllComplete(mainSave, unsetPrevious).addOnCompleteListener(task -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Classes & Subjects assigned and roles adjusted!", Toast.LENGTH_SHORT).show();
                finish();
            });
        } else {
            mainSave.addOnSuccessListener(aVoid -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Classes & Subjects assigned successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}
