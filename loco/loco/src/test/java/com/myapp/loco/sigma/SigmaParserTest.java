package com.myapp.loco.sigma;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SigmaParserTest {

    @Test
    void testParseValidYaml() {
        String yaml = """
                title: Test Rule
                id: 12345
                status: experimental
                description: A test rule
                author: Tester
                level: high
                logsource:
                    product: windows
                detection:
                    selection:
                        EventID: 4688
                    condition: selection
                """;

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
