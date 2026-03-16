package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ShimmerFrameLayout shimmerViewContainer;
    private RecyclerView rvReport;
    private ReportAdapter adapter;
    private List<ReportItem> reportItemList;
    private List<String[]> exportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        db = FirebaseSource.getInstance().getFirestore();
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        rvReport = findViewById(R.id.rv_monthly_report);
        
        reportItemList = new ArrayList<>();
        exportData = new ArrayList<>();
        exportData.add(new String[]{"Month", "Average Attendance %", "Records Processed"});

        rvReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(this, reportItemList);
        rvReport.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_export).setOnClickListener(v -> showExportFormatDialog());

        fetchMonthlyData();
    }

    private void fetchMonthlyData() {
        shimmerViewContainer.startShimmer();

        db.collection("attendance_records").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Map<String, List<Double>> monthlyAttendanceMap = new HashMap<>();

            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            SimpleDateFormat fullFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                
                if (record.getTotalCount() > 0) {
                    try {
                        Date date = fullFormat.parse(record.getDate());
                        if (date != null) {
                            String monthYear = monthFormat.format(date);
                            double perc = (double) record.getPresentCount() / record.getTotalCount() * 100;
                            
                            if (!monthlyAttendanceMap.containsKey(monthYear)) {
                                monthlyAttendanceMap.put(monthYear, new ArrayList<>());
                            }
                            monthlyAttendanceMap.get(monthYear).add(perc);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            reportItemList.clear();
            for (Map.Entry<String, List<Double>> entry : monthlyAttendanceMap.entrySet()) {
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
                Toast.makeText(this, "No attendance records found.", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e -> {
            shimmerViewContainer.stopShimmer();
            shimmerViewContainer.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
        });
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
        ReportManager.exportToCSV(this, "Monthly_Report", "Admin/Monthly", csv.toString(),
                new ReportManager.ExportCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        ReportManager.showExportSuccessDialog(MonthlyReportActivity.this, filePath);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MonthlyReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exportPDF() {
        ReportManager.exportToPDF(this, "Monthly Attendance Trends", "Monthly_Report", "Admin/Monthly",
                exportData, new ReportManager.ExportCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        ReportManager.showExportSuccessDialog(MonthlyReportActivity.this, filePath);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MonthlyReportActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
