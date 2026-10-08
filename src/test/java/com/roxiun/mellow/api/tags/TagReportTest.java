package com.roxiun.mellow.api.tags;

import com.google.gson.JsonObject;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.feature.tags.TagPolicy;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TagReportTest {
    @Test public void commandsCanPreserveTheirOriginalTagLayouts() {
        TagReport report = new TagReport(Arrays.asList(
            new PlayerTag("Coral", "sniper", "Sniper", "S", true),
            new PlayerTag("Coral", "cheater", "Cheater", "C", true)), Collections.emptyMap());
        assertEquals(Arrays.asList("§5§lCoral§r§5: Sniper", "Cheater"), report.messages());
        assertEquals(Collections.singletonList("§5§lCoral§r§5: Sniper, Cheater"), report.messages(true));
    }

    @Test public void partialFailureRetainsSuccessfulTagsAndWarningPolicy() {
        TagReport report = new TagReport(Arrays.asList(new PlayerTag("Coral", "cheater", "Cheater", "C", true)),
            Collections.singletonMap("Xadia", "HTTP 429"));
        assertEquals(1, TagPolicy.warnings(report, true, false).size());
        assertTrue(TagPolicy.warnings(report, true, true).isEmpty());
        assertTrue(TagPolicy.warnings(report, false, false).isEmpty());
        assertEquals(2, report.messages().size());
        assertTrue(report.messages().get(1).contains("unavailable"));
    }
    @Test public void cubelifyMappingDistinguishesErrorsMetadataAndWarnings() {
        CubelifyParser.Mapping mapping = new CubelifyParser.Mapping() {
            public String error(JsonObject response) { return response.has("error") ? response.get("error").getAsString() : null; }
            public PlayerTag tag(JsonObject tag) {
                if (tag.has("metadata")) return null;
                return new PlayerTag("Example", "risk", tag.get("tooltip").getAsString(), "R", true);
            }
        };
        assertFalse(CubelifyParser.parse("{\"error\":\"Invalid key\",\"tags\":[]}", mapping).isSuccess());
        assertFalse(CubelifyParser.parse("{}", mapping).isSuccess());
        ProviderResult<List<PlayerTag>> parsed = CubelifyParser.parse("{\"tags\":[{\"metadata\":true},{\"tooltip\":\"Risk\"}]}", mapping);
        assertTrue(parsed.isSuccess());
        assertEquals(1, parsed.getValue().size());
        assertEquals("Risk", parsed.getValue().get(0).getText());
    }
    @Test public void refreshRetainsOnlyFailedSourcesAndClearsSuccessfulEmptySources() {
        TagReport old = new TagReport(Arrays.asList(new PlayerTag("Coral", "risk", "Risk", "R", true),
            new PlayerTag("Xadia", "sniper", "Sniper", "S", true)), Collections.emptyMap());
        TagReport update = new TagReport(Collections.emptyList(), Collections.singletonMap("Coral", "timeout"));
        TagReport merged = update.retainFailedSources(old);
        assertTrue(merged.has("Coral"));
        assertFalse(merged.has("Xadia"));
        assertTrue(merged.getFailures().containsKey("Coral"));
    }
}
