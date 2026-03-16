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
import com.edu.track.models.AttendanceRecord;
import com.edu.track.utils.FirebaseSource;
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
    private ShimmerFrameLayout shimmerView;
    private android.widget.TextView tvRecordCount;
    private Spinner spinnerClass;

    // Teacher's assigned classes (for filtering)
    private List<String> assignedClasses = new ArrayList<>();
    private String selectedClassFilter = "All Classes";
    private boolean teacherFilterMode = false; // only show teacher's own classes

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

        // Default date range: last 7 days
        toCal = Calendar.getInstance();
        fromCal = Calendar.getInstance();
        fromCal.add(Calendar.DAY_OF_YEAR, -6);

        if (etFromDate != null) etFromDate.setText(displayFmt.format(fromCal.getTime()));
        if (etToDate != null) etToDate.setText(displayFmt.format(toCal.getTime()));

        setupRecyclerView();
        setupClickListeners();

        if (teacherFilterMode) {
            loadTeacherClassesAndFetch();
        } else {
            // Admin view — show all classes with an optional spinner filter loaded from DB
            loadAllClassesForSpinner();
            fetchHistory();
        }
    }

    /** Teacher login: load assigned classes, populate spinner, then fetch only those classes */
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
                fetchHistory();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
}
