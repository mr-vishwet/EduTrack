package com.edu.track.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edu.track.R;
import com.edu.track.models.User;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private final List<User> teacherList;
    private final OnTeacherClickListener listener;

    public interface OnTeacherClickListener {
        void onAssign(User teacher);
    }

    public TeacherAdapter(List<User> teacherList, OnTeacherClickListener listener) {
        this.teacherList = teacherList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_row, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        User teacher = teacherList.get(position);
        holder.bind(teacher, listener);
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvSubject, tvClassTag;
        MaterialButton btnAssign;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tv_teacher_initials);
            tvName = itemView.findViewById(R.id.tv_teacher_name);
            tvSubject = itemView.findViewById(R.id.tv_teacher_subject);
            tvClassTag = itemView.findViewById(R.id.tv_class_teacher_tag);
            btnAssign = itemView.findViewById(R.id.btn_assign);
        }

        public void bind(User teacher, OnTeacherClickListener listener) {
            tvName.setText(teacher.getName() != null ? teacher.getName() : "Unknown");

            // Initials
            String initials = "T";
            if (teacher.getName() != null && !teacher.getName().isEmpty()) {
                String[] words = teacher.getName().split(" ");
                if (words.length > 1) {
                    initials = (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
                } else {
                    initials = words[0].substring(0, 1).toUpperCase();
                }
            }
            tvInitials.setText(initials);

            // Expertise and Class Teacher Tag
            tvSubject.setText(teacher.getDisplayExpertise());
            
            String classT = teacher.getClassTeacher();
            if (classT != null && !classT.isEmpty()) {
                tvClassTag.setVisibility(View.VISIBLE);
                tvClassTag.setText("Class Teacher: " + classT);
            } else {
                tvClassTag.setVisibility(View.GONE);
            }

            btnAssign.setOnClickListener(v -> listener.onAssign(teacher));
        }
    }
}
