package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.api.aurora.AuroraApi;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DenickQueryTest {
    @Test public void normalizesBothOrdersAndSingleStats() throws Exception {
        for (String command : new String[]{"BEDS 14,068 finals 24,526", "finals 24,526 beds 14,068", "beds 14,068", "finals 24,526"}) {
            DenickQuery query = DenickQuery.parse(command.split(" "));
            assertEquals(command.split(" ").length == 4, query.isCombined());
            List<DenickQuery.Match> result = query.search(new AuroraApi() {
                @Override public AuroraResponse queryStats(String type, String value, int range, int max, String key) {
                    assertEquals(type.equals("beds") ? "14068" : "24526", value);
                    assertEquals(type.equals("beds") ? 50 : 100, range);
                    assertEquals(10, max);
                    return response(player("Alice", 0));
                }
            }, 100, 50, 10, "key");
            assertEquals(1, result.size());
        }
    }

    @Test public void rejectsInvalidOrDuplicatePairs() {
        for (String command : new String[]{"beds", "beds 1 finals", "beds 1 BEDS 2", "wins 2", "finals -1", "beds abc", "beds 2147483648"}) {
            assertThrows(command, IllegalArgumentException.class, () -> DenickQuery.parse(command.split(" ")));
        }
    }

    @Test public void intersectsCaseInsensitivelyAndRanksBothDistances() {
        List<DenickQuery.Match> matches = DenickQuery.match(
            Arrays.asList(player("Near", 2), player("Exact", 0), player("OnlyFinals", 0), player("NEAR", 2)),
            Arrays.asList(player("near", 3), player("EXACT", 0), player("OnlyBeds", 0)));
        assertEquals(2, matches.size());
        assertEquals("Exact", matches.get(0).name);
        assertEquals("Near", matches.get(1).name);
        assertEquals(Integer.valueOf(2), matches.get(1).finalsDistance);
        assertEquals(Integer.valueOf(3), matches.get(1).bedsDistance);
        assertTrue(DenickQuery.match(Collections.singletonList(player("Alice", 0)), Collections.emptyList()).isEmpty());
    }

    @Test public void failedSecondLookupDoesNotReturnPartialMatches() {
        AuroraApi api = new AuroraApi() {
            @Override public AuroraResponse queryStats(String type, String value, int range, int max, String key) {
                return type.equals("finals") ? response(player("Alice", 0)) : null;
            }
        };
        assertThrows(IOException.class, () -> DenickQuery.parse(new String[]{"finals", "1", "beds", "2"})
            .search(api, 0, 0, 5, "key"));
    }

    @Test public void appliesEachCountersRange() throws Exception {
        AuroraApi api = new AuroraApi() {
            @Override public AuroraResponse queryStats(String type, String value, int range, int max, String key) {
                return response(player("Alice", type.equals("finals") ? 1 : 0));
            }
        };
        assertTrue(DenickQuery.parse(new String[]{"finals", "1", "beds", "2"}).search(api, 0, 50, 5, "key").isEmpty());
    }

    private static AuroraApi.PlayerMatch player(String name, int distance) {
        AuroraApi.PlayerMatch player = new AuroraApi.PlayerMatch();
        player.name = name;
        player.distance = distance;
        return player;
    }

    private static AuroraApi.AuroraResponse response(AuroraApi.PlayerMatch player) {
        AuroraApi.AuroraResponse response = new AuroraApi.AuroraResponse();
        response.success = true;
        response.data = Collections.singletonList(player);
        return response;
    }
}
