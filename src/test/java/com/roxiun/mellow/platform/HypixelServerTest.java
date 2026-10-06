package com.roxiun.mellow.platform;

import org.junit.Test;
import static org.junit.Assert.*;

public class HypixelServerTest {
    @Test public void recognizesProxiesByServerBrand() {
        assertTrue(HypixelServer.matchesIdentity("proxy.example:25565", "Hypixel BungeeCord"));
        assertTrue(HypixelServer.matchesIdentity("192.0.2.1", "Hypixel"));
    }

    @Test public void retainsDirectAddressFallbackBeforeBrandArrives() {
        assertTrue(HypixelServer.matchesIdentity("MC.HYPIXEL.NET:25565", null));
        assertTrue(HypixelServer.matchesIdentity("hypixel.net", null));
    }

    @Test public void doesNotRecognizeUnrelatedServers() {
        assertFalse(HypixelServer.matchesIdentity("hypixel.net.example", "vanilla"));
        assertFalse(HypixelServer.matchesIdentity("nothypixel.net", null));
        assertFalse(HypixelServer.matchesIdentity(null, null));
    }
}
