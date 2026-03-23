package com.edu.track.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.models.AttendanceRecord;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<AttendanceRecord> records;
    private OnRecordClickListener listener;
    private boolean isTeacher;
    /** The classTeacher class string for the logged-in teacher (e.g. "8A"), "" if not a CT */
    private String classTeacherClass = "";

    public interface OnRecordClickListener {
        void onEdit(AttendanceRecord record);
        void onDelete(AttendanceRecord record);
    }

    public AttendanceAdapter(List<AttendanceRecord> records, boolean isTeacher, OnRecordClickListener listener) {
        this.records = records;
        this.isTeacher = isTeacher;
        this.listener = listener;
    }

    public void setClassTeacherClass(String ct) {
        this.classTeacherClass = ct != null ? ct.toUpperCase() : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_history_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        String classId = (record.getStandard() != null ? record.getStandard() : "")
                       + (record.getDivision() != null ? record.getDivision() : "");

        // Title
        holder.tvTitle.setText("Std " + classId + "  ·  Attendance");
        holder.tvDate.setText("📅 " + record.getDate());
        holder.tvStudents.setText("👥 " + record.getTotalCount() + " students");
        holder.tvPresent.setText(String.valueOf(record.getPresentCount()));
        holder.tvAbsent.setText(String.valueOf(record.getTotalCount() - record.getPresentCount()));

        // ── Teacher mode: Show color tag for class teacher vs subject teacher records ──
        if (isTeacher && holder.tvTag != null) {
            boolean isMyClass = !classTeacherClass.isEmpty()
                    && classId.toUpperCase().equals(classTeacherClass);

            if (isMyClass) {
                // Green tag — this teacher's own class
                holder.tvTag.setVisibility(View.VISIBLE);
                holder.tvTag.setText("My Class");
                holder.tvTag.setTextColor(Color.parseColor("#43A047"));
                holder.tvTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            } else if (!classTeacherClass.isEmpty()) {
                // Blue tag — other/subject class
                holder.tvTag.setVisibility(View.VISIBLE);
                holder.tvTag.setText("Other Class");
                holder.tvTag.setTextColor(Color.parseColor("#1565C0"));
                holder.tvTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            } else {
                holder.tvTag.setVisibility(View.GONE);
            }
        } else if (holder.tvTag != null) {
            holder.tvTag.setVisibility(View.GONE);
        }

        // Edit/Delete only for admin
        if (isTeacher) {
            if (holder.btnEdit != null) holder.btnEdit.setVisibility(View.GONE);
            if (holder.btnDelete != null) holder.btnDelete.setVisibility(View.GONE);
        } else {
            if (holder.btnEdit != null) {
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnEdit.setOnClickListener(v -> listener.onEdit(record));
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> listener.onDelete(record));
            }
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStudents, tvPresent, tvAbsent, tvTag;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_record_title);
            tvDate     = itemView.findViewById(R.id.tv_record_date);
            tvStudents = itemView.findViewById(R.id.tv_record_students);
            tvPresent  = itemView.findViewById(R.id.tv_present_count);
            tvAbsent   = itemView.findViewById(R.id.tv_absent_count);
            tvTag      = itemView.findViewById(R.id.tv_class_tag);   // new
            btnEdit    = itemView.findViewById(R.id.btn_edit_record);
            btnDelete  = itemView.findViewById(R.id.btn_delete_record);
        }
    }
}
