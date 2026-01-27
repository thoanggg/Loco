package com.myapp.loco.sigma;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SigmaParserTest {

    @Test
    void testParseValidYaml() {
        String yaml = "title: Test Rule\n" +
                "id: 12345\n" +
                "status: experimental\n" +
                "description: A test rule\n" +
                "author: Tester\n" +
                "level: high\n" +
                "logsource:\n" +
                "    product: windows\n" +
                "detection:\n" +
                "    selection:\n" +
                "        EventID: 4688\n" +
                "    condition: selection";

        SigmaRule rule = SigmaParser.parse(yaml);

        assertNotNull(rule);
        assertEquals("Test Rule", rule.getTitle());
        assertEquals("12345", rule.getId());
        assertEquals("high", rule.getLevel());
        assertEquals("windows", rule.getLogsource().get("product"));
    }

    @Test
    void testParseInvalidYaml() {
        String invalidYaml = "invalid: [ yaml : content";
        assertThrows(IllegalArgumentException.class, () -> {
            SigmaParser.parse(invalidYaml);
        });
    }

    @Test
    void testParseEmptyYaml() {
        assertThrows(IllegalArgumentException.class, () -> {
            SigmaParser.parse("");
        });
    }
}
