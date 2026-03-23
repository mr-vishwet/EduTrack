package com.edu.track.activities.parent;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.models.Student;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChildAttendanceDetailActivity extends AppCompatActivity {

    private String studentId;
    private Student student;
    private TextView tvOverallPct, tvPresentDays;
    private LinearLayout containerLog;
    private FirebaseFirestore db;
    private List<AttendanceRecord> childRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_attendance_detail);

        studentId = getIntent().getStringExtra("student_id");
        db = FirebaseSource.getInstance().getFirestore();

        initViews();
        loadChildData();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        tvOverallPct = findViewById(R.id.tv_overall_percentage);
        tvPresentDays = findViewById(R.id.tv_present_days);
        containerLog = findViewById(R.id.container_log);
        
        findViewById(R.id.btn_download_report).setOnClickListener(v -> showExportFormatDialog());
    }

    private void showExportFormatDialog() {
        if (childRecords.isEmpty()) {
            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] options = {"Download as CSV", "Download as PDF (Premium)"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Export Format")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) exportChildReport(false);
                    else exportChildReport(true);
                })
                .show();
    }

    private void loadChildData() {
        if (studentId != null) {
            db.collection("students").document(studentId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    student = doc.toObject(Student.class);
                    loadAttendanceHistory();
                }
            });
        } else {
            // Fetch student based on logged-in parent
            com.google.firebase.auth.FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                db.collection("students").whereEqualTo("parentUid", uid).limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            processStudentDoc(snap.iterator().next());
                        } else {
                            Toast.makeText(this, "No child found for this parent", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
            }
        }
    }

    private void processStudentDoc(com.google.firebase.firestore.DocumentSnapshot doc) {
        studentId = doc.getId();
        student = doc.toObject(Student.class);
        loadAttendanceHistory();
    }

    private void loadAttendanceHistory() {
        db.collection("attendance_records")
                .whereEqualTo("standard", student.getStandard())
                .whereEqualTo("division", student.getDivision())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childRecords.clear();
                    containerLog.removeAllViews();
                    int present = 0;
                    int total = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                        Map<String, Boolean> statuses = record.getStatuses();
                        if (statuses != null && statuses.containsKey(studentId)) {
                            childRecords.add(record);
                            total++;
                            boolean isPresent = Boolean.TRUE.equals(statuses.get(studentId));
                            if (isPresent) present++;
                            
                            addLogItem(record, isPresent);
                        }
                    }

                    updateStats(present, total);
                });
    }

    private void addLogItem(AttendanceRecord record, boolean isPresent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_attendance_log, containerLog, false);
        
        View indicator = view.findViewById(R.id.indicator);
        TextView tvDate = view.findViewById(R.id.tv_date);
        TextView tvSub = view.findViewById(R.id.tv_subtitle);
        TextView tvStatus = view.findViewById(R.id.tv_status_badge);

        tvDate.setText(record.getDate());
        tvSub.setText("Academic Session 2025-26");
        
        if (isPresent) {
            indicator.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.present_green)));
            tvStatus.setText("P");
            tvStatus.setTextColor(getResources().getColor(R.color.present_green));
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.present_green_light)));
        } else {
            indicator.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.absent_red)));
            tvStatus.setText("A");
            tvStatus.setTextColor(getResources().getColor(R.color.absent_red));
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.absent_red_light)));
        }

        containerLog.addView(view);
    }

    private void updateStats(int present, int total) {
        tvPresentDays.setText(present + " Days");
        if (total > 0) {
            int pct = (present * 100) / total;
            tvOverallPct.setText(pct + "%");
        } else {
            tvOverallPct.setText("0%");
        }
    }

    private void exportChildReport(boolean isPdf) {
        if (childRecords.isEmpty()) {
            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = (student != null ? student.getName().replace(" ", "_") : "Child") + "_Attendance";

        if (isPdf) {
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Date", "Status", "Standard", "Section"});
            for (AttendanceRecord r : childRecords) {
                boolean p = Boolean.TRUE.equals(r.getStatuses().get(studentId));
                data.add(new String[]{
                    ReportManager.formatTwoLineDate(r.getDate()),
                    p ? "Present" : "Absent",
                    r.getStandard() != null ? r.getStandard() : "N/A",
                    r.getDivision() != null ? r.getDivision() : "N/A"
                });
            }
            String docTitle = (student != null ? student.getName() : "Student") + " - Attendance";
            ReportManager.exportToPDF(this, docTitle, fileName, "Parent", data, new ReportManager.ExportCallback() {
                @Override public void onSuccess(String filePath) { ReportManager.showExportSuccessDialog(ChildAttendanceDetailActivity.this, filePath); }
                @Override public void onFailure(Exception e) { Toast.makeText(ChildAttendanceDetailActivity.this, "PDF Download failed", Toast.LENGTH_SHORT).show(); }
            });
        } else {
            StringBuilder csv = new StringBuilder();
            csv.append("Exported Student Report\n");
            if (student != null) {
                csv.append("Student Name:,").append(student.getName()).append("\n");
                csv.append("Class:,").append(student.getStandard() != null ? student.getStandard() : "N/A").append(" ").append(student.getDivision() != null ? student.getDivision() : "N/A").append("\n\n");
            }
            csv.append("Date,Attendance Status,Standard,Section\n");
            
            for (AttendanceRecord r : childRecords) {
                boolean p = Boolean.TRUE.equals(r.getStatuses().get(studentId));
                csv.append(r.getDate()).append(",")
                   .append(p ? "Present" : "Absent").append(",")
                   .append(r.getStandard() != null ? r.getStandard() : "N/A").append(",")
                   .append(r.getDivision() != null ? r.getDivision() : "N/A").append("\n");
            }

            ReportManager.exportToCSV(this, fileName, "Parent", csv.toString(), new ReportManager.ExportCallback() {
                @Override
                public void onSuccess(String filePath) {
                    ReportManager.showExportSuccessDialog(ChildAttendanceDetailActivity.this, filePath);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ChildAttendanceDetailActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
