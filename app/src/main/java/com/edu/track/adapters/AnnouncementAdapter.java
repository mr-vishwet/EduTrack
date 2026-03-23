package com.edu.track.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edu.track.R;
import com.edu.track.models.Announcement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private List<Announcement> announcements = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

    public void setAnnouncements(List<Announcement> announcements) {
        this.announcements = announcements;
        notifyDataSetChanged();
    }

    public AnnouncementAdapter(List<Announcement> initialList) {
        if (initialList != null) this.announcements = initialList;
    }

    public AnnouncementAdapter() {}

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement announcement = announcements.get(position);
        holder.tvTitle.setText(announcement.getTitle());
        holder.tvContent.setText(announcement.getContent());
        
        long ts = announcement.getTimestampMillis();
        holder.tvDate.setText(ts != 0 ? dateFormat.format(new java.util.Date(ts)) : "N/A");
        
        // Show class tag if subject level, else show category
        if (announcement.isSubjectLevel() && announcement.getClassId() != null) {
            holder.tvCategory.setText("Class " + announcement.getClassId());
        } else {
            String cat = announcement.getCategory();
            if (cat == null || cat.equalsIgnoreCase("All")) {
                cat = announcement.getAudience() != null ? announcement.getAudience() : "General";
            }
            holder.tvCategory.setText(cat);
        }

        // Handle View More
        if (announcement.getContent() != null && announcement.getContent().length() > 100) {
            holder.tvViewMore.setVisibility(View.VISIBLE);
        } else {
            holder.tvViewMore.setVisibility(View.GONE);
        }

        View.OnClickListener clickListener = v -> {
            android.content.Context ctx = v.getContext();
            android.content.Intent intent = new android.content.Intent(ctx, com.edu.track.activities.AnnouncementDetailActivity.class);
            intent.putExtra("title", announcement.getTitle());
            intent.putExtra("content", announcement.getContent());
            intent.putExtra("date", holder.tvDate.getText().toString());
            intent.putExtra("metadata", holder.tvCategory.getText().toString());
            ctx.startActivity(intent);
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.tvViewMore.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvDate, tvCategory, tvViewMore;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_announcement_title);
            tvContent = itemView.findViewById(R.id.tv_announcement_content);
            tvDate = itemView.findViewById(R.id.tv_announcement_date);
            tvCategory = itemView.findViewById(R.id.tv_announcement_category);
            tvViewMore = itemView.findViewById(R.id.tv_view_more);
        }
    }
}
