package com.roxiun.mellow.test;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;

/** Test-only mod, excluded from release jars. */
@Mod(modid = "mellow_client_tests", name = "Mellow client tests", version = "1", dependencies = "after:mellow")
public final class ForgeClientSmokeTest {
    private int ticks;
    @Mod.EventHandler public void init(FMLInitializationEvent e) { MinecraftForge.EVENT_BUS.register(this); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++ticks != 10) return;
        try {
            if (Boolean.getBoolean("mellow.compatTest") && !net.minecraftforge.fml.common.Loader.isModLoaded("vanillahud"))
                throw new AssertionError("VanillaHUD was not fully initialized");
            verifyPreferences();
            TabIntegrationSmokeTest.verify();
            java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("mellow.smokeResult")), "PASS".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("MELLOW CLIENT SMOKE PASS");
        } catch (Throwable failure) { failure.printStackTrace(); }
        finally { Minecraft.getMinecraft().shutdown(); }
    }
    /** Exercise live v0 controls and the same load path used when switching profiles. */
    private static void verifyPreferences() throws Exception {
        com.roxiun.mellow.config.MellowOneConfig config = com.roxiun.mellow.Mellow.config;
        config.save();
        java.nio.file.Path file = cc.polyfrost.oneconfig.config.core.ConfigUtils
            .getProfileFile(com.roxiun.mellow.Mellow.MODID + ".json").toPath();
        byte[] original = java.nio.file.Files.readAllBytes(file);
        try {
            config.requestPopupsEnabled = false;
            require(!config.optionNames.get("friendRequestPopupsEnabled").isEnabled(), "Popup dependency missing");
            config.requestPopupsEnabled = true;
            require(config.optionNames.get("friendRequestPopupsEnabled").isEnabled(), "Popup child did not reenable");
            config.tabStats = false;
            config.extendedTabStatsView = true;
            require(!config.optionNames.get("extendedTabStatsHeaders").isEnabled(), "Extended view ignored tab master");
            config.tabStats = true;
            config.extendedTabStatsView = false;
            require(!config.optionNames.get("highlightTaggedPlayers").isEnabled(), "Highlight ignored extended view");
            require(config.optionNames.get("showDot12").isEnabled(), "Standard separators wrongly depend on extended view");
            config.extendedTabStatsTeamColumnMode = 1;
            require(config.optionNames.get("extendedTabStatsStripCombinedTeamPadding").isHidden(), "Irrelevant padding visible");
            config.extendedTabStatsTeamColumnMode = 3;
            require(!config.optionNames.get("extendedTabStatsStripCombinedTeamPadding").isHidden(), "Padding stayed hidden");
            config.numberDenicker = false;
            require(!config.optionNames.get("numberDenickerFuzzy").isEnabled(), "Automatic denicker dependency missing");
            require(config.optionNames.get("finalsRange").isEnabled(), "Manual denick settings disabled");
            config.coloredHitboxes = false;
            config.coloredNametagBackgrounds = false;
            require(!config.optionNames.get("hitboxHueMode").isEnabled(), "Unused colours enabled");
            config.coloredNametagBackgrounds = true;
            require(config.optionNames.get("hitboxHueMode").isEnabled(), "Nametags cannot enable shared colours");
            config.pregameStats = false;
            config.autoLeaveBlacklistedPregameChat = true;
            require(!config.optionNames.get("autoLeaveBlacklistedPregameCommand").isEnabled(), "Auto leave prerequisite missing");
            require(config.optionNames.get("mentionLobbyStats").isEnabled(), "Independent lobby trigger disabled");
            require(!config.optionNames.containsKey("seraph") && !config.optionNames.containsKey("seraphKey")
                && !config.optionNames.containsKey("nametagClientIconPosition"), "Retired controls still visible");

            config.statsProvider = 4; // Bedlify must survive a profile load.
            config.pingProvider = 3; // Retired Seraph must become None.
            config.customStat1 = 14;
            config.skywarsCustomStat1 = 11;
            config.duelsCustomStat1 = 14;
            require(config.optionNames.containsKey("buildBattleCustomStat1") && config.optionNames.containsKey("tntRunCustomStat1"), "Game column controls absent from OneConfig");
            config.buildBattleCustomStat1 = 3;
            config.tntRunCustomStat1 = 7;
            config.customStat2 = 13; // Existing Ping index must not move.
            config.save();
            config.statsProvider = 0;
            config.load();
            require(config.buildBattleCustomStat1 == 3 && config.tntRunCustomStat1 == 7, "New game layouts were not persisted");
            require(com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns.getConfiguredStatsForScope(
                com.roxiun.mellow.stats.StatScope.TNT_RUN, config)[0] == 7, "TNT Run dropdown did not reach renderer");
            require(config.statsProvider == 4, "Bedlify clamped to another provider");
            require(config.pingProvider == 0 && config.customStat1 == 10 && config.skywarsCustomStat1 == 7
                && config.duelsCustomStat1 == 10 && config.customStat2 == 13, "Retired selections migrated incorrectly");
            require(!config.optionNames.get("autoLeaveBlacklistedPregameCommand").isEnabled(), "Dependencies lost after load");
            for (String field : new String[]{"finalsRange", "bedsRange", "maxResults"}) {
                String[] labels = config.getClass().getField(field)
                    .getAnnotation(cc.polyfrost.oneconfig.config.annotations.Dropdown.class).options();
                for (int index = 0; index < labels.length; index++) {
                    int actual = field.equals("maxResults")
                        ? com.roxiun.mellow.feature.nicks.DenickSearchOptions.limit(index)
                        : com.roxiun.mellow.feature.nicks.DenickSearchOptions.range(index);
                    require(actual == Integer.parseInt(labels[index]), "Denick lookup disagrees with its label");
                }
            }
        } finally {
            java.nio.file.Files.write(file, original);
            config.load();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
