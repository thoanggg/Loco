package com.myapp.loco;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private static final String TEST_DB = "target/test_loco.db";

    @BeforeEach
    void setUp() throws Exception {
        // Set DB URL for testing
        DatabaseManager.setDbUrl("jdbc:sqlite:" + TEST_DB);

        // Reset Singleton instance
        Field instance = DatabaseManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        // Delete previous DB file to ensure fresh state
        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        // Cleanup
        // We might need to close connection? DatabaseManager opens/closes on each op.
        // So file should be deletable if not locked.
        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testSingleton() {
        DatabaseManager db1 = DatabaseManager.getInstance();
        DatabaseManager db2 = DatabaseManager.getInstance();
        assertSame(db1, db2);
    }

    @Test
    void testAgentOps() {
        DatabaseManager db = DatabaseManager.getInstance();

        Agent a = new Agent("Agent1", "1.2.3.4", "Online", "User1", "Now");
        db.upsertAgent(a);

        List<Agent> agents = db.getAllAgents();
        assertEquals(1, agents.size());
        assertEquals("Agent1", agents.get(0).getName());

        db.removeAgent("1.2.3.4");
        assertEquals(0, db.getAllAgents().size());
    }

    @Test
    void testAlertOps() {
        DatabaseManager db = DatabaseManager.getInstance();
        LogEvent log = new LogEvent.Builder()
                .eventId("100")
                .timeCreated("2023-01-01")
                .providerName("TestProvider")
                .level("Info")
                .description("Test Description")
                .user("User")
                .host("Host")
                .fullDetails("Details")
                .eventData(Map.of("key", "value"))
                .build();
        log.setAlert(true);
        log.setDetectionName("Test Rule");
        log.setAlertSeverity("High");
        log.setStatus("New");

        db.insertAlert(log);

        List<LogEvent> alerts = db.getAllAlerts();
        assertEquals(1, alerts.size());
        assertEquals("Test Rule", alerts.get(0).getDetectionName());

        log.setStatus("Resolved");
        db.updateAlertStatus(log);

        // Fetch again to verify update
        alerts = db.getAllAlerts();
        assertEquals("Resolved", alerts.get(0).getStatus());
    }
}
