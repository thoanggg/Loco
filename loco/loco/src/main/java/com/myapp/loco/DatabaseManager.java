package com.myapp.loco;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.loco.sigma.SigmaRule;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:loco.db";
    private static DatabaseManager instance;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private DatabaseManager() {
        initialize();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {

            // Agents Table
            stmt.execute("CREATE TABLE IF NOT EXISTS agents (" +
                    "ip TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "user TEXT, " +
                    "status TEXT, " +
                    "last_seen TEXT" +
                    ")");

            // Alerts Table
            stmt.execute("CREATE TABLE IF NOT EXISTS alerts (" +
                    "event_id TEXT, " +
                    "time_created TEXT, " +
                    "provider TEXT, " +
                    "level TEXT, " +
                    "description TEXT, " +
                    "user TEXT, " +
                    "host TEXT, " +
                    "full_details TEXT, " +
                    "event_data TEXT, " + // JSON
                    "alert_severity TEXT, " +
                    "detection_name TEXT, " +
                    "mitre_id TEXT, " +
                    "status TEXT, " + // Acknowledged/Not
                    "PRIMARY KEY (event_id, time_created)" +
                    ")");

            // Dynamic Rules Table
            stmt.execute("CREATE TABLE IF NOT EXISTS rules (" +
                    "id TEXT PRIMARY KEY, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "level TEXT, " +
                    "yaml_content TEXT" +
                    ")");

        } catch (SQLException e) {
            System.err.println("DB Init Failed: " + e.getMessage());
        }
    }

    // --- Agents ---
    public void upsertAgent(Agent agent) {
        String sql = "INSERT OR REPLACE INTO agents(ip, name, user, status, last_seen) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, agent.getIp());
            pstmt.setString(2, agent.getName());
            pstmt.setString(3, agent.getUser());
            pstmt.setString(4, agent.getStatus());
            pstmt.setString(5, agent.getLastSeen());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Upsert Agent Failed: " + e.getMessage());
        }
    }

    public void removeAgent(String ip) {
        String sql = "DELETE FROM agents WHERE ip = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Remove Agent Failed: " + e.getMessage());
        }
    }

    public List<Agent> getAllAgents() {
        List<Agent> agents = new ArrayList<>();
        String sql = "SELECT * FROM agents";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Agent a = new Agent(
                        rs.getString("name"),
                        rs.getString("ip"),
                        rs.getString("status"),
                        rs.getString("user"),
                        rs.getString("last_seen"));
                agents.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Get Agents Failed: " + e.getMessage());
        }
        return agents;
    }

    // --- Alerts ---
    public void insertAlert(LogEvent log) {
        String sql = "INSERT OR IGNORE INTO alerts(event_id, time_created, provider, level, description, user, host, full_details, event_data, alert_severity, detection_name, mitre_id, status) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getEventId());
            pstmt.setString(2, log.getTimeCreated());
            pstmt.setString(3, log.getProviderName());
            pstmt.setString(4, log.getLevel());
            pstmt.setString(5, log.getDescription());
            pstmt.setString(6, log.getUser());
            pstmt.setString(7, log.getHost());
            pstmt.setString(8, log.getFullDetails());
            try {
                pstmt.setString(9, jsonMapper.writeValueAsString(log.getEventData()));
            } catch (JsonProcessingException e) {
                pstmt.setString(9, "{}");
            }
            pstmt.setString(10, log.getAlertSeverity());
            pstmt.setString(11, log.getDetectionName());
            pstmt.setString(12, log.getMitreId());
            pstmt.setString(13, log.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Insert Alert Failed: " + e.getMessage());
        }
    }

    public void updateAlertStatus(LogEvent log) {
        String sql = "UPDATE alerts SET status = ? WHERE event_id = ? AND time_created = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getStatus());
            pstmt.setString(2, log.getEventId());
            pstmt.setString(3, log.getTimeCreated());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update Alert Status Failed: " + e.getMessage());
        }
    }

    public List<LogEvent> getAllAlerts() {
        List<LogEvent> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts ORDER BY time_created DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> eventData = new HashMap<>();
                try {
                    String jsonData = rs.getString("event_data");
                    if (jsonData != null && !jsonData.isEmpty()) {
                        eventData = jsonMapper.readValue(jsonData, new TypeReference<>() {
                        });
                    }
                } catch (Exception e) {
                }

                LogEvent log = new LogEvent(
                        rs.getString("event_id"),
                        rs.getString("time_created"),
                        rs.getString("provider"),
                        rs.getString("level"),
                        rs.getString("description"),
                        rs.getString("user"),
                        rs.getString("host"),
                        rs.getString("full_details"),
                        eventData);
                log.setAlert(true);
                log.setAlertSeverity(rs.getString("alert_severity"));
                log.setDetectionName(rs.getString("detection_name"));
                log.setMitreId(rs.getString("mitre_id"));
                log.setStatus(rs.getString("status"));
                alerts.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Get Alerts Failed: " + e.getMessage());
        }
        return alerts;
    }

    // --- Rules ---
    public void insertRule(SigmaRule rule, String yamlContent) {
        String sql = "INSERT OR REPLACE INTO rules(id, title, description, level, yaml_content) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rule.getId()); // ID is required now
            pstmt.setString(2, rule.getTitle());
            pstmt.setString(3, rule.getDescription());
            pstmt.setString(4, rule.getLevel());
            pstmt.setString(5, yamlContent);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Insert Rule Failed: " + e.getMessage());
        }
    }

    public void deleteRule(String id) {
        String sql = "DELETE FROM rules WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete Rule Failed: " + e.getMessage());
        }
    }

    public List<Map<String, String>> getAllRules() {
        List<Map<String, String>> rules = new ArrayList<>();
        String sql = "SELECT * FROM rules";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> map = new HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("yaml", rs.getString("yaml_content"));
                rules.add(map);
            }
        } catch (SQLException e) {
            System.err.println("Get Rules Failed: " + e.getMessage());
        }
        return rules;
    }
}
