package com.myapp.loco;

import javafx.beans.property.SimpleStringProperty;

public class LogEvent {
    private final SimpleStringProperty eventId;
    private final SimpleStringProperty timeCreated;
    private final SimpleStringProperty providerName;
    private final SimpleStringProperty level;
    private final SimpleStringProperty description;
    private final SimpleStringProperty user;
    private final SimpleStringProperty host;
    private final String fullDetails;
    private final java.util.Map<String, String> eventData; // Structured data for Rules Engine

    // --- THÊM MỚI: Cờ đánh dấu Alert ---
    private boolean isAlert = false;
    private String alertSeverity = "";
    private String detectionName = "";
    private String mitreId = ""; // Txxxx
    private final SimpleStringProperty status = new SimpleStringProperty("Not Acknowledged");

    public LogEvent(String eventId, String timeCreated, String providerName, String level, String description,
            String user, String host, String fullDetails, java.util.Map<String, String> eventData) {
        this.eventId = new SimpleStringProperty(eventId);
        this.timeCreated = new SimpleStringProperty(timeCreated);
        this.providerName = new SimpleStringProperty(providerName);
        this.level = new SimpleStringProperty(level);
        this.description = new SimpleStringProperty(description);
        this.user = new SimpleStringProperty(user);
        this.host = new SimpleStringProperty(host);
        this.fullDetails = fullDetails;
        this.eventData = eventData != null ? eventData : new java.util.HashMap<>();
    }

    public String getEventId() {
        return eventId.get();
    }

    public SimpleStringProperty eventIdProperty() {
        return eventId;
    }

    public String getTimeCreated() {
        return timeCreated.get();
    }

    public SimpleStringProperty timeCreatedProperty() {
        return timeCreated;
    }

    public String getProviderName() {
        return providerName.get();
    }

    public SimpleStringProperty providerNameProperty() {
        return providerName;
    }

    public String getLevel() {
        return level.get();
    }

    public SimpleStringProperty levelProperty() {
        return level;
    }

    public String getDescription() {
        return description.get();
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public String getUser() {
        return user.get();
    }

    public SimpleStringProperty userProperty() {
        return user;
    }

    public String getHost() {
        return host.get();
    }

    public SimpleStringProperty hostProperty() {
        return host;
    }

    public String getFullDetails() {
        return fullDetails;
    }

    // --- Getters/Setters cho Alert ---
    public boolean isAlert() {
        return isAlert;
    }

    public void setAlert(boolean alert) {
        isAlert = alert;
    }

    public String getAlertSeverity() {
        return alertSeverity;
    }

    public void setAlertSeverity(String severity) {
        this.alertSeverity = severity;
    }

    public String getDetectionName() {
        return detectionName;
    }

    public void setDetectionName(String detectionName) {
        this.detectionName = detectionName;
    }

    public String getMitreId() {
        return mitreId;
    }

    public void setMitreId(String mitreId) {
        this.mitreId = mitreId;
    }

    public java.util.Map<String, String> getEventData() {
        return eventData;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String s) {
        this.status.set(s);
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }
}