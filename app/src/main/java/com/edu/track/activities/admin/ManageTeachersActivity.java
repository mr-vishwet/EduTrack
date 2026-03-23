package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.adapters.TeacherAdapter;
import com.edu.track.models.User;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class ManageTeachersActivity extends AppCompatActivity {

    private EditText etSearch;
    private ShimmerFrameLayout shimmerView;
    private RecyclerView rvTeachers;
    private TeacherAdapter adapter;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private final List<User> allTeachers = new ArrayList<>();   // full list from DB
    private final List<User> displayList = new ArrayList<>();   // filtered list shown in RV
    private int currentTab = 0; // 0: All, 1: Class, 2: Subject

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_teachers);

        initViews();
        setupSearch();
        setupRecyclerView();
        setupClickListeners();
        fetchAllTeachers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list so newly added teachers appear immediately
        fetchAllTeachers();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        shimmerView = findViewById(R.id.shimmer_view_container);
        rvTeachers = findViewById(R.id.rv_teachers);
        tabLayout = findViewById(R.id.tab_layout_teachers);
        
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    currentTab = tab.getPosition();
                    applyFilter(etSearch != null ? etSearch.getText().toString() : "");
                }
                @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new TeacherAdapter(displayList, teacher -> {
            Intent intent = new Intent(ManageTeachersActivity.this, AssignClassActivity.class);
            intent.putExtra("teacher_uid", teacher.getUid());
            intent.putExtra("teacher_name", teacher.getName());
            startActivity(intent);
        });
        rvTeachers.setLayoutManager(new LinearLayoutManager(this));
        rvTeachers.setAdapter(adapter);
    }

    private void fetchAllTeachers() {
        if (shimmerView != null) {
            shimmerView.setVisibility(View.VISIBLE);
            shimmerView.startShimmer();
        }
        rvTeachers.setVisibility(View.GONE);

        FirebaseSource.getInstance().getTeachersRef()
                .orderBy("name")
                .get()
                .addOnSuccessListener(snap -> {
                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(View.GONE);
                    }
                    rvTeachers.setVisibility(View.VISIBLE);

                    allTeachers.clear();
                    allTeachers.addAll(snap.toObjects(User.class));

                    // Apply current search filter
                    applyFilter(etSearch != null ? etSearch.getText().toString() : "");
                })
                .addOnFailureListener(e -> {
                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(View.GONE);
                    }
                    rvTeachers.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error loading teachers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void applyFilter(String query) {
        displayList.clear();
        String lower = (query == null) ? "" : query.toLowerCase().trim();
        
        for (User t : allTeachers) {
            // Search match
            String name = t.getName() != null ? t.getName().toLowerCase() : "";
            String expertise = t.getExpertise() != null ? t.getExpertise().toLowerCase() : "";
            String cls = t.getClassTeacher() != null ? t.getClassTeacher().toLowerCase() : "";
            boolean matchesSearch = lower.isEmpty() || name.contains(lower) || expertise.contains(lower) || cls.contains(lower);
            
            if (!matchesSearch) continue;

            // Tab match
            boolean isClassTeacher = t.getClassTeacher() != null && !t.getClassTeacher().isEmpty();
            if (currentTab == 0) {
                displayList.add(t);
            } else if (currentTab == 1 && isClassTeacher) {
                displayList.add(t);
            } else if (currentTab == 2 && !isClassTeacher) {
                displayList.add(t);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        ImageView btnAdd = findViewById(R.id.btn_add_teacher);
        if (btnAdd != null) btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditTeacherActivity.class)));
    }
}
