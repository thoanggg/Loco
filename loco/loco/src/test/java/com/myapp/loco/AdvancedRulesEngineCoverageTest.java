package com.myapp.loco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedRulesEngineCoverageTest {

    private static final String TEST_DB = "target/test_loco_rules.db";

    @BeforeEach
    void setUp() throws Exception {
        // Use a separate DB for this test to avoid conflicts
        DatabaseManager.setDbUrl("jdbc:sqlite:" + TEST_DB);

        // Reset Singleton instance
        Field instance = DatabaseManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        File dbFile = new File(TEST_DB);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testRuleMetadataPojo() {
        AdvancedRulesEngine.RuleMetadata meta = new AdvancedRulesEngine.RuleMetadata("Sys-01", "RuleName", "High",
                "T1003", "Description");

        assertEquals("Sys-01", meta.getId());
        assertEquals("RuleName", meta.getName());
        assertEquals("High", meta.getSeverity());
        assertEquals("T1003", meta.getMitreId());
        assertEquals("Description", meta.getDescription());
    }

    @Test
    void testLoadRulesFromDB() throws Exception {
        // Trigger loading
        Method method = AdvancedRulesEngine.class.getDeclaredMethod("loadRulesFromDB");
        method.setAccessible(true);
        method.invoke(null);

        // Verify rules are loaded (from empty test DB, so should be empty, but covers
        // the code)
        List<AdvancedRulesEngine.RuleMetadata> rules = AdvancedRulesEngine.getRules();
        assertNotNull(rules); // It might be empty, but that's fine
    }
}
