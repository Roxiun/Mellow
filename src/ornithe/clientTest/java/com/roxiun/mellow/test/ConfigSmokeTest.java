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

/** Regression checks for settings persistence, dependencies and composed HUD output. */
final class ConfigSmokeTest {
    static void verify() throws Exception {
        MellowOneConfig config = Mellow.config;
        verifyDenickSearchLabels();
        String[] originalOrder = config.bedwarsStatOrder;
        String[] originalBuildBattle = config.buildBattleStatOrder;
        String[] originalTntRun = config.tntRunStatOrder;
        String[] originalTntTag = config.tntTagStatOrder;
        String[] originalBowSpleef = config.bowSpleefStatOrder;
        String[] originalMurderMystery = config.murderMysteryStatOrder;
        int originalHueMode = config.hitboxHueMode;
        int originalSaturationMode = config.hitboxSaturationMode;
        int originalBrightnessMode = config.hitboxBrightnessMode;
        String profile = ConfigManager.activeProfile();
        String temporary = "mellow-settings-test";
        try {
            config.getProperty("bedwarsStatOrder").setAs(new String[]{"Ping", "Name"});
            config.getProperty("buildBattleStatOrder").setAs(new String[]{"Name", "Title", "Wins"});
            config.getProperty("tntRunStatOrder").setAs(new String[]{"Ratio", "Name", "Ping"});
            config.getProperty("tntTagStatOrder").setAs(new String[]{"Ping", "Name", "Wins"});
            config.getProperty("bowSpleefStatOrder").setAs(new String[]{"Ping", "Name", "Wins"});
            config.getProperty("murderMysteryStatOrder").setAs(new String[]{"Ping", "Name", "Wins"});
            config.save();
            verifyDependencies(config);
            ConfigManager.createProfile(temporary);
            verifyDependencies(config);
            ConfigManager.openProfile(profile);
            verifyDependencies(config);
            // Values unrelated to the dependency checks must survive the switch back.
            require(Arrays.equals(config.bedwarsStatOrder, new String[]{"Ping", "Name"}), "Stat order lost after profile switch");
            require(Arrays.equals(config.buildBattleStatOrder, new String[]{"Name", "Title", "Wins"}), "Build Battle order lost after profile switch");
            require(Arrays.equals(config.tntRunStatOrder, new String[]{"Ratio", "Name", "Ping"}), "TNT Run order lost after profile switch");
            require(Arrays.equals(com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns.getConfiguredStatsForScope(
                com.roxiun.mellow.stats.StatScope.TNT_RUN, config), new int[]{3, 2, 7}), "TNT Run UI order did not reach renderer");
            require(Arrays.equals(config.tntTagStatOrder, new String[]{"Ping", "Name", "Wins"}), "TntTag order lost after profile switch");
            require(Arrays.equals(com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns.getConfiguredStatsForScope(
                com.roxiun.mellow.stats.StatScope.TNT_TAG, config), new int[]{6, 2, 1}), "TntTag order did not reach renderer");
            require(Arrays.equals(config.bowSpleefStatOrder, new String[]{"Ping", "Name", "Wins"}), "BowSpleef order lost after profile switch");
            require(Arrays.equals(com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns.getConfiguredStatsForScope(
                com.roxiun.mellow.stats.StatScope.BOW_SPLEEF, config), new int[]{8, 2, 1}), "BowSpleef order did not reach renderer");
            require(Arrays.equals(config.murderMysteryStatOrder, new String[]{"Ping", "Name", "Wins"}), "MurderMystery order lost after profile switch");
            require(Arrays.equals(com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns.getConfiguredStatsForScope(
                com.roxiun.mellow.stats.StatScope.MURDER_MYSTERY, config), new int[]{8, 2, 1}), "MurderMystery order did not reach renderer");
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
            config.buildBattleStatOrder = originalBuildBattle;
            config.tntRunStatOrder = originalTntRun;
            config.tntTagStatOrder = originalTntTag;
            config.bowSpleefStatOrder = originalBowSpleef;
            config.murderMysteryStatOrder = originalMurderMystery;
            config.hitboxHueMode = originalHueMode;
            config.hitboxSaturationMode = originalSaturationMode;
            config.hitboxBrightnessMode = originalBrightnessMode;
            config.save();
            if (ConfigManager.profiles().contains(temporary)) ConfigManager.deleteProfile(temporary);
        }
    }


    private static void verifyDependencies(MellowOneConfig config) {
        verifyParentDependencies(config);
        for (String component : new String[]{"Hue", "Saturation", "Brightness"}) {
            var mode = config.getProperty("hitbox" + component + "Mode");
            mode.setAs(0);
            require(config.getProperty("hitbox" + component + "Value").getDisplay() == Property.Display.HIDDEN, "Static control visible in offset mode");
            require(config.getProperty("hitbox" + component + "Offset").canDisplay(), "Offset control hidden");
            mode.setAs(1);
            require(config.getProperty("hitbox" + component + "Value").canDisplay(), "Static control stayed hidden after mode change");
            require(config.getProperty("hitbox" + component + "Offset").getDisplay() == Property.Display.HIDDEN, "Offset control stayed visible");
        }
    }

