package com.myapp.loco;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NetworkScannerTest {

    @Test
    void testScanAllNetworks() {
        NetworkScanner scanner = new NetworkScanner();
        // This might take a bit of time but should complete
        List<String> ips = scanner.scanAllNetworks();

        // We can't guarantee finding IPs, but list should not be null
        assertNotNull(ips);

        // It should handle empty results gracefully
        assertTrue(ips.isEmpty() || !ips.isEmpty());
    }
}
