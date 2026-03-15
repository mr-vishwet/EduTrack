package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

public class AssignClassActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_class);

        setupClickListeners();
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            Toast.makeText(this, "Assignment saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
