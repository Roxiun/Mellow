package com.roxiun.mellow.feature.nicks;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class NumberDenickerTest {
    @Test
    public void requiresBothKindsOfEvidenceInEitherOrder() {
        for (String first : Arrays.asList("finals", "beds")) {
            NumberDenicker.PotentialNick player = new NumberDenicker.PotentialNick();
            player.recordMatches(first, Arrays.asList("RealName", "Other"));
            assertNull(player.getMatch());
            player.recordMatches(first.equals("finals") ? "beds" : "finals",
                Collections.singletonList("RealName"));
            assertEquals("RealName", player.getMatch());
        }
    }

    @Test
    public void ambiguousMatchesRemainUnresolved() {
        NumberDenicker.PotentialNick player = new NumberDenicker.PotentialNick();
        player.recordMatches("finals", Arrays.asList("One", "Two"));
        player.recordMatches("beds", Arrays.asList("One", "Two"));
        assertNull(player.getMatch());
    }

    @Test
    public void emptyOrConflictingEvidenceCannotResolve() {
        for (java.util.List<String> first : Arrays.asList(
            Collections.<String>emptyList(), Collections.singletonList("Other"))) {
            NumberDenicker.PotentialNick player = new NumberDenicker.PotentialNick();
            player.recordMatches("finals", first);
            player.recordMatches("beds", Collections.singletonList("RealName"));
            assertNull(player.getMatch());
        }
    }

    @Test
    public void namesAreCaseInsensitiveAndDuplicatesAreNotAmbiguous() {
        NumberDenicker.PotentialNick player = new NumberDenicker.PotentialNick();
        player.recordMatches("beds", Arrays.asList("RealName", "realname"));
        player.recordMatches("finals", Collections.singletonList("REALNAME"));
        assertEquals("RealName", player.getMatch());
    }
}
