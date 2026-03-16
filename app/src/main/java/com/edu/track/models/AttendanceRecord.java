package com.edu.track.models;

import java.util.Map;

public class AttendanceRecord {
    private String date;
    private String standard;
    private String division;
    private String subject;
    private String teacherId;
    private Map<String, Boolean> statuses;
    private com.google.firebase.Timestamp timestamp;

    public AttendanceRecord() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public Map<String, Boolean> getStatuses() { return statuses; }
    public void setStatuses(Map<String, Boolean> statuses) { this.statuses = statuses; }

    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }

    public int getPresentCount() {
        if (statuses == null) return 0;
        int count = 0;
        for (Boolean present : statuses.values()) {
            if (present) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return statuses != null ? statuses.size() : 0;
    }
}
