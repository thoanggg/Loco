package com.myapp.loco;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Agent {
    private final StringProperty name; // Hostname
    private final StringProperty ip;
    private final StringProperty status;
    private final StringProperty user;
    private final StringProperty lastSeen;

    public Agent(String name, String ip, String status, String user, String lastSeen) {
        this.name = new SimpleStringProperty(name);
        this.ip = new SimpleStringProperty(ip);
        this.status = new SimpleStringProperty(status);
        this.user = new SimpleStringProperty(user);
        this.lastSeen = new SimpleStringProperty(lastSeen);
    }

    // --- GETTERS & SETTERS ---

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    // QUAN TRỌNG: Setter để cập nhật tên máy
    public void setName(String name) { this.name.set(name); }

    public String getIp() { return ip.get(); }
    public StringProperty ipProperty() { return ip; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }

    public String getUser() { return user.get(); }
    public StringProperty userProperty() { return user; }
    public void setUser(String user) { this.user.set(user); }

    public String getLastSeen() { return lastSeen.get(); }
    public StringProperty lastSeenProperty() { return lastSeen; }
}