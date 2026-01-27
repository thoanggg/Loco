package com.myapp.loco;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PojoTest {

    @Test
    void testAgentPojo() {
        Agent agent = new Agent("Agent1", "192.168.1.10", "Online", "User1", "10:00");

        assertEquals("Agent1", agent.getName());
        assertEquals("192.168.1.10", agent.getIp());
        assertEquals("Online", agent.getStatus());
        assertEquals("User1", agent.getUser());
        assertEquals("10:00", agent.getLastSeen());

        // Test Setters via Property binding
        agent.setName("Agent2");
        agent.setStatus("Offline");
        agent.setUser("Admin");
        agent.lastSeenProperty().set("11:00");

        assertEquals("Agent2", agent.getName());
        assertEquals("Offline", agent.getStatus());
        assertEquals("Admin", agent.getUser());
        assertEquals("11:00", agent.getLastSeen());

        // Test Properties for JavaFX
        assertNotNull(agent.nameProperty());
        assertNotNull(agent.ipProperty());
        assertNotNull(agent.statusProperty());
        assertNotNull(agent.userProperty());
        assertNotNull(agent.lastSeenProperty());
    }

    @Test
    void testDetectionRulePojo() {
        DetectionRule rule = new DetectionRule("Rule1", "Field1", "Contains", "Value1", "High");

        assertEquals("Rule1", rule.getName());
        assertEquals("Field1", rule.getField());
        assertEquals("Contains", rule.getCondition());
        assertEquals("Value1", rule.getValue());
        assertEquals("High", rule.getSeverity());

        // DetectionRule is immutable (no setters), checking properties
        assertNotNull(rule.nameProperty());
        assertNotNull(rule.fieldProperty());
        assertNotNull(rule.conditionProperty());
        assertNotNull(rule.valueProperty());
        assertNotNull(rule.severityProperty());
    }

    @Test
    void testLogRequestPojo() {
        LogRequest req = new LogRequest();
        req.setLogChannel("Application");
        assertEquals("Application", req.getLogChannel());
    }
}
