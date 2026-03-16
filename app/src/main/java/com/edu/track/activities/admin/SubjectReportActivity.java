package com.edu.track.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.adapters.ReportAdapter;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.models.ReportItem;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubjectReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ShimmerFrameLayout shimmerViewContainer;
    private RecyclerView rvReport;
    private ReportAdapter adapter;
    private List<ReportItem> reportItemList;
    private List<String[]> exportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_report);

        db = FirebaseSource.getInstance().getFirestore();
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        rvReport = findViewById(R.id.rv_subject_report);
        
        reportItemList = new ArrayList<>();
        exportData = new ArrayList<>();
        exportData.add(new String[]{"Subject", "Average Attendance %", "Classes Sessioned"});

        rvReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(this, reportItemList);
        rvReport.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_export).setOnClickListener(v -> showExportFormatDialog());

        fetchSubjectData();
    }

    private void fetchSubjectData() {
        shimmerViewContainer.startShimmer();

        db.collection("subjects").get().addOnSuccessListener(subjectsSnap -> {
            Map<String, String> subjectMap = new HashMap<>(); // ID -> Name
            for (QueryDocumentSnapshot doc : subjectsSnap) {
                String sName = doc.getString("name");
                if (sName != null) {
                    subjectMap.put(doc.getId(), sName);
                }
            }

            db.collection("attendance_records").get().addOnSuccessListener(queryDocumentSnapshots -> {
                Map<String, List<Double>> subjectAttendanceMap = new HashMap<>();
                
                // Initialize map with all known subjects to ensure zero-session subjects appear
                for (String subjName : subjectMap.values()) {
                    subjectAttendanceMap.put(subjName, new ArrayList<>());
                }

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                    
                    String subjectIdOrName = record.getSubject();
                    if (subjectIdOrName == null || subjectIdOrName.isEmpty()) {
                        subjectIdOrName = "General / Core"; 
                    }
                    
                    // Resolve ID to Name if it's an ID
                    String resolvedName = subjectMap.getOrDefault(subjectIdOrName, subjectIdOrName);

                    if (record.getTotalCount() > 0) {
                        double perc = (double) record.getPresentCount() / record.getTotalCount() * 100;
                        
                        if (!subjectAttendanceMap.containsKey(resolvedName)) {
                            subjectAttendanceMap.put(resolvedName, new ArrayList<>());
                        }
                        subjectAttendanceMap.get(resolvedName).add(perc);
                    }
                }

                reportItemList.clear();
                exportData.clear();
                exportData.add(new String[]{"Subject", "Average Attendance %", "Classes Sessioned"});

                for (Map.Entry<String, List<Double>> entry : subjectAttendanceMap.entrySet()) {
                    String subjName = entry.getKey();
                    List<Double> percentages = entry.getValue();
                    
                    double avg = 0;
                    if (!percentages.isEmpty()) {
                        double sum = 0;
                        for (Double d : percentages) sum += d;
                        avg = sum / percentages.size();
                    }
                    
                    String val1 = percentages.isEmpty() ? "N/A" : String.format(Locale.getDefault(), "%.2f%%", avg);
                    String val2 = percentages.size() + " Sessions";
                    
                    reportItemList.add(new ReportItem(subjName, val1, val2));
                    exportData.add(new String[]{subjName, val1, String.valueOf(percentages.size())});
                }

                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
                rvReport.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();

                if (reportItemList.isEmpty()) {
                    Toast.makeText(this, "No analytics available.", Toast.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> {
                finishError();
            });
        }).addOnFailureListener(e -> {
            finishError();
        });
    }

    private void finishError() {
        shimmerViewContainer.stopShimmer();
        shimmerViewContainer.setVisibility(View.GONE);
        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
    }

    private void showExportFormatDialog() {
        if (exportData.size() <= 1) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
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
        for (String[] row : exportData) {
            csv.append(String.join(",", row)).append("\n");
        }
        ReportManager.exportToCSV(this, "Subject_Analytics_Report", "Admin/Subject", csv.toString(),
                new ReportManager.ExportCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        ReportManager.showExportSuccessDialog(SubjectReportActivity.this, filePath);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SubjectReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exportPDF() {
        ReportManager.exportToPDF(this, "Global Subject Analytics", "Subject_Analytics_Report", "Admin/Subject",
                exportData, new ReportManager.ExportCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        ReportManager.showExportSuccessDialog(SubjectReportActivity.this, filePath);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SubjectReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
