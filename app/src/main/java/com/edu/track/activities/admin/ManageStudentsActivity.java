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
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ManageStudentsActivity extends AppCompatActivity {

    private EditText etSearch;
    private FloatingActionButton fabAdd;
    private TextView chipAll, chip8a, chip8b, chip9a, chip9b, chip10a;
    private TextView currentChip;

    // Data handling
    private RecyclerView rvStudents;
    private com.edu.track.adapters.StudentAdapter adapter;
    private java.util.List<com.edu.track.models.Student> studentList = new java.util.ArrayList<>();
    private com.google.firebase.firestore.FirebaseFirestore db;
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
        setupClickListeners();
        setupSearch();
        setupRecyclerView();
        
        fetchStudents(null); // Initial load
    }

    private void initViews() {
        etSearch  = findViewById(R.id.et_search);
        fabAdd    = findViewById(R.id.fab_add_student);
        chipAll   = findViewById(R.id.chip_all);
        chip8a    = findViewById(R.id.chip_8a);
        chip8b    = findViewById(R.id.chip_8b);
        chip9a    = findViewById(R.id.chip_9a);
        chip9b    = findViewById(R.id.chip_9b);
        chip10a   = findViewById(R.id.chip_10a);
        
        rvStudents = findViewById(R.id.rv_students);
        shimmerView = findViewById(R.id.shimmer_view_container);
        progressLoadMore = findViewById(R.id.progress_load_more);

        currentChip = chipAll;

        if (fixedStandard != null && fixedDivision != null) {
            View svChips = findViewById(R.id.sv_chips);
            if (svChips != null) svChips.setVisibility(View.GONE);
        }
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

        // Apply standard and division filter from chips or intent
        String selectedStandard = fixedStandard != null ? fixedStandard : getStandardFromChip(currentChip);
        String selectedDivision = fixedDivision != null ? fixedDivision : getDivisionFromChip(currentChip);
        
        if (!selectedStandard.equals("ALL")) {
            query = query.whereEqualTo("standard", selectedStandard);
            if (!selectedDivision.equals("ALL")) {
                query = query.whereEqualTo("division", selectedDivision);
            }
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

    private String getStandardFromChip(TextView chip) {
        if (chip == chip8a || chip == chip8b) return "8";
        if (chip == chip9a || chip == chip9b) return "9";
        if (chip == chip10a) return "10";
        return "ALL";
    }

    private String getDivisionFromChip(TextView chip) {
        if (chip == chip8a || chip == chip9a || chip == chip10a) return "A";
        if (chip == chip8b || chip == chip9b) return "B";
        return "ALL";
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

        for (TextView chip : new TextView[]{chipAll, chip8a, chip8b, chip9a, chip9b, chip10a}) {
            if (chip != null) {
                chip.setOnClickListener(v -> selectChip(chip));
            }
        }
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

    private void selectChip(TextView selected) {
        if (currentChip == selected) return;
        
        // Reset all chips
        for (TextView chip : new TextView[]{chipAll, chip8a, chip8b, chip9a, chip9b, chip10a}) {
            if (chip != null) {
                chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
                chip.setTextColor(ContextCompat.getColor(this, R.color.gray_body));
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
        // Highlight selected
        selected.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
        selected.setTextColor(ContextCompat.getColor(this, R.color.white));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
        
        currentChip = selected;
        isLastPage = false;
        lastVisible = null;
        fetchStudents(null); // Refresh with filter
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
