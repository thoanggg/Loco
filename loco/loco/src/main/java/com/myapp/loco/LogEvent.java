package com.myapp.loco;

import javafx.beans.property.SimpleStringProperty;

/**
 * Lớp (Model) đại diện cho một sự kiện log
 * Được sử dụng để hiển thị trong TableView.
 */
public class LogEvent {
    private final SimpleStringProperty eventId;
    private final SimpleStringProperty timeCreated;
    private final SimpleStringProperty providerName;
    private final SimpleStringProperty level;
    private final SimpleStringProperty description; // Dòng tóm tắt
    private final String fullDetails; // Toàn bộ chi tiết

    public LogEvent(String eventId, String timeCreated, String providerName, String level, String description, String fullDetails) {
        this.eventId = new SimpleStringProperty(eventId);
        this.timeCreated = new SimpleStringProperty(timeCreated);
        this.providerName = new SimpleStringProperty(providerName);
        this.level = new SimpleStringProperty(level);
        this.description = new SimpleStringProperty(description);
        this.fullDetails = fullDetails;
    }

    // --- Getters (bắt buộc phải có để PropertyValueFactory hoạt động) ---

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

    // Getter cho fullDetails (dùng cho cửa sổ pop-up)
    public String getFullDetails() {
        return fullDetails;
    }
}