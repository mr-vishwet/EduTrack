package com.edu.track.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseUser;

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

    private void loadStudentData() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user == null) return;

        FirebaseSource.getInstance().getFirestore().collection("students")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    String roll = String.valueOf(doc.get("rollNumber"));
                    String std = doc.getString("standard");
                    String div = doc.getString("division");

                    if (tvStudentInfo != null)
                        tvStudentInfo.setText(name + " · Roll No. " + roll + " · " + std + div);

                    fetchAttendanceStats(doc.getId());
                } else {
                    FirebaseSource.getInstance().getFirestore().collection("students")
                        .whereEqualTo("email", user.getEmail())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            if (!snapshots.isEmpty()) {
                                com.google.firebase.firestore.DocumentSnapshot sDoc = snapshots.getDocuments().get(0);
                                if (tvStudentInfo != null) {
                                    tvStudentInfo.setText(sDoc.getString("name") + " · Roll No. " + sDoc.get("rollNumber") + " · " + sDoc.getString("standard") + sDoc.getString("division"));
                                }
                                fetchAttendanceStats(sDoc.getId());
                            }
                        });
                }
            });
    }

    private void fetchAttendanceStats(String studentId) {
        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener(snapshots -> {
                int total = snapshots.size();
                int presentCount = 0;
                for (com.google.firebase.firestore.DocumentSnapshot d : snapshots) {
                    if (Boolean.TRUE.equals(d.getBoolean("present"))) {
                        presentCount++;
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
    }
}
