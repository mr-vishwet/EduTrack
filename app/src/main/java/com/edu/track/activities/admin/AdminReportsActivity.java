package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

public class AdminReportsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        setupClickListeners();
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.card_class_summary).setOnClickListener(v ->
                startActivity(new Intent(this, ReportFiltersActivity.class)));

        findViewById(R.id.card_student_detailed).setOnClickListener(v ->
                startActivity(new Intent(this, DetailedStudentReportActivity.class)));

        findViewById(R.id.card_monthly).setOnClickListener(v ->
                Toast.makeText(this, "Monthly Reports Coming Soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.card_subject).setOnClickListener(v ->
                Toast.makeText(this, "Subject-wise Reports Coming Soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.card_teacher_performance).setOnClickListener(v ->
                Toast.makeText(this, "Teacher Analytics Coming Soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.card_school_report).setOnClickListener(v ->
                Toast.makeText(this, "Master School Report Coming Soon", Toast.LENGTH_SHORT).show());
        
        ImageView btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) btnFilter.setOnClickListener(v ->
                startActivity(new Intent(this, ReportFiltersActivity.class)));
    }
}
