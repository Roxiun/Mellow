package com.roxiun.mellow.feature.tags;

import org.junit.Test;
import static org.junit.Assert.*;

public class TagUtilsTest {
    @Test public void newLoginTagUsesSuppliedTimestampWithoutProviderLookup() {
        TagUtils tags = new TagUtils(null);
        // No UUID means no skin request; first-login metadata still needs no provider or Mellow instance.
        assertTrue(tags.buildTags("Player", null, 10, 1, 0, 1, 1,
            System.currentTimeMillis()).contains("NL"));
        assertFalse(tags.buildTags("Player", null, 10, 1, 0, 1, 1, 0L).contains("NL"));
        assertFalse(tags.buildTags("Player", null, 10, 1, 0, 1, 1, 1L).contains("NL"));
    }
}
