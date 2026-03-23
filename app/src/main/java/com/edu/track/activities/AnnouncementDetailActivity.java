package com.edu.track.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.edu.track.R;

public class AnnouncementDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvDate = findViewById(R.id.tv_detail_date);
        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvContent = findViewById(R.id.tv_detail_content);

        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String date = getIntent().getStringExtra("date");
        String metadata = getIntent().getStringExtra("metadata");

        if (title != null) tvTitle.setText(title);
        if (content != null) tvContent.setText(content);
        if (date != null) tvDate.setText(date);
        if (metadata != null) tvCategory.setText(metadata);
    }
}
