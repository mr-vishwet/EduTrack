package com.edu.track.activities.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.edu.track.R;
import com.edu.track.models.Student;
import com.edu.track.models.User;
import com.edu.track.utils.FirebaseSource;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.WriteBatch;
import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BulkUploadActivity extends AppCompatActivity {

    private static final String TAG = "BulkUploadActivity";
    private Uri selectedFileUri;
    private MaterialButton btnUploadSave;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        btnUploadSave.setEnabled(true);
                        Toast.makeText(this, "File selected: " + selectedFileUri.getPath(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulk_upload);

        btnUploadSave = findViewById(R.id.btn_upload_save);
        progressBar = findViewById(R.id.footer_save).findViewById(R.id.progress_bar); // Assuming there's one, or adding it

        setupClickListeners();
    }

    private void setupClickListeners() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_download_template).setOnClickListener(v -> 
            Toast.makeText(this, "CSV Template: roll_no,name,standard,division,parent_phone", Toast.LENGTH_LONG).show());

        findViewById(R.id.btn_choose_file).setOnClickListener(v -> openFilePicker());
            
        btnUploadSave.setOnClickListener(v -> processCsvFile());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*"); // More generic to catch all CSV types on different Android versions
        String[] mimeTypes = {"text/csv", "application/csv", "text/comma-separated-values"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void processCsvFile() {
        if (selectedFileUri == null) return;

        btnUploadSave.setEnabled(false);
        // Show progress if exists
        
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            if (inputStream == null) throw new Exception("Could not open file");

            CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
            List<String[]> lines = reader.readAll();
            reader.close();

            if (lines.size() <= 1) {
                Toast.makeText(this, "CSV file is empty or missing headers", Toast.LENGTH_SHORT).show();
                btnUploadSave.setEnabled(true);
                return;
            }

            // Skip header
            List<Student> students = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] row = lines.get(i);
                if (row.length < 4) continue;

                String rollStr = row[0].trim();
                String name = row[1].trim();
                String std = row[2].trim();
                String div = row[3].trim();
                String phone = row.length > 4 ? row[4].trim() : "";

                // Generate IDs
                String studentId = std.toLowerCase() + "_" + div.toLowerCase() + "_" + rollStr;
                int rollNum = Integer.parseInt(rollStr);

                Student student = new Student(studentId, name, std, div, rollNum, studentId); // parentUid = studentId for simple mapping
                student.setParentPhone(phone);
                students.add(student);
            }

            uploadToFirestore(students);

        } catch (Exception e) {
            Log.e(TAG, "CSV Processing error", e);
            Toast.makeText(this, "Error processing CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnUploadSave.setEnabled(true);
        }
    }

    private void uploadToFirestore(List<Student> students) {
        WriteBatch batch = FirebaseSource.getInstance().getFirestore().batch();

        for (Student s : students) {
            // 1. Create Student record
            batch.set(FirebaseSource.getInstance().getStudentsRef().document(s.getStudentId()), s);

            // 2. Create User record for Parent (login ID: roll_no_{num}_{classid})
            String loginId = "roll_no_" + s.getRollNumber() + "_" + s.getStandard().toLowerCase() + s.getDivision().toLowerCase();
            String email = loginId + "@edutrack.com";
            
            // We don't create real Firebase Auth accounts here because that requires Admin SDK or logic.
            // But we create the 'User' document in 'users' collection so LoginActivity can find it.
            // Note: Password will be checked via Firebase Auth, so one would need to seed Auth too.
            // For now, we follow the user's seed approach which assumes Auth accounts exist.
            
            User parentUser = new User(s.getStudentId(), s.getName() + " Parent", email, s.getParentPhone(), "PARENT");
            batch.set(FirebaseSource.getInstance().getUsersRef().document(s.getStudentId()), parentUser);
        }

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Successfully uploaded " + students.size() + " students!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Log.e(TAG, "Batch write failed", task.getException());
                Toast.makeText(this, "Failed to upload: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                btnUploadSave.setEnabled(true);
            }
        });
    }
}
