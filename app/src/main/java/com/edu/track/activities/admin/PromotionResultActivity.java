package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.google.android.material.chip.Chip;

public class PromotionResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_result);

        int promoted = getIntent().getIntExtra("PROMOTED", 0);
        int left = getIntent().getIntExtra("LEFT", 0);
        String dest = getIntent().getStringExtra("DEST");

        TextView tvDesc = findViewById(R.id.tv_result_desc);
        tvDesc.setText(promoted + " students successfully moved to " + dest);

        Chip chipPromoted = findViewById(R.id.result_chip_promoted);
        chipPromoted.setText("✓ Promoted: " + promoted);

        Chip chipLeft = findViewById(R.id.result_chip_left);
        chipLeft.setText("👤- Left School: " + left);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_back_dashboard).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.btn_view_students).setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageStudentsActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
