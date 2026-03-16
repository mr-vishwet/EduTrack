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

public class ClassSummaryReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ShimmerFrameLayout shimmerViewContainer;
    private RecyclerView rvReport;
    private ReportAdapter adapter;
    private List<ReportItem> reportItemList;
    private List<String[]> exportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_summary_report);

        db = FirebaseSource.getInstance().getFirestore();
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        rvReport = findViewById(R.id.rv_class_summary);

        reportItemList = new ArrayList<>();
        exportData = new ArrayList<>();
        exportData.add(new String[]{"Class", "Avg Attendance %", "Records"});

        rvReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(this, reportItemList);
        rvReport.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_export).setOnClickListener(v -> showExportFormatDialog());

        fetchClassSummaryData();
    }

    private void fetchClassSummaryData() {
        shimmerViewContainer.startShimmer();
        shimmerViewContainer.setVisibility(View.VISIBLE);
        rvReport.setVisibility(View.GONE);

        db.collection("attendance_records").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Map<String, List<Double>> classAttendanceMap = new HashMap<>();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                String className = "Std " + record.getStandard() + " - " + record.getDivision();

                if (record.getTotalCount() > 0) {
                    double perc = (double) record.getPresentCount() / record.getTotalCount() * 100;
                    if (!classAttendanceMap.containsKey(className)) {
                        classAttendanceMap.put(className, new ArrayList<>());
                    }
                    classAttendanceMap.get(className).add(perc);
                }
            }

            reportItemList.clear();
            for (Map.Entry<String, List<Double>> entry : classAttendanceMap.entrySet()) {
                double sum = 0;
                for (Double d : entry.getValue()) sum += d;
                double avg = sum / entry.getValue().size();

                String val1 = String.format(Locale.getDefault(), "%.2f%%", avg);
                String val2 = entry.getValue().size() + " Records";

                reportItemList.add(new ReportItem(entry.getKey(), val1, val2));
                exportData.add(new String[]{entry.getKey(), val1, String.valueOf(entry.getValue().size())});
            }

            shimmerViewContainer.stopShimmer();
            shimmerViewContainer.setVisibility(View.GONE);
            rvReport.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();

            if (reportItemList.isEmpty()) {
                Toast.makeText(this, "No records found.", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e -> {
            shimmerViewContainer.stopShimmer();
            shimmerViewContainer.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showExportFormatDialog() {
        if (exportData.size() <= 1) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Export Class Summary")
                .setItems(new String[]{"Download as CSV", "Download as PDF"}, (dialog, which) -> {
                    if (which == 0) exportCSV();
                    else exportPDF();
                })
                .show();
    }

    private void exportCSV() {
        StringBuilder csv = new StringBuilder();
        for (String[] row : exportData) csv.append(String.join(",", row)).append("\n");
        ReportManager.exportToCSV(this, "Class_Summary_Report", "Admin/Summaries", csv.toString(),
                new ReportManager.ExportCallback() {
                    @Override public void onSuccess(String fp) { ReportManager.showExportSuccessDialog(ClassSummaryReportActivity.this, fp); }
                    @Override public void onFailure(Exception e) { Toast.makeText(ClassSummaryReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
    }

    private void exportPDF() {
        ReportManager.exportToPDF(this, "Class Attendance Summary", "Class_Summary_Report", "Admin/Summaries",
                exportData, new ReportManager.ExportCallback() {
                    @Override public void onSuccess(String fp) { ReportManager.showExportSuccessDialog(ClassSummaryReportActivity.this, fp); }
                    @Override public void onFailure(Exception e) { Toast.makeText(ClassSummaryReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
    }
}
