package com.edu.track.activities.teacher;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.edu.track.R;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherReportsActivity extends AppCompatActivity {

    private TextView tabClass, tabDate, tabStudent;
    private TextView tvOverallPct, tvTodayPresent;
    private FirebaseFirestore db;
    private String teacherUid;
    private List<AttendanceRecord> teacherRecords = new ArrayList<>();
    private List<String> assignedClasses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_reports);

        db = FirebaseSource.getInstance().getFirestore();
        teacherUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                     FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        initViews();
        setupClickListeners();
        loadTeacherAndData();
        
        selectTab(tabClass);
    }

    private void initViews() {
        tabClass   = findViewById(R.id.tab_class);
        tabDate    = findViewById(R.id.tab_date);
        tabStudent = findViewById(R.id.tab_student);
        tvOverallPct = findViewById(R.id.tv_overall_pct);
        tvTodayPresent = findViewById(R.id.tv_today_present);
    }

    private void loadTeacherAndData() {
        if (teacherUid == null) return;

        // Fetch teacher's assigned classes first
        db.collection("teachers").document(teacherUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                assignedClasses = (List<String>) doc.get("assignedClasses");
                fetchAttendanceData();
            }
        });
    }

    private void fetchAttendanceData() {
        if (assignedClasses == null || assignedClasses.isEmpty()) {
            tvOverallPct.setText("0%");
            tvTodayPresent.setText("0/0");
            return;
        }

        // Query all records and filter in memory to handle standard+division logic
        db.collection("attendance_records")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teacherRecords.clear();
                    int totalPresent = 0;
                    int totalStudents = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                        String std = record.getStandard();
                        String div = record.getDivision();
                        if (std == null || div == null) continue;
                        
                        String classId = std + div;
                        if (assignedClasses.contains(classId)) {
                            teacherRecords.add(record);
                            totalPresent += record.getPresentCount();
                            totalStudents += record.getTotalCount();
                        }
                    }

                    if (totalStudents > 0) {
                        int avg = (totalPresent * 100) / totalStudents;
                        tvOverallPct.setText(avg + "%");
                        tvTodayPresent.setText(totalPresent + "/" + totalStudents);
                    } else {
                        tvOverallPct.setText("0%");
                        tvTodayPresent.setText("0/0");
                    }
                });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (tabClass   != null) tabClass.setOnClickListener(v -> selectTab(tabClass));
        if (tabDate    != null) tabDate.setOnClickListener(v -> selectTab(tabDate));
        if (tabStudent != null) tabStudent.setOnClickListener(v -> selectTab(tabStudent));

        findViewById(R.id.fab_export).setOnClickListener(v -> showExportFormatDialog());
    }

    private void showExportFormatDialog() {
        String[] options = {"Download as CSV", "Download as PDF (Premium)"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Export Format")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) exportTeacherReport(false);
                    else exportTeacherReport(true);
                })
                .show();
    }

    private void selectTab(TextView selected) {
        TextView[] tabs = {tabClass, tabDate, tabStudent};
        for (TextView tab : tabs) {
            if (tab == null) continue;
            if (tab == selected) {
                tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tab_selected));
                tab.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
                tab.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tab_unselected));
                tab.setTextColor(ContextCompat.getColor(this, R.color.gray_caption));
                tab.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void exportTeacherReport(boolean isPdf) {
        if (teacherRecords.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPdf) {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Date", "Class", "Section", "P/T", "%"});
            for (AttendanceRecord r : teacherRecords) {
                double p = r.getTotalCount() > 0 ? (double) r.getPresentCount() / r.getTotalCount() * 100 : 0;
                data.add(new String[]{
                    ReportManager.formatTwoLineDate(r.getDate()),
                    r.getStandard() != null ? r.getStandard() : "N/A",
                    r.getDivision() != null ? r.getDivision() : "N/A",
                    r.getPresentCount() + "/" + r.getTotalCount(),
                    String.format("%.1f%%", p)
                });
            }
            String fileName = "Teacher_Report_" + System.currentTimeMillis();
            ReportManager.exportToPDF(this, "Class Attendance Report", fileName, "Teacher", data, new ReportManager.ExportCallback() {
                @Override public void onSuccess(String filePath) { ReportManager.showExportSuccessDialog(TeacherReportsActivity.this, filePath); }
                @Override public void onFailure(Exception e) { Toast.makeText(TeacherReportsActivity.this, "PDF Export failed", Toast.LENGTH_SHORT).show(); }
            });
        } else {
            StringBuilder csv = new StringBuilder("Date,Class,Section,Present,Total,Percentage\n");
            for (AttendanceRecord r : teacherRecords) {
                double p = r.getTotalCount() > 0 ? (double) r.getPresentCount() / r.getTotalCount() * 100 : 0;
                csv.append(r.getDate()).append(",")
                   .append(r.getStandard() != null ? r.getStandard() : "N/A").append(",")
                   .append(r.getDivision() != null ? r.getDivision() : "N/A").append(",")
                   .append(r.getPresentCount()).append(",")
                   .append(r.getTotalCount()).append(",")
                   .append(String.format("%.2f", p)).append("\n");
            }

            ReportManager.exportToCSV(this, "Attendance_Report_" + System.currentTimeMillis(), "Teacher", csv.toString(), new ReportManager.ExportCallback() {
                @Override
            public void onSuccess(String filePath) {
                ReportManager.showExportSuccessDialog(TeacherReportsActivity.this, filePath);
            }
    

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(TeacherReportsActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
