package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.adapters.ReportAdapter;
import com.edu.track.models.ReportItem;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherPerformanceActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ShimmerFrameLayout shimmerViewContainer;
    private RecyclerView rvReport;
    private ReportAdapter adapter;
    private List<ReportItem> reportItemList;

    // To pass to the export activity
    private final Map<String, String> nameToUid = new HashMap<>();
    private List<String[]> globalExportData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_performance);

        db = FirebaseSource.getInstance().getFirestore();
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        rvReport = findViewById(R.id.rv_teacher_performance);
        reportItemList = new ArrayList<>();

        rvReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(this, reportItemList);
        rvReport.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_export_all).setOnClickListener(v -> exportGlobalReport());

        adapter.setOnItemClickListener(item -> {
            String teacherName = item.getTitle();
            String uid = nameToUid.get(teacherName);
            if (uid != null) {
                Intent intent = new Intent(this, TeacherPerformanceExportActivity.class);
                intent.putExtra("teacher_uid", uid);
                intent.putExtra("teacher_name", teacherName);
                startActivity(intent);
            }
        });

        fetchTeacherPerformanceData();
    }

    private void fetchTeacherPerformanceData() {
        shimmerViewContainer.startShimmer();
        shimmerViewContainer.setVisibility(View.VISIBLE);
        rvReport.setVisibility(View.GONE);

        // Step 1: Load all teachers
        db.collection("teachers").get().addOnSuccessListener(teachersSnap -> {
            Map<String, String> classToTeacher = new HashMap<>();
            Map<String, String[]> teacherInfo = new HashMap<>(); // name -> [expertise, classCount]
            Map<String, List<String>> teacherSubjects = new HashMap<>();
            nameToUid.clear();

            for (QueryDocumentSnapshot doc : teachersSnap) {
                String uid = doc.getId();
                String name = doc.getString("name");
                String expertise = doc.getString("expertise");
                if (expertise == null || expertise.isEmpty()) expertise = "General";

                List<String> assigned = (List<String>) doc.get("assignedClasses");
                List<String> subjects = (List<String>) doc.get("subjectIds");
                if (subjects == null) subjects = new ArrayList<>();
                int classCount = (assigned != null) ? assigned.size() : 0;

                if (name != null) {
                    nameToUid.put(name, uid);
                    teacherInfo.put(name, new String[]{expertise, String.valueOf(classCount)});
                    teacherSubjects.put(name, subjects);
                    if (assigned != null) {
                        for (String cls : assigned) {
                            classToTeacher.put(cls, name);
                        }
                    }
                }
            }

            // Step 2: Count sessions per class from attendance_records
            db.collection("attendance_records").get().addOnSuccessListener(recordsSnap -> {
                Map<String, Integer> teacherSessions = new HashMap<>();
                Map<String, List<com.edu.track.models.AttendanceRecord>> teacherRecords = new HashMap<>();

                for (QueryDocumentSnapshot doc : recordsSnap) {
                    com.edu.track.models.AttendanceRecord rec = doc.toObject(com.edu.track.models.AttendanceRecord.class);
                    String std = rec.getStandard();
                    String div = rec.getDivision();
                    if (std != null && div != null) {
                        String classId = std + div;
                        String teacherName = classToTeacher.get(classId);
                        if (teacherName != null) {
                            teacherSessions.put(teacherName, teacherSessions.getOrDefault(teacherName, 0) + 1);
                            
                            teacherRecords.putIfAbsent(teacherName, new ArrayList<>());
                            teacherRecords.get(teacherName).add(rec);
                        }
                    }
                }

                reportItemList.clear();
                globalExportData.clear();

                for (Map.Entry<String, String[]> entry : teacherInfo.entrySet()) {
                    String name = entry.getKey();
                    String expertise = entry.getValue()[0];
                    String classCount = entry.getValue()[1];
                    int sessions = teacherSessions.getOrDefault(name, 0);
                    List<String> subjects = teacherSubjects.get(name);

                    String subtitle = sessions + " Sessions";
                    reportItemList.add(new ReportItem(name, "", subtitle, subjects));
                    
                    // Add to global export
                    globalExportData.add(new String[]{"---", "---", "---", "---", "---"});
                    globalExportData.add(new String[]{"TEACHER", name, "Expertise", expertise, ""});
                    globalExportData.add(new String[]{"Date", "Class", "Subject", "Present", "Absent"});
                    
                    List<com.edu.track.models.AttendanceRecord> recs = teacherRecords.get(name);
                    if (recs != null && !recs.isEmpty()) {
                        for (com.edu.track.models.AttendanceRecord r : recs) {
                            int present = r.getPresentCount();
                            int total = r.getTotalCount();
                            String subj = r.getSubject() != null ? r.getSubject() : "N/A";
                            globalExportData.add(new String[]{
                                r.getDate(), "Std " + r.getStandard() + r.getDivision(), subj,
                                String.valueOf(present), String.valueOf(total - present)
                            });
                        }
                    } else {
                        globalExportData.add(new String[]{"No sessions", "", "", "", ""});
                    }
                }

                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
                rvReport.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();

                if (reportItemList.isEmpty()) {
                    Toast.makeText(this, "No teacher data found", Toast.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> finishLoadingWithError(e.getMessage()));
        }).addOnFailureListener(e -> finishLoadingWithError(e.getMessage()));
    }

    private void exportGlobalReport() {
        if (globalExportData.isEmpty()) {
            Toast.makeText(this, "No data available to export yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        com.edu.track.utils.ReportManager.exportToPDF(this, "Global Teacher Performance", "Global_Teacher_Performance", "Admin/Performance", globalExportData,
            new com.edu.track.utils.ReportManager.ExportCallback() {
                @Override public void onSuccess(String fp) { com.edu.track.utils.ReportManager.showExportSuccessDialog(TeacherPerformanceActivity.this, fp); }
                @Override public void onFailure(Exception e) { Toast.makeText(TeacherPerformanceActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
            });
    }

    private void finishLoadingWithError(String msg) {
        shimmerViewContainer.stopShimmer();
        shimmerViewContainer.setVisibility(View.GONE);
        Toast.makeText(this, "Failed to load: " + msg, Toast.LENGTH_SHORT).show();
    }
}
