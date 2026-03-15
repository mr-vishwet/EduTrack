package com.edu.track.models;

public class SchoolClass {
    private String classId;
    private String standard;
    private String division;
    private String teacherUid;
    private String classTeacherName;
    private int studentCount;

    public SchoolClass() {}

    public SchoolClass(String classId, String standard, String division, String teacherUid, String classTeacherName, int studentCount) {
        this.classId = classId;
        this.standard = standard;
        this.division = division;
        this.teacherUid = teacherUid;
        this.classTeacherName = classTeacherName;
        this.studentCount = studentCount;
    }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }
    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }
    public String getTeacherUid() { return teacherUid; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; }
    public String getClassTeacherName() { return classTeacherName; }
    public void setClassTeacherName(String classTeacherName) { this.classTeacherName = classTeacherName; }
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
}
