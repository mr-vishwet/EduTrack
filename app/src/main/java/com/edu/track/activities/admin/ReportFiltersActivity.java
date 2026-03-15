package com.edu.track.activities.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReportFiltersActivity extends AppCompatActivity {

    private EditText etFromDate, etToDate;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_filters);

        etFromDate = findViewById(R.id.et_from_date);
        etToDate = findViewById(R.id.et_to_date);

        setupSpinners();
        setupClickListeners();
    }

    private void setupSpinners() {
        String[] classes = {"All Classes", "8th A", "8th B", "9th A", "9th B"};
        String[] subjects = {"All Subjects", "Mathematics", "Science", "English"};

        Spinner spinnerClass = findViewById(R.id.spinner_class);
        Spinner spinnerSubject = findViewById(R.id.spinner_subject);

        if (spinnerClass != null)
            spinnerClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classes));
        if (spinnerSubject != null)
            spinnerSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (etFromDate != null) etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        if (etToDate != null) etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        findViewById(R.id.btn_apply).setOnClickListener(v -> 
            Toast.makeText(this, "Filters applied! Showing preview.", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_export_csv).setOnClickListener(v -> 
            Toast.makeText(this, "Generating CSV...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> 
            Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker(EditText editText) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            editText.setText(dateFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
