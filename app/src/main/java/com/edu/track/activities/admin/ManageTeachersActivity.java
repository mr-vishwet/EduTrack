package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;

public class ManageTeachersActivity extends androidx.appcompat.app.AppCompatActivity {

    private android.widget.EditText etSearch;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;
    private androidx.recyclerview.widget.RecyclerView rvTeachers;
    private com.edu.track.adapters.TeacherAdapter adapter;
    private java.util.List<com.edu.track.models.User> teacherList = new java.util.ArrayList<>();
    private android.widget.ProgressBar progressLoadMore;

    // Pagination
    private com.google.firebase.firestore.DocumentSnapshot lastVisible;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_teachers);

        db = FirebaseSource.getInstance().getFirestore();
        
        initViews();
        setupClickListeners();
        setupSearch();
        setupRecyclerView();
        
        fetchTeachers(null);
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        shimmerView = findViewById(R.id.shimmer_view_container);
        rvTeachers = findViewById(R.id.rv_teachers);
        progressLoadMore = findViewById(R.id.progress_load_more);
    }

    private void setupRecyclerView() {
        adapter = new com.edu.track.adapters.TeacherAdapter(teacherList, teacher -> {
            Intent intent = new Intent(ManageTeachersActivity.this, AssignClassActivity.class);
            intent.putExtra("teacher_uid", teacher.getUid());
            intent.putExtra("teacher_name", teacher.getName());
            startActivity(intent);
        });

        rvTeachers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvTeachers.setAdapter(adapter);

        rvTeachers.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                androidx.recyclerview.widget.LinearLayoutManager layoutManager = (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        fetchTeachers(lastVisible);
                    }
                }
            }
        });
    }

    private void fetchTeachers(com.google.firebase.firestore.DocumentSnapshot startAfter) {
        isLoading = true;
        if (startAfter == null) {
            shimmerView.setVisibility(android.view.View.VISIBLE);
            shimmerView.startShimmer();
            rvTeachers.setVisibility(android.view.View.GONE);
            teacherList.clear();
        } else {
            progressLoadMore.setVisibility(android.view.View.VISIBLE);
        }

        com.google.firebase.firestore.Query query = FirebaseSource.getInstance().getTeachersRef()
                .orderBy("name")
                .limit(PAGE_SIZE);

        if (startAfter != null) {
            query = query.startAfter(startAfter);
        }

        query.get().addOnSuccessListener(documentSnapshots -> {
            isLoading = false;
            shimmerView.stopShimmer();
            shimmerView.setVisibility(android.view.View.GONE);
            rvTeachers.setVisibility(android.view.View.VISIBLE);
            progressLoadMore.setVisibility(android.view.View.GONE);

            if (documentSnapshots.isEmpty()) {
                isLastPage = true;
                return;
            }

            teacherList.addAll(documentSnapshots.toObjects(com.edu.track.models.User.class));
            adapter.notifyDataSetChanged();

            if (documentSnapshots.size() < PAGE_SIZE) {
                isLastPage = true;
            } else {
                lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
            }
        }).addOnFailureListener(e -> {
            isLoading = false;
            Toast.makeText(this, "Error fetching teachers", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        ImageView btnAdd = findViewById(R.id.btn_add_teacher);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> 
            startActivity(new Intent(this, AddEditTeacherActivity.class)));
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter logic
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }
}
