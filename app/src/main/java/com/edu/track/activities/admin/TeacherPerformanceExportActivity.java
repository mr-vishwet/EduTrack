package com.edu.track.activities.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TeacherPerformanceExportActivity extends AppCompatActivity {

    private String teacherUid;
    private String teacherName;
    private List<String> assignedClasses = new ArrayList<>();
    private String teacherExpertise = "General";
    private android.widget.Spinner spinnerSubject;
    private List<String> teacherSubjects = new ArrayList<>();
    private String selectedSubject = "All Subjects";

    private EditText etFromDate, etToDate;
    private Calendar fromCal, toCal;
    private final SimpleDateFormat displayFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private View layoutResults, footerExport;
    private TextView tvSummaryStats, tvTeacherSubtitle;
    private ShimmerFrameLayout shimmerView;

    private List<String[]> exportDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_performance_export);

        teacherUid = getIntent().getStringExtra("teacher_uid");
        teacherName = getIntent().getStringExtra("teacher_name");

        TextView tvName = findViewById(R.id.tv_teacher_name);
        tvTeacherSubtitle = findViewById(R.id.tv_teacher_subtitle);
        if (tvName != null) tvName.setText(teacherName != null ? teacherName : "Teacher");

        etFromDate = findViewById(R.id.et_from_date);
        etToDate = findViewById(R.id.et_to_date);
        layoutResults = findViewById(R.id.layout_results);
        footerExport = findViewById(R.id.footer_export);
        tvSummaryStats = findViewById(R.id.tv_summary_stats);
        shimmerView = findViewById(R.id.shimmer_view_container);

        // Default range: last 30 days
        toCal = Calendar.getInstance();
        fromCal = Calendar.getInstance();
        fromCal.add(Calendar.DAY_OF_YEAR, -29);

        etFromDate.setText(displayFmt.format(fromCal.getTime()));
        etToDate.setText(displayFmt.format(toCal.getTime()));
        spinnerSubject = findViewById(R.id.spinner_subject);

        setupClickListeners();
        loadTeacherContext();
    }

    private void loadTeacherContext() {
        if (teacherUid == null) return;
        FirebaseSource.getInstance().getTeachersRef().document(teacherUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String exp = doc.getString("expertise");
                        if (exp != null && !exp.isEmpty()) teacherExpertise = exp;

                        List<String> classes = (List<String>) doc.get("assignedClasses");
                        if (classes != null) {
                            assignedClasses.clear();
                            assignedClasses.addAll(classes);
                        }
                        
                        List<String> subjects = (List<String>) doc.get("subjectIds");
                        teacherSubjects.clear();
                        teacherSubjects.add("All Subjects");
                        if (subjects != null && !subjects.isEmpty()) {
                            teacherSubjects.addAll(subjects);
                        }
                        
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                                TeacherPerformanceExportActivity.this,
                                android.R.layout.simple_spinner_item,
                                teacherSubjects);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        if (spinnerSubject != null) {
                            spinnerSubject.setAdapter(adapter);
                            spinnerSubject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                    selectedSubject = teacherSubjects.get(position);
                                    hideResults();
                                }
                                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                            });
                        }
                        
                        tvTeacherSubtitle.setText(teacherExpertise + " \u00B7 " + assignedClasses.size() + " Assigned Class(es)");
                    }
                });
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        etFromDate.setOnClickListener(v -> showDatePicker(fromCal, d -> {
            fromCal.setTime(d); etFromDate.setText(displayFmt.format(d));
            hideResults();
        }));
        etToDate.setOnClickListener(v -> showDatePicker(toCal, d -> {
            toCal.setTime(d); etToDate.setText(displayFmt.format(d));
            hideResults();
        }));

        findViewById(R.id.btn_generate_report).setOnClickListener(v -> generateReport());
        findViewById(R.id.btn_export_csv).setOnClickListener(v -> exportCSV());
        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportPDF());
    }

    private void hideResults() {
        layoutResults.setVisibility(View.GONE);
        footerExport.setVisibility(View.GONE);
    }

    private void generateReport() {
        if (assignedClasses.isEmpty()) {
            Toast.makeText(this, "No classes assigned to this teacher", Toast.LENGTH_SHORT).show();
            return;
        }

        shimmerView.setVisibility(View.VISIBLE);
        shimmerView.startShimmer();
        hideResults();

        String fromStr = dbFmt.format(fromCal.getTime());
        String toStr = dbFmt.format(toCal.getTime());

        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
                .whereGreaterThanOrEqualTo("date", fromStr)
                .whereLessThanOrEqualTo("date", toStr)
                .get()
                .addOnSuccessListener(snap -> {
                    exportDataList.clear();
                    exportDataList.add(new String[]{"Date", "Class", "Total Present", "Total Absent", "Total Students"});

                    int validSessions = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                        if (rec == null) continue;

                        String classId = (rec.getStandard() != null ? rec.getStandard() : "") +
                                (rec.getDivision() != null ? rec.getDivision() : "");

                        if (assignedClasses.contains(classId)) {
                            if (!selectedSubject.equals("All Subjects")) {
                                String recSub = rec.getSubject();
                                if (recSub == null || !recSub.equalsIgnoreCase(selectedSubject)) {
                                    continue;
                                }
                            }
                            validSessions++;
                            int present = rec.getPresentCount();
                            int total = rec.getTotalCount();
                            int absent = total - present;
                            exportDataList.add(new String[]{
                                    rec.getDate(), "Std " + rec.getStandard() + rec.getDivision(),
                                    String.valueOf(present), String.valueOf(absent), String.valueOf(total)
                            });
                        }
                    }

                    shimmerView.stopShimmer();
                    shimmerView.setVisibility(View.GONE);

                    tvSummaryStats.setText(validSessions + " Sessions taken across " + assignedClasses.size() + " Classes");
                    layoutResults.setVisibility(View.VISIBLE);
                    footerExport.setVisibility(View.VISIBLE);

                    if (validSessions == 0) {
                        Toast.makeText(this, "No valid sessions found for this teacher in the date range.", Toast.LENGTH_SHORT).show();
                    }

                }).addOnFailureListener(e -> {
                    shimmerView.stopShimmer();
                    shimmerView.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load records: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker(Calendar initial, DateCallback cb) {
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d);
            cb.onDate(sel.getTime());
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void exportCSV() {
        if (exportDataList.size() <= 1) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder csv = new StringBuilder();
        // Add Header meta
        csv.append("Exported Teacher Performance Report\n");
        csv.append("Teacher Name:,").append(teacherName).append("\n");
        csv.append("Expertise:,").append(teacherExpertise).append("\n");
        csv.append("Subject Filter:,").append(selectedSubject).append("\n");
        csv.append("Date Range:,").append(dbFmt.format(fromCal.getTime())).append(" to ").append(dbFmt.format(toCal.getTime())).append("\n\n");

        for (String[] row : exportDataList) csv.append(String.join(",", row)).append("\n");

        ReportManager.exportToCSV(this, "Teacher_" + teacherName.replaceAll("\\s+", "_") + "_Perf", "Admin/Performance", csv.toString(),
                new ReportManager.ExportCallback() {
                    @Override public void onSuccess(String fp) { ReportManager.showExportSuccessDialog(TeacherPerformanceExportActivity.this, fp); }
                    @Override public void onFailure(Exception e) { Toast.makeText(TeacherPerformanceExportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
    }

    private void exportPDF() {
        if (exportDataList.size() <= 1) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        ReportManager.exportToPDF(this, teacherName + " - Session Report", teacherName.replaceAll("\\s+", "_") + "_Performance",
                "Admin/Performance", exportDataList,
                new ReportManager.ExportCallback() {
                    @Override public void onSuccess(String fp) { ReportManager.showExportSuccessDialog(TeacherPerformanceExportActivity.this, fp); }
                    @Override public void onFailure(Exception e) { Toast.makeText(TeacherPerformanceExportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
    }

    @FunctionalInterface
    interface DateCallback { void onDate(java.util.Date d); }
}
