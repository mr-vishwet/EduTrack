package com.edu.track.models;

public class Announcement {
    private String id;
    private String title;
    private String content;
    private String audience;
    private boolean isPinned;
    private String author;
    private java.util.Date timestamp;

    public Announcement() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public java.util.Date getTimestamp() { return timestamp; }
    public void setTimestamp(java.util.Date timestamp) { this.timestamp = timestamp; }
}
