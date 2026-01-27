package com.myapp.loco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import com.myapp.loco.sigma.SigmaRule;
import static org.junit.jupiter.api.Assertions.*;

class SigmaIntegrationTest {

    // Use separate DB
    private static final String TEST_DB = "target/test_loco_sigma_int.db";

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.setDbUrl("jdbc:sqlite:" + TEST_DB);
        Field instance = DatabaseManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testDynamicSigmaRuleLifeCycle() {
        // Create a Sigma Rule manually
        SigmaRule rule = new SigmaRule();
        rule.setId("sigma-1");
        rule.setTitle("Test Sigma");
        rule.setLevel("High");

        Map<String, Object> detection = new HashMap<>();
        Map<String, String> selection = new HashMap<>();
        selection.put("EventID", "1234");
        detection.put("selection", selection);
        detection.put("condition", "selection"); // condition string is not actually parsed in MVP yet? code check
        rule.setDetection(detection);

        // Add rule
        AdvancedRulesEngine.addSigmaRule(rule, "original: yaml");

        // Test Trigger
        LogEvent log = new LogEvent.Builder() // Using Builder
                .eventId("1234")
                .build();

        AdvancedRulesEngine.applyRules(log);

        assertTrue(log.isAlert(), "Dynamic Sigma rule should trigger");
        assertEquals("Test Sigma", log.getDetectionName());

        // Remove rule
        boolean removed = AdvancedRulesEngine.removeSigmaRule("sigma-1");
        assertTrue(removed);

        // Test Trigger again (should fail)
        LogEvent log2 = new LogEvent.Builder()
                .eventId("1234")
                .build();
        AdvancedRulesEngine.applyRules(log2); // Should not trigger

        assertFalse(log2.isAlert(), "Removed Sigma rule should NOT trigger");
    }
}
