package com.edu.track.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.track.R;
import com.edu.track.models.SchoolClass;
import java.util.List;

public class SchoolClassAdapter extends RecyclerView.Adapter<SchoolClassAdapter.ClassViewHolder> {

    private final List<SchoolClass> classList;
    private final OnClassClickListener listener;

    public interface OnClassClickListener {
        void onClassClick(SchoolClass schoolClass);
    }

    public SchoolClassAdapter(List<SchoolClass> classList, OnClassClickListener listener) {
        this.classList = classList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class_row, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        SchoolClass schoolClass = classList.get(position);
        holder.bind(schoolClass, listener);
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvStudentCount, tvTeacherName;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tv_class_name);
            tvStudentCount = itemView.findViewById(R.id.tv_student_count);
            tvTeacherName = itemView.findViewById(R.id.tv_class_teacher);
        }

        public void bind(SchoolClass schoolClass, OnClassClickListener listener) {
            tvClassName.setText("Class " + schoolClass.getClassId());
            tvStudentCount.setText(schoolClass.getStudentCount() + " Students");
            tvTeacherName.setText("Teacher: " + (schoolClass.getClassTeacherName() != null ? schoolClass.getClassTeacherName() : "Not Assigned"));

            itemView.setOnClickListener(v -> listener.onClassClick(schoolClass));
        }
    }
}
