package com.edu.track.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.models.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentReviewAdapter extends RecyclerView.Adapter<StudentReviewAdapter.ViewHolder> {

    public enum Status { PROMOTE, HOLD_BACK, LEFT_SCHOOL }

    public static class StudentPromotion {
        public Student student;
        public Status status = Status.PROMOTE;
        public boolean isSelected = true;

        public StudentPromotion(Student student) {
            this.student = student;
        }
    }

    private List<StudentPromotion> items;
    private final OnStatusChangedListener listener;

    public interface OnStatusChangedListener {
        void onStatusChanged();
    }

    public StudentReviewAdapter(List<Student> students, OnStatusChangedListener listener) {
        this.items = new ArrayList<>();
        for (Student s : students) {
            this.items.add(new StudentPromotion(s));
        }
        this.listener = listener;
    }

    public List<StudentPromotion> getItems() {
        return items;
    }

    public void selectAll(boolean select) {
        for (StudentPromotion item : items) {
            item.isSelected = select;
        }
        notifyDataSetChanged();
        listener.onStatusChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_review_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvRoll, tvName, tvSubtitle;
        Spinner spinnerStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_student);
            tvRoll = itemView.findViewById(R.id.tv_roll_chip);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvSubtitle = itemView.findViewById(R.id.tv_student_subtitle);
            spinnerStatus = itemView.findViewById(R.id.spinner_promo_status);

            String[] options = {"Promote", "Hold Back", "Left School"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_dropdown_item, options);
            spinnerStatus.setAdapter(adapter);
        }

        void bind(StudentPromotion item) {
            checkBox.setChecked(item.isSelected);
            tvName.setText(item.student.getName());
            
            String roll = String.valueOf(item.student.getRollNumber());
            if (roll.length() == 1) roll = "0" + roll;
            tvRoll.setText(roll);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.isSelected = isChecked;
                listener.onStatusChanged();
            });

            spinnerStatus.setSelection(item.status.ordinal());
            
            updateStyles(item);

            spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    item.status = Status.values()[position];
                    updateStyles(item);
                    listener.onStatusChanged();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void updateStyles(StudentPromotion item) {
            if (item.status == Status.LEFT_SCHOOL) {
                itemView.setBackgroundColor(0x00000000);
                tvName.setPaintFlags(tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                tvSubtitle.setText("Will be removed");
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setTextColor(itemView.getContext().getColor(R.color.absent_red));
            } else if (item.status == Status.HOLD_BACK) {
                itemView.setBackgroundColor(0xFFFFF3E0); // Light orange
                tvName.setPaintFlags(tvName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                tvSubtitle.setText("Stays in " + item.student.getStandard() + item.student.getDivision());
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setTextColor(0xFFF57F17);
            } else {
                itemView.setBackgroundColor(0x00000000);
                tvName.setPaintFlags(tvName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                tvSubtitle.setVisibility(View.GONE);
            }
        }
    }
}
