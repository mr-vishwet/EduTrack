package com.edu.track.activities.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.adapters.ReportAdapter;
import com.edu.track.models.ReportItem;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFiltersActivity extends AppCompatActivity {

    private EditText etFromDate, etToDate;
    private final Calendar calendar = Calendar.getInstance();
    // Store in "dd/MM/yyyy" for display, convert to "yyyy-MM-dd" for DB query
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat dbFormat     = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private Spinner spinnerClass;
    private RecyclerView rvResults;
    private ReportAdapter adapter;
    private ShimmerFrameLayout shimmerContainer;
    private List<ReportItem> resultItems;
    private List<String[]> exportData;
    private FirebaseFirestore db;
    private List<String> classOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_filters);

        db = FirebaseSource.getInstance().getFirestore();

        etFromDate = findViewById(R.id.et_from_date);
        etToDate   = findViewById(R.id.et_to_date);
        spinnerClass       = findViewById(R.id.spinner_class);
        shimmerContainer   = findViewById(R.id.shimmer_view_container);
        rvResults          = findViewById(R.id.rv_filter_results);

        resultItems = new ArrayList<>();
        exportData  = new ArrayList<>();

        if (rvResults != null) {
            rvResults.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ReportAdapter(this, resultItems);
            rvResults.setAdapter(adapter);
        }

        // Default dates: from March 1 to today
        etFromDate.setText("01/03/2026");
        etToDate.setText(displayFormat.format(new Date()));

        loadClassesIntoSpinner();
        setupClickListeners();
    }

    private void loadClassesIntoSpinner() {
        classOptions = new ArrayList<>();
        classOptions.add("All Classes");

        db.collection("classes").orderBy("standard").get().addOnSuccessListener(snap -> {
            for (QueryDocumentSnapshot doc : snap) {
                String std = doc.getString("standard");
                String div = doc.getString("division");
                if (std != null && div != null) {
                    classOptions.add("Std " + std + " - " + div);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, classOptions);
            if (spinnerClass != null) spinnerClass.setAdapter(adapter);
        });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (etFromDate != null) etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        if (etToDate   != null) etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        View btnApply = findViewById(R.id.btn_apply);
        if (btnApply != null) btnApply.setOnClickListener(v -> applyFilters());

        View btnExportCsv = findViewById(R.id.btn_export_csv);
        if (btnExportCsv != null) btnExportCsv.setOnClickListener(v -> exportReport(false));

        View btnExportPdf = findViewById(R.id.btn_export_pdf);
        if (btnExportPdf != null) btnExportPdf.setOnClickListener(v -> showPdfPreviewDialog());
    }

    private void showPdfPreviewDialog() {
        if (exportData.size() <= 1) {
            Toast.makeText(this, "Apply filters first to load data", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pdf_preview, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_cancel_dialog).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btn_download_pdf).setOnClickListener(v -> {
            boolean openAfter = ((com.google.android.material.switchmaterial.SwitchMaterial) 
                    dialogView.findViewById(R.id.switch_open_after)).isChecked();
            dialog.dismiss();
            exportReportSync(true, openAfter);
        });

        dialog.show();
    }

    private void exportReportSync(boolean isPdf, boolean openAfter) {
        String fromStr = etFromDate.getText().toString().trim();
        String toStr   = etToDate.getText().toString().trim();
        String dRange = " (" + fromStr + " to " + toStr + ")";

        String fileName = "ClassWise_Report_" + System.currentTimeMillis();
        if (isPdf) {
            ReportManager.exportToPDF(this, "Class-wise Attendance" + dRange, fileName, "Admin/ClassWise", exportData,
                    new ReportManager.ExportCallback() {
                        @Override public void onSuccess(String fp) { 
                            if (openAfter) {
                                ReportManager.showExportSuccessDialog(ReportFiltersActivity.this, fp); 
                            } else {
                                Toast.makeText(ReportFiltersActivity.this, "PDF saved: " + fp, Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override public void onFailure(Exception e) { Toast.makeText(ReportFiltersActivity.this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                    });
        }
    }

    private void applyFilters() {
        String fromStr = etFromDate.getText().toString().trim();
        String toStr   = etToDate.getText().toString().trim();

        if (fromStr.isEmpty() || toStr.isEmpty()) {
            Toast.makeText(this, "Please select both From and To dates", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromDb, toDb;
        try {
            fromDb = dbFormat.format(displayFormat.parse(fromStr));
            toDb   = dbFormat.format(displayFormat.parse(toStr));
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedClass = spinnerClass != null &&
                spinnerClass.getSelectedItemPosition() > 0
                ? classOptions.get(spinnerClass.getSelectedItemPosition()) : null;

        // Parse standard and division from "Std 7 - B"
        String filterStd = null, filterDiv = null;
        if (selectedClass != null) {
            // format: "Std 7 - B"
            String[] parts = selectedClass.replace("Std ", "").split(" - ");
            if (parts.length == 2) {
                filterStd = parts[0].trim();
                filterDiv = parts[1].trim();
            }
        }

        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
        }
        if (rvResults != null) rvResults.setVisibility(View.GONE);

        final String fStd = filterStd, fDiv = filterDiv;
        queryAttendance(fromDb, toDb, fStd, fDiv);
    }

    private void queryAttendance(String fromDb, String toDb, String filterStd, String filterDiv) {
        com.google.firebase.firestore.Query query = db.collection("attendance_records")
                .whereGreaterThanOrEqualTo("date", fromDb)
                .whereLessThanOrEqualTo("date", toDb);

        query.get().addOnSuccessListener(snap -> {
            resultItems.clear();
            exportData.clear();
            exportData.add(new String[]{"Date", "Standard", "Section", "Present", "Total", "%"});

            // Group by class → date to aggregate per-class per-day
            Map<String, Map<String, int[]>> classDateMap = new HashMap<>();

            for (QueryDocumentSnapshot doc : snap) {
                String std = doc.getString("standard");
                String div = doc.getString("division");
                String date = doc.getString("date");

                // Apply class filter
                if (filterStd != null && (!filterStd.equals(std) || !filterDiv.equals(div))) continue;

                Map<String, Boolean> statuses = (Map<String, Boolean>) doc.get("statuses");
                int total   = statuses != null ? statuses.size() : 0;
                int present = 0;
                if (statuses != null) for (Boolean b : statuses.values()) if (b) present++;

                String classKey = "Std " + std + " - " + div;
                classDateMap.computeIfAbsent(classKey, k -> new HashMap<>())
                        .put(date, new int[]{present, total});
            }

            for (Map.Entry<String, Map<String, int[]>> e : classDateMap.entrySet()) {
                String cls = e.getKey();
                int totalPresent = 0, totalStudents = 0;
                for (int[] pt : e.getValue().values()) {
                    totalPresent  += pt[0];
                    totalStudents += pt[1];
                }
                double pct = totalStudents > 0 ? totalPresent * 100.0 / totalStudents : 0;
                String pctStr = String.format(Locale.getDefault(), "%.1f%%", pct);
                String recordsStr = e.getValue().size() + " days";

                resultItems.add(new ReportItem(cls, pctStr, recordsStr));
                
                // Extract Standard and Section for export from "Std 7 - B"
                String stdStr = "N/A", divStr = "N/A";
                if (cls.contains("Std ") && cls.contains(" - ")) {
                    String clean = cls.replace("Std ", "");
                    String[] parts = clean.split(" - ");
                    if (parts.length == 2) {
                        stdStr = parts[0].trim();
                        divStr = parts[1].trim();
                    }
                }

                exportData.add(new String[]{
                        fromDb + "\nto " + toDb, stdStr, divStr, String.valueOf(totalPresent),
                        String.valueOf(totalStudents), pctStr
                });
            }

            if (shimmerContainer != null) {
                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
            }
            if (rvResults != null) rvResults.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.notifyDataSetChanged();

            if (resultItems.isEmpty()) {
                Toast.makeText(this, "No records found for selected filters.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (shimmerContainer != null) {
                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void exportReport(boolean isPdf) {
        String fromStr = etFromDate.getText().toString().trim();
        String toStr   = etToDate.getText().toString().trim();
        String dRange = " (" + fromStr + " to " + toStr + ")";

        if (exportData.size() <= 1) {
            Toast.makeText(this, "Apply filters first to load data", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = "ClassWise_Report_" + System.currentTimeMillis();
        if (isPdf) {
            ReportManager.exportToPDF(this, "Class-wise Attendance" + dRange, fileName, "Admin/ClassWise", exportData,
                    new ReportManager.ExportCallback() {
                        @Override public void onSuccess(String fp) { ReportManager.showExportSuccessDialog(ReportFiltersActivity.this, fp); }
                        @Override public void onFailure(Exception e) { Toast.makeText(ReportFiltersActivity.this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                    });
        } else {
            StringBuilder csv = new StringBuilder();
            csv.append("Report Date Range: ").append(fromStr).append(" to ").append(toStr).append("\n\n");
            for (String[] row : exportData) csv.append(String.join(",", row)).append("\n");
            ReportManager.exportToCSV(this, fileName, "Admin/ClassWise", csv.toString(),
                    new ReportManager.ExportCallback() {
                        @Override public void onSuccess(String fp) { ReportManager.showExportSuccessDialog(ReportFiltersActivity.this, fp); }
                        @Override public void onFailure(Exception e) { Toast.makeText(ReportFiltersActivity.this, "CSV Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                    });
        }
    }

    private void showDatePicker(EditText editText) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            editText.setText(displayFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
