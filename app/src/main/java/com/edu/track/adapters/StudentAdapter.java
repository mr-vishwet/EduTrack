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
import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;
    private List<Student> studentListFull; // For filtering
    private final OnStudentClickListener listener;

    private boolean isReadOnly = false;

    public interface OnStudentClickListener {
        void onEdit(Student student);
        void onDelete(Student student);
        void onItemClick(Student student);
    }

    public StudentAdapter(List<Student> studentList, OnStudentClickListener listener) {
        this.studentList = studentList;
        this.studentListFull = new ArrayList<>(studentList);
        this.listener = listener;
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        notifyDataSetChanged();
    }

    public void updateList(List<Student> newList) {
        this.studentList = newList;
        this.studentListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        List<Student> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(studentListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Student item : studentListFull) {
                if (item.getName().toLowerCase().contains(filterPattern)) {
                    filteredList.add(item);
                }
            }
        }
        this.studentList = filteredList;
        notifyDataSetChanged();
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
        holder.bind(student, listener, isReadOnly);
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

        public void bind(Student student, OnStudentClickListener listener, boolean isReadOnly) {
            // Extract roll number from ID (e.g., ROLL_1_1A -> 1)
            String roll = "0";
            if (student.getStudentId() != null && student.getStudentId().startsWith("ROLL_")) {
                String[] parts = student.getStudentId().split("_");
                if (parts.length > 1) roll = parts[1];
            } else {
                roll = String.valueOf(student.getRollNumber());
            }
            if (roll.length() == 1) roll = "0" + roll;

            tvRoll.setText(roll);
            tvName.setText(student.getName());
            tvDetails.setText("Class " + student.getStandard() + student.getDivision());

            if (isReadOnly) {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            } else {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> listener.onEdit(student));
                btnDelete.setOnClickListener(v -> listener.onDelete(student));
            }
            
            itemView.setOnClickListener(v -> listener.onItemClick(student));
        }
    }
}
