package com.roxiun.mellow.launch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.apache.logging.log4j.LogManager;

/**
 * Includes standalone API installations in the official loader's version election.
 * Inspired by Yedelo/YedelMod's LocalCopyCompatibilityTweaker (LGPLv3).
 * Independently implemented; shares its blackboard key to coexist with YedelMod.
 */
final class LocalApiCopies {
    private static final String LOCAL_VERSION_KEY = "lcc.local-copy-loading-version";
    private final List<File> files = new ArrayList<>();
    private final List<Long> versions = new ArrayList<>();
    private File selected;

    LocalApiCopies(File gameDir) {
        scan(new File(gameDir, "mods"));
        scan(new File(gameDir, "mods/1.8.9"));
    }

    private void scan(File directory) {
        File[] jars = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return;
        for (File file : jars) {
            try (JarFile jar = new JarFile(file)) {
                JarEntry entry = jar.getJarEntry("mcmod.info");
                if (entry == null) continue;
                try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                    JsonElement root = new JsonParser().parse(reader);
                    if (!root.isJsonArray()) continue;
                    // Only suppress standalone API jars, never a multi-mod container.
                    if (root.getAsJsonArray().size() != 1) continue;
                    JsonObject mod = root.getAsJsonArray().get(0).getAsJsonObject();
                    if (mod.has("modid") && "hypixel_mod_api".equals(mod.get("modid").getAsString())) {
                        files.add(file);
                        try {
                            versions.add(ApiVersion.parse(mod.get("version").getAsString()));
                        } catch (RuntimeException e) {
                            files.remove(files.size() - 1);
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                LogManager.getLogger("Mellow API loader").warn("Could not inspect mod metadata: " + file, e);
            }
        }
    }

    void offerVersion() {
        long highest = HypixelModAPITweaker.getBlackboardVersion();
        Object local = Launch.blackboard.get(LOCAL_VERSION_KEY);
        for (int i = 0; i < files.size(); i++) {
            long version = versions.get(i);
            if (version > highest || (selected == null && Long.valueOf(version).equals(local) && version == highest)) {
                highest = version;
                selected = files.get(i);
                Launch.blackboard.put(HypixelModAPITweaker.VERSION_KEY, version);
                Launch.blackboard.put(LOCAL_VERSION_KEY, version);
            }
        }
    }

    void resolve() {
        long winner = HypixelModAPITweaker.getBlackboardVersion();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (file.equals(selected) && versions.get(i) == winner) continue;
            CoreModManager.getReparseableCoremods().remove(file.getName());
            CoreModManager.getReparseableCoremods().remove(file.getPath());
            if (!CoreModManager.getIgnoredMods().contains(file.getName())) {
                CoreModManager.getIgnoredMods().add(file.getName());
            }
        }
        if (selected != null && versions.get(files.indexOf(selected)) == winner) {
            try {
                // Make the selected API available before any API-dependent mod classes load.
                Launch.classLoader.addURL(selected.toURI().toURL());
            } catch (java.net.MalformedURLException e) {
                throw new IllegalStateException("Cannot load Hypixel API " + selected, e);
            }
        } else {
            Launch.blackboard.remove(LOCAL_VERSION_KEY);
        }
    }
}
