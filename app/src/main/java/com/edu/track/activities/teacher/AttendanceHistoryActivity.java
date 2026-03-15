package com.edu.track.activities.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private EditText etFromDate, etToDate;
    private Calendar fromCal, toCal;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat firestoreDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private androidx.recyclerview.widget.RecyclerView rvHistory;
    private com.edu.track.adapters.AttendanceAdapter adapter;
    private List<com.edu.track.models.AttendanceRecord> recordList = new java.util.ArrayList<>();
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;
    private android.widget.TextView tvRecordCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        etFromDate = findViewById(R.id.et_from_date);
        etToDate   = findViewById(R.id.et_to_date);
        rvHistory  = findViewById(R.id.rv_attendance_history);
        shimmerView = findViewById(R.id.shimmer_view_container);
        tvRecordCount = findViewById(R.id.tv_record_count);

        // Default range: last 7 days
        toCal   = Calendar.getInstance();
        fromCal = Calendar.getInstance();
        fromCal.add(Calendar.DAY_OF_YEAR, -6);

        if (etFromDate != null) etFromDate.setText(dateFormat.format(fromCal.getTime()));
        if (etToDate   != null) etToDate.setText(dateFormat.format(toCal.getTime()));

        setupRecyclerView();
        setupClickListeners();
        fetchAttendanceHistory(false);
    }

    private com.google.firebase.firestore.DocumentSnapshot lastVisible;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 20;

    private void setupRecyclerView() {
        adapter = new com.edu.track.adapters.AttendanceAdapter(recordList, new com.edu.track.adapters.AttendanceAdapter.OnRecordClickListener() {
            @Override
            public void onEdit(com.edu.track.models.AttendanceRecord record) {
                Intent intent = new Intent(AttendanceHistoryActivity.this, TakeAttendanceActivity.class);
                intent.putExtra("standard", record.getStandard());
                intent.putExtra("division", record.getDivision());
                intent.putExtra("date", record.getDate());
                startActivity(intent);
            }

            @Override
            public void onDelete(com.edu.track.models.AttendanceRecord record) {
                FirebaseSource.getInstance().getFirestore()
                        .collection("attendance_records")
                        .document(record.getDate() + "_" + record.getStandard() + record.getDivision())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AttendanceHistoryActivity.this, "Record deleted", Toast.LENGTH_SHORT).show();
                            fetchAttendanceHistory(false);
                        });
            }
        });
        rvHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        rvHistory.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                androidx.recyclerview.widget.LinearLayoutManager layoutManager = (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == recordList.size() - 1) {
                    if (!isLoading && !isLastPage) {
                        fetchAttendanceHistory(true);
                    }
                }
            }
        });
    }

    private void fetchAttendanceHistory(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;

        if (!isLoadMore) {
            if (shimmerView != null) {
                shimmerView.setVisibility(android.view.View.VISIBLE);
                shimmerView.startShimmer();
            }
            rvHistory.setVisibility(android.view.View.GONE);
            recordList.clear();
            lastVisible = null;
            isLastPage = false;
        }

        String fromStr = firestoreDateFormat.format(fromCal.getTime());
        String toStr = firestoreDateFormat.format(toCal.getTime());

        com.google.firebase.firestore.Query query = FirebaseSource.getInstance().getFirestore()
                .collection("attendance_records")
                .whereGreaterThanOrEqualTo("date", fromStr)
                .whereLessThanOrEqualTo("date", toStr)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (isLoadMore && lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isLoading = false;
                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(android.view.View.GONE);
                    }
                    rvHistory.setVisibility(android.view.View.VISIBLE);

                    List<com.edu.track.models.AttendanceRecord> newRecords = queryDocumentSnapshots.toObjects(com.edu.track.models.AttendanceRecord.class);
                    recordList.addAll(newRecords);
                    adapter.notifyDataSetChanged();

                    if (queryDocumentSnapshots.size() < PAGE_SIZE) {
                        isLastPage = true;
                    } else if (queryDocumentSnapshots.size() > 0) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                    }
                    
                    if (tvRecordCount != null) {
                        tvRecordCount.setText("Showing " + recordList.size() + " records");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                    }
                    Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (etFromDate != null) {
            etFromDate.setOnClickListener(v -> showDatePicker(fromCal, date -> {
                fromCal.setTime(date);
                etFromDate.setText(dateFormat.format(date));
            }));
        }

        if (etToDate != null) {
            etToDate.setOnClickListener(v -> showDatePicker(toCal, date -> {
                toCal.setTime(date);
                etToDate.setText(dateFormat.format(date));
            }));
        }

        MaterialButton btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                if (fromCal.after(toCal)) {
                    Toast.makeText(this, "From date must be before To date", Toast.LENGTH_SHORT).show();
                    return;
                }
                fetchAttendanceHistory(false);
            });
        }
    }

    private void showDatePicker(Calendar initial, DateCallback callback) {
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day);
                    callback.onDate(selected.getTime());
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    @FunctionalInterface
    interface DateCallback {
        void onDate(java.util.Date date);
    }
}
