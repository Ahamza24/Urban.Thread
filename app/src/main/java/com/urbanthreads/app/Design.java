package com.urbanthreads.app;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.List;

public class Design implements Serializable {

    private String id;
    private String imageUrl;
    private String title;
    private String tailorId;
    private String description;
    private List<String> tags;
    private Timestamp createdAt;

    public Design() {
        // Required empty constructor for Firestore
    }

    public Design(String id, String imageUrl, String title, String tailorId, String description, List<String> tags) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.tailorId = tailorId;
        this.description = description;
        this.tags = tags;
        this.createdAt = Timestamp.now();
    }

    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getTitle() { return title; }
    public String getTailorId() { return tailorId; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTitle(String title) { this.title = title; }
    public void setTailorId(String tailorId) { this.tailorId = tailorId; }
    public void setDescription(String description) { this.description = description; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
