package com.edu.track.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.activities.admin.AddEditStudentActivity;
import com.edu.track.activities.admin.DetailedStudentReportActivity;
import com.edu.track.utils.FirebaseSource;
import com.edu.track.utils.ReportManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageStudentsActivity extends AppCompatActivity {

    private EditText etSearch;
    private android.widget.LinearLayout chipContainer;
    private String selectedClassId = "ALL";
    private FloatingActionButton fabAdd;

    // Data handling
    private RecyclerView rvStudents;
    private com.edu.track.adapters.StudentAdapter adapter;
    private List<com.edu.track.models.Student> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerView;
    private android.widget.ProgressBar progressLoadMore;

    // Pagination
    private com.google.firebase.firestore.DocumentSnapshot lastVisible;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 20;

    private String fixedStandard = null;
    private String fixedDivision = null;

    private final androidx.activity.result.ActivityResultLauncher<Intent> csvPickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            parseCsvAndUpload(result.getData().getData());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        db = FirebaseSource.getInstance().getFirestore();

        fixedStandard = getIntent().getStringExtra("standard");
        fixedDivision = getIntent().getStringExtra("division");

        initViews();
        loadClasses();
        setupClickListeners();
        setupSearch();
        setupRecyclerView();
        
        fetchStudents(null);
    }

    private void initViews() {
        etSearch  = findViewById(R.id.et_search);
        fabAdd    = findViewById(R.id.fab_add_student);
        chipContainer = findViewById(R.id.chip_container);
        
        rvStudents = findViewById(R.id.rv_students);
        shimmerView = findViewById(R.id.shimmer_view_container);
        progressLoadMore = findViewById(R.id.progress_load_more);

        if (fixedStandard != null && fixedDivision != null) {
            View svChips = findViewById(R.id.sv_chips);
            if (svChips != null) svChips.setVisibility(View.GONE);
        }
    }

    private void loadClasses() {
        if (fixedStandard != null) return; // Don't show chips if already filtered

        db.collection("classes").get().addOnSuccessListener(snapshots -> {
            chipContainer.removeAllViews();
            
            // Add "All Classes"
            addFilterChip("All Classes", "ALL");
            
            List<com.google.firebase.firestore.DocumentSnapshot> sortedDocs = new ArrayList<>(snapshots.getDocuments());
            sortedDocs.sort((d1, d2) -> {
                String s1 = d1.getString("standard");
                String s2 = d2.getString("standard");
                String v1 = d1.getString("division");
                String v2 = d2.getString("division");
                
                int n1 = extractNumber(s1);
                int n2 = extractNumber(s2);
                
                if (n1 != n2) return Integer.compare(n1, n2);
                if (v1 != null && v2 != null) return v1.compareTo(v2);
                return 0;
            });
            
            for (com.google.firebase.firestore.DocumentSnapshot doc : sortedDocs) {
                String std = doc.getString("standard");
                String div = doc.getString("division");
                if (std != null && div != null) {
                    addFilterChip(std + (std.matches("\\d+") ? "th " : " ") + div, std + div);
                }
            }
        });
    }

    private int extractNumber(String s) {
        if (s == null) return 0;
        String digits = s.replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private void addFilterChip(String label, String classId) {
        TextView chip = new TextView(this);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (34 * getResources().getDisplayMetrics().density));
        params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        chip.setLayoutParams(params);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding((int) (16 * getResources().getDisplayMetrics().density), 0, 
                        (int) (16 * getResources().getDisplayMetrics().density), 0);
        chip.setText(label);
        chip.setTextSize(13);
        chip.setTag(classId);

        if (selectedClassId.equals(classId)) {
            chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
            chip.setTextColor(ContextCompat.getColor(this, R.color.gray_body));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        chip.setOnClickListener(v -> {
            selectedClassId = (String) v.getTag();
            for (int i = 0; i < chipContainer.getChildCount(); i++) {
                View child = chipContainer.getChildAt(i);
                if (child instanceof TextView) {
                    boolean isSelected = selectedClassId.equals(child.getTag());
                    child.setBackground(ContextCompat.getDrawable(this, isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected));
                    ((TextView) child).setTextColor(ContextCompat.getColor(this, isSelected ? R.color.white : R.color.gray_body));
                    ((TextView) child).setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                }
            }
            isLastPage = false;
            lastVisible = null;
            fetchStudents(null);
        });

        chipContainer.addView(chip);
    }

    private void setupRecyclerView() {
        adapter = new com.edu.track.adapters.StudentAdapter(studentList, new com.edu.track.adapters.StudentAdapter.OnStudentClickListener() {
            @Override
            public void onEdit(com.edu.track.models.Student student) {
                Intent intent = new Intent(ManageStudentsActivity.this, AddEditStudentActivity.class);
                intent.putExtra("student_id", student.getStudentId());
                intent.putExtra("edit_mode", true);
                startActivity(intent);
            }

            @Override
            public void onDelete(com.edu.track.models.Student student) {
                showDeleteConfirm(student);
            }

            @Override
            public void onItemClick(com.edu.track.models.Student student) {
                Intent intent = new Intent(ManageStudentsActivity.this, DetailedStudentReportActivity.class);
                intent.putExtra("student_id", student.getStudentId());
                intent.putExtra("student_name", student.getName());
                intent.putExtra("standard", student.getStandard());
                intent.putExtra("division", student.getDivision());
                startActivity(intent);
            }
        });

        rvStudents.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvStudents.setAdapter(adapter);

        // Pagination Scroll Listener
        rvStudents.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                androidx.recyclerview.widget.LinearLayoutManager layoutManager = (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        fetchStudents(lastVisible);
                    }
                }
            }
        });
    }

    private void showDeleteConfirm(com.edu.track.models.Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Remove " + student.getName() + " from the system?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.collection("students").document(student.getStudentId()).delete();
                    Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show();
                    // Real-time listener would handle this normally, but for now manual remove
                    studentList.remove(student);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchStudents(com.google.firebase.firestore.DocumentSnapshot startAfter) {
        isLoading = true;
        if (startAfter == null) {
            shimmerView.setVisibility(View.VISIBLE);
            shimmerView.startShimmer();
            rvStudents.setVisibility(View.GONE);
            studentList.clear();
        } else {
            progressLoadMore.setVisibility(View.VISIBLE);
        }

        com.google.firebase.firestore.Query query = db.collection("students")
                .orderBy("name")
                .limit(PAGE_SIZE);

        if (fixedStandard != null) {
            query = query.whereEqualTo("standard", fixedStandard);
            if (fixedDivision != null) {
                query = query.whereEqualTo("division", fixedDivision);
            }
        } else if (!selectedClassId.equals("ALL")) {
            // classId is std+div, e.g. "8A"
            String std = selectedClassId.replaceAll("[^0-9]", "");
            String div = selectedClassId.replaceAll("[^A-Za-z]", "");
            query = query.whereEqualTo("standard", std).whereEqualTo("division", div);
        }

        if (startAfter != null) {
            query = query.startAfter(startAfter);
        }

        query.get().addOnSuccessListener(documentSnapshots -> {
            isLoading = false;
            shimmerView.stopShimmer();
            shimmerView.setVisibility(View.GONE);
            rvStudents.setVisibility(View.VISIBLE);
            progressLoadMore.setVisibility(View.GONE);

            if (documentSnapshots.isEmpty()) {
                isLastPage = true;
                return;
            }

            studentList.addAll(documentSnapshots.toObjects(com.edu.track.models.Student.class));
            adapter.updateList(studentList);

            if (documentSnapshots.size() < PAGE_SIZE) {
                isLastPage = true;
            } else {
                lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
            }
        }).addOnFailureListener(e -> {
            isLoading = false;
            Toast.makeText(this, "Error fetching students", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                String[] options = {"Add Manually", "Import via CSV", "Download CSV Template"};
                new AlertDialog.Builder(this)
                        .setTitle("Add Students")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                Intent intent = new Intent(this, AddEditStudentActivity.class);
                                if (fixedStandard != null) intent.putExtra("standard", fixedStandard);
                                if (fixedDivision != null) intent.putExtra("division", fixedDivision);
                                startActivity(intent);
                            } else if (which == 1) {
                                openFilePickerForCsv();
                            } else if (which == 2) {
                                downloadCsvTemplate();
                            }
                        })
                        .show();
            });
        }
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    if (s.length() > 0) {
                        adapter.filter(s.toString());
                    } else {
                        adapter.filter("");
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void openFilePickerForCsv() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        csvPickerLauncher.launch(intent);
    }

    private void parseCsvAndUpload(android.net.Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            String line;
            boolean isFirstRow = true;
            java.util.List<com.edu.track.models.Student> newStudents = new java.util.ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (isFirstRow) { isFirstRow = false; continue; } // skip header
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    com.edu.track.models.Student s = new com.edu.track.models.Student();
                    s.setStudentId(java.util.UUID.randomUUID().toString());
                    s.setName(parts[0].trim());
                    try {
                        s.setRollNumber(Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException e) {
                        s.setRollNumber(0);
                    }
                    s.setStandard(parts[2].trim());
                    s.setDivision(parts[3].trim());
                    s.setParentEmail(parts[4].trim());
                    s.setParentPhone(parts[5].trim());
                    newStudents.add(s);
                }
            }
            reader.close();

            if (newStudents.isEmpty()) {
                Toast.makeText(this, "No valid students found in CSV", Toast.LENGTH_SHORT).show();
                return;
            }

            progressLoadMore.setVisibility(View.VISIBLE);
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            for (com.edu.track.models.Student st : newStudents) {
                batch.set(db.collection("students").document(st.getStudentId()), st);
            }
            batch.commit().addOnSuccessListener(aVoid -> {
                progressLoadMore.setVisibility(View.GONE);
                Toast.makeText(this, "Imported " + newStudents.size() + " students successfully!", Toast.LENGTH_LONG).show();
                isLastPage = false;
                lastVisible = null;
                fetchStudents(null);
            }).addOnFailureListener(e -> {
                progressLoadMore.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to import students.", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadCsvTemplate() {
        String template = "Name,RollNumber,Standard,Division,ParentEmail,ParentPhone\nJohn Doe,1,8,A,johnparent@example.com,1234567890\n";
        com.edu.track.utils.ReportManager.exportToCSV(this, "Student_Import_Template", "Templates", template, new com.edu.track.utils.ReportManager.ExportCallback() {
            @Override
            public void onSuccess(String filePath) {
                com.edu.track.utils.ReportManager.showExportSuccessDialog(ManageStudentsActivity.this, filePath);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ManageStudentsActivity.this, "Error generating template: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
