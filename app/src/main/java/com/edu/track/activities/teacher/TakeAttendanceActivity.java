package com.edu.track.activities.teacher;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.edu.track.R;
import com.google.android.material.button.MaterialButton;

import com.edu.track.utils.FirebaseSource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TakeAttendanceActivity extends AppCompatActivity {

    private GridLayout gridAttendance;
    private TextView tvClassInfo, tvSummary;
    private MaterialButton btnMarkAllPresent, btnSameAsYesterday, btnSave;

    private String standard, division;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private List<com.edu.track.models.Student> studentList = new ArrayList<>();
    private java.util.Map<String, Boolean> attendanceMap = new java.util.HashMap<>();
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_attendance);

        db = FirebaseSource.getInstance().getFirestore();
        
        standard = getIntent().getStringExtra("standard");
        division = getIntent().getStringExtra("division");

        gridAttendance = findViewById(R.id.grid_attendance);
        tvClassInfo    = findViewById(R.id.tv_class_info);
        tvSummary      = findViewById(R.id.tv_summary);
        btnMarkAllPresent   = findViewById(R.id.btn_mark_all_present);
        btnSameAsYesterday  = findViewById(R.id.btn_same_as_yesterday);
        btnSave             = findViewById(R.id.btn_save_attendance);
        shimmerView         = findViewById(R.id.shimmer_view_container);

        // Set info bar
        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        if (tvClassInfo != null) tvClassInfo.setText((standard != null ? standard : "8") + division + " · Attendance · " + today);

        fetchStudents();
        setupClickListeners();
    }

    private void fetchStudents() {
        if (shimmerView != null) {
            shimmerView.setVisibility(android.view.View.VISIBLE);
            shimmerView.startShimmer();
        }
        gridAttendance.setVisibility(android.view.View.GONE);

        db.collection("students")
                .whereEqualTo("standard", standard != null ? standard : "8")
                .whereEqualTo("division", division != null ? division : "A")
                .orderBy("rollNumber")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(android.view.View.GONE);
                    }
                    gridAttendance.setVisibility(android.view.View.VISIBLE);
                    
                    studentList.clear();
                    studentList.addAll(queryDocumentSnapshots.toObjects(com.edu.track.models.Student.class));
                    
                    attendanceMap.clear();
                    for (com.edu.track.models.Student s : studentList) {
                        attendanceMap.put(s.getStudentId(), true); // Default all present
                    }
                    
                    buildGrid();
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch students", Toast.LENGTH_SHORT).show();
                });
    }

    private void buildGrid() {
        if (gridAttendance == null) return;
        gridAttendance.removeAllViews();

        int cellSize = (int) (getResources().getDisplayMetrics().density * 44); // 44dp

        for (com.edu.track.models.Student student : studentList) {
            TextView cell = new TextView(this);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = cellSize;
            params.height = cellSize;
            params.setMargins(6, 6, 6, 6);
            cell.setLayoutParams(params);

            String rollStr = String.valueOf(student.getRollNumber());
            if (rollStr.length() == 1) rollStr = "0" + rollStr;
            
            cell.setText(rollStr);
            cell.setGravity(Gravity.CENTER);
            cell.setTextColor(Color.WHITE);
            cell.setTextSize(14f);
            cell.setTypeface(null, android.graphics.Typeface.BOLD);

            boolean isPresent = attendanceMap.get(student.getStudentId());
            applyCellBackground(cell, isPresent);

            cell.setOnClickListener(v -> {
                boolean current = attendanceMap.get(student.getStudentId());
                attendanceMap.put(student.getStudentId(), !current);
                applyCellBackground(cell, !current);
                updateSummary();
            });

            gridAttendance.addView(cell);
        }
    }

    private void applyCellBackground(TextView cell, boolean present) {
        if (present) {
            cell.setBackgroundResource(R.drawable.bg_cell_present);
        } else {
            cell.setBackgroundResource(R.drawable.bg_cell_absent);
        }
    }

    private void updateSummary() {
        if (tvSummary == null) return;
        int presentCount = 0;
        for (Boolean status : attendanceMap.values()) {
            if (status) presentCount++;
        }
        int total = studentList.size();
        int absentCount = total - presentCount;
        tvSummary.setText("Present: " + presentCount + "  |  Absent: " + absentCount);
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (btnMarkAllPresent != null) {
            btnMarkAllPresent.setOnClickListener(v -> {
                for (String id : attendanceMap.keySet()) {
                    attendanceMap.put(id, true);
                }
                buildGrid();
                updateSummary();
            });
        }

        if (btnSameAsYesterday != null) {
            btnSameAsYesterday.setOnClickListener(v -> {
                // Placeholder: In a real app, this would fetch yesterday's record
                Toast.makeText(this, "Loading yesterday's data...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveAttendance());
        }
    }

    private void saveAttendance() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        java.util.Map<String, Object> record = new java.util.HashMap<>();
        record.put("date", today);
        record.put("standard", standard);
        record.put("division", division);
        record.put("timestamp", com.google.firebase.Timestamp.now());
        
        java.util.Map<String, Boolean> statuses = new java.util.HashMap<>(attendanceMap);
        record.put("statuses", statuses);

        db.collection("attendance_records")
                .document(today + "_" + standard + division)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attendance saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving attendance", Toast.LENGTH_SHORT).show();
                });
    }
}
