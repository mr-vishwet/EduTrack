package com.edu.track.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.track.R;
import com.edu.track.models.Student;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final List<Student> studentList;
    private final OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onEdit(Student student);
        void onDelete(Student student);
    }

    public StudentAdapter(List<Student> studentList, OnStudentClickListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_row, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvName, tvDetails;
        ImageView btnEdit, btnDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tv_roll_number);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvDetails = itemView.findViewById(R.id.tv_student_details);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Student student, OnStudentClickListener listener) {
            // Extract roll number from ID (e.g., ROLL_1_1A -> 1)
            String roll = "0";
            if (student.getStudentId() != null && student.getStudentId().startsWith("ROLL_")) {
                String[] parts = student.getStudentId().split("_");
                if (parts.length > 1) roll = parts[1];
            }
            if (roll.length() == 1) roll = "0" + roll;

            tvRoll.setText(roll);
            tvName.setText(student.getName());
            tvDetails.setText("Class " + student.getStandard() + student.getDivision());

            btnEdit.setOnClickListener(v -> listener.onEdit(student));
            btnDelete.setOnClickListener(v -> listener.onDelete(student));
        }
    }
}
