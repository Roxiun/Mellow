package com.roxiun.mellow.launch;

import org.junit.Test;
import static org.junit.Assert.*;

public class ApiVersionTest {
    @Test public void acceptsPublishedThreeComponentVersion() {
        assertEquals(ApiVersion.parse("1.0.2.0"), ApiVersion.parse("1.0.2"));
        assertEquals(1000000020000L, ApiVersion.parse("1.0.2"));
    }
    @Test public void ordersOfficialReleases() {
        assertTrue(ApiVersion.parse("1.0.2") > ApiVersion.parse("1.0.1.2"));
        assertTrue(ApiVersion.parse("1.0.10") > ApiVersion.parse("1.0.2"));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsOverflowingComponent() { ApiVersion.parse("1.0.10000"); }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownFormat() { ApiVersion.parse("1.0.2-beta"); }
}
