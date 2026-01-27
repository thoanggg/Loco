package com.myapp.loco;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class AdvancedRulesEngineTest {

    @Test
    void testSuspiciousOfficeChildProcess() {
        Map<String, String> data = new HashMap<>();
        data.put("ParentImage", "C:\\Program Files\\Microsoft Office\\root\\Office16\\WINWORD.EXE");
        data.put("Image", "C:\\Windows\\System32\\cmd.exe");

        LogEvent log = new LogEvent.Builder()
                .eventId("4688")
                .timeCreated("2023-01-01T12:00:00")
                .providerName("Microsoft-Windows-Security-Auditing")
                .level("Information")
                .description("Process Creation")
                .user("User")
                .host("Workstation")
                .fullDetails("Details")
                .eventData(data)
                .build();

        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert(), "Should trigger alert for Office -> CMD");
        assertEquals("Suspicious Office Child Process", log.getDetectionName());
        assertEquals("High", log.getAlertSeverity());
    }

    @Test
    void testLsassMemoryAccess() {
        Map<String, String> data = new HashMap<>();
        data.put("TargetImage", "C:\\Windows\\System32\\lsass.exe");
        data.put("GrantedAccess", "0x1F3FFF");
        data.put("SourceImage", "C:\\Temp\\malware.exe");

        LogEvent log = new LogEvent.Builder()
                .eventId("10") // Sysmon ProcessAccess
                .timeCreated("2023-01-01T12:00:00")
                .providerName("Microsoft-Windows-Sysmon")
                .level("Information")
                .description("Process Access")
                .user("User")
                .host("Workstation")
                .fullDetails("Details")
                .eventData(data)
                .build();

        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert(), "Should trigger alert for LSASS access");
        assertEquals("LSASS Memory Access Detected", log.getDetectionName());
        assertEquals("Critical", log.getAlertSeverity());
    }

    @Test
    void testNoAlertNormalProcess() {
        Map<String, String> data = new HashMap<>();
        data.put("ParentImage", "C:\\Windows\\explorer.exe");
        data.put("Image", "C:\\Windows\\System32\\notepad.exe");

        LogEvent log = new LogEvent.Builder()
                .eventId("4688")
                .timeCreated("2023-01-01T12:00:00")
                .providerName("Microsoft-Windows-Security-Auditing")
                .level("Information")
                .description("Process Creation")
                .user("User")
                .host("Workstation")
                .fullDetails("Details")
                .eventData(data)
                .build();

        AdvancedRulesEngine.applyRules(log);

        assertFalse(log.isAlert(), "Should NOT trigger alert for Explorer -> Notepad");
    }
}
