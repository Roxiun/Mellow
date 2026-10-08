package com.roxiun.mellow.test;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.replay.ReplayNetworkManager;
import com.roxiun.mellow.feature.stats.tab.*;
import com.roxiun.mellow.gamestate.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraftforge.fml.common.Loader;
import net.hypixel.data.type.GameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.*;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.*;
import net.minecraft.world.*;

/** Exercises the transformed method with an offline world; no server or API calls. */
final class TabIntegrationSmokeTest {
    @SuppressWarnings("unchecked")
    static void verify() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        java.lang.reflect.Field managerField = HypixelFeatures.class.getDeclaredField("gameStateManager");
        managerField.setAccessible(true);
        Object manager = managerField.get(HypixelFeatures.getInstance());
        java.lang.reflect.Field snapshotField = GameStateManager.class.getDeclaredField("snapshot");
        snapshotField.setAccessible(true);
        AtomicReference<GameSnapshot> state = (AtomicReference<GameSnapshot>) snapshotField.get(manager);
        GameSnapshot saved = state.get();
        WorldClient oldWorld = mc.theWorld;
        EntityPlayerSP oldPlayer = mc.thePlayer;
        int[] oldColumns = getColumns();
        boolean oldStats = Mellow.config.tabStats, oldExtended = Mellow.config.extendedTabStatsView;
        Map<String, TabStats> oldStatsMap = new HashMap<>(Mellow.tabStats);
        int oldGuiScale = mc.gameSettings.guiScale;
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        com.roxiun.mellow.mixin.PlayerTabOverlayAccessor accessor = (com.roxiun.mellow.mixin.PlayerTabOverlayAccessor) tab;
        IChatComponent oldHeader = accessor.mellow$getHeader(), oldFooter = accessor.mellow$getFooter();
        List<NetworkPlayerInfo> players = new ArrayList<>();
        NetHandlerPlayClient net = new NetHandlerPlayClient(mc, null, new ReplayNetworkManager() {
            @Override public java.net.SocketAddress getRemoteAddress() {
                return new java.net.InetSocketAddress("127.0.0.1", 25565);
            }
        }, mc.getSession().getProfile()) {
            @Override public Collection<NetworkPlayerInfo> getPlayerInfoMap() { return players; }
            @Override public NetworkPlayerInfo getPlayerInfo(UUID id) {
                return players.stream().filter(p -> p.getGameProfile().getId().equals(id)).findFirst().orElse(null);
            }
        };
        try {
            mc.theWorld = new WorldClient(net, new WorldSettings(0, WorldSettings.GameType.SURVIVAL,
                false, false, WorldType.DEFAULT), 0, EnumDifficulty.NORMAL, mc.mcProfiler);
            mc.thePlayer = new EntityPlayerSP(mc, mc.theWorld, net, new StatFileWriter());
            state.set(new GameSnapshot(true, "mini-test", GameType.BEDWARS, "BEDWARS_EIGHT_ONE", "Test",
                GamePhase.LIVE, "BED WARS", Collections.emptyList(), PartyState.empty(), 0, 1));
            Mellow.config.tabStats = true;
            Mellow.config.extendedTabStatsView = true;
            setColumns(1, 2, 3, 5, 13);
            for (int i = 0; i < 16; i++) addPlayer(players, i);
            tab.setHeader(new ChatComponentText("§bYou are playing on §eMC.HYPIXEL.NET\n§7Offline integration preview"));
            tab.setFooter(new ChatComponentText("You are currently §cBUSY\n\n§aRanks, Boosters & MORE! §cSTORE.HYPIXEL.NET"));
            Scoreboard board = mc.theWorld.getScoreboard();
            ScoreObjective objective = board.addScoreObjective("health", IScoreObjectiveCriteria.DUMMY);
            objective.setDisplayName("HP");
            board.setObjectiveInDisplaySlot(0, objective);
            for (NetworkPlayerInfo player : players) board.getValueFromObjective(player.getGameProfile().getName(), objective).setScorePoints(20);
            verifyHealth(mc, players.get(0), board, objective);
            tab.updatePlayerList(true);
            ExtendedStatsTabOverlay.Layout layout = VanillaHudTabIntegration.measureCurrent();
            check(layout != null && layout.players().size() == 16, "Missing integrated layout");
            check(layout.objectiveWidth() > 0, "Tab scoreboard objective was lost");
            check(layout.totalHeight() > layout.bodyHeight(), "Header/footer not included in measurement");
            render(mc, tab, board, objective, "tab-integrated.png");
            java.lang.reflect.Field layoutField = Arrays.stream(tab.getClass().getDeclaredFields())
                .filter(f -> f.getType() == ExtendedStatsTabOverlay.Layout.class).findFirst().orElseThrow(() -> new AssertionError("Missing mixin layout"));
            layoutField.setAccessible(true);
            check(layoutField.get(tab) != null, "Transformed render did not prepare custom body");
            verifyPin(mc);
            if (Loader.isModLoaded("vanillahud")) {
                verifyVanillaHudSettings();
                float scale = org.polyfrost.vanillahud.hud.TabList.hud.getScale();
                try {
                    org.polyfrost.vanillahud.hud.TabList.hud.setScale(0.75F, false);
                    render(mc, tab, board, objective, "tab-hud-scaled.png");
                    verifyPanelPixels(mc, "tab-hud-scaled.png");
                } finally { org.polyfrost.vanillahud.hud.TabList.hud.setScale(scale, false); }
            }
            int savedHeaders = Mellow.config.extendedTabStatsHeaders;
            try {
                int normalHeight = 0;
                for (int mode = 0; mode < 3; mode++) {
                    Mellow.config.extendedTabStatsHeaders = mode;
                    ExtendedStatsTabOverlay.Layout headerLayout = VanillaHudTabIntegration.measureCurrent();
                    if (mode == 1) normalHeight = headerLayout.bodyHeight();
                    if (mode == 2) check(headerLayout.headerHeight() == 0 && headerLayout.bodyHeight() < normalHeight,
                        "Hidden headings still reserved a header row");
                    render(mc, tab, board, objective, "tab-headers-" + mode + ".png");
                }
            } finally {
                Mellow.config.extendedTabStatsHeaders = savedHeaders;
            }
            for (int i = 16; i < 100; i++) addPlayer(players, i);
            for (int scale : new int[]{1, 2, 3, 0}) {
                mc.gameSettings.guiScale = scale;
                layout = VanillaHudTabIntegration.measureCurrent();
                ScaledResolution resolution = new ScaledResolution(mc);
                check(layout.width() + 2 <= resolution.getScaledWidth(), "Tab exceeded screen width");
                check(layout.totalHeight() + 10 <= resolution.getScaledHeight(), "Footer exceeded screen height");
                check(layout.visibleCount() < layout.players().size(), "Large list did not scroll");
                Mellow.tabOverlayRouter.getOverlay().handleMouseWheel(-120);
                render(mc, tab, board, objective, "tab-scroll-scale-" + scale + ".png");
            }
            setColumns(1, 2, 3, 4, 5, 6, 7, 8, 9, 13);
            check(VanillaHudTabIntegration.measureCurrent().scale() < 1F, "Wide-table fixture did not exercise scaling");
            render(mc, tab, board, objective, "tab-wide.png");
            objective.setRenderType(IScoreObjectiveCriteria.EnumRenderType.HEARTS);
            setColumns(2, 11);
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() == 0, "Duplicate health columns");
            setColumns(2);
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() == 90, "Native hearts were lost");
            render(mc, tab, board, objective, "tab-hearts.png");
            players.clear();
            render(mc, tab, board, objective, "tab-empty.png");
            Mellow.config.extendedTabStatsView = false;
            check(VanillaHudTabIntegration.measureCurrent() == null, "Disabled extended mode stayed active");
            tab.renderPlayerlist(new ScaledResolution(mc).getScaledWidth(), board, objective);
            check(layoutField.get(tab) == null, "Stale layout survived mode change");
        } finally {
            mc.theWorld = oldWorld;
            mc.thePlayer = oldPlayer;
            state.set(saved);
            setColumns(oldColumns);
            Mellow.config.tabStats = oldStats;
            Mellow.config.extendedTabStatsView = oldExtended;
            Mellow.tabStats.clear(); Mellow.tabStats.putAll(oldStatsMap);
            mc.gameSettings.guiScale = oldGuiScale;
            tab.setHeader(oldHeader); tab.setFooter(oldFooter);
        }
    }

    private static void verifyHealth(Minecraft mc, NetworkPlayerInfo player, Scoreboard board,
                                     ScoreObjective original) {
        net.minecraft.client.entity.EntityOtherPlayerMP entity = new net.minecraft.client.entity.EntityOtherPlayerMP(mc.theWorld, player.getGameProfile());
        entity.setHealth(8F);
        entity.setAbsorptionAmount(2F);
        mc.theWorld.playerEntities.add(entity);
        int[] columns = getColumns();
        ScoreObjective health = board.addScoreObjective("health-fixture", IScoreObjectiveCriteria.DUMMY);
        try {
            board.setObjectiveInDisplaySlot(0, health);
            setColumns(2, 11);
            for (String title : new String[]{"§c♥", "§c❤\uFE0F", "HP", "Health"}) {
                health.setDisplayName(title);
                check(TabHealthValueResolver.isHealthObjective(health), "Health label was not recognised: " + title);
            }
            health.setDisplayName("§c❤");
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§610"),
                "Missing server score did not fall back to entity health plus absorption");
            mc.ingameGUI.getTabList().renderPlayerlist(new ScaledResolution(mc).getScaledWidth(), board, health);
            check(board.getSortedScores(health).isEmpty(), "Rendering missing health created a score");
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§610"),
                "Native sizing created a zero that masked entity health");
            Score score = board.getValueFromObjective(player.getGameProfile().getName(), health);
            score.setScorePoints(14);
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§e14"),
                "Server health did not take precedence over entity health");
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() == 0, "Numeric health duplicated HP");
            score.setScorePoints(0);
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§c0"), "Zero health incorrectly used fallback");
            mc.theWorld.playerEntities.remove(entity);
            score.setScorePoints(17);
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§a17"), "Distant player lost server health");
            health.setDisplayName("Kills");
            check(!TabHealthValueResolver.isHealthObjective(health), "Unrelated score treated as health");
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§7--"), "Stale score reused as health");
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() > 0, "Unrelated scoreboard column was hidden");
            mc.theWorld.playerEntities.add(entity);
            check(TabHealthValueResolver.getFormattedHealth(mc, player).equals("§610"), "Non-health score blocked entity fallback");
            health.setDisplayName("§c❤");
            setColumns(2);
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() > 0, "Native health lost when HP is disabled");
        } finally {
            mc.theWorld.playerEntities.remove(entity);
            board.removeObjective(health);
            board.setObjectiveInDisplaySlot(0, original);
            setColumns(columns);
        }
    }

    private static void addPlayer(List<NetworkPlayerInfo> players, int index) {
        String name = "Player" + String.format(java.util.Locale.ROOT, "%02d", index);
        NetworkPlayerInfo info = new NetworkPlayerInfo(new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name)) {
            @Override public ResourceLocation getLocationSkin() { return new ResourceLocation("textures/entity/steve.png"); }
            @Override public WorldSettings.GameType getGameType() { return WorldSettings.GameType.SURVIVAL; }
            @Override public int getResponseTime() { return 40 + index * 7; }
        };
        info.setDisplayName(new ChatComponentText("§b[MVP+] " + name));
        players.add(info);
        Mellow.tabStats.put(name, new TabStats(com.roxiun.mellow.api.tags.TagReport.empty(), "§b[MVP+] " + name,
            "§b" + (100 + index * 25) + "✫", "§e" + (index + 1) + ".25", "§a12", "§f1.52", "§71.0", "§a250", "§f200", "§e130", "§a1200"));
    }

    private static void render(Minecraft mc, GuiPlayerTabOverlay tab, Scoreboard board, ScoreObjective objective, String image) {
        mc.getFramebuffer().bindFramebuffer(true);
        mc.entityRenderer.setupOverlayRendering();
        int width = new ScaledResolution(mc).getScaledWidth();
        int height = new ScaledResolution(mc).getScaledHeight();
        Gui.drawRect(0, 0, width, height, 0xff526779);
        try {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindPlayerList.getKeyCode(), true);
            Class<?> forge = net.minecraftforge.client.GuiIngameForge.class;
            java.lang.reflect.Field parent = forge.getDeclaredField("eventParent"); parent.setAccessible(true);
            parent.set(mc.ingameGUI, new net.minecraftforge.client.event.RenderGameOverlayEvent(0, new ScaledResolution(mc)));
            if (Loader.isModLoaded("vanillahud")) prepareVanillaHud(mc);
            java.lang.reflect.Method render = forge.getDeclaredMethod("renderPlayerList", int.class, int.class);
            render.setAccessible(true);
            render.invoke(mc.ingameGUI, width, height);
            if (Loader.isModLoaded("vanillahud")) {
                ExtendedStatsTabOverlay.Layout layout = VanillaHudTabIntegration.measureCurrent();
                check(org.polyfrost.vanillahud.hud.TabList.width == 2 * (layout.width() / 2) + 2, "VanillaHUD width mismatch: " + org.polyfrost.vanillahud.hud.TabList.width + " vs " + layout.width());
                check(org.polyfrost.vanillahud.hud.TabList.height > layout.bodyHeight(), "VanillaHUD lost footer height");
            }
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        finally { net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindPlayerList.getKeyCode(), false); }
        GlStateManager.color(1, 1, 1, 1);
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, image, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        if (image.equals("tab-integrated.png") || image.equals("tab-wide.png")) verifyPanelPixels(mc, image);
    }

    /** Catches deferred text drawn at (0, 0) after the body's matrix has been popped. */
    private static void verifyPanelPixels(Minecraft mc, String file) {
        try {
            java.awt.image.BufferedImage pixels = javax.imageio.ImageIO.read(new java.io.File(mc.mcDataDir, "screenshots/" + file));
            ExtendedStatsTabOverlay.Layout layout = VanillaHudTabIntegration.measureCurrent();
            ScaledResolution resolution = new ScaledResolution(mc);
            int factor = resolution.getScaleFactor();
            int left = (resolution.getScaledWidth() / 2 - layout.width() / 2 - 2) * factor;
            int right = (resolution.getScaledWidth() / 2 + layout.width() / 2 + 2) * factor;
            int top = 8 * factor;
            if (Loader.isModLoaded("vanillahud")) {
                cc.polyfrost.oneconfig.hud.Position position = org.polyfrost.vanillahud.hud.TabList.hud.position;
                left = (int) (position.getX() * factor) - 2;
                right = (int) (position.getRightX() * factor) + 2;
                top = (int) (position.getY() * factor) - 2;
            }
            int clear = pixels.getRGB(pixels.getWidth() - 1, pixels.getHeight() - 1);
            int inside = 0;
            for (int y = 0; y < pixels.getHeight(); y++) {
                for (int x = 0; x < pixels.getWidth(); x++) {
                    if (pixels.getRGB(x, y) == clear) continue;
                    check(x >= left && x <= right && y >= top,
                        "Tab drew outside its panel at " + x + "," + y + " in " + file);
                    inside++;
                }
            }
            check(inside > 1000, "Tab rendering fixture was blank");
        } catch (java.io.IOException failure) {
            throw new AssertionError("Could not inspect tab screenshot", failure);
        }
    }

    private static void prepareVanillaHud(Minecraft mc) {
        org.polyfrost.vanillahud.hud.TabList.isGuiIngame = true;
        org.polyfrost.vanillahud.hud.TabList.TabHud.tabAnimation = false;
        org.polyfrost.vanillahud.hud.TabList.TabHud.displayMode = false;
        // Let the real hidden render and BasicHud layout establish the panel bounds.
        org.polyfrost.vanillahud.hooks.TabHook.gettingSize = true;
        try {
            mc.ingameGUI.getTabList().renderPlayerlist(new ScaledResolution(mc).getScaledWidth(),
                mc.theWorld.getScoreboard(), mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(0));
        } finally { org.polyfrost.vanillahud.hooks.TabHook.gettingSize = false; }
        org.polyfrost.vanillahud.hud.TabList.hud.drawAll(new cc.polyfrost.oneconfig.libs.universal.UMatrixStack(), false);
        cc.polyfrost.oneconfig.hud.Position position = org.polyfrost.vanillahud.hud.TabList.hud.position;
        position.setX((new ScaledResolution(mc).getScaledWidth() - position.getWidth()) / 2);
        position.setY(10);
    }
    private static void verifyVanillaHudSettings() {
        ExtendedStatsTabOverlay.Layout shown = VanillaHudTabIntegration.measureCurrent();
        org.polyfrost.vanillahud.hud.TabList.TabHud.showHeader = false;
        org.polyfrost.vanillahud.hud.TabList.TabHud.showFooter = false;
        try {
            ExtendedStatsTabOverlay.Layout hidden = VanillaHudTabIntegration.measureCurrent();
            check(hidden.totalHeight() == hidden.bodyHeight(), "Hidden VanillaHUD header/footer still reserved space");
            check(hidden.totalHeight() < shown.totalHeight(), "VanillaHUD toggles ignored");
        } finally {
            org.polyfrost.vanillahud.hud.TabList.TabHud.showHeader = true;
            org.polyfrost.vanillahud.hud.TabList.TabHud.showFooter = true;
        }
        org.polyfrost.vanillahud.hud.TabList.TabHud.displayMode = true;
        try { check(VanillaHudTabIntegration.usesToggle(), "VanillaHUD toggle mode ignored"); }
        finally { org.polyfrost.vanillahud.hud.TabList.TabHud.displayMode = false; }
        org.polyfrost.vanillahud.hud.TabList.TabHud.tabPlayerLimit = 10;
        try { check(VanillaHudTabIntegration.measureCurrent().players().size() == 10, "VanillaHUD player limit ignored"); }
        finally { org.polyfrost.vanillahud.hud.TabList.TabHud.tabPlayerLimit = 80; }
    }
    private static void verifyPin(Minecraft mc) throws Exception {
        java.lang.reflect.Field pin = com.roxiun.mellow.core.event.TabOverlayRouter.class.getDeclaredField("pinnedByDoubleTap");
        pin.setAccessible(true);
        pin.setBoolean(Mellow.tabOverlayRouter, true);
        check(mc.gameSettings.keyBindPlayerList.isKeyDown(), "Pin did not reach Forge visibility path");
        pin.setBoolean(Mellow.tabOverlayRouter, false);
        check(!mc.gameSettings.keyBindPlayerList.isKeyDown(), "Pin stayed latched");
    }
    private static int[] getColumns() {
        return ExtendedTabStatsColumns.getConfiguredStatsForScope(com.roxiun.mellow.api.provider.model.StatScope.BEDWARS, Mellow.config);
    }
    private static void setColumns(int... columns) {
        for (int i = 0; i < 10; i++) {
            try { Mellow.config.getClass().getField("customStat" + (i + 1)).setInt(Mellow.config, i < columns.length ? columns[i] : 10); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
