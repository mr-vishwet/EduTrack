package com.edu.track.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SchoolReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ShimmerFrameLayout shimmerViewContainer;
    private View contentView;
    
    private TextView tvGlobalAttendance;
    private TextView tvTotalStudents;
    private TextView tvTotalTeachers;
    private TextView tvTotalClasses;

    private String expGlobalAtt = "--";
    private String expStudents = "--";
    private String expTeachers = "--";
    private String expClasses = "--";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_report);

        db = FirebaseSource.getInstance().getFirestore();
        
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        contentView = findViewById(R.id.ll_content);
        
        tvGlobalAttendance = findViewById(R.id.tv_global_attendance);
        tvTotalStudents = findViewById(R.id.tv_total_students);
        tvTotalTeachers = findViewById(R.id.tv_total_teachers);
        tvTotalClasses = findViewById(R.id.tv_total_classes);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_export).setOnClickListener(v -> showExportFormatDialog());

        fetchSchoolVitals();
    }

    private void fetchSchoolVitals() {
        shimmerViewContainer.startShimmer();

        // 1. Get Students Count
        db.collection("students").get().addOnSuccessListener(studentsSnap -> {
            int studentCount = studentsSnap.size();
            expStudents = String.valueOf(studentCount);
            
            // 2. Get Teachers Count
            db.collection("teachers").get().addOnSuccessListener(teachersSnap -> {
                int teacherCount = teachersSnap.size();
                expTeachers = String.valueOf(teacherCount);
                
                // 3. Get Classes Count
                db.collection("classes").get().addOnSuccessListener(classesSnap -> {
                    int classCount = classesSnap.size();
                    expClasses = String.valueOf(classCount);
                    
                    // 4. Calculate Global Attendance
                    db.collection("attendance_records").get().addOnSuccessListener(recordsSnap -> {
                        long totalPossible = 0;
                        long totalPresent = 0;
                        
                        for (QueryDocumentSnapshot doc : recordsSnap) {
                            AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                            totalPossible += record.getTotalCount();
                            totalPresent += record.getPresentCount();
                        }
                        
                        if (totalPossible > 0) {
                            double gAtt = (double) totalPresent / totalPossible * 100;
                            expGlobalAtt = String.format(Locale.getDefault(), "%.2f%%", gAtt);
                        } else {
                            expGlobalAtt = "0.00%";
                        }

                        // Update UI
                        tvTotalStudents.setText(expStudents);
                        tvTotalTeachers.setText(expTeachers);
                        tvTotalClasses.setText(expClasses);
                        tvGlobalAttendance.setText(expGlobalAtt);

                        shimmerViewContainer.stopShimmer();
                        shimmerViewContainer.setVisibility(View.GONE);
                        contentView.setVisibility(View.VISIBLE);

                    }).addOnFailureListener(this::handleError);
                }).addOnFailureListener(this::handleError);
            }).addOnFailureListener(this::handleError);
        }).addOnFailureListener(this::handleError);
    }

    private void handleError(Exception e) {
        shimmerViewContainer.stopShimmer();
        shimmerViewContainer.setVisibility(View.GONE);
        Toast.makeText(this, "Failed to load school vitals", Toast.LENGTH_SHORT).show();
    }

    private void showExportFormatDialog() {
        if (expStudents.equals("--")) {
            Toast.makeText(this, "Data not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Download as CSV", "Download as PDF"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Export Format")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) exportCSV();
                    else exportPDF();
                })
                .show();
    }

    private List<String[]> prepareExportData() {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Metric", "Value"});
        data.add(new String[]{"Global Attendance", expGlobalAtt});
        data.add(new String[]{"Total Active Students", expStudents});
        data.add(new String[]{"Total Faculty", expTeachers});
        data.add(new String[]{"Configured Classes", expClasses});
        return data;
    }

    private void exportCSV() {
        StringBuilder csv = new StringBuilder();
        for (String[] row : prepareExportData()) {
            csv.append(String.join(",", row)).append("\n");
        }
        ReportManager.exportToCSV(this, "Master_School_Report", "Admin/Master", csv.toString(),
                new ReportManager.ExportCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        ReportManager.showExportSuccessDialog(SchoolReportActivity.this, filePath);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SchoolReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exportPDF() {
        ReportManager.exportToPDF(this, "Master School Vitals", "Master_School_Report", "Admin/Master",
                prepareExportData(), new ReportManager.ExportCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        ReportManager.showExportSuccessDialog(SchoolReportActivity.this, filePath);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SchoolReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
