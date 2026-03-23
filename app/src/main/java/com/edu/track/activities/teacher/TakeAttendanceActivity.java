package com.edu.track.activities.teacher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

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
    private TextView tvClassInfo, tvSummary, tvMode;
    private MaterialButton btnMarkAllPresent, btnSameAsYesterday, btnSave;

    private String standard, division;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private List<com.edu.track.models.Student> studentList = new ArrayList<>();
    private java.util.Map<String, Boolean> attendanceMap = new java.util.HashMap<>();
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;

    /** View-only because teacher is not class teacher for this class. */
    private boolean isViewOnly = false;
    /** Today's attendance was already submitted. */
    private boolean isAlreadySubmitted = false;

    private String classTeacher = "";
    private final String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_attendance);

        db = FirebaseSource.getInstance().getFirestore();

        standard = getIntent().getStringExtra("standard");
        division = getIntent().getStringExtra("division");
        // Allow explicit view-only override from caller
        isViewOnly = getIntent().getBooleanExtra("view_only", false);

        gridAttendance = findViewById(R.id.grid_attendance);
        tvClassInfo    = findViewById(R.id.tv_class_info);
        tvSummary      = findViewById(R.id.tv_summary);
        tvMode         = findViewById(R.id.tv_mode_label);
        btnMarkAllPresent   = findViewById(R.id.btn_mark_all_present);
        btnSameAsYesterday  = findViewById(R.id.btn_same_as_yesterday);
        btnSave             = findViewById(R.id.btn_save_attendance);
        shimmerView         = findViewById(R.id.shimmer_view_container);

        String displayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        if (tvClassInfo != null)
            tvClassInfo.setText((standard != null ? standard : "8") + division + " · Attendance · " + displayDate);

        checkPermissionsAndLoad();
    }

    /** Step 1: Determine if this teacher is the class-teacher for this specific class. */
    private void checkPermissionsAndLoad() {
        com.google.firebase.auth.FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user == null) {
            // No user — don't allow marking
            isViewOnly = true;
            checkTodayAttendance();
            return;
        }

        db.collection("teachers").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    classTeacher = doc.getString("classTeacher");
                    if (classTeacher == null) classTeacher = "";

                    // Normalize both sides: strip spaces/special chars, uppercase
                    String normalizedCT = classTeacher.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                    String normalizedStd = (standard != null ? standard : "").trim();
                    String normalizedDiv = (division != null ? division : "").trim().toUpperCase();
                    String targetClass = normalizedStd + normalizedDiv;

                    // Only override if NOT already forced view-only by caller
                    if (!isViewOnly) {
                        isViewOnly = !(targetClass.equalsIgnoreCase(normalizedCT));
                    }
                } else {
                    // No teacher document found — default to view-only
                    isViewOnly = true;
                }
                checkTodayAttendance();
            })
            .addOnFailureListener(e -> {
                // Network/DB error — DON'T default to view-only, let the teacher try
                // They'll be able to exit if something is wrong
                isViewOnly = false;
                checkTodayAttendance();
            });
    }

    /** Step 2: Check whether today's attendance is already submitted AND who submitted it. */
    private void checkTodayAttendance() {
        String docId = todayDate + "_" + standard + division;
        com.google.firebase.auth.FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        String currentUid = user != null ? user.getUid() : "";

        db.collection("attendance_records").document(docId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String submittedBy = doc.getString("submittedBy");
                
                // Already submitted by THIS class teacher — they can still view it
                // But if submitted by someone else (edge case), just view-only
                isAlreadySubmitted = true;

                // Pre-load saved attendance data
                java.util.Map<String, Boolean> saved = (java.util.Map<String, Boolean>) doc.get("statuses");
                if (saved != null) attendanceMap.putAll(saved);
            } else {
                isAlreadySubmitted = false;
            }
            updateUIForMode();
            fetchStudents();
        }).addOnFailureListener(e -> {
            isAlreadySubmitted = false;
            updateUIForMode();
            fetchStudents();
        });
    }

    private void updateUIForMode() {
        if (isAlreadySubmitted) {
            // MODE: View already-submitted attendance
            if (tvMode != null) {
                tvMode.setVisibility(View.VISIBLE);
                tvMode.setText("✓ Attendance Already Submitted — Long-press a student to view details");
            }
            if (btnSave != null) btnSave.setVisibility(View.GONE);
            if (btnMarkAllPresent != null) btnMarkAllPresent.setVisibility(View.GONE);
            if (btnSameAsYesterday != null) btnSameAsYesterday.setVisibility(View.GONE);
            if (tvClassInfo != null) {
                String current = tvClassInfo.getText().toString();
                tvClassInfo.setText(current.replace("Attendance", "View Attendance"));
            }
        } else if (isViewOnly) {
            // MODE: View-only for non-class-teacher
            if (tvMode != null) {
                tvMode.setVisibility(View.VISIBLE);
                tvMode.setText("View Only: Tap a student to see details");
            }
            if (btnSave != null) btnSave.setVisibility(View.GONE);
            if (btnMarkAllPresent != null) btnMarkAllPresent.setVisibility(View.GONE);
            if (btnSameAsYesterday != null) btnSameAsYesterday.setVisibility(View.GONE);
        } else {
            // MODE: Take attendance
            if (tvMode != null) {
                tvMode.setVisibility(View.VISIBLE);
                tvMode.setText("Tap to toggle Present/Absent · Long-press for student details");
            }
            setupActionClickListeners();
        }
    }

    private void fetchStudents() {
        if (shimmerView != null) { shimmerView.setVisibility(View.VISIBLE); shimmerView.startShimmer(); }
        if (gridAttendance != null) gridAttendance.setVisibility(View.GONE);

        db.collection("students")
            .whereEqualTo("standard", standard != null ? standard : "8")
            .whereEqualTo("division", division != null ? division : "A")
            .orderBy("rollNumber")
            .get()
            .addOnSuccessListener(snap -> {
                if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }
                if (gridAttendance != null) gridAttendance.setVisibility(View.VISIBLE);

                studentList.clear();
                studentList.addAll(snap.toObjects(com.edu.track.models.Student.class));

                // Default all present ONLY if not already submitted
                if (!isAlreadySubmitted) {
                    attendanceMap.clear();
                    for (com.edu.track.models.Student s : studentList) {
                        attendanceMap.put(s.getStudentId(), true);
                    }
                }

                buildGrid();
                updateSummary();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch students", Toast.LENGTH_SHORT).show());
    }

    private void buildGrid() {
        if (gridAttendance == null) return;
        gridAttendance.removeAllViews();

        int cellSize = (int) (getResources().getDisplayMetrics().density * 44);

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

            Boolean present = attendanceMap.get(student.getStudentId());
            if (present == null) present = true;
            applyCellBackground(cell, present);

            final String studentId = student.getStudentId();

            if (isAlreadySubmitted) {
                // Already submitted: single press shows submitted status banner, long press shows details
                cell.setOnClickListener(v -> {
                    Toast.makeText(this, "Attendance already submitted. Long-press to view student.", Toast.LENGTH_SHORT).show();
                });
                cell.setOnLongClickListener(v -> { showStudentDetails(student); return true; });

            } else if (isViewOnly) {
                // View-only: single press shows details
                cell.setOnClickListener(v -> showStudentDetails(student));
            } else {
                // Take mode: single press toggles, long press shows details
                cell.setOnClickListener(v -> {
                    boolean cur = Boolean.TRUE.equals(attendanceMap.get(studentId));
                    attendanceMap.put(studentId, !cur);
                    applyCellBackground(cell, !cur);
                    updateSummary();
                });
                cell.setOnLongClickListener(v -> { showStudentDetails(student); return true; });
            }

            gridAttendance.addView(cell);
        }
    }

    private void showStudentDetails(com.edu.track.models.Student student) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Student Details");

        StringBuilder msg = new StringBuilder();
        msg.append("Name: ").append(student.getName()).append("\n");
        msg.append("Roll No: ").append(student.getRollNumber()).append("\n");
        if (student.getDob() != null && !student.getDob().isEmpty())
            msg.append("DOB: ").append(student.getDob()).append("\n");
        if (isAlreadySubmitted) {
            Boolean p = attendanceMap.get(student.getStudentId());
            msg.append("\nStatus: ").append(Boolean.TRUE.equals(p) ? "✓ Present" : "✗ Absent").append("\n");
        }

        builder.setMessage(msg.toString());

        if (student.getBirthdayCertificateUrl() != null && !student.getBirthdayCertificateUrl().isEmpty()) {
            builder.setPositiveButton("View Certificate", (dialog, which) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(student.getBirthdayCertificateUrl())));
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid certificate URL", Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void applyCellBackground(TextView cell, boolean present) {
        cell.setBackgroundResource(present ? R.drawable.bg_cell_present : R.drawable.bg_cell_absent);
    }

    private void updateSummary() {
        if (tvSummary == null) return;
        int presentCount = 0;
        for (Boolean status : attendanceMap.values()) if (Boolean.TRUE.equals(status)) presentCount++;
        int total = studentList.size();
        tvSummary.setText("Present: " + presentCount + "  |  Absent: " + (total - presentCount));
    }

    private void setupActionClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (btnMarkAllPresent != null) {
            btnMarkAllPresent.setOnClickListener(v -> {
                for (String id : attendanceMap.keySet()) attendanceMap.put(id, true);
                buildGrid();
                updateSummary();
            });
        }

        if (btnSameAsYesterday != null) {
            btnSameAsYesterday.setOnClickListener(v -> loadYesterdayAttendance());
        }

        if (btnSave != null) btnSave.setOnClickListener(v -> saveAttendance());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Setup back button always
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadYesterdayAttendance() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        String docId = yesterday + "_" + standard + division;

        db.collection("attendance_records").document(docId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                java.util.Map<String, Boolean> saved = (java.util.Map<String, Boolean>) doc.get("statuses");
                if (saved != null) {
                    attendanceMap.putAll(saved);
                    buildGrid();
                    updateSummary();
                    Toast.makeText(this, "Yesterday's attendance loaded", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No attendance record found for yesterday", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAttendance() {
        com.google.firebase.auth.FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        String teacherUid = user != null ? user.getUid() : "";

        java.util.Map<String, Object> record = new java.util.HashMap<>();
        record.put("date", todayDate);
        record.put("standard", standard != null ? standard : "");
        record.put("division", division != null ? division : "");
        record.put("timestamp", com.google.firebase.Timestamp.now());
        record.put("statuses", new java.util.HashMap<>(attendanceMap));
        record.put("submittedBy", teacherUid);  // Track who submitted
        record.put("classId", (standard != null ? standard : "") + (division != null ? division : ""));

        db.collection("attendance_records")
            .document(todayDate + "_" + standard + division)
            .set(record)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Attendance saved successfully!", Toast.LENGTH_SHORT).show();
                Intent histIntent = new Intent(this, AttendanceHistoryActivity.class);
                histIntent.putExtra("standard", standard);
                histIntent.putExtra("division", division);
                startActivity(histIntent);
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Error saving attendance", Toast.LENGTH_SHORT).show());
    }
}
