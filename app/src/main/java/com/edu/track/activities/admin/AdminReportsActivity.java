package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.widget.TextView;
import java.text.SimpleDateFormat;

public class AdminReportsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvStatSchoolAvg, tvStatToday, tvStatMonth, tvStatWeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        db = FirebaseSource.getInstance().getFirestore();
        progressBar = findViewById(R.id.progress_loading);
        if (progressBar != null)
            progressBar.setVisibility(View.GONE);

        tvStatSchoolAvg = findViewById(R.id.tv_stat_school_avg);
        tvStatToday = findViewById(R.id.tv_stat_today);
        tvStatMonth = findViewById(R.id.tv_stat_month);
        tvStatWeek = findViewById(R.id.tv_stat_week);

        setupClickListeners();
        loadRealTimeStats();
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null)
            btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.card_class_summary).setOnClickListener(v -> startActivity(new Intent(this, ReportFiltersActivity.class)));

        findViewById(R.id.card_student_detailed)
                .setOnClickListener(v -> startActivity(new Intent(this, ManageStudentsActivity.class))); // Go to
                                                                                                         // selection
                                                                                                         // first

        findViewById(R.id.card_monthly).setOnClickListener(
                v -> startActivity(new Intent(this, MonthlyReportActivity.class)));

        findViewById(R.id.card_subject).setOnClickListener(
                v -> startActivity(new Intent(this, SubjectReportActivity.class)));

        findViewById(R.id.card_teacher_performance).setOnClickListener(
                v -> startActivity(new Intent(this, TeacherPerformanceActivity.class)));

        findViewById(R.id.card_school_report).setOnClickListener(
                v -> startActivity(new Intent(this, SchoolReportActivity.class)));

        // Filter button removed per design decision
    }

    private void showExportFormatDialog() {
        String[] options = { "Download as CSV", "Download as PDF (Premium)" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Export Format")
                .setItems(options, (dialog, which) -> {
                    if (which == 0)
                        generateClassSummaryReport(false);
                    else
                        generateClassSummaryReport(true);
                })
                .show();
    }

    private void generateClassSummaryReport(boolean isPdf) {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Generating Class Summary...", Toast.LENGTH_SHORT).show();

        db.collection("attendance_records").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);

            // Map<ClassName, List<Perc>>
            Map<String, List<Double>> classAttendanceMap = new HashMap<>();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                String className = record.getStandard() + record.getDivision();

                if (record.getTotalCount() > 0) {
                    double perc = (double) record.getPresentCount() / record.getTotalCount() * 100;
                    if (!classAttendanceMap.containsKey(className)) {
                        classAttendanceMap.put(className, new ArrayList<>());
                    }
                    classAttendanceMap.get(className).add(perc);
                }
            }

            if (isPdf) {
                List<String[]> data = new ArrayList<>();
                data.add(new String[] { "Standard", "Section", "Avg Attendance %", "Total Records" });
                for (Map.Entry<String, List<Double>> entry : classAttendanceMap.entrySet()) {
                    double sum = 0;
                    for (Double d : entry.getValue())
                        sum += d;
                    double avg = sum / entry.getValue().size();
                    
                    // Simple extraction from Combined ID (8A)
                    String key = entry.getKey();
                    String std = key.replaceAll("[^0-9]", "");
                    String div = key.replaceAll("[0-9]", "");
                    if (std.isEmpty()) std = key;
                    if (div.isEmpty()) div = "All";

                    data.add(new String[] {
                            std,
                            div,
                            String.format("%.2f%%", avg),
                            String.valueOf(entry.getValue().size())
                    });
                }
                ReportManager.exportToPDF(this, "Class Attendance Summary", "Class_Summary_Report", "Admin/Summaries",
                        data, new ReportManager.ExportCallback() {
                            @Override
                            public void onSuccess(String filePath) {
                                ReportManager.showExportToast(AdminReportsActivity.this, filePath);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(AdminReportsActivity.this, "PDF Export failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                StringBuilder csv = new StringBuilder("Standard,Section,Average Attendance %,Total Records\n");
                for (Map.Entry<String, List<Double>> entry : classAttendanceMap.entrySet()) {
                    double sum = 0;
                    for (Double d : entry.getValue())
                        sum += d;
                    double avg = sum / entry.getValue().size();
                    
                    String key = entry.getKey();
                    String std = key.replaceAll("[^0-9]", "");
                    String div = key.replaceAll("[0-9]", "");
                    if (std.isEmpty()) std = key;
                    if (div.isEmpty()) div = "All";

                    csv.append(std).append(",")
                            .append(div).append(",")
                            .append(String.format("%.2f", avg)).append(",")
                            .append(entry.getValue().size()).append("\n");
                }

                ReportManager.exportToCSV(this, "Class_Summary_Report", "Admin/Summaries", csv.toString(),
                        new ReportManager.ExportCallback() {
                            @Override
                            public void onSuccess(String filePath) {
                                ReportManager.showExportSuccessDialog(AdminReportsActivity.this, filePath);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(AdminReportsActivity.this, "Export failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

        }).addOnFailureListener(e -> {
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to fetch attendance data", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadRealTimeStats() {
        db.collection("attendance_records").get().addOnSuccessListener(snap -> {
            int totalSchoolPresent = 0, totalSchoolStudents = 0;
            int todayPresent = 0, todayStudents = 0;
            int monthPresent = 0, monthStudents = 0;
            int weekPresent = 0, weekStudents = 0;

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            String todayStr = fmt.format(cal.getTime());
            
            String monthPrefix = todayStr.substring(0, 7); // yyyy-MM
            
            cal.add(Calendar.DAY_OF_YEAR, -7);
            String lastWeekStart = fmt.format(cal.getTime());

            for (QueryDocumentSnapshot doc : snap) {
                AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                if (rec.getTotalCount() == 0) continue;
                
                int p = rec.getPresentCount();
                int t = rec.getTotalCount();
                String d = rec.getDate();
                if (d == null) continue;

                totalSchoolPresent += p;
                totalSchoolStudents += t;

                if (d.equals(todayStr)) {
                    todayPresent += p;
                    todayStudents += t;
                }

                if (d.startsWith(monthPrefix)) {
                    monthPresent += p;
                    monthStudents += t;
                }

                if (d.compareTo(lastWeekStart) >= 0 && d.compareTo(todayStr) <= 0) {
                    weekPresent += p;
                    weekStudents += t;
                }
            }

            if (tvStatSchoolAvg != null) {
                int avg = totalSchoolStudents > 0 ? (int)((totalSchoolPresent * 100f) / totalSchoolStudents) : 0;
                tvStatSchoolAvg.setText("School Avg: " + avg + "%");
            }
            if (tvStatToday != null) {
                tvStatToday.setText("Today: " + todayPresent + "/" + todayStudents);
            }
            if (tvStatMonth != null) {
                int avg = monthStudents > 0 ? (int)((monthPresent * 100f) / monthStudents) : 0;
                tvStatMonth.setText("This Month: " + avg + "%");
            }
            if (tvStatWeek != null) {
                int avg = weekStudents > 0 ? (int)((weekPresent * 100f) / weekStudents) : 0;
                tvStatWeek.setText("Last 7 Days: " + avg + "%");
            }
        });
    }
}
