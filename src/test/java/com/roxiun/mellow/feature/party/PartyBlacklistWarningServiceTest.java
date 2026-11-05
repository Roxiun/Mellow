package com.roxiun.mellow.feature.party;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PartyBlacklistWarningServiceTest {
    @Test public void combinesSourcesWithoutExposingImportMarkers() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("Local", "Added from external file");
        sources.put("Coral", "Sniper\nsecond reason");
        String details = PartyBlacklistWarningService.formatDetails(sources);
        assertFalse(details.contains("Added from external file"));
        assertFalse(details.contains("\n"));
        assertTrue(details.contains(" §7| "));
        assertTrue(details.contains("Sniper§7, second reason"));
    }

    @Test public void normalizesMissingReasons() {
        for (String reason : new String[]{null, "  ", "(null)"}) {
            String details = PartyBlacklistWarningService.formatDetails(Collections.singletonMap("Local", reason));
            assertTrue(details.contains("Unknown reason"));
        }
    }
}
