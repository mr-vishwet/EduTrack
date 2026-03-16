package com.edu.track.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.models.ReportItem;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private Context context;
    private List<ReportItem> reportItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ReportItem item);
    }

    public ReportAdapter(Context context, List<ReportItem> reportItems) {
        this.context = context;
        this.reportItems = reportItems;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportItem item = reportItems.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvValue1.setText(item.getValue1());
        holder.tvValue2.setText(item.getValue2());
        
        if (holder.layoutTags != null) {
            holder.layoutTags.removeAllViews();
            if (item.getTags() != null && !item.getTags().isEmpty()) {
                holder.layoutTags.setVisibility(View.VISIBLE);
                for (String tag : item.getTags()) {
                    TextView tv = new TextView(context);
                    tv.setText(tag);
                    tv.setTextSize(10f);
                    tv.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.primary_blue));
                    tv.setBackgroundResource(R.drawable.bg_chip_unselected);
                    tv.setPadding(24, 8, 24, 8);
                    
                    android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMarginEnd(12);
                    holder.layoutTags.addView(tv, params);
                }
            } else {
                holder.layoutTags.setVisibility(View.GONE);
            }
        }

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return reportItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvValue1, tvValue2;
        android.widget.LinearLayout layoutTags;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvValue1 = itemView.findViewById(R.id.tv_item_value1);
            tvValue2 = itemView.findViewById(R.id.tv_item_value2);
            layoutTags = itemView.findViewById(R.id.layout_tags);
        }
    }
}
