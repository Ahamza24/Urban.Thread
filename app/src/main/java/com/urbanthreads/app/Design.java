package com.urbanthreads.app;

public class Design implements java.io.Serializable {

    private String id;
    private String imageUrl;
    private String title;
    private String tailorId;
    private String category;
    private java.util.List<String> tags;
    private com.google.firebase.Timestamp timestamp;

    public Design() {
        // Required empty constructor for Firestore
    }

    public Design(String id, String imageUrl, String title, String tailorId, String category, java.util.List<String> tags) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.tailorId = tailorId;
        this.category = category;
        this.tags = tags;
        this.timestamp = com.google.firebase.Timestamp.now();
    }

    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getTitle() { return title; }
    public String getTailorId() { return tailorId; }
    public String getCategory() { return category; }
    public java.util.List<String> getTags() { return tags; }
    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTitle(String title) { this.title = title; }
    public void setTailorId(String tailorId) { this.tailorId = tailorId; }
    public void setCategory(String category) { this.category = category; }
    public void setTags(java.util.List<String> tags) { this.tags = tags; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }
}
