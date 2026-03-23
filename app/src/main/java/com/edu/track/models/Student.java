package com.edu.track.models;

public class Student {
    private String studentId;
    private String name;
    private String standard;
    private String division;
    private int rollNumber;
    private String parentUid;
    private String parentEmail;
    private String parentPhone;
    private String dob;
    private String birthdayCertificateUrl;
    private boolean isActive;

    public Student() {}

    public Student(String studentId, String name, String standard, String division, int rollNumber, String parentUid) {
        this.studentId = studentId;
        this.name = name;
        this.standard = standard;
        this.division = division;
        this.rollNumber = rollNumber;
        this.parentUid = parentUid;
        this.isActive = true;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }
    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }
    public int getRollNumber() { return rollNumber; }
    public void setRollNumber(int rollNumber) { this.rollNumber = rollNumber; }
    public String getParentUid() { return parentUid; }
    public void setParentUid(String parentUid) { this.parentUid = parentUid; }
    public String getParentEmail() { return parentEmail; }
    public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }
    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getBirthdayCertificateUrl() { return birthdayCertificateUrl; }
    public void setBirthdayCertificateUrl(String birthdayCertificateUrl) { this.birthdayCertificateUrl = birthdayCertificateUrl; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
