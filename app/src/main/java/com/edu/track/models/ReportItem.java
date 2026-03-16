package com.edu.track.models;

public class ReportItem {
    private String title;
    private String value1;
    private String value2;
    private java.util.List<String> tags;

    public ReportItem() {}

    public ReportItem(String title, String value1, String value2) {
        this(title, value1, value2, null);
    }
    
    public ReportItem(String title, String value1, String value2, java.util.List<String> tags) {
        this.title = title;
        this.value1 = value1;
        this.value2 = value2;
        this.tags = tags;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getValue1() { return value1; }
    public void setValue1(String value1) { this.value1 = value1; }

    public String getValue2() { return value2; }
    public void setValue2(String value2) { this.value2 = value2; }
    
    public java.util.List<String> getTags() { return tags; }
    public void setTags(java.util.List<String> tags) { this.tags = tags; }
}
