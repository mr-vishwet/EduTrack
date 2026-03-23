package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditStudentActivity extends AppCompatActivity {

    private boolean isEditMode = false;
    private String studentId;
    private android.widget.EditText etName, etRoll, etPassword, etPhone, etDob, etBdayUrl;
    private Spinner spinnerStd, spinnerDiv;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_student);

        db = FirebaseSource.getInstance().getFirestore();
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);
        studentId = getIntent().getStringExtra("student_id");

        initViews();
        setupSpinners();
        setupClickListeners();

        if (isEditMode && studentId != null) {
            loadStudentData();
        }
    }

    private void initViews() {
        etName = findViewById(R.id.et_full_name);
        etRoll = findViewById(R.id.et_roll_number);
        etPassword = findViewById(R.id.et_password);
        etPhone = findViewById(R.id.et_phone);
        etDob = findViewById(R.id.et_dob);
        etBdayUrl = findViewById(R.id.et_bday_url);
        spinnerStd = findViewById(R.id.spinner_standard);
        spinnerDiv = findViewById(R.id.spinner_division);

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) tvTitle.setText(isEditMode ? "Edit Student" : "Add Student");
        
        MaterialButton btnAction = findViewById(R.id.btn_add_student);
        if (btnAction != null) btnAction.setText(isEditMode ? "UPDATE STUDENT" : "ADD STUDENT");
    }

    private void setupSpinners() {
        String[] standards = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"};
        String[] divisions  = {"A", "B", "C"};

        if (spinnerStd != null)
            spinnerStd.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, standards));
        if (spinnerDiv != null)
            spinnerDiv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, divisions));
    }

    private void loadStudentData() {
        db.collection("students").document(studentId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (etName != null) etName.setText(doc.getString("name"));
                if (etRoll != null) etRoll.setText(String.valueOf(doc.get("rollNumber")));
                if (etDob != null) etDob.setText(doc.getString("dob"));
                if (etBdayUrl != null) etBdayUrl.setText(doc.getString("birthdayCertificateUrl"));
                
                String std = doc.getString("standard");
                String div = doc.getString("division");
                
                // Set spinner selections
                setSpinnerValue(spinnerStd, std);
                setSpinnerValue(spinnerDiv, div);
            }
        });
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (spinner == null || value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupClickListeners() {
        if (findViewById(R.id.btn_close) != null) findViewById(R.id.btn_close).setOnClickListener(v -> onBackPressed());
        if (findViewById(R.id.btn_save_header) != null) findViewById(R.id.btn_save_header).setOnClickListener(v -> saveStudent());
        if (findViewById(R.id.btn_add_student) != null) findViewById(R.id.btn_add_student).setOnClickListener(v -> saveStudent());
    }

    private void saveStudent() {
        String name = etName != null ? etName.getText().toString().trim() : "";
        String rollStr = etRoll != null ? etRoll.getText().toString().trim() : "";
        String std = spinnerStd != null ? spinnerStd.getSelectedItem().toString() : "";
        String div = spinnerDiv != null ? spinnerDiv.getSelectedItem().toString() : "";

        if (name.isEmpty()) { etName.setError("Required"); return; }
        if (rollStr.isEmpty()) { etRoll.setError("Required"); return; }

        int roll = Integer.parseInt(rollStr);
        
        java.util.Map<String, Object> studentMap = new java.util.HashMap<>();
        studentMap.put("name", name);
        studentMap.put("rollNumber", roll);
        studentMap.put("standard", std);
        studentMap.put("division", div);
        studentMap.put("dob", etDob.getText().toString().trim());
        studentMap.put("birthdayCertificateUrl", etBdayUrl.getText().toString().trim());
        studentMap.put("isActive", true);

        if (!isEditMode) {
            studentId = "STU_" + System.currentTimeMillis();
            studentMap.put("studentId", studentId);
        }

        db.collection("students").document(studentId)
                .set(studentMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isEditMode ? "Updated successfully" : "Added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }
}
