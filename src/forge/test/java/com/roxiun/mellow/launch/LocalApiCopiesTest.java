package com.roxiun.mellow.launch;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.jar.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LocalApiCopiesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Before public void setup() {
        Launch.blackboard = new HashMap<>();
        Launch.classLoader = new LaunchClassLoader(new URL[0]);
        CoreModManager.getIgnoredMods().clear();
        CoreModManager.getReparseableCoremods().clear();
    }
    private void api(String name, String version) throws Exception {
        File mods = new File(folder.getRoot(), "mods");
        mods.mkdirs();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(new File(mods, name)))) {
            out.putNextEntry(new JarEntry("mcmod.info"));
            out.write(("[{\"modid\":\"hypixel_mod_api\",\"version\":\"" + version + "\"}]").getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }
    @Test public void noInstalledApiLeavesElectionToBundle() {
        LocalApiCopies copies = new LocalApiCopies(folder.getRoot());
        copies.offerVersion();
        assertEquals(Long.MIN_VALUE, HypixelModAPITweaker.getBlackboardVersion());
        copies.resolve();
    }
    @Test public void olderLocalCopyIsSuppressedWhenBundleWins() throws Exception {
        api("old.jar", "1.0.1.2");
        LocalApiCopies copies = new LocalApiCopies(folder.getRoot());
        copies.offerVersion();
        Launch.blackboard.put(HypixelModAPITweaker.VERSION_KEY, ApiVersion.parse("1.0.2"));
        copies.resolve();
        assertTrue(CoreModManager.getIgnoredMods().contains("old.jar"));
        assertFalse(Launch.blackboard.containsKey("lcc.local-copy-loading-version"));
    }
    @Test public void newestLocalCopyWinsAndDuplicateIsSuppressed() throws Exception {
        api("old.jar", "1.0.1.2");
        api("new.jar", "1.0.2");
        LocalApiCopies copies = new LocalApiCopies(folder.getRoot());
        copies.offerVersion();
        copies.resolve();
        assertEquals(ApiVersion.parse("1.0.2"), HypixelModAPITweaker.getBlackboardVersion());
        assertTrue(CoreModManager.getIgnoredMods().contains("old.jar"));
        assertFalse(CoreModManager.getIgnoredMods().contains("new.jar"));
        assertEquals(1, Launch.classLoader.getSources().size());
    }
    @Test public void equalBundledWinnerSuppressesLocalCopy() throws Exception {
        api("local.jar", "1.0.2");
        Launch.blackboard.put(HypixelModAPITweaker.VERSION_KEY, ApiVersion.parse("1.0.2"));
        LocalApiCopies copies = new LocalApiCopies(folder.getRoot());
        copies.offerVersion();
        copies.resolve();
        assertTrue(CoreModManager.getIgnoredMods().contains("local.jar"));
    }
    @Test public void missingApiExtractsAndLoadsBundledForgeImplementation() throws Exception {
        HypixelModAPITweaker tweaker = new HypixelModAPITweaker();
        tweaker.acceptOptions(java.util.Collections.emptyList(), folder.getRoot(), null, "test");
        tweaker.getLaunchArguments();
        File extracted = new File(folder.getRoot(), "hypixel-mod-api/mellow/HypixelModAPI-1.0.2.jar");
        assertTrue(extracted.isFile());
        try (JarFile jar = new JarFile(extracted)) {
            assertNotNull(jar.getJarEntry("net/hypixel/modapi/HypixelModAPI.class"));
            assertNotNull(jar.getJarEntry("mcmod.info"));
        }
        assertEquals(1, Launch.classLoader.getSources().size());
    }
    @Test public void compatibleStandalonePreventsBundleExtraction() throws Exception {
        api("current.jar", "1.0.2");
        HypixelModAPITweaker tweaker = new HypixelModAPITweaker();
        tweaker.acceptOptions(java.util.Collections.emptyList(), folder.getRoot(), null, "test");
        tweaker.getLaunchArguments();
        assertFalse(new File(folder.getRoot(), "hypixel-mod-api").exists());
        assertFalse(CoreModManager.getIgnoredMods().contains("current.jar"));
    }
    @Test public void bundledPacketHandlerUsesProductionMinecraftNames() throws Exception {
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                getClass().getResourceAsStream("/META-INF/mellow/HypixelModAPI-1.0.2.bin"))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("net/hypixel/modapi/forge/ForgeModAPI$HypixelPacketHandler.class")) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = zip.read(buffer)) != -1) bytes.write(buffer, 0, count);
                    // Constant-pool method names identify the exact development/runtime mismatch.
                    String constants = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
                    assertFalse(constants.contains("getChannelName"));
                    assertFalse(constants.contains("getBufferData"));
                    assertTrue(constants.contains("func_149169_c"));
                    assertTrue(constants.contains("func_180735_b"));
                    return;
                }
            }
            fail("Bundled API is missing its Forge packet handler");
        }
    }
}