    private static void verifyParentDependencies(MellowOneConfig config) {
        Map<String, Object> saved = new LinkedHashMap<>();
        for (String name : new String[]{"requestPopupsEnabled", "tabStats", "extendedTabStatsView",
            "extendedTabStatsTeamColumnMode", "numberDenicker", "pregameStats", "autoLeaveBlacklistedPregameChat",
            "coloredHitboxes", "coloredNametagBackgrounds", "showHiddenWinstreaks", "anticheatEnabled"}) {
            saved.put(name, config.getProperty(name).get());
        }
        try {
            boolean friendSetting = config.friendRequestPopupsEnabled;
            config.getProperty("requestPopupsEnabled").setAs(false);
            require(config.getProperty("friendRequestPopupsEnabled").getDisplay() == Property.Display.DISABLED,
                "Request child stayed editable with parent off");
            require(config.friendRequestPopupsEnabled == friendSetting, "Disabling a parent erased its child value");
            config.getProperty("requestPopupsEnabled").setAs(true);
            require(config.getProperty("friendRequestPopupsEnabled").getDisplay() == Property.Display.SHOWN,
                "Request child did not reenable");

            config.getProperty("tabStats").setAs(false);
            config.getProperty("extendedTabStatsView").setAs(true);
            require(config.getProperty("extendedTabStatsHeaders").getDisplay() == Property.Display.DISABLED,
                "Nested dependency ignored tab stats master switch");
            config.getProperty("tabStats").setAs(true);
            require(config.getProperty("extendedTabStatsHeaders").getDisplay() == Property.Display.SHOWN,
                "Extended options did not reenable");
            config.getProperty("extendedTabStatsView").setAs(false);
            require(config.getProperty("highlightTaggedPlayers").getDisplay() == Property.Display.DISABLED,
                "Extended-only highlight stayed enabled");
            require(config.getProperty("showDot12").getDisplay() == Property.Display.SHOWN,
                "Standard separators wrongly depend on extended view");
            config.getProperty("extendedTabStatsView").setAs(true);
            config.getProperty("extendedTabStatsTeamColumnMode").setAs(1);
            require(config.getProperty("extendedTabStatsStripCombinedTeamPadding").getDisplay() == Property.Display.HIDDEN,
                "Combined padding visible for a separate team column");
            config.getProperty("extendedTabStatsTeamColumnMode").setAs(3);
            require(config.getProperty("extendedTabStatsStripCombinedTeamPadding").getDisplay() == Property.Display.SHOWN,
                "Combined padding did not return");

            config.getProperty("numberDenicker").setAs(false);
            require(config.getProperty("numberDenickerFuzzy").getDisplay() == Property.Display.DISABLED,
                "Automatic denicker child stayed enabled");
            require(config.getProperty("finalsRange").getDisplay() == Property.Display.SHOWN,
                "Manual denick search settings were disabled");
            config.getProperty("autoLeaveBlacklistedPregameChat").setAs(true);
            config.getProperty("pregameStats").setAs(false);
            require(config.getProperty("autoLeaveBlacklistedPregameCommand").getDisplay() == Property.Display.DISABLED,
                "Auto leave ignored its pregame prerequisite");
            require(config.getProperty("mentionLobbyStats").getDisplay() == Property.Display.SHOWN,
                "Independent lobby trigger was disabled");

            config.getProperty("coloredHitboxes").setAs(false);
            config.getProperty("coloredNametagBackgrounds").setAs(false);
            require(config.getProperty("hitboxHueMode").getDisplay() == Property.Display.DISABLED,
                "Unused colour settings stayed editable");
            config.getProperty("coloredNametagBackgrounds").setAs(true);
            require(config.getProperty("hitboxHueMode").getDisplay() == Property.Display.SHOWN,
                "Nametags could not independently enable shared colours");
            config.getProperty("showHiddenWinstreaks").setAs(false);
            require(config.getProperty("winstreakMinStars").getDisplay() == Property.Display.DISABLED,
                "Winstreak filter ignored its master switch");
            config.getProperty("anticheatEnabled").setAs(false);
            require(config.getProperty("anticheatVl").getDisplay() == Property.Display.DISABLED,
                "Accordion alert option ignored its master switch");
            config.getProperty("anticheatEnabled").setAs(true);
            require(config.getProperty("anticheatVl").getDisplay() == Property.Display.SHOWN,
                "Accordion alert option did not reenable");
        } finally {
            saved.forEach((name, value) -> config.getProperty(name).setAs(value));
        }
    }

    private static void verifyDenickSearchLabels() throws Exception {
        for (String field : new String[]{"finalsRange", "bedsRange", "maxResults"}) {
            String[] labels = MellowOneConfig.class.getField(field)
                .getAnnotation(org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown.class).options();
            for (int index = 0; index < labels.length; index++) {
                int actual = field.equals("maxResults")
                    ? com.roxiun.mellow.feature.nicks.DenickSearchOptions.limit(index)
                    : com.roxiun.mellow.feature.nicks.DenickSearchOptions.range(index);
                require(actual == Integer.parseInt(labels[index]), "Denick lookup value disagrees with " + field + " label");
            }
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
