package com.roxiun.mellow.test;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.hud.*;
import java.util.*;
import org.polyfrost.compose.node.*;
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.hud.v1.Font;

/** Regression checks for legacy imports, profile switching and the composed HUD output. */
final class ConfigMigrationSmokeTest {
    static void verify() throws Exception {
        MellowOneConfig config = Mellow.config;
        String[] originalOrder = config.bedwarsStatOrder;
        int originalHueMode = config.hitboxHueMode;
        int originalSaturationMode = config.hitboxSaturationMode;
        int originalBrightnessMode = config.hitboxBrightnessMode;
        String profile = ConfigManager.activeProfile();
        String temporary = "mellow-migration-test";
        try {
            config.getTree().getProp("bedwarsStatOrder").setAs(new String[]{"Ping", "Name"});
            config.save();
            verifyDependencies(config);
            ConfigManager.createProfile(temporary);
            verifyDependencies(config);
            ConfigManager.openProfile(profile);
            verifyDependencies(config);
            // Values unrelated to the dependency checks must survive the switch back.
            require(Arrays.equals(config.bedwarsStatOrder, new String[]{"Ping", "Name"}), "Stat order lost after profile switch");
            verifyLegacyProfile(config, profile);
            verifyHuds(config);
            ConfigManager.openProfile(temporary);
            ConfigManager.openProfile(profile);
            require(config.upgradesTrapsHUD.headingColor.getRawArgb() == 0xff123456,
                "HUD color lost after profile switch");
            require(config.upgradesTrapsHUD.shortNames && !config.upgradesTrapsHUD.romanNumerals,
                "HUD custom options lost after profile switch");
        } finally {
            if (!ConfigManager.activeProfile().equals(profile)) ConfigManager.openProfile(profile);
            config.bedwarsStatOrder = originalOrder;
            config.hitboxHueMode = originalHueMode;
            config.hitboxSaturationMode = originalSaturationMode;
            config.hitboxBrightnessMode = originalBrightnessMode;
            config.save();
            if (ConfigManager.profiles().contains(temporary)) ConfigManager.deleteProfile(temporary);
        }
    }

    private static void verifyLegacyProfile(MellowOneConfig config, String originalProfile) throws Exception {
        String profile = "mellow-v0-import-test";
        var source = java.nio.file.Path.of("OneConfig", "profiles", profile, "mellow.json");
        java.nio.file.Files.createDirectories(source.getParent());
        String legacy = """
            {"autoWho":true,"tabStats":"not-a-boolean","minFkdr":-999,"maxStoredReplays":-50,"customStat1":13,
             "customStat2":2,"customStat3":10,"customStat4":10,"customStat5":10,"customStat6":10,
             "upgradesTrapsHUD":{"headingColorIndex":12,"textColorIndex":10,"shortNames":true,"romanNumerals":false,"enabled":true},
             "emeraldCounterHUD":{"enabled":true,"background":true,"scale":1.5,"textType":1,
                "bgColor":{"hsba":[0,0,0,180],"dataBit":-1}},
             "diamondCounterHUD":{"enabled":false,"background":false}}
            """;
        java.nio.file.Files.writeString(source, legacy);
        try {
            ConfigManager.createProfile(profile);
            require(config.tabStats, "Malformed legacy boolean replaced the default");
            require(config.autoWho && config.minFkdr == -1 && config.maxStoredReplays == 0, "Legacy scalar settings/bounds not migrated");
            require(Arrays.equals(config.bedwarsStatOrder, new String[]{"Ping", "Name"}), "Legacy slots not migrated");
            require(config.emeraldCounterHUD.isReal() && !config.emeraldCounterHUD.getHidden(), "Legacy counter enablement lost");
            require(config.diamondCounterHUD.isReal() && config.diamondCounterHUD.getHidden(), "Disabled legacy counter was enabled");
            require(config.emeraldCounterHUD.getShowBackground() && config.emeraldCounterHUD.getBgColor() == 0xb4000000,
                "Legacy background/opacity lost");
            require(config.emeraldCounterHUD.getCustomScale() == 1.5f, "Legacy counter scale lost");
            require(config.upgradesTrapsHUD.headingColor.getRawArgb() == 0xffff5555 && config.upgradesTrapsHUD.getTextColor() == 0xff55ff55,
                "Legacy HUD palette not migrated");
            require(config.upgradesTrapsHUD.shortNames && !config.upgradesTrapsHUD.romanNumerals, "Legacy HUD formatting lost");
            require(java.nio.file.Files.readString(source).equals(legacy), "Legacy profile was modified");
            config.getTree().getProp("autoWho").setAs(false);
            config.save();
            ConfigManager.openProfile(originalProfile);
            ConfigManager.openProfile(profile);
            require(!config.autoWho, "Legacy importer overwrote a saved v1 setting");
            require(config.emeraldCounterHUD.getShowBackground(), "Background migration repeated on profile switch");
        } finally {
            ConfigManager.openProfile(originalProfile);
            if (ConfigManager.profiles().contains(profile)) ConfigManager.deleteProfile(profile);
            java.nio.file.Files.deleteIfExists(source);
            java.nio.file.Files.deleteIfExists(source.getParent());
        }
    }

