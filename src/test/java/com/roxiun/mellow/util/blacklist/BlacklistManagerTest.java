package com.roxiun.mellow.util.blacklist;

import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class BlacklistManagerTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    @Test
    public void savedNameRemovalSurvivesReload() {
        BlacklistManager manager = new BlacklistManager(directory.getRoot());
        UUID uuid = UUID.randomUUID();
        manager.addPlayer(uuid, "OldName", "reason");
        manager = new BlacklistManager(directory.getRoot());
        assertEquals(uuid, manager.findPlayerByName("oldNAME"));
        assertTrue(manager.removePlayer(manager.findPlayerByName("OldName")));
        assertFalse(manager.removePlayer(uuid));
        assertFalse(new BlacklistManager(directory.getRoot()).isBlacklisted(uuid));
    }

    @Test
    public void unknownNameAllowsCurrentNameLookup() {
        BlacklistManager manager = new BlacklistManager(directory.getRoot());
        manager.addPlayer(UUID.randomUUID(), "OldName", "reason");
        assertNull(manager.findPlayerByName("NewName"));
    }

    @Test
    public void duplicateNamesRequireUuidAndLeaveEntriesIntact() {
        BlacklistManager manager = new BlacklistManager(directory.getRoot());
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        manager.addPlayer(first, "OldName", "first");
        manager.addPlayer(second, "oldname", "second");
        try {
            manager.findPlayerByName("OLDNAME");
            fail("Expected an ambiguous name to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(2, manager.getBlacklist().size());
        }
        assertTrue(manager.removePlayer(first));
        assertTrue(manager.isBlacklisted(second));
    }
}
