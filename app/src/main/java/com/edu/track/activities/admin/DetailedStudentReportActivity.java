package com.edu.track.activities.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.edu.track.R;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DetailedStudentReportActivity extends AppCompatActivity {

    private String studentId, studentName, standard, division;
    private TextView tvName, tvDetail, tvPercentage, tvPresent, tvAbsent, tvAvatar;
    private LinearLayout containerLog;
    private FirebaseFirestore db;
    private List<AttendanceRecord> records = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_student_report);

        db = FirebaseSource.getInstance().getFirestore();
        
        studentId = getIntent().getStringExtra("student_id");
        studentName = getIntent().getStringExtra("student_name");
        standard = getIntent().getStringExtra("standard");
        division = getIntent().getStringExtra("division");

        initViews();
        loadStudentReport();
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        tvName = findViewById(R.id.tv_student_name);
        tvDetail = findViewById(R.id.tv_student_detail);
        tvPercentage = findViewById(R.id.tv_overall_percentage);
        tvPresent = findViewById(R.id.tv_present_days);
        tvAbsent = findViewById(R.id.tv_absent_days);
        tvAvatar = findViewById(R.id.tv_avatar);
        containerLog = findViewById(R.id.container_log);

        tvName.setText(studentName);
        tvDetail.setText("Class " + standard + " " + division);
        if (studentName != null && !studentName.isEmpty()) {
            tvAvatar.setText(studentName.substring(0, 1).toUpperCase());
        }
        
        containerLog.removeAllViews();
    }

    private void loadStudentReport() {
        // Query attendance_records where the student exists in the statuses map
        // We query and filter locally to get both present and absent records
        db.collection("attendance_records")
                .whereEqualTo("standard", standard)
                .whereEqualTo("division", division)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    records.clear();
                    int present = 0;
                    int absent = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                        if (record.getStatuses() != null && record.getStatuses().containsKey(studentId)) {
                            records.add(record);
                            boolean isPresent = record.getStatuses().get(studentId);
                            if (isPresent) present++;
                            else absent++;
                            
                            addLogItem(record, isPresent);
                        }
                    }

                    updateStats(present, absent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load report data", Toast.LENGTH_SHORT).show();
                });
    }

    private void addLogItem(AttendanceRecord record, boolean isPresent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_attendance_log, containerLog, false);
        
        View indicator = view.findViewById(R.id.indicator);
        TextView tvDate = view.findViewById(R.id.tv_date);
        TextView tvSub = view.findViewById(R.id.tv_subtitle);
        TextView tvStatus = view.findViewById(R.id.tv_status_badge);

        tvDate.setText(record.getDate());
        tvSub.setText("Class Attendance"); // Could add subject if record had it
        
        if (isPresent) {
            indicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.present_green)));
            tvStatus.setText("P");
            tvStatus.setTextColor(getResources().getColor(R.color.present_green));
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.present_green_light)));
        } else {
            indicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.absent_red)));
            tvStatus.setText("A");
            tvStatus.setTextColor(getResources().getColor(R.color.absent_red));
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.absent_red_light)));
        }

        containerLog.addView(view);
    }

    private void updateStats(int present, int absent) {
        int total = present + absent;
        tvPresent.setText(present + " days");
        tvAbsent.setText(absent + " days");
        
        if (total > 0) {
            int perc = (present * 100) / total;
            tvPercentage.setText(perc + "%");
        } else {
            tvPercentage.setText("0%");
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_export_csv).setOnClickListener(v -> exportCsv());
        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportPdf());
    }

    private void exportPdf() {
        if (records.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Date", "Status", "Details"}); // Header
        
        for (AttendanceRecord r : records) {
            boolean p = r.getStatuses().get(studentId);
            data.add(new String[]{
                r.getDate(),
                p ? "Present" : "Absent",
                "Class " + standard + " " + division
            });
        }

        String fileName = studentName.replace(" ", "_") + "_Report";
        String reportTitle = "Attendance Report: " + studentName;
        
        ReportManager.exportToPDF(this, reportTitle, fileName, "Admin/Students", data, new ReportManager.ExportCallback() {
            @Override
            public void onSuccess(String filePath) {
                ReportManager.showExportSuccessDialog(DetailedStudentReportActivity.this, filePath);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DetailedStudentReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportCsv() {
        StringBuilder csv = new StringBuilder("Date,Status,Details\n");
        for (AttendanceRecord r : records) {
            boolean p = r.getStatuses().get(studentId);
            csv.append(r.getDate()).append(",")
               .append(p ? "Present" : "Absent").append(",")
               .append("Class ").append(standard).append(division).append("\n");
        }

        String fileName = studentName.replace(" ", "_") + "_Report";
        ReportManager.exportToCSV(this, fileName, "Admin/Students", csv.toString(), new ReportManager.ExportCallback() {
            @Override
            public void onSuccess(String filePath) {
                ReportManager.showExportSuccessDialog(DetailedStudentReportActivity.this, filePath);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DetailedStudentReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
