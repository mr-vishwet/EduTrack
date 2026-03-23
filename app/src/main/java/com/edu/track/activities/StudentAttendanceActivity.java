package com.edu.track.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseUser;
import com.edu.track.models.Student;
import com.edu.track.models.AttendanceRecord;
import java.util.List;
import java.util.ArrayList;
public class StudentAttendanceActivity extends AppCompatActivity {

    private CircularProgressIndicator progressOverall;
    private TextView tvOverallPct, tvPresentCount, tvAbsentCount, tvStudentInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        initViews();
        loadStudentData();
        setupClickListeners();
    }

    private void initViews() {
        progressOverall = findViewById(R.id.progress_overall);
        tvOverallPct    = findViewById(R.id.tv_overall_pct);
        tvPresentCount  = findViewById(R.id.tv_present_count);
        tvAbsentCount   = findViewById(R.id.tv_absent_count);
        tvStudentInfo   = findViewById(R.id.tv_student_info);
    }

    private Student student;
    private List<AttendanceRecord> myRecords = new ArrayList<>();

    private void loadStudentData() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user == null) return;

        FirebaseSource.getInstance().getFirestore().collection("students")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    student = doc.toObject(Student.class);
                    student.setStudentId(doc.getId());
                    if (tvStudentInfo != null)
                        tvStudentInfo.setText(student.getName() + " · Roll No. " + student.getRollNumber() + " · " + student.getStandard() + student.getDivision());
                    fetchAttendanceStats();
                } else {
                    FirebaseSource.getInstance().getFirestore().collection("students")
                        .whereEqualTo("email", user.getEmail())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            if (!snapshots.isEmpty()) {
                                com.google.firebase.firestore.DocumentSnapshot sDoc = snapshots.getDocuments().get(0);
                                student = sDoc.toObject(Student.class);
                                student.setStudentId(sDoc.getId());
                                if (tvStudentInfo != null) {
                                    tvStudentInfo.setText(student.getName() + " · Roll No. " + student.getRollNumber() + " · " + student.getStandard() + student.getDivision());
                                }
                                fetchAttendanceStats();
                            }
                        });
                }
            });
    }

    private void fetchAttendanceStats() {
        if (student == null) return;
        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
            .whereEqualTo("standard", student.getStandard())
            .whereEqualTo("division", student.getDivision())
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                myRecords.clear();
                int total = 0;
                int presentCount = 0;

                for (com.google.firebase.firestore.DocumentSnapshot d : snapshots) {
                    AttendanceRecord record = d.toObject(AttendanceRecord.class);
                    if (record != null && record.getStatuses() != null && record.getStatuses().containsKey(student.getStudentId())) {
                        myRecords.add(record);
                        total++;
                        if (Boolean.TRUE.equals(record.getStatuses().get(student.getStudentId()))) {
                            presentCount++;
                        }
                    }
                }

                int absentCount = total - presentCount;
                int pct = total > 0 ? (presentCount * 100 / total) : 0;

                if (progressOverall != null) progressOverall.setProgress(pct);
                if (tvOverallPct != null)    tvOverallPct.setText(pct + "%");
                if (tvPresentCount != null)  tvPresentCount.setText("Present: " + presentCount + " days");
                if (tvAbsentCount != null)   tvAbsentCount.setText("Absent: " + absentCount + " days");
            });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());
        ImageView btnExport = findViewById(R.id.btn_export);
        if (btnExport != null) btnExport.setOnClickListener(v -> showExportFormatDialog());
    }

    private void showExportFormatDialog() {
        if (myRecords.isEmpty()) {
            android.widget.Toast.makeText(this, "No history available", android.widget.Toast.LENGTH_SHORT).show();
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

    private void exportCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Exported Student Report\n");
        if (student != null) {
            csv.append("Student Name:,").append(student.getName()).append("\n");
            csv.append("Class:,").append(student.getStandard() != null ? student.getStandard() : "N/A").append(" ").append(student.getDivision() != null ? student.getDivision() : "N/A").append("\n\n");
        }
        csv.append("Date,Attendance Status,Standard,Section\n");
        for (AttendanceRecord r : myRecords) {
            boolean p = Boolean.TRUE.equals(r.getStatuses().get(student.getStudentId()));
            csv.append(r.getDate()).append(",")
               .append(p ? "Present" : "Absent").append(",")
               .append(r.getStandard() != null ? r.getStandard() : "N/A").append(",")
               .append(r.getDivision() != null ? r.getDivision() : "N/A").append("\n");
        }

        String fileName = (student != null ? student.getName().replace(" ", "_") : "Student") + "_Attendance";
        com.edu.track.utils.ReportManager.exportToCSV(this, fileName, "Student", csv.toString(), new com.edu.track.utils.ReportManager.ExportCallback() {
            @Override public void onSuccess(String filePath) { com.edu.track.utils.ReportManager.showExportSuccessDialog(StudentAttendanceActivity.this, filePath); }
            @Override public void onFailure(Exception e) { android.widget.Toast.makeText(StudentAttendanceActivity.this, "Export failed", android.widget.Toast.LENGTH_SHORT).show(); }
        });
    }

    private void exportPDF() {
        java.util.List<String[]> data = new java.util.ArrayList<>();
        data.add(new String[]{"Date", "Status", "Standard", "Section"});
        for (AttendanceRecord r : myRecords) {
            boolean p = Boolean.TRUE.equals(r.getStatuses().get(student.getStudentId()));
            data.add(new String[]{
                com.edu.track.utils.ReportManager.formatTwoLineDate(r.getDate()),
                p ? "Present" : "Absent",
                r.getStandard() != null ? r.getStandard() : "N/A",
                r.getDivision() != null ? r.getDivision() : "N/A"
            });
        }
        String fileName = (student != null ? student.getName().replace(" ", "_") : "Student") + "_Attendance";
        String docTitle = (student != null ? student.getName() : "Student") + " - Attendance";
        com.edu.track.utils.ReportManager.exportToPDF(this, docTitle, fileName, "Student", data, new com.edu.track.utils.ReportManager.ExportCallback() {
            @Override public void onSuccess(String filePath) { com.edu.track.utils.ReportManager.showExportSuccessDialog(StudentAttendanceActivity.this, filePath); }
            @Override public void onFailure(Exception e) { android.widget.Toast.makeText(StudentAttendanceActivity.this, "PDF Export failed", android.widget.Toast.LENGTH_SHORT).show(); }
        });
    }
}
