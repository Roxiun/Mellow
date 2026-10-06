package com.roxiun.mellow.launch;

import java.io.File;
import java.net.URL;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.CoreModManager;
import org.spongepowered.asm.launch.MixinBootstrap;

/** Coordinates OneConfig and the bundled API, following YedelMod's startup approach. */
public final class MellowTweaker implements ITweaker {
    @Override
    @SuppressWarnings("unchecked")
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        List<String> tweakers = (List<String>) Launch.blackboard.get("TweakClasses");
        tweakers.add("cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker");
        if (!Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            tweakers.add("com.roxiun.mellow.launch.HypixelModAPITweaker");
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader loader) {}

    @Override
    public String getLaunchTarget() { return ""; }

    @Override
    public String[] getLaunchArguments() {
        // Forge initially ignores tweaker jars; keep Mellow discoverable as a mod and mixin container.
        URL source = getClass().getProtectionDomain().getCodeSource().getLocation();
        if (source != null && "file".equals(source.getProtocol())) {
            try {
                File file = new File(source.toURI());
                if (file.isFile()) {
                    MixinBootstrap.getPlatform().addContainer(source.toURI());
                    CoreModManager.getIgnoredMods().remove(file.getName());
                    CoreModManager.getIgnoredMods().remove(file.getPath());
                    if (!CoreModManager.getReparseableCoremods().contains(file.getName())) {
                        CoreModManager.getReparseableCoremods().add(file.getName());
                    }
                }
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException("Cannot register Mellow's mod jar", e);
            }
        }
        return new String[0];
    }
}
