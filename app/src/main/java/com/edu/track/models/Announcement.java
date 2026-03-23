package com.edu.track.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Announcement {
    private String id;
    private String title;
    private String content;
    private String audience;
    private String author; // "ADMIN" or Teacher UID
    private String subjectId;
    private String classId;
    private String targetType; // "school", "subject", or "class"
    private String category; // "Academic", "Events", "Sports", etc.

    // Use @PropertyName to avoid the double-"is" deserialization bug with boolean fields
    @PropertyName("isPinned")
    private boolean pinned;

    @PropertyName("isSubjectLevel")
    private boolean subjectLevel;

    // Firestore stores timestamps as com.google.firebase.Timestamp — NOT long
    private Timestamp timestamp;

    public Announcement() {}

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @PropertyName("isPinned")
    public boolean isPinned() { return pinned; }

    @PropertyName("isPinned")
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    @PropertyName("isSubjectLevel")
    public boolean isSubjectLevel() { return subjectLevel; }

    @PropertyName("isSubjectLevel")
    public void setSubjectLevel(boolean subjectLevel) { this.subjectLevel = subjectLevel; }

    /** Returns timestamp as millis (safe to use for sorting/display) */
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    /** Convenience: millis for sorting */
    public long getTimestampMillis() {
        return timestamp != null ? timestamp.toDate().getTime() : 0L;
    }
}
