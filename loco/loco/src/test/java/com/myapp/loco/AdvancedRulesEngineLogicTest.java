package com.myapp.loco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedRulesEngineLogicTest {

    @BeforeEach
    void setup() {
        // Ensure clean state if necessary (though engine is static)
    }

    private LogEvent createLog(String eventId, Map<String, String> data) {
        return new LogEvent.Builder()
                .eventId(eventId)
                .timeCreated("Now")
                .providerName("Prov")
                .level("Info")
                .description("Desc")
                .user("User")
                .host("Host")
                .fullDetails("Details")
                .eventData(data != null ? data : new HashMap<>())
                .build();
    }

    @Test
    void testUnsignedExecutable() {
        Map<String, String> data = new HashMap<>();
        data.put("Image", "C:\\Users\\Public\\malware.exe");
        data.put("Signed", "false");

        LogEvent log = createLog("4688", data);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("Unsigned Executable in Suspect Folder", log.getDetectionName());
    }

    @Test
    void testMimikatz() {
        Map<String, String> data = new HashMap<>();
        data.put("CommandLine", "sekurlsa::logonpasswords");

        LogEvent log = createLog("4688", data);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("Mimikatz Activity Detected", log.getDetectionName());
    }

    @Test
    void testStickyKeys() {
        Map<String, String> data = new HashMap<>();
        data.put("ObjectName", "Image File Execution Options\\sethc.exe");

        LogEvent log = createLog("4657", data);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("Sticky Keys Backdoor Attempt", log.getDetectionName());
    }

    @Test
    void testNewService() {
        LogEvent log = createLog("4697", null);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("New Service Installed", log.getDetectionName());
    }

    @Test
    void testLogClearing() {
        LogEvent log = createLog("1102", null);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("Security Log Cleared", log.getDetectionName());

        // Test command line variant
        Map<String, String> data = new HashMap<>();
        data.put("CommandLine", "wevtutil cl security");
        LogEvent log2 = createLog("4688", data);
        AdvancedRulesEngine.applyRules(log2); // Fixed from LogEvent log to log2
        assertTrue(log2.isAlert());
        assertEquals("Log Clearing Attempt Command", log2.getDetectionName());
    }

    @Test
    void testRdpLogin() {
        Map<String, String> data = new HashMap<>();
        data.put("LogonType", "10");

        LogEvent log = createLog("4624", data);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("RDP Login Detected", log.getDetectionName());
    }

    @Test
    void testSuspiciousNetConn() {
        Map<String, String> data = new HashMap<>();
        data.put("Image", "notepad.exe");
        data.put("DestinationPort", "80");

        LogEvent log = createLog("3", data);
        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert());
        assertEquals("Suspicious Process Network Connection", log.getDetectionName());
    }
}
