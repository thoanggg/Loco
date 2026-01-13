package com.myapp.loco.sigma;

import java.util.List;
import java.util.Map;

public class SigmaRule {
    private String title;
    private String id;
    private String status;
    private String description;
    private String author;
    private Map<String, String> logsource;
    private Map<String, Object> detection;
    private String level;
    private List<String> tags;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, String> getLogsource() {
        return logsource;
    }

    public void setLogsource(Map<String, String> logsource) {
        this.logsource = logsource;
    }

    public Map<String, Object> getDetection() {
        return detection;
    }

    public void setDetection(Map<String, Object> detection) {
        this.detection = detection;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
