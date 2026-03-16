package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.edu.track.R;
import com.edu.track.utils.FirebaseSource;

public class ManageClassesActivity extends androidx.appcompat.app.AppCompatActivity {

    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;
    private androidx.recyclerview.widget.RecyclerView rvClasses;
    private com.edu.track.adapters.SchoolClassAdapter adapter;
    private java.util.List<com.edu.track.models.SchoolClass> classList = new java.util.ArrayList<>();
    private com.google.firebase.firestore.ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_classes);

        db = FirebaseSource.getInstance().getFirestore();
        
        initViews();
        setupRecyclerView();
        listenToClasses();
    }

    private void initViews() {
        android.widget.ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        shimmerView = findViewById(R.id.shimmer_view_container);
        rvClasses = findViewById(R.id.rv_classes);
        
        android.view.View fabAdd = findViewById(R.id.fab_add_class);
        if (fabAdd != null) fabAdd.setOnClickListener(v -> 
            startActivity(new Intent(this, AddEditClassActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new com.edu.track.adapters.SchoolClassAdapter(classList, schoolClass -> {
            // Navigate to students of this class
            Intent intent = new Intent(this, ManageStudentsActivity.class);
            intent.putExtra("standard", schoolClass.getStandard());
            intent.putExtra("division", schoolClass.getDivision());
            startActivity(intent);
        });

        rvClasses.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvClasses.setAdapter(adapter);
    }

    private void listenToClasses() {
        shimmerView.setVisibility(android.view.View.VISIBLE);
        shimmerView.startShimmer();
        rvClasses.setVisibility(android.view.View.GONE);

        listenerRegistration = db.collection("classes")
                .orderBy("standard", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    shimmerView.stopShimmer();
                    shimmerView.setVisibility(android.view.View.GONE);
                    rvClasses.setVisibility(android.view.View.VISIBLE);

                    if (error != null) {
                        android.util.Log.e("ManageClasses", "Listen failed", error);
                        return;
                    }

                    if (value != null) {
                        classList.clear();
                        classList.addAll(value.toObjects(com.edu.track.models.SchoolClass.class));
                        
                        db.collection("teachers").get().addOnSuccessListener(teachersSnap -> {
                            java.util.Map<String, String> classToTeacher = new java.util.HashMap<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : teachersSnap.getDocuments()) {
                                String tName = doc.getString("name");
                                String assignedClass = doc.getString("classTeacher");
                                if (tName != null && assignedClass != null && !assignedClass.isEmpty()) {
                                    classToTeacher.put(assignedClass, tName);
                                }
                            }
                            
                            for (com.edu.track.models.SchoolClass sc : classList) {
                                String classId = sc.getStandard() + sc.getDivision();
                                if (classToTeacher.containsKey(classId)) {
                                    sc.setClassTeacherName(classToTeacher.get(classId));
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }).addOnFailureListener(e -> {
                            adapter.notifyDataSetChanged();
                        });
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
