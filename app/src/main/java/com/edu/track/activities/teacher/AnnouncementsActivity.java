package com.edu.track.activities.teacher;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.adapters.AnnouncementAdapter;
import com.edu.track.models.Announcement;
import com.edu.track.utils.FirebaseSource;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows announcements relevant to the logged-in teacher.
 * Fetches: (1) school-wide announcements, (2) announcements targeting the teacher's subjects/class.
 */
public class AnnouncementsActivity extends AppCompatActivity {

    private RecyclerView rvAnnouncements;
    private AnnouncementAdapter adapter;
    private final List<Announcement> announcementList = new ArrayList<>();
    private ShimmerFrameLayout shimmerView;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements_teacher);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        rvAnnouncements = findViewById(R.id.rv_announcements);
        shimmerView = findViewById(R.id.shimmer_view_container);
        tvEmpty = findViewById(R.id.tv_empty);

        setupRecyclerView();
        loadAnnouncements();
    }

    private void setupRecyclerView() {
        adapter = new AnnouncementAdapter(announcementList);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        rvAnnouncements.setAdapter(adapter);
    }

    private void loadAnnouncements() {
        FirebaseUser user = FirebaseSource.getInstance().getAuth().getCurrentUser();
        if (user == null) return;

        if (shimmerView != null) { shimmerView.setVisibility(View.VISIBLE); shimmerView.startShimmer(); }

        // Load teacher profile to get subjects and class info
        FirebaseSource.getInstance().getTeachersRef().document(user.getUid()).get()
            .addOnSuccessListener(teacherDoc -> {
                List<String> subjectIds = new ArrayList<>();
                String classTeacher = "";
                if (teacherDoc.exists()) {
                    List<String> sIds = (List<String>) teacherDoc.get("subjectIds");
                    if (sIds != null) subjectIds.addAll(sIds);
                    String ct = teacherDoc.getString("classTeacher");
                    if (ct != null) classTeacher = ct;
                }

                fetchAnnouncements(subjectIds, classTeacher);
            })
            .addOnFailureListener(e -> fetchAnnouncements(new ArrayList<>(), ""));
    }

    private void fetchAnnouncements(List<String> teacherSubjectIds, String classTeacherId) {
        FirebaseSource.getInstance().getFirestore()
            .collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener(snap -> {
                if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }

                announcementList.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Announcement a;
                    try {
                        a = doc.toObject(Announcement.class);
                    } catch (Exception ex) {
                        // Skip any document that fails to deserialize (e.g. type mismatch)
                        continue;
                    }
                    if (a == null) continue;
                    a.setId(doc.getId());

                    // Check Audience First
                    String audience = a.getAudience();
                    if (audience != null && !audience.equalsIgnoreCase("All") && !audience.equalsIgnoreCase("Teachers")) {
                        continue;
                    }

                    // Then targetType filtering
                    String target = a.getTargetType();
                    if (target == null || target.isEmpty() || "school".equalsIgnoreCase(target)) {
                        announcementList.add(a);
                    } else if ("subject".equalsIgnoreCase(target)) {
                        String subjectId = a.getSubjectId();
                        if (subjectId != null && teacherSubjectIds.contains(subjectId)) {
                            announcementList.add(a);
                        }
                    } else if ("class".equalsIgnoreCase(target)) {
                        String classId = a.getClassId();
                        if (classId != null && classId.equalsIgnoreCase(classTeacherId)) {
                            announcementList.add(a);
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                if (tvEmpty != null)
                    tvEmpty.setVisibility(announcementList.isEmpty() ? View.VISIBLE : View.GONE);
            })
            .addOnFailureListener(e -> {
                if (shimmerView != null) { shimmerView.stopShimmer(); shimmerView.setVisibility(View.GONE); }
                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            });
    }
}
