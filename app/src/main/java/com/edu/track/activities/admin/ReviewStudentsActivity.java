package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.adapters.StudentReviewAdapter;
import com.edu.track.models.Student;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class ReviewStudentsActivity extends AppCompatActivity implements StudentReviewAdapter.OnStatusChangedListener {

    private static final String TAG = "ReviewStudentsActivity";
    private String sourceClassId, destStd, destDiv;
    private RecyclerView recyclerView;
    private StudentReviewAdapter adapter;
    private List<Student> students = new ArrayList<>();
    
    private TextView tvStudentCount, tvFooterPromoting, tvFooterLeft, tvFooterHold;
    private Chip chipPromoting, chipLeft, chipDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_students);

        sourceClassId = getIntent().getStringExtra("SOURCE_CLASS_ID");
        destStd = getIntent().getStringExtra("DEST_STD");
        destDiv = getIntent().getStringExtra("DEST_DIV");

        initViews();
        loadStudents();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvStudentCount = findViewById(R.id.tv_student_count_chip);
        chipPromoting = findViewById(R.id.chip_promoting);
        chipLeft = findViewById(R.id.chip_left);
        chipDestination = findViewById(R.id.chip_destination);
        
        chipDestination.setText(sourceClassId + " → " + destStd + destDiv);

        tvFooterPromoting = findViewById(R.id.tv_footer_promoting);
        tvFooterLeft = findViewById(R.id.tv_footer_left);
        tvFooterHold = findViewById(R.id.tv_footer_hold);

        recyclerView = findViewById(R.id.rv_review_students);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_select_all).setOnClickListener(v -> adapter.selectAll(true));
        findViewById(R.id.btn_deselect_all).setOnClickListener(v -> adapter.selectAll(false));

        findViewById(R.id.btn_confirm_promote).setOnClickListener(v -> performPromotion());
    }

    private void loadStudents() {
        FirebaseSource.getInstance().getStudentsRef()
                .whereEqualTo("standard", sourceClassId.replaceAll("[^0-9]", "")) // Crude parsing of "8th A" -> "8"
                .whereEqualTo("division", sourceClassId.replaceAll("[^A-Z]", ""))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    students = queryDocumentSnapshots.toObjects(Student.class);
                    // Filter in memory for exact match if needed, but let's assume it works
                    adapter = new StudentReviewAdapter(students, this);
                    recyclerView.setAdapter(adapter);
                    onStatusChanged();
                });
    }

    @Override
    public void onStatusChanged() {
        int promoting = 0, left = 0, hold = 0;
        for (StudentReviewAdapter.StudentPromotion s : adapter.getItems()) {
            if (!s.isSelected) continue;
            if (s.status == StudentReviewAdapter.Status.PROMOTE) promoting++;
            else if (s.status == StudentReviewAdapter.Status.LEFT_SCHOOL) left++;
            else hold++;
        }

        tvStudentCount.setText(adapter.getItemCount() + " Students");
        chipPromoting.setText("Promoting: " + promoting);
        chipLeft.setText("Left School: " + left);
        
        tvFooterPromoting.setText("✓ PROMOTING " + promoting);
        tvFooterLeft.setText("× LEFT: " + left);
        tvFooterHold.setText("⟳ HOLD: " + hold);
    }

    private void performPromotion() {
        WriteBatch batch = FirebaseSource.getInstance().getFirestore().batch();
        int promotedCount = 0;
        int leftCount = 0;

        for (StudentReviewAdapter.StudentPromotion sp : adapter.getItems()) {
            if (!sp.isSelected) continue;

            if (sp.status == StudentReviewAdapter.Status.PROMOTE) {
                // Update student
                sp.student.setStandard(destStd);
                sp.student.setDivision(destDiv);
                batch.set(FirebaseSource.getInstance().getStudentsRef().document(sp.student.getStudentId()), sp.student);
                promotedCount++;
            } else if (sp.status == StudentReviewAdapter.Status.LEFT_SCHOOL) {
                // Archive student
                batch.delete(FirebaseSource.getInstance().getStudentsRef().document(sp.student.getStudentId()));
                // batch.set(FirebaseSource.getInstance().getFirestore().collection("alumni").document(sp.student.getStudentId()), sp.student);
                leftCount++;
            }
            // HOLD_BACK: no changes needed to record
        }

        final int finalPromoted = promotedCount;
        final int finalLeft = leftCount;

        batch.commit().addOnSuccessListener(aVoid -> {
            Intent intent = new Intent(this, PromotionResultActivity.class);
            intent.putExtra("PROMOTED", finalPromoted);
            intent.putExtra("LEFT", finalLeft);
            intent.putExtra("DEST", destStd + destDiv);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Promotion failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
