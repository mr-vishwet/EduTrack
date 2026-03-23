package com.edu.track.activities.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.adapters.AttendanceAdapter;
import com.edu.track.adapters.StudentAdapter;
import com.edu.track.models.AttendanceRecord;
import com.edu.track.models.Student;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private EditText etFromDate, etToDate;
    private Calendar fromCal, toCal;
    private final SimpleDateFormat displayFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private androidx.recyclerview.widget.RecyclerView rvHistory;
    private AttendanceAdapter adapter;
    private final List<AttendanceRecord> recordList = new ArrayList<>();
    
    // Tab and Student Wise variables
    private TabLayout tabLayout;
    private StudentAdapter studentAdapter;
    private final List<Student> studentList = new ArrayList<>();
    
    private ShimmerFrameLayout shimmerView;
    private android.widget.TextView tvRecordCount;
    private Spinner spinnerClass;
    private android.widget.LinearLayout llStudentWiseToggles;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleExportMode;
    private ImageView btnExport;

    // Teacher's assigned classes (for filtering)
    private List<String> assignedClasses = new ArrayList<>();
    private String selectedClassFilter = "All Classes";
    private boolean teacherFilterMode = false;
    private String teacherClassTeacher = ""; // for color tags in adapter

    private DocumentSnapshot lastVisible;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        teacherFilterMode = getIntent().getBooleanExtra("teacher_filter", false);

        etFromDate = findViewById(R.id.et_from_date);
        etToDate = findViewById(R.id.et_to_date);
        rvHistory = findViewById(R.id.rv_attendance_history);
        shimmerView = findViewById(R.id.shimmer_view_container);
        tvRecordCount = findViewById(R.id.tv_record_count);
        spinnerClass = findViewById(R.id.spinner_class_filter);
        llStudentWiseToggles = findViewById(R.id.ll_student_wise_toggles);
        toggleExportMode = findViewById(R.id.toggle_export_mode);
        btnExport = findViewById(R.id.btn_export);

        if (toggleExportMode != null) {
            toggleExportMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btn_toggle_all_students) {
                        if (btnExport != null) btnExport.setVisibility(View.VISIBLE);
                    } else if (checkedId == R.id.btn_toggle_each_student) {
                        if (btnExport != null) btnExport.setVisibility(View.GONE);
                    }
                }
            });
        }

        // Default date range: last 7 days
        toCal = Calendar.getInstance();
        fromCal = Calendar.getInstance();
        fromCal.add(Calendar.DAY_OF_YEAR, -6);

        if (etFromDate != null) etFromDate.setText(displayFmt.format(fromCal.getTime()));
        if (etToDate != null) etToDate.setText(displayFmt.format(toCal.getTime()));

        tabLayout = findViewById(R.id.tab_layout);
        setupTabs();

        setupRecyclerView();
        setupClickListeners();

        if (teacherFilterMode) {
            // Show legend and export button in teacher mode
            View legend = findViewById(R.id.ll_legend);
            if (legend != null) legend.setVisibility(View.VISIBLE);
            if (btnExport != null) {
                btnExport.setVisibility(View.VISIBLE);
                btnExport.setOnClickListener(v -> showExportFormatDialog());
            }
            loadTeacherClassesAndFetch();
        } else {
            loadAllClassesForSpinner();
            fetchHistory();
        }
    }

    /** Teacher login: load assigned classes + classTeacher, populate spinner, then fetch */
    private void loadTeacherClassesAndFetch() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user == null) { fetchHistory(); return; }

        FirebaseSource.getInstance().getTeachersRef().document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> classes = (List<String>) doc.get("assignedClasses");
                        if (classes != null) {
                            assignedClasses.clear();
                            assignedClasses.addAll(classes);
                        }
                        String ct = doc.getString("classTeacher");
                        teacherClassTeacher = ct != null ? ct.toUpperCase() : "";
                        if (adapter != null) adapter.setClassTeacherClass(teacherClassTeacher);
                    }
                    populateClassSpinner(assignedClasses);
                    fetchHistory();
                })
                .addOnFailureListener(e -> fetchHistory());
    }

    private void loadAllClassesForSpinner() {
        FirebaseSource.getInstance().getFirestore().collection("classes")
                .orderBy("standard").get()
                .addOnSuccessListener(snap -> {
                    List<String> classIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String std = doc.getString("standard");
                        String div = doc.getString("division");
                        if (std != null && div != null) classIds.add(std + div);
                    }
                    assignedClasses.addAll(classIds);
                    populateClassSpinner(classIds);
                });
    }

    private void populateClassSpinner(List<String> classes) {
        if (spinnerClass == null) return;
        List<String> options = new ArrayList<>();
        options.add("All Classes");
        options.addAll(classes);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(spinnerAdapter);
        spinnerClass.setVisibility(View.VISIBLE);

        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedClassFilter = options.get(position);
                updateTabVisibility();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTabs() {
        if (tabLayout == null) return;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showDateWiseView();
                } else {
                    showStudentWiseView();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateTabVisibility() {
        if (tabLayout == null) return;
        boolean isClassTeacher = teacherFilterMode && 
            teacherClassTeacher != null && !teacherClassTeacher.isEmpty();
            
        if (isClassTeacher) {
            tabLayout.setVisibility(View.VISIBLE);
        } else {
            tabLayout.setVisibility(View.GONE);
        }

        // Always check bounds and safely switch or respect current selection
        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (tab != null && tab.getPosition() == 1 && isClassTeacher) {
            showStudentWiseView();
        } else {
            TabLayout.Tab firstTab = tabLayout.getTabAt(0);
            if (firstTab != null && !firstTab.isSelected()) firstTab.select();
            else showDateWiseView();
        }
    }

    private void showDateWiseView() {
        View legend = findViewById(R.id.ll_legend);
        if (legend != null && teacherFilterMode) legend.setVisibility(View.VISIBLE);
        
        if (spinnerClass != null) spinnerClass.setVisibility(View.VISIBLE);
        if (llStudentWiseToggles != null) llStudentWiseToggles.setVisibility(View.GONE);
        if (btnExport != null && teacherFilterMode) btnExport.setVisibility(View.VISIBLE);

        if (rvHistory != null) rvHistory.setAdapter(adapter);
        fetchHistory();
    }

    private void showStudentWiseView() {
        View legend = findViewById(R.id.ll_legend);
        if (legend != null) legend.setVisibility(View.GONE);
        
        if (spinnerClass != null) spinnerClass.setVisibility(View.GONE);
        if (llStudentWiseToggles != null) llStudentWiseToggles.setVisibility(View.VISIBLE);

        // Sync btnExport visibility with the current toggle state
        if (toggleExportMode != null) {
            if (toggleExportMode.getCheckedButtonId() == R.id.btn_toggle_all_students) {
                if (btnExport != null) btnExport.setVisibility(View.VISIBLE);
            } else {
                if (btnExport != null) btnExport.setVisibility(View.GONE);
            }
        }

        if (studentAdapter == null) {
            studentAdapter = new StudentAdapter(studentList, new StudentAdapter.OnStudentClickListener() {
                @Override
                public void onEdit(Student student) {}
                @Override
                public void onDelete(Student student) {}
                @Override
                public void onItemClick(Student student) {
                    if (toggleExportMode != null && toggleExportMode.getCheckedButtonId() == R.id.btn_toggle_all_students) {
                        Toast.makeText(AttendanceHistoryActivity.this, "Switch to 'Each Student' to view individual reports.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(AttendanceHistoryActivity.this, com.edu.track.activities.admin.DetailedStudentReportActivity.class);
                    // Standard routing properties
                    intent.putExtra("studentId", student.getStudentId());
                    intent.putExtra("studentName", student.getName());
                    intent.putExtra("rollNumber", student.getRollNumber());
                    intent.putExtra("standard", student.getStandard());
                    intent.putExtra("division", student.getDivision());
                    startActivity(intent);
                }
            });
            studentAdapter.setReadOnly(true);
        }
        if (rvHistory != null) rvHistory.setAdapter(studentAdapter);
        fetchStudentsForClass(teacherClassTeacher);
    }

    private void fetchStudentsForClass(String classFilter) {
        if (shimmerView != null) { shimmerView.setVisibility(View.VISIBLE); shimmerView.startShimmer(); }
        rvHistory.setVisibility(View.GONE);
        studentList.clear();

        String std = classFilter.replaceAll("[^0-9]", "");
        String div = classFilter.replaceAll("[0-9]", "");

        FirebaseSource.getInstance().getFirestore().collection("students")
                .whereEqualTo("standard", std)
                .whereEqualTo("division", div)
                .orderBy("rollNumber")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isDestroyed()) {
                        if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }
                        rvHistory.setVisibility(View.VISIBLE);
                        
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Student s = doc.toObject(Student.class);
                            if (s != null) {
                                s.setStudentId(doc.getId());
                                studentList.add(s);
                            }
                        }
                        if (studentAdapter != null) studentAdapter.updateList(studentList);
                        if (tvRecordCount != null) tvRecordCount.setText(studentList.size() + " students");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isDestroyed()) {
                        if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }
                        rvHistory.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new AttendanceAdapter(recordList, teacherFilterMode, new AttendanceAdapter.OnRecordClickListener() {
            @Override
            public void onEdit(AttendanceRecord record) {
                Intent intent = new Intent(AttendanceHistoryActivity.this, TakeAttendanceActivity.class);
                intent.putExtra("standard", record.getStandard());
                intent.putExtra("division", record.getDivision());
                intent.putExtra("date", record.getDate());
                startActivity(intent);
            }
            @Override
            public void onDelete(AttendanceRecord record) {
                FirebaseSource.getInstance().getFirestore()
                        .collection("attendance_records")
                        .document(record.getDate() + "_" + record.getStandard() + record.getDivision())
                        .delete()
                        .addOnSuccessListener(v -> {
                            Toast.makeText(AttendanceHistoryActivity.this, "Record deleted", Toast.LENGTH_SHORT).show();
                            fetchHistory();
                        });
            }
        });
        rvHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        rvHistory.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                androidx.recyclerview.widget.LinearLayoutManager lm =
                        (androidx.recyclerview.widget.LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && lm.findLastCompletelyVisibleItemPosition() == recordList.size() - 1) {
                    if (!isLoading && !isLastPage) fetchHistoryPage(true);
                }
            }
        });
    }

    private void fetchHistory() {
        fetchHistoryPage(false);
    }

    private void fetchHistoryPage(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;

        if (!isLoadMore) {
            if (shimmerView != null) { shimmerView.setVisibility(View.VISIBLE); shimmerView.startShimmer(); }
            rvHistory.setVisibility(View.GONE);
            recordList.clear();
            lastVisible = null;
            isLastPage = false;
        }

        String fromStr = dbFmt.format(fromCal.getTime());
        String toStr = dbFmt.format(toCal.getTime());

        Query query = FirebaseSource.getInstance().getFirestore()
                .collection("attendance_records")
                .whereGreaterThanOrEqualTo("date", fromStr)
                .whereLessThanOrEqualTo("date", toStr)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (isLoadMore && lastVisible != null) query = query.startAfter(lastVisible);

        query.get().addOnSuccessListener(snap -> {
            isLoading = false;
            if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }
            rvHistory.setVisibility(View.VISIBLE);

            for (DocumentSnapshot doc : snap.getDocuments()) {
                AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                if (rec == null) continue;

                // Teacher filter: only show assigned classes
                String classId = (rec.getStandard() != null ? rec.getStandard() : "") +
                                 (rec.getDivision() != null ? rec.getDivision() : "");

                if (teacherFilterMode && !assignedClasses.isEmpty()) {
                    if (!assignedClasses.contains(classId)) continue;
                }

                // Spinner class filter
                if (!"All Classes".equals(selectedClassFilter) && !classId.equals(selectedClassFilter)) {
                    continue;
                }

                recordList.add(rec);
            }

            adapter.notifyDataSetChanged();

            if (snap.size() < PAGE_SIZE) isLastPage = true;
            else if (snap.size() > 0) lastVisible = snap.getDocuments().get(snap.size() - 1);

            if (tvRecordCount != null) tvRecordCount.setText("Showing " + recordList.size() + " records");
        }).addOnFailureListener(e -> {
            isLoading = false;
            if (shimmerView != null) shimmerView.stopShimmer();
            Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (etFromDate != null) etFromDate.setOnClickListener(v -> showDatePicker(fromCal, d -> {
            fromCal.setTime(d); etFromDate.setText(displayFmt.format(d));
        }));
        if (etToDate != null) etToDate.setOnClickListener(v -> showDatePicker(toCal, d -> {
            toCal.setTime(d); etToDate.setText(displayFmt.format(d));
        }));

        MaterialButton btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) btnFilter.setOnClickListener(v -> {
            if (fromCal.after(toCal)) {
                Toast.makeText(this, "From date must be before To date", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchHistory();
        });
    }

    private void showDatePicker(Calendar initial, DateCallback cb) {
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d);
            cb.onDate(sel.getTime());
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    @FunctionalInterface
    interface DateCallback { void onDate(java.util.Date d); }

    /** Show UI dialog to pick export format */
    private void showExportFormatDialog() {
        if (tabLayout != null && tabLayout.getSelectedTabPosition() == 1) {
            // Student Wise Export Tab
            if (studentList.isEmpty()) {
                Toast.makeText(this, "No students available", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] options = {"Download as CSV", "Download as PDF"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Export All Students Data")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) exportStudentWiseCSV();
                        else exportStudentWisePDF();
                    })
                    .show();
        } else {
            // General Export Tab
            if (recordList.isEmpty()) {
                Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
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
    }

    private void fetchFullAttendanceDataForExport(OnFullDataFetchedListener listener) {
        if (shimmerView != null) { shimmerView.setVisibility(View.VISIBLE); shimmerView.startShimmer(); }

        String fromStr = dbFmt.format(fromCal.getTime());
        String toStr = dbFmt.format(toCal.getTime());

        FirebaseSource.getInstance().getFirestore().collection("attendance_records")
                .whereGreaterThanOrEqualTo("date", fromStr)
                .whereLessThanOrEqualTo("date", toStr)
                .get()
                .addOnSuccessListener(snap -> {
                    if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }

                    List<AttendanceRecord> fullList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                        if (rec == null) continue;
                        String classId = (rec.getStandard() != null ? rec.getStandard() : "") +
                                         (rec.getDivision() != null ? rec.getDivision() : "");
                        if (classId.equals(teacherClassTeacher)) {
                            fullList.add(rec);
                        }
                    }
                    listener.onFetched(fullList);
                })
                .addOnFailureListener(e -> {
                    if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }
                    Toast.makeText(this, "Failed to fetch full attendance data", Toast.LENGTH_SHORT).show();
                });
    }

    interface OnFullDataFetchedListener {
        void onFetched(List<AttendanceRecord> fullList);
    }

    private void exportStudentWiseCSV() {
        String dRange = "Report Date Range: " + displayFmt.format(fromCal.getTime()) + " to " + displayFmt.format(toCal.getTime());
        fetchFullAttendanceDataForExport(fullList -> {
            StringBuilder csv = new StringBuilder();
            csv.append(dRange).append("\n\n");
            csv.append("Roll No,Name,Present Days,Absent Days,Total Days,Percentage\n");

            for (Student student : studentList) {
                int present = 0;
                int absent = 0;
                String sid = student.getStudentId();

                for (AttendanceRecord rec : fullList) {
                    java.util.Map<String, Boolean> statuses = rec.getStatuses();
                    if (statuses != null && statuses.containsKey(sid)) {
                        Boolean isPresent = statuses.get(sid);
                        if (isPresent != null && isPresent) present++;
                        else absent++;
                    }
                }
                int total = present + absent;
                String percentage = total > 0 ? String.format(Locale.getDefault(), "%.1f%%", (present * 100.0f) / total) : "N/A";

                csv.append(student.getRollNumber()).append(",")
                   .append(student.getName().replace(",", " ")).append(",")
                   .append(present).append(",")
                   .append(absent).append(",")
                   .append(total).append(",")
                   .append(percentage).append("\n");
            }

            com.edu.track.utils.ReportManager.exportToCSV(this, "Student_Wise_Export", "Teacher/History", csv.toString(), new com.edu.track.utils.ReportManager.ExportCallback() {
                @Override public void onSuccess(String filePath) {
                    com.edu.track.utils.ReportManager.showExportSuccessDialog(AttendanceHistoryActivity.this, filePath);
                }
                @Override public void onFailure(Exception e) {
                    Toast.makeText(AttendanceHistoryActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void exportStudentWisePDF() {
        fetchFullAttendanceDataForExport(fullList -> {
            java.util.List<String[]> data = new java.util.ArrayList<>();
            data.add(new String[]{"Roll No", "Name", "Present", "Absent", "Total", "%"});

            for (Student student : studentList) {
                int present = 0;
                int absent = 0;
                String sid = student.getStudentId();

                for (AttendanceRecord rec : fullList) {
                    java.util.Map<String, Boolean> statuses = rec.getStatuses();
                    if (statuses != null && statuses.containsKey(sid)) {
                        Boolean isPresent = statuses.get(sid);
                        if (isPresent != null && isPresent) present++;
                        else absent++;
                    }
                }
                int total = present + absent;
                String percentage = total > 0 ? String.format(Locale.getDefault(), "%.1f", (present * 100.0f) / total) + "%" : "N/A";

                data.add(new String[]{
                        String.valueOf(student.getRollNumber()),
                        student.getName(),
                        String.valueOf(present),
                        String.valueOf(absent),
                        String.valueOf(total),
                        percentage
                });
            }

            String dRange = displayFmt.format(fromCal.getTime()) + " to " + displayFmt.format(toCal.getTime());
            com.edu.track.utils.ReportManager.exportToPDF(this, "Student Wise Summary (" + dRange + ")", "Student_Wise_Export", "Teacher/History", data, new com.edu.track.utils.ReportManager.ExportCallback() {
                @Override public void onSuccess(String filePath) {
                    com.edu.track.utils.ReportManager.showExportSuccessDialog(AttendanceHistoryActivity.this, filePath);
                }
                @Override public void onFailure(Exception e) {
                    Toast.makeText(AttendanceHistoryActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void exportCSV() {
        String dRange = "Report Date Range: " + displayFmt.format(fromCal.getTime()) + " to " + displayFmt.format(toCal.getTime());
        StringBuilder csv = new StringBuilder();
        csv.append(dRange).append("\n\n");
        csv.append("Date,Class,Present,Absent,Total\n");
        for (AttendanceRecord rec : recordList) {
            String classId = (rec.getStandard() != null ? rec.getStandard() : "")
                           + (rec.getDivision() != null ? rec.getDivision() : "");
            int total   = rec.getTotalCount();
            int present = rec.getPresentCount();
            int absent  = total - present;
            csv.append(rec.getDate()).append(",")
               .append("Std ").append(classId).append(",")
               .append(present).append(",")
               .append(absent).append(",")
               .append(total).append("\n");
        }

        com.edu.track.utils.ReportManager.exportToCSV(this, "Attendance_History_Export", "Teacher/History", csv.toString(), new com.edu.track.utils.ReportManager.ExportCallback() {
            @Override
            public void onSuccess(String filePath) {
                com.edu.track.utils.ReportManager.showExportSuccessDialog(AttendanceHistoryActivity.this, filePath);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AttendanceHistoryActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportPDF() {
        java.util.List<String[]> data = new java.util.ArrayList<>();
        data.add(new String[]{"Date", "Class", "Present", "Absent", "Total"});
        for (AttendanceRecord rec : recordList) {
            String classId = (rec.getStandard() != null ? rec.getStandard() : "")
                           + (rec.getDivision() != null ? rec.getDivision() : "");
            int total   = rec.getTotalCount();
            int present = rec.getPresentCount();
            int absent  = total - present;
            data.add(new String[]{
                rec.getDate(),
                "Std " + classId,
                String.valueOf(present),
                String.valueOf(absent),
                String.valueOf(total)
            });
        }
        String dRange = displayFmt.format(fromCal.getTime()) + " to " + displayFmt.format(toCal.getTime());
        com.edu.track.utils.ReportManager.exportToPDF(this, "Attendance History Export (" + dRange + ")", "Attendance_History_Export", "Teacher/History", data, new com.edu.track.utils.ReportManager.ExportCallback() {
            @Override
            public void onSuccess(String filePath) {
                com.edu.track.utils.ReportManager.showExportSuccessDialog(AttendanceHistoryActivity.this, filePath);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AttendanceHistoryActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
