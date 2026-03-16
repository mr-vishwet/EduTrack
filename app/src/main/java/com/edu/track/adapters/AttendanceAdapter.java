package com.edu.track.adapters;

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

    public interface OnRecordClickListener {
        void onEdit(AttendanceRecord record);
        void onDelete(AttendanceRecord record);
    }

    public AttendanceAdapter(List<AttendanceRecord> records, boolean isTeacher, OnRecordClickListener listener) {
        this.records = records;
        this.isTeacher = isTeacher;
        this.listener = listener;
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
        holder.tvTitle.setText(record.getStandard() + record.getDivision() + " · Attendance");
        holder.tvDate.setText("📅 " + record.getDate());
        holder.tvStudents.setText("👥 " + record.getTotalCount() + " students");
        holder.tvPresent.setText(String.valueOf(record.getPresentCount()));
        holder.tvAbsent.setText(String.valueOf(record.getTotalCount() - record.getPresentCount()));

        if (isTeacher) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(record));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(record));
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStudents, tvPresent, tvAbsent;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_record_title);
            tvDate = itemView.findViewById(R.id.tv_record_date);
            tvStudents = itemView.findViewById(R.id.tv_record_students);
            tvPresent = itemView.findViewById(R.id.tv_present_count);
            tvAbsent = itemView.findViewById(R.id.tv_absent_count);
            btnEdit = itemView.findViewById(R.id.btn_edit_record);
            btnDelete = itemView.findViewById(R.id.btn_delete_record);
        }
    }
}
