package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.util.localdenick.LocalDenickManager;
import java.io.File;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Test;

public class NicksCommandTest {

    @Test
    public void usageContainsExpectedAddOrdering() {
        File tempDir = createTempDir();
        NicksCommand command = new NicksCommand(
            LocalDenickManager.createForTests(tempDir),
            new MojangApi()
        );

        Assert.assertEquals(
            "/nicks <add | remove | list>",
            command.getCommandUsage(null)
        );
    }

    private File createTempDir() {
        try {
            File dir = Files.createTempDirectory("nicks-command-test").toFile();
            dir.deleteOnExit();
            return dir;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
