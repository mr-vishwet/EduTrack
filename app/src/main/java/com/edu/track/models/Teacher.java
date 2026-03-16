package com.edu.track.models;

import java.util.List;

public class Teacher {
    private String uid;
    private String name;
    private String email;
    private String expertise;
    private List<String> subjects;
    private List<String> assignedClasses;
    private boolean isActive;

    public Teacher() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    public List<String> getAssignedClasses() { return assignedClasses; }
    public void setAssignedClasses(List<String> assignedClasses) { this.assignedClasses = assignedClasses; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
