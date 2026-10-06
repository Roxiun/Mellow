package com.roxiun.mellow.config;

import java.nio.file.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LegacyConfigMigrationTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void findsActiveV0ProfileWithoutCopyingRootIntoNamedProfiles() throws Exception {
        Path game = temp.getRoot().toPath();
        Path old = game.resolve("OneConfig/profiles/My Profile/mellow.json");
        Files.createDirectories(old.getParent());
        Files.writeString(old, "{}");
        Files.writeString(game.resolve("OneConfig/OneConfig.json"), "{\"currentProfile\":\"My Profile\"}");
        assertEquals(old, LegacyConfigMigration.findSource(game, game.resolve("config"), ""));
        assertEquals(old, LegacyConfigMigration.findSource(game, game.resolve("profiles/My Profile"), "My Profile"));
        assertNull(LegacyConfigMigration.findSource(game, game.resolve("profiles/New Profile"), "New Profile"));
    }

    @Test public void existingProfileLocalFileTakesPriority() throws Exception {
        Path game = temp.getRoot().toPath();
        Path folder = game.resolve("profiles/Test");
        Files.createDirectories(folder);
        Path file = folder.resolve("mellow.json");
        Files.writeString(file, "{}");
        Path competing = game.resolve("OneConfig/profiles/Test/mellow.json");
        Files.createDirectories(competing.getParent());
        Files.writeString(competing, "{}");
        assertEquals(file, LegacyConfigMigration.findSource(game, folder, "Test"));
    }

}
