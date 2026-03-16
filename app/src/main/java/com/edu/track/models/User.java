package com.edu.track.models;

import com.google.firebase.firestore.Exclude;
import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String role;
    private long createdAt;
    private Map<String, Object> metadata;

    // Teacher-specific fields (stored at top level in 'teachers' collection)
    private String expertise;       // Subject specialty e.g. "Mathematics"
    private String classTeacher;    // e.g. "7B" if this teacher is class teacher of 7B
    private List<String> assignedClasses;  // e.g. ["7B", "8A"]
    private List<String> subjectIds;       // e.g. ["math", "science"]

    public User() {} // Required for Firestore

    public User(String uid, String name, String email, String phone, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public long getCreatedAt() { return createdAt; }

    public void setCreatedAt(Object createdAt) {
        if (createdAt instanceof Long) {
            this.createdAt = (Long) createdAt;
        } else if (createdAt != null) {
            try {
                if (createdAt instanceof com.google.firebase.Timestamp) {
                    this.createdAt = ((com.google.firebase.Timestamp) createdAt).toDate().getTime();
                }
            } catch (Exception e) {
                this.createdAt = System.currentTimeMillis();
            }
        }
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    // Teacher-specific
    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }

    public String getClassTeacher() { return classTeacher; }
    public void setClassTeacher(String classTeacher) { this.classTeacher = classTeacher; }

    public List<String> getAssignedClasses() { return assignedClasses; }
    public void setAssignedClasses(List<String> assignedClasses) { this.assignedClasses = assignedClasses; }

    public List<String> getSubjectIds() { return subjectIds; }
    public void setSubjectIds(List<String> subjectIds) { this.subjectIds = subjectIds; }

    /** Convenience: display subject label (expertise or "—") */
    @Exclude
    public String getDisplayExpertise() {
        if (expertise != null && !expertise.isEmpty()) return expertise;
        if (metadata != null && metadata.containsKey("expertise")) return String.valueOf(metadata.get("expertise"));
        return "—";
    }

    /** Convenience: number of assigned classes */
    @Exclude
    public int getAssignedClassCount() {
        return assignedClasses != null ? assignedClasses.size() : 0;
    }
}
