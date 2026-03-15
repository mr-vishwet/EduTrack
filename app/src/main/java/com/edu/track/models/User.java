package com.edu.track.models;

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
        } else if (createdAt != null && createdAt.toString().contains("Timestamp")) {
            // Handle Timestamp objects safely without a hard dependency at compile time if possible,
            // but since we have the SDK, we can use the class.
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
}
