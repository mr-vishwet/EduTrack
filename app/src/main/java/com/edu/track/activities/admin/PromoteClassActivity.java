package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

public class PromoteClassActivity extends AppCompatActivity {

    private String selectedSourceClass = "8th A";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_class);

        setupSpinners();
        setupClickListeners();
    }

    private void setupSpinners() {
        String[] sourceClasses = {"8th A", "8th B", "9th A", "9th B"};
        Spinner spinner = findViewById(R.id.spinner_source_class);
        if (spinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sourceClasses);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSourceClass = sourceClasses[position];
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_review_students).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewStudentsActivity.class);
            intent.putExtra("SOURCE_CLASS_ID", selectedSourceClass);
            // Simple logic for destination: next standard
            String std = selectedSourceClass.replaceAll("[^0-9]", "");
            int nextStd = Integer.parseInt(std) + 1;
            String div = selectedSourceClass.replaceAll("[^A-Z]", "");
            
            intent.putExtra("DEST_STD", nextStd + "th");
            intent.putExtra("DEST_DIV", div);
            startActivity(intent);
        });
    }
}
