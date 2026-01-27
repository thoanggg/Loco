package com.myapp.loco;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LogEventCoverageTest {

    @Test
    void testLegacyConstructor() {
        Map<String, String> data = new HashMap<>();
        data.put("k", "v");

        LogEvent log = new LogEvent("1", "Time", "Prov", "Info", "Desc", "User", "Host", "Details", data);

        assertEquals("1", log.getEventId());
        assertEquals("Time", log.getTimeCreated());
        assertEquals("Prov", log.getProviderName());
        assertEquals("Info", log.getLevel());
        assertEquals("Desc", log.getDescription());
        assertEquals("User", log.getUser());
        assertEquals("Host", log.getHost());
        assertEquals("Details", log.getFullDetails());
        assertEquals(data, log.getEventData());
    }

    @Test
    void testGettersAndMutators() {
        LogEvent log = new LogEvent.Builder().build();

        // Core fields are mutable via Properties
        log.eventIdProperty().set("2");
        log.timeCreatedProperty().set("T2");
        log.providerNameProperty().set("P2");
        log.levelProperty().set("Warn");
        log.descriptionProperty().set("D2");
        log.userProperty().set("U2");
        log.hostProperty().set("H2");

        // Full details is immutable after build (String, not Property)
        // EventData is immutable reference, but map content mutable if not wrapped.

        assertEquals("2", log.getEventId());
        assertEquals("T2", log.getTimeCreated());
        assertEquals("P2", log.getProviderName());
        assertEquals("Warn", log.getLevel());
        assertEquals("D2", log.getDescription());
        assertEquals("U2", log.getUser());
        assertEquals("H2", log.getHost());

        // Alert fields have explicit setters
        log.setAlert(true);
        assertTrue(log.isAlert());

        log.setAlertSeverity("Crit");
        assertEquals("Crit", log.getAlertSeverity());

        log.setDetectionName("Det1");
        assertEquals("Det1", log.getDetectionName());

        log.setMitreId("T1");
        assertEquals("T1", log.getMitreId());

        log.setStatus("Ack");
        assertEquals("Ack", log.getStatus());

        // Property Accessors
        assertNotNull(log.statusProperty());
        // assertNotNull(log.alertSeverityProperty()); // Does not exist
        // assertNotNull(log.detectionNameProperty()); // Does not exist
        assertNotNull(log.timeCreatedProperty());

        // assertNotNull(log.getObservableStatus()); // Does not exist
    }
}