    private static void verifyDependencies(MellowOneConfig config) {
        for (String component : new String[]{"Hue", "Saturation", "Brightness"}) {
            var mode = config.getTree().getProp("hitbox" + component + "Mode");
            mode.setAs(0);
            require(config.getTree().getProp("hitbox" + component + "Value").getDisplay() == Property.Display.HIDDEN, "Static control visible in offset mode");
            require(config.getTree().getProp("hitbox" + component + "Offset").canDisplay(), "Offset control hidden");
            mode.setAs(1);
            require(config.getTree().getProp("hitbox" + component + "Value").canDisplay(), "Static control stayed hidden after mode change");
            require(config.getTree().getProp("hitbox" + component + "Offset").getDisplay() == Property.Display.HIDDEN, "Offset control stayed visible");
        }
    }

    private static void verifyHuds(MellowOneConfig config) {
        var upgrades = config.upgradesTrapsHUD;
        require(upgrades.isReal(), "Default upgrades HUD not registered");
        upgrades.getTree().getProp("shortNames").setAs(true);
        upgrades.getTree().getProp("romanNumerals").setAs(false);
        upgrades.getTree().getProp("headingColor").setAs(new PolyColor(0xff123456));
        upgrades.setTextColor(0xff654321);
        upgrades.setTextChroma(false);
        for (Font font : Font.values()) {
            upgrades.setFont(font);
            upgrades.update();
            var runtime = upgrades.getRuntime();
            runtime.frame(960f, 540f, System.nanoTime());
            require(runtime.getRoot().getHeight() > 40f, "Multiline HUD collapsed for " + font);
            require(runtime.getRoot().getWidth() > 10f, "HUD did not measure for " + font);
            if (font == Font.Minecraft) {
                List<McTextNode> text = new ArrayList<>();
                collectText(runtime.getRoot(), text);
                require(text.stream().anyMatch(n -> n.getText().contains("Sharp 2")), "Preview ignored short names/numerals");
                require(text.stream().filter(n -> n.getText().contains("Sharp 2")).allMatch(n -> n.getColor().getRawArgb() == 0xff654321), "Body color ignored");
                require(text.stream().filter(n -> n.getText().contains("Upgrades:")).anyMatch(n -> n.getColor().getRawArgb() == 0xff123456), "Heading color ignored");
            }
        }
        upgrades.headingColor = new PolyColor(0xff123456, true, 1f);
        upgrades.update();
        require(upgrades.getAlwaysRedraw(), "Heading chroma would freeze in the HUD cache");
        // Counter providers need not be active by default (matching the Forge defaults).
        for (MellowTextHud hud : new MellowTextHud[]{new EmeraldCounterHUD(), new DiamondCounterHUD()}) {
            hud.setTextColor(0xff246810);
            hud.update();
            hud.getRuntime().frame(960f, 540f, System.nanoTime());
            List<McTextNode> text = new ArrayList<>();
            collectText(hud.getRuntime().getRoot(), text);
            require(text.stream().anyMatch(n -> n.getText().contains("(2): 15s") && n.getColor().getRawArgb() == 0xff246810), "Counter ignored native text color");
            hud.getRuntime().dispose();
        }
        upgrades.setFont(Font.Minecraft);
        upgrades.save();
        var saved = ConfigManager.active().load(upgrades.getTree().getID());
        upgrades.headingColor = new PolyColor(0xff000000);
        upgrades.getTree().overwrite(saved, true);
        require(upgrades.headingColor.getChroma() && upgrades.headingColor.getRawArgb() == 0xff123456, "Heading colour/chroma did not persist");
    }

    private static void collectText(PolyNode node, List<McTextNode> out) {
        if (node instanceof McTextNode text) out.add(text);
        for (PolyNode child : node.getChildren()) collectText(child, out);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
