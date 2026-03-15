package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

public class PromoteClassActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_class);

        setupSpinners();
        setupClickListeners();
    }

    private void setupSpinners() {
        String[] sourceClasses = {"8th A · 42 students", "8th B · 38 students", "7th A · 45 students"};
        Spinner spinner = findViewById(R.id.spinner_source_class);
        if (spinner != null) {
            spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sourceClasses));
        }
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_review_students).setOnClickListener(v -> 
            Toast.makeText(this, "Opening students review list...", Toast.LENGTH_SHORT).show());
    }
}
