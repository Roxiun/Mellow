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
import net.fabricmc.loader.api.FabricLoader;
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
        var managerField = HypixelFeatures.class.getDeclaredField("gameStateManager");
        managerField.setAccessible(true);
        Object manager = managerField.get(HypixelFeatures.getInstance());
        var snapshotField = GameStateManager.class.getDeclaredField("snapshot");
        snapshotField.setAccessible(true);
        AtomicReference<GameSnapshot> state = (AtomicReference<GameSnapshot>) snapshotField.get(manager);
        GameSnapshot saved = state.get();
        var oldWorld = mc.theWorld;
        var oldPlayer = mc.thePlayer;
        String[] oldColumns = Mellow.config.bedwarsStatOrder;
        boolean oldStats = Mellow.config.tabStats, oldExtended = Mellow.config.extendedTabStatsView;
        Map<String, TabStats> oldStatsMap = new HashMap<>(Mellow.tabStats);
        int oldGuiScale = mc.gameSettings.guiScale;
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        var accessor = (com.roxiun.mellow.mixin.PlayerTabOverlayAccessor) tab;
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
                false, false, PregameReason.NONE, "BED WARS", List.of(), PartyState.empty(), 0, 1));
            Mellow.config.tabStats = true;
            Mellow.config.extendedTabStatsView = true;
            Mellow.config.bedwarsStatOrder = new String[]{"Stars", "Name", "FKDR", "WLR", "Ping"};
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
            var layout = VanillaHudTabIntegration.measureCurrent();
            check(layout != null && layout.players().size() == 16, "Missing integrated layout");
            check(layout.objectiveWidth() > 0, "Tab scoreboard objective was lost");
            check(layout.totalHeight() > layout.bodyHeight(), "Header/footer not included in measurement");
            render(mc, tab, board, objective, "tab-integrated.png");
            var layoutField = Arrays.stream(tab.getClass().getDeclaredFields())
                .filter(f -> f.getType() == ExtendedStatsTabOverlay.Layout.class).findFirst().orElseThrow();
            layoutField.setAccessible(true);
            check(layoutField.get(tab) != null, "Transformed render did not prepare custom body");
            verifyVisibility(mc);
            if (FabricLoader.getInstance().isModLoaded("vanillahud")) {
                verifyVanillaHud(layout);
                verifyWrappedRender(mc, tab, board, objective);
            }
            int savedHeaders = Mellow.config.extendedTabStatsHeaders;
            try {
                int normalHeight = 0;
                for (int mode = 0; mode < 3; mode++) {
                    Mellow.config.getTree().getProp("extendedTabStatsHeaders").setAs(mode);
                    var headerLayout = VanillaHudTabIntegration.measureCurrent();
                    if (mode == 1) normalHeight = headerLayout.bodyHeight();
                    if (mode == 2) check(headerLayout.headerHeight() == 0 && headerLayout.bodyHeight() < normalHeight,
                        "Hidden headings still reserved a header row");
                    render(mc, tab, board, objective, "tab-headers-" + mode + ".png");
                }
            } finally {
                Mellow.config.getTree().getProp("extendedTabStatsHeaders").setAs(savedHeaders);
            }
            for (int i = 16; i < 100; i++) addPlayer(players, i);
            for (int scale : new int[]{1, 2, 3, 0}) {
                mc.gameSettings.guiScale = scale;
                layout = VanillaHudTabIntegration.measureCurrent();
                var resolution = new ScaledResolution(mc);
                check(layout.width() + 2 <= resolution.getScaledWidth(), "Tab exceeded screen width");
                check(layout.totalHeight() + 10 <= resolution.getScaledHeight(), "Footer exceeded screen height");
                check(layout.visibleCount() < layout.players().size(), "Large list did not scroll");
                Mellow.tabOverlayRouter.getOverlay().handleMouseWheel(-120);
                render(mc, tab, board, objective, "tab-scroll-scale-" + scale + ".png");
            }
            Mellow.config.bedwarsStatOrder = new String[]{"Team", "Stars", "Name", "FKDR", "Winstreak", "WLR", "BBLR", "Wins", "Beds", "Finals", "HP", "Tags", "Ping", "Client"};
            check(VanillaHudTabIntegration.measureCurrent().scale() < 1F, "Wide-table fixture did not exercise scaling");
            render(mc, tab, board, objective, "tab-wide.png");
            objective.setRenderType(IScoreObjectiveCriteria.EnumRenderType.HEARTS);
            Mellow.config.bedwarsStatOrder = new String[]{"Name", "HP"};
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() == 0, "Duplicate health columns");
            Mellow.config.bedwarsStatOrder = new String[]{"Name"};
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
            Mellow.config.bedwarsStatOrder = oldColumns;
            Mellow.config.tabStats = oldStats;
            Mellow.config.extendedTabStatsView = oldExtended;
            Mellow.tabStats.clear(); Mellow.tabStats.putAll(oldStatsMap);
            mc.gameSettings.guiScale = oldGuiScale;
            tab.setHeader(oldHeader); tab.setFooter(oldFooter);
        }
    }

    private static void verifyHealth(Minecraft mc, NetworkPlayerInfo player, Scoreboard board,
                                     ScoreObjective original) {
        var entity = new net.minecraft.client.entity.EntityOtherPlayerMP(mc.theWorld, player.getGameProfile());
        entity.setHealth(8F);
        entity.setAbsorptionAmount(2F);
        mc.theWorld.playerEntities.add(entity);
        String[] columns = Mellow.config.bedwarsStatOrder;
        ScoreObjective health = board.addScoreObjective("health-fixture", IScoreObjectiveCriteria.DUMMY);
        try {
            board.setObjectiveInDisplaySlot(0, health);
            Mellow.config.bedwarsStatOrder = new String[]{"Name", "HP"};
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
            var score = board.getValueFromObjective(player.getGameProfile().getName(), health);
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
            Mellow.config.bedwarsStatOrder = new String[]{"Name"};
            check(VanillaHudTabIntegration.measureCurrent().objectiveWidth() > 0, "Native health lost when HP is disabled");
        } finally {
            mc.theWorld.playerEntities.remove(entity);
            board.removeObjective(health);
            board.setObjectiveInDisplaySlot(0, original);
            Mellow.config.bedwarsStatOrder = columns;
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
        Mellow.tabStats.put(name, new TabStats(List.of(), List.of(), "§b[MVP+] " + name,
            "§b" + (100 + index * 25) + "✫", "§e" + (index + 1) + ".25", "§a12", "§f1.52", "§71.0", "§a250", "§f200", "§e130", "§a1200"));
    }

    private static void render(Minecraft mc, GuiPlayerTabOverlay tab, Scoreboard board, ScoreObjective objective, String image) {
        mc.getFramebuffer().bindFramebuffer(true);
        mc.entityRenderer.setupOverlayRendering();
        int width = new ScaledResolution(mc).getScaledWidth();
        int height = new ScaledResolution(mc).getScaledHeight();
        Gui.drawRect(0, 0, width, height, 0xff526779);
        if (FabricLoader.getInstance().isModLoaded("vanillahud")) {
            var hud = org.polyfrost.vanillahud.hud.Huds.INSTANCE.getTabList();
            boolean animation = hud.getAnimation();
            try {
                hud.setAnimation(false);
                hud.updateOpen(true);
                var wrapper = Arrays.stream(GuiIngame.class.getDeclaredMethods())
                    .filter(m -> m.getName().contains("vanillahud$tabList") && m.getParameterCount() == 5)
                    .findFirst().orElseThrow();
                wrapper.setAccessible(true);
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original = args -> {
                    ((GuiPlayerTabOverlay) args[0]).renderPlayerlist((int) args[1], (Scoreboard) args[2], (ScoreObjective) args[3]);
                    return null;
                };
                wrapper.invoke(mc.ingameGUI, tab, width, board, objective, original);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            } finally {
                hud.setAnimation(animation);
            }
        } else {
            tab.renderPlayerlist(width, board, objective);
        }
        GlStateManager.color(1, 1, 1, 1);
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, image, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        if (image.equals("tab-integrated.png") || image.equals("tab-wide.png")) verifyPanelPixels(mc, image);
    }

    /** Catches deferred text drawn at (0, 0) after the body's matrix has been popped. */
    private static void verifyPanelPixels(Minecraft mc, String file) {
        try {
            var pixels = javax.imageio.ImageIO.read(new java.io.File(mc.mcDataDir, "screenshots/" + file));
            var layout = VanillaHudTabIntegration.measureCurrent();
            var resolution = new ScaledResolution(mc);
            int factor = resolution.getScaleFactor();
            int left = (resolution.getScaledWidth() / 2 - layout.width() / 2 - 2) * factor;
            int right = (resolution.getScaledWidth() / 2 + layout.width() / 2 + 2) * factor;
            int top = 8 * factor;
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

    private static void verifyVisibility(Minecraft mc) throws Exception {
        var key = mc.gameSettings.keyBindPlayerList;
        boolean saved = key.isKeyDown();
        var wrapper = Arrays.stream(GuiIngame.class.getDeclaredMethods())
            .filter(m -> m.getName().contains("mellow$pinned")).findFirst().orElseThrow();
        wrapper.setAccessible(true);
        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original = args ->
            ((net.minecraft.client.settings.KeyBinding) args[0]).isKeyDown();
        try {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), false);
            Mellow.tabOverlayRouter.onClientTick(null);
            for (int i = 0; i < 2; i++) {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), true);
                Mellow.tabOverlayRouter.onClientTick(null);
                net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), false);
                Mellow.tabOverlayRouter.onClientTick(null);
            }
            check(Mellow.tabOverlayRouter.isPinned(), "Double tap did not pin");
            check((boolean) wrapper.invoke(mc.ingameGUI, key, original), "Pinned tab did not enter native render path");
            net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), true);
            Mellow.tabOverlayRouter.onClientTick(null);
            check(!Mellow.tabOverlayRouter.isPinned(), "Tab press did not unpin");
        } finally {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), saved);
            Mellow.tabOverlayRouter.onClientTick(null);
        }
    }

    private static void verifyWrappedRender(Minecraft mc, GuiPlayerTabOverlay tab,
                                             Scoreboard board, ScoreObjective objective) throws Exception {
        var hud = org.polyfrost.vanillahud.hud.Huds.INSTANCE.getTabList();
        boolean animation = hud.getAnimation();
        var wrapper = Arrays.stream(GuiIngame.class.getDeclaredMethods())
            .filter(m -> m.getName().contains("vanillahud$tabList") && m.getParameterCount() == 5)
            .findFirst().orElseThrow();
        wrapper.setAccessible(true);
        int[] draws = {0};
        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original = args -> {
            draws[0]++;
            ((GuiPlayerTabOverlay) args[0]).renderPlayerlist((int) args[1], (Scoreboard) args[2], (ScoreObjective) args[3]);
            return null;
        };
        try {
            hud.setAnimation(false);
            hud.updateOpen(true);
            mc.entityRenderer.setupOverlayRendering();
            wrapper.invoke(mc.ingameGUI, tab, new ScaledResolution(mc).getScaledWidth(), board, objective, original);
            check(draws[0] == 1, "VanillaHUD suppressed or duplicated the integrated tab");
            hud.setAnimation(true);
            hud.updateOpen(false);
            check(hud.isRendering(), "Closing animation was suppressed");
            wrapper.invoke(mc.ingameGUI, tab, new ScaledResolution(mc).getScaledWidth(), board, objective, original);
            check(draws[0] == 2, "Closing animation did not render the integrated body");
        } finally {
            hud.setAnimation(animation);
            hud.updateOpen(false);
        }
    }

    private static void verifyVanillaHud(ExtendedStatsTabOverlay.Layout layout) throws Exception {
        var hud = org.polyfrost.vanillahud.hud.Huds.INSTANCE.getTabList();
        var width = hud.getClass().getDeclaredMethod("measuredWidth"); width.setAccessible(true);
        var height = hud.getClass().getDeclaredMethod("measuredHeight"); height.setAccessible(true);
        check((float) width.invoke(hud) == layout.width() + 2F, "VanillaHUD width mismatch");
        check((float) height.invoke(hud) == layout.totalHeight() + 1F, "VanillaHUD height mismatch");
        check(hud.foreignBounds().getHeight() == layout.totalHeight() + 1F, "Animation bounds mismatch");
        boolean header = hud.getShowHeader(), footer = hud.getShowFooter();
        int mode = hud.getDisplayMode();
        try {
            hud.setShowHeader(false); hud.setShowFooter(false);
            var hidden = VanillaHudTabIntegration.measureCurrent();
            check(hidden.totalHeight() == hidden.bodyHeight(), "Header/footer toggles ignored");
            hud.setDisplayMode(1);
            check(VanillaHudTabIntegration.usesToggle(), "VanillaHUD toggle mode ignored");
        } finally {
            hud.setShowHeader(header); hud.setShowFooter(footer); hud.setDisplayMode(mode);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
