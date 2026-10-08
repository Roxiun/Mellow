/*
 * Adapted from HypixelDev/ForgeModAPI (MIT); see META-INF/licenses/Hypixel-ForgeModAPI-MIT.txt.
 * Mellow modifications: private bundle resource, three-component versions, local-copy coordination.
 */
package com.roxiun.mellow.launch;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 * A tweaker class to automatically load the hypixel mod api while resolving conflicts
 * from multiple mods providing a copy. This saves users from having to download the
 * mod api separately.
 */
public class HypixelModAPITweaker implements ITweaker {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String VERSION_NAME;
    public static final long VERSION;

    static {
        VERSION_NAME = "1.0.2";
        VERSION = ApiVersion.parse(VERSION_NAME);
    }

    public static final String BUNDLED_JAR_NAME = "HypixelModAPI-" + VERSION_NAME + ".jar";

    /**
     * This is the key which is used in the {@link Launch#blackboard} to perform
     * version negotiation.
     *
     * @see #getBlackboardVersion()
     * @see #offerVersionToBlackboard()
     */
    public static final String VERSION_KEY = "net.hypixel.mod-api.version:1";

    private boolean hasOfferedVersion = false;
    private LocalApiCopies localCopies;
    private File gameDirectory;

    /**
     * Get the current version declared on the blackboard.
     * The blackboard allows us to store arbitrary values. We store a long indicating the max version installed.
     *
     * @see #VERSION_KEY
     */
    public static long getBlackboardVersion() {
        Object blackboardVersion = Launch.blackboard.get(VERSION_KEY);
        // In case nobody has declared a version on the blackboard yet, we return an incredibly outdated past version
        if (blackboardVersion == null) return Long.MIN_VALUE;
        // In case we later switch to another version format we declare any non-integer as an incredibly advanced future version
        if (!(blackboardVersion instanceof Long)) return Long.MAX_VALUE;
        return (Long) blackboardVersion;
    }

    /**
     * Inject our API jar into forge if we are the highest available version.
     *
     * @see #injectAPI()
     */
    private void tryInjectAPI() {
        // If the maximum installed version isn't our version return
        if (getBlackboardVersion() != VERSION) {
            LOGGER.info("Blackboard version newer than our version {}. Skipping injecting API.", VERSION);
            return;
        }
        // If we didn't offer to install this version return
        if (!hasOfferedVersion) {
            LOGGER.info("Someone else with the same version number {} offered to inject themselves first. Skipping injecting API.", VERSION);
            return;
        }

        injectAPI();
    }

    /**
     * Unpacks the actual API file into the game directory from where it can be loaded.
     *
     * @return the location of the extracted API
     */
    private File unpackAPI() {
        File extractedFile = new File(gameDirectory, "hypixel-mod-api/mellow/" + BUNDLED_JAR_NAME).getAbsoluteFile();
        LOGGER.info("Unpacking mod API to {}", extractedFile);
        //noinspection ResultOfMethodCallIgnored
        extractedFile.getParentFile().mkdirs();
        try (InputStream bundledJar = Objects.requireNonNull(
                getClass().getResourceAsStream("/META-INF/mellow/HypixelModAPI-1.0.2.bin"),
                "Could not find bundled hypixel mod api");
             OutputStream outputStream = Files.newOutputStream(extractedFile.toPath())) {
            IOUtils.copy(bundledJar, outputStream);
            LOGGER.info("Successfully extracted mod API file");
            return extractedFile;
        } catch (IOException e) {
            LOGGER.error("Could not extract mod API file", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Inject the API into Forge to be loaded as a mod. This will also extract the JAR.
     */
    private void injectAPI() {
        LOGGER.info("Injecting mod API of version {}", VERSION_NAME);
        try {
            Launch.classLoader.addURL(unpackAPI().toURI().toURL());
            LOGGER.info("Added mod API to classpath");
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not add Hypixel API to classpath", e);
        }
    }

    /**
     * Offers our bundled version to the {@link Launch#blackboard}. If there is already a
     * higher version than ours installed then this will not do anything. Otherwise, it
     * will set {@link #hasOfferedVersion} and increment the version in the blackboard.
     *
     * @see #VERSION_KEY
     */
    private void offerVersionToBlackboard() {
        if (getBlackboardVersion() < VERSION) {
            LOGGER.info("Offering newer version {} > {}", VERSION, getBlackboardVersion());
            hasOfferedVersion = true;
            Launch.blackboard.put(VERSION_KEY, VERSION);
        }
    }

    /*
     Below here are all the ITweaker methods. The tweaker methods are executed in rounds.

     1. Run class init and init (constructor) for each tweaker
     2. Run acceptOptions for each tweaker
     3. Run injectIntoClassLoader for each tweaker
     4. If any cascading tweakers have been registered, go back to step 1
     5. After there are no new tweakers after an entire round:
     6. Run getLaunchArguments for each tweaker
     7. Run getLaunchTarget only on the first tweaker found (and execute that class)

     By first offering our version negotiation in acceptOptions or injectIntoClassloader and
     then injection our JARs in getLaunchArguments, we can ensure that every tweaker had time
     to make their version announcement heard.
     */
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        gameDirectory = gameDir;
        localCopies = new LocalApiCopies(gameDir);
        localCopies.offerVersion();
        offerVersionToBlackboard();
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        localCopies.resolve();
        tryInjectAPI();
        return new String[0];
    }
}
