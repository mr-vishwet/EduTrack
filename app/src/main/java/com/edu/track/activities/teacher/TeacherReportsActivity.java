package com.edu.track.activities.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.edu.track.R;

public class TeacherReportsActivity extends AppCompatActivity {

    private TextView tabClass, tabDate, tabStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_reports);

        tabClass   = findViewById(R.id.tab_class);
        tabDate    = findViewById(R.id.tab_date);
        tabStudent = findViewById(R.id.tab_student);

        setupClickListeners();
        // Default: class tab selected
        selectTab(tabClass);
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (tabClass   != null) tabClass.setOnClickListener(v -> selectTab(tabClass));
        if (tabDate    != null) tabDate.setOnClickListener(v -> selectTab(tabDate));
        if (tabStudent != null) tabStudent.setOnClickListener(v -> selectTab(tabStudent));
    }

    private void selectTab(TextView selected) {
        TextView[] tabs = {tabClass, tabDate, tabStudent};
        for (TextView tab : tabs) {
            if (tab == null) continue;
            if (tab == selected) {
                tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tab_selected));
                tab.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
                tab.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_tab_unselected));
                tab.setTextColor(ContextCompat.getColor(this, R.color.gray_caption));
                tab.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
        // TODO: swap content view per tab when backend data available
    }
}
