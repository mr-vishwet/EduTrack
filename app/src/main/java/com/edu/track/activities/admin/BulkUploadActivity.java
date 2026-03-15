package com.edu.track.activities.admin;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

public class BulkUploadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulk_upload);

        setupClickListeners();
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_download_template).setOnClickListener(v -> 
            Toast.makeText(this, "CSV Template downloading...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_choose_file).setOnClickListener(v -> 
            Toast.makeText(this, "Opening file picker...", Toast.LENGTH_SHORT).show());
            
        findViewById(R.id.btn_upload_save).setOnClickListener(v -> 
            Toast.makeText(this, "Uploading students...", Toast.LENGTH_SHORT).show());
    }
}
