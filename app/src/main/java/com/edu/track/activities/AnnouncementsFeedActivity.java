package com.edu.track.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.edu.track.R;
import com.edu.track.adapters.AnnouncementAdapter;
import com.edu.track.models.Announcement;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementsFeedActivity extends AppCompatActivity {

    private RecyclerView rvAnnouncements;
    private AnnouncementAdapter adapter;
    private ShimmerFrameLayout shimmerView;
    private List<Announcement> announcementList = new ArrayList<>(); // Currently displayed list
    private List<Announcement> fullRoleFilteredList = new ArrayList<>(); // Cache for role-filtered data
    
    private android.widget.TextView chipAll, chipAcademic, chipEvents, chipSports;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements_feed);

        rvAnnouncements = findViewById(R.id.rv_announcements);
        shimmerView = findViewById(R.id.shimmer_announcements);
        
        chipAll = findViewById(R.id.chip_all);
        chipAcademic = findViewById(R.id.chip_academic);
        chipEvents = findViewById(R.id.chip_events);
        chipSports = findViewById(R.id.chip_sports);

        setupRecyclerView();
        setupClickListeners();
        setupChipListeners();
        fetchAnnouncements();
    }

    private void setupChipListeners() {
        View.OnClickListener listener = v -> {
            String category = "All";
            if (v.getId() == R.id.chip_academic) category = "Academic";
            else if (v.getId() == R.id.chip_events) category = "Events";
            else if (v.getId() == R.id.chip_sports) category = "Sports";
            
            if (!selectedCategory.equals(category)) {
                selectedCategory = category;
                updateChipUi();
                applyCategoryFilter();
            }
        };

        if (chipAll != null) chipAll.setOnClickListener(listener);
        if (chipAcademic != null) chipAcademic.setOnClickListener(listener);
        if (chipEvents != null) chipEvents.setOnClickListener(listener);
        if (chipSports != null) chipSports.setOnClickListener(listener);
    }

    private void updateChipUi() {
        int selectedBg = R.drawable.bg_chip_selected;
        int unselectedBg = R.drawable.bg_chip_unselected;
        int white = getResources().getColor(R.color.white);
        int gray = getResources().getColor(R.color.gray_body);

        android.widget.TextView[] chips = {chipAll, chipAcademic, chipEvents, chipSports};
        String[] cats = {"All", "Academic", "Events", "Sports"};

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] != null) {
                boolean selected = cats[i].equals(selectedCategory);
                chips[i].setBackgroundResource(selected ? selectedBg : unselectedBg);
                chips[i].setTextColor(selected ? white : gray);
                chips[i].setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void applyCategoryFilter() {
        if ("All".equals(selectedCategory)) {
            announcementList = new ArrayList<>(fullRoleFilteredList);
        } else {
            announcementList = new ArrayList<>();
            for (Announcement a : fullRoleFilteredList) {
                if (selectedCategory.equalsIgnoreCase(a.getCategory())) {
                    announcementList.add(a);
                }
            }
        }
        adapter.setAnnouncements(announcementList);
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter();
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        rvAnnouncements.setAdapter(adapter);
    }

    private void fetchAnnouncements() {
        if (shimmerView != null) {
            shimmerView.startShimmer();
            shimmerView.setVisibility(View.VISIBLE);
        }
        rvAnnouncements.setVisibility(View.GONE);

        String role = getSharedPreferences("EduTrackPrefs", MODE_PRIVATE).getString("USER_ROLE", "");
        com.google.firebase.auth.FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();

        if (user == null) {
            if (shimmerView != null) shimmerView.stopShimmer();
            return;
        }

        // 1. Fetch Metadata (Class/Subjects) based on role
        if ("ADMIN".equals(role)) {
            listenToAnnouncements(role, null, null);
        } else if ("TEACHER".equals(role)) {
            FirebaseSource.getInstance().getTeachersRef().document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    List<String> classes = (List<String>) doc.get("assignedClasses");
                    List<String> subjects = (List<String>) doc.get("subjectIds");
                    String classTeacher = doc.getString("classTeacher");
                    List<String> allTeacherClasses = new ArrayList<>();
                    if (classes != null) allTeacherClasses.addAll(classes);
                    if (classTeacher != null && !classTeacher.isEmpty() && !allTeacherClasses.contains(classTeacher)) {
                        allTeacherClasses.add(classTeacher);
                    }
                    listenToAnnouncements(role, allTeacherClasses, subjects);
                })
                .addOnFailureListener(e -> listenToAnnouncements(role, null, null));
        } else if ("PARENT".equals(role)) {
            // For parent, fetch linked child's class
            FirebaseSource.getInstance().getFirestore().collection("students")
                .whereEqualTo("parentUid", user.getUid()).limit(1).get()
                .addOnSuccessListener(snap -> {
                    String classId = "";
                    if (!snap.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = snap.getDocuments().get(0);
                        String std = doc.getString("standard");
                        String div = doc.getString("division");
                        if (std != null && div != null) classId = std + div;
                    }
                    List<String> classes = new ArrayList<>();
                    if (!classId.isEmpty()) classes.add(classId);
                    listenToAnnouncements(role, classes, null);
                })
                .addOnFailureListener(e -> listenToAnnouncements(role, null, null));
        } else {
            // Default/Student
            FirebaseSource.getInstance().getFirestore().collection("students")
                .document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String classId = "";
                    if (doc.exists()) {
                        String std = doc.getString("standard");
                        String div = doc.getString("division");
                        if (std != null && div != null) classId = std + div;
                    }
                    List<String> classes = new ArrayList<>();
                    if (!classId.isEmpty()) classes.add(classId);
                    listenToAnnouncements(role, classes, null);
                })
                .addOnFailureListener(e -> listenToAnnouncements(role, null, null));
        }
    }

    private void listenToAnnouncements(String role, List<String> userClasses, List<String> subjects) {
        FirebaseSource.getInstance().getFirestore().collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(View.GONE);
                    }
                    return;
                }

                if (value != null) {
                    List<Announcement> all = value.toObjects(Announcement.class);
                    List<Announcement> filtered = new ArrayList<>();
                    
                    for (int i = 0; i < all.size(); i++) {
                        Announcement a = all.get(i);
                        a.setId(value.getDocuments().get(i).getId());
                        if (isRelevant(a, role, userClasses, subjects)) {
                            filtered.add(a);
                        }
                    }
                    
                    fullRoleFilteredList = filtered;
                    applyCategoryFilter();

                    if (shimmerView != null) {
                        shimmerView.stopShimmer();
                        shimmerView.setVisibility(View.GONE);
                    }
                    rvAnnouncements.setVisibility(View.VISIBLE);
                }
            });
    }

    private boolean isRelevant(Announcement a, String role, List<String> classes, List<String> subjects) {
        if ("ADMIN".equals(role)) return true;

        String audience = a.getAudience();
        String targetType = a.getTargetType();

        // 1. Audience Filter
        if (audience != null && !audience.equalsIgnoreCase("All") && !audience.equalsIgnoreCase("Class")) {
            if ("TEACHER".equals(role) && !audience.equalsIgnoreCase("Teachers")) return false;
            if ("PARENT".equals(role) && !audience.equalsIgnoreCase("Parents")) return false;
            if ("STUDENT".equals(role) && !audience.equalsIgnoreCase("Students")) return false;
        }

        // 2. Class/Subject Filter
        if (targetType == null || targetType.isEmpty() || "school".equalsIgnoreCase(targetType)) {
            return true;
        } else if ("class".equalsIgnoreCase(targetType)) {
            return a.getClassId() != null && classes != null && classes.contains(a.getClassId());
        } else if ("subject".equalsIgnoreCase(targetType)) {
            return a.getSubjectId() != null && subjects != null && subjects.contains(a.getSubjectId());
        }

        return false;
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        String role = getSharedPreferences("EduTrackPrefs", MODE_PRIVATE).getString("USER_ROLE", "");
        
        ImageView btnCreate = findViewById(R.id.btn_create_announcement);
        if (btnCreate != null) {
            if ("ADMIN".equals(role)) {
                btnCreate.setVisibility(View.VISIBLE);
                btnCreate.setOnClickListener(v -> {
                    startActivity(new android.content.Intent(this, com.edu.track.activities.admin.CreateAnnouncementActivity.class));
                });
            } else {
                btnCreate.setVisibility(View.GONE);
            }
        }
    }
}
