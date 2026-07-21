package com.roxiun.mellow.core.event;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsMode;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TabOverlayRouter {

    private static final long DOUBLE_TAP_WINDOW_MS = 300L;
    private static final long MAX_TAP_HOLD_MS = 180L;
    private static final long NO_TIME = Long.MIN_VALUE;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private ExtendedStatsTabOverlay overlay;

    private boolean wasActiveLastFrame;
    private StatScope lastScope;
    private long lastStateVersion = Long.MIN_VALUE;
    private int lastDimensionId = Integer.MIN_VALUE;
    private boolean tabWasDown;
    private long tabPressStartedAtMs = NO_TIME;
    private long pendingTapReleaseAtMs = NO_TIME;
    private boolean pinnedByDoubleTap;

    public TabOverlayRouter(MellowOneConfig config) {
        this.config = config;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderPlayerList(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            return;
        }

        StatScope scope = ExtendedTabStatsMode.resolveScope();
        if (!isExtendedModeActive(scope)) {
            clearDoubleTapState();
            resetIfNeeded();
            return;
        }

        if (!isTabKeyDown()) {
            if (pinnedByDoubleTap) {
                // Suppress any third-party trailing tab animation frames while pinned mode is active.
                event.setCanceled(true);
                return;
            }

            resetIfNeeded();
            // Suppress any third-party trailing tab animation frames while extended mode is active.
            event.setCanceled(true);
            return;
        }

        ExtendedStatsTabOverlay statsOverlay = getOverlay();
        if (statsOverlay == null) {
            return;
        }

        if (shouldResetScroll(scope)) {
            statsOverlay.resetScroll();
        }

        event.setCanceled(true);
        statsOverlay.renderExtendedPlayerList(scope);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderPinnedOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        StatScope scope = ExtendedTabStatsMode.resolveScope();
        if (!isExtendedModeActive(scope)) {
            clearDoubleTapState();
            resetIfNeeded();
            return;
        }

        if (!pinnedByDoubleTap || isTabKeyDown()) {
            return;
        }

        ExtendedStatsTabOverlay statsOverlay = getOverlay();
        if (statsOverlay == null) {
            return;
        }

        if (shouldResetScroll(scope)) {
            statsOverlay.resetScroll();
        }

        statsOverlay.renderExtendedPlayerList(scope);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        boolean tabDown = isTabKeyDown();
        StatScope scope = ExtendedTabStatsMode.resolveScope();
        if (!isExtendedModeActive(scope)) {
            clearDoubleTapState();
            resetIfNeeded();
            tabWasDown = tabDown;
            return;
        }

        long now = System.currentTimeMillis();
        clearStaleTapIfNeeded(now);

        if (!tabWasDown && tabDown) {
            onTabPressed(now);
        } else if (tabWasDown && !tabDown) {
            onTabReleased(now);
        }

        tabWasDown = tabDown;
    }

    public boolean isExtendedModeActive() {
        return isExtendedModeActive(ExtendedTabStatsMode.resolveScope());
    }

    public boolean isTabOverlayInputActive() {
        return isExtendedModeActive() && (isTabKeyDown() || pinnedByDoubleTap);
    }

    public ExtendedStatsTabOverlay getOverlay() {
        if (overlay == null && mc != null && mc.ingameGUI != null && config != null) {
            overlay = new ExtendedStatsTabOverlay(mc, mc.ingameGUI, config);
        }
        return overlay;
    }

    private boolean isExtendedModeActive(StatScope scope) {
        return (
            Mellow.isEnabled() &&
            config.tabStats &&
            config.extendedTabStatsView &&
            scope != null &&
            mc != null &&
            mc.thePlayer != null &&
            mc.theWorld != null
        );
    }

    private boolean shouldResetScroll(StatScope scope) {
        long stateVersion = getStateVersion();
        int dimensionId = getDimensionId();

        boolean shouldReset =
            !wasActiveLastFrame ||
            scope != lastScope ||
            stateVersion != lastStateVersion ||
            dimensionId != lastDimensionId;

        wasActiveLastFrame = true;
        lastScope = scope;
        lastStateVersion = stateVersion;
        lastDimensionId = dimensionId;
        return shouldReset;
    }

    private void resetIfNeeded() {
        if (!wasActiveLastFrame) {
            return;
        }

        ExtendedStatsTabOverlay statsOverlay = getOverlay();
        if (statsOverlay != null) {
            statsOverlay.resetScroll();
        }

        wasActiveLastFrame = false;
        lastScope = null;
        lastStateVersion = Long.MIN_VALUE;
        lastDimensionId = Integer.MIN_VALUE;
    }

    private void onTabPressed(long now) {
        tabPressStartedAtMs = now;
        if (pinnedByDoubleTap) {
            pinnedByDoubleTap = false;
            clearTapSequence();
            return;
        }
    }

    private void onTabReleased(long now) {
        long holdDurationMs = tabPressStartedAtMs == NO_TIME
            ? Long.MAX_VALUE
            : now - tabPressStartedAtMs;
        tabPressStartedAtMs = NO_TIME;

        if (holdDurationMs > MAX_TAP_HOLD_MS) {
            clearTapSequence();
            return;
        }

        if (
            pendingTapReleaseAtMs != NO_TIME &&
            now - pendingTapReleaseAtMs <= DOUBLE_TAP_WINDOW_MS
        ) {
            pinnedByDoubleTap = true;
            clearTapSequence();
            return;
        }

        pendingTapReleaseAtMs = now;
    }

    private void clearStaleTapIfNeeded(long now) {
        if (
            pendingTapReleaseAtMs != NO_TIME &&
            now - pendingTapReleaseAtMs > DOUBLE_TAP_WINDOW_MS
        ) {
            pendingTapReleaseAtMs = NO_TIME;
        }
    }

    private void clearTapSequence() {
        pendingTapReleaseAtMs = NO_TIME;
    }

    private void clearDoubleTapState() {
        pinnedByDoubleTap = false;
        tabPressStartedAtMs = NO_TIME;
        clearTapSequence();
    }

    private long getStateVersion() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        return snapshot == null ? Long.MIN_VALUE : snapshot.getStateVersion();
    }

    private int getDimensionId() {
        if (mc == null || mc.theWorld == null || mc.theWorld.provider == null) {
            return Integer.MIN_VALUE;
        }
        return mc.theWorld.provider.getDimensionId();
    }

    private boolean isTabKeyDown() {
        return (
            mc != null &&
            mc.gameSettings != null &&
            mc.gameSettings.keyBindPlayerList != null &&
            mc.gameSettings.keyBindPlayerList.isKeyDown()
        );
    }
}
