package com.myapp.loco;

import javafx.beans.property.SimpleStringProperty;
import java.util.Map;
import java.util.HashMap;

public class LogEvent {
    private final SimpleStringProperty eventId;
    private final SimpleStringProperty timeCreated;
    private final SimpleStringProperty providerName;
    private final SimpleStringProperty level;
    private final SimpleStringProperty description;
    private final SimpleStringProperty user;
    private final SimpleStringProperty host;
    private final String fullDetails;
    private final Map<String, String> eventData; // Structured data for Rules Engine

    // --- Alert Flags ---
    private boolean isAlert = false;
    private String alertSeverity = "";
    private String detectionName = "";
    private String mitreId = ""; // Txxxx
    private final SimpleStringProperty status = new SimpleStringProperty("Not Acknowledged");

    private LogEvent(Builder builder) {
        this.eventId = new SimpleStringProperty(builder.eventId);
        this.timeCreated = new SimpleStringProperty(builder.timeCreated);
        this.providerName = new SimpleStringProperty(builder.providerName);
        this.level = new SimpleStringProperty(builder.level);
        this.description = new SimpleStringProperty(builder.description);
        this.user = new SimpleStringProperty(builder.user);
        this.host = new SimpleStringProperty(builder.host);
        this.fullDetails = builder.fullDetails;
        this.eventData = builder.eventData != null ? builder.eventData : new HashMap<>();
    }

    // Legacy Constructor for backward compatibility (delegates to Builder)
    public LogEvent(String eventId, String timeCreated, String providerName, String level, String description,
            String user, String host, String fullDetails, Map<String, String> eventData) {
        this(new Builder()
                .eventId(eventId)
                .timeCreated(timeCreated)
                .providerName(providerName)
                .level(level)
                .description(description)
                .user(user)
                .host(host)
                .fullDetails(fullDetails)
                .eventData(eventData));
    }

    public static class Builder {
        private String eventId;
        private String timeCreated;
        private String providerName;
        private String level;
        private String description;
        private String user;
        private String host;
        private String fullDetails;
        private Map<String, String> eventData;

        public Builder eventId(String val) {
            eventId = val;
            return this;
        }

        public Builder timeCreated(String val) {
            timeCreated = val;
            return this;
        }

        public Builder providerName(String val) {
            providerName = val;
            return this;
        }

        public Builder level(String val) {
            level = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Builder user(String val) {
            user = val;
            return this;
        }

        public Builder host(String val) {
            host = val;
            return this;
        }

        public Builder fullDetails(String val) {
            fullDetails = val;
            return this;
        }

        public Builder eventData(Map<String, String> val) {
            eventData = val;
            return this;
        }

        public LogEvent build() {
            return new LogEvent(this);
        }
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