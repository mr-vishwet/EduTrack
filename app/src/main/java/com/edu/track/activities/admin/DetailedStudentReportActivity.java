package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.edu.track.R;

public class DetailedStudentReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_student_report);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_export_csv).setOnClickListener(v -> 
            Toast.makeText(this, "Exporting Detailed CSV...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> 
            Toast.makeText(this, "Generating Student PDF Report...", Toast.LENGTH_SHORT).show());
    }
}
