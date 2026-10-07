package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** Retains the native shell and VanillaHUD's sizing pass. */
@Mixin(value = GuiPlayerTabOverlay.class, priority = 1200)
public abstract class ExtendedPlayerTabMixin {
    @Shadow private IChatComponent header;
    @Shadow private IChatComponent footer;
    @Unique private ExtendedStatsTabOverlay.Layout mellow$layout;

    @Inject(method = "renderPlayerlist", at = @At("HEAD"), require = 1)
    private void mellow$measure(int width, Scoreboard board, ScoreObjective objective, CallbackInfo ci) {
        mellow$layout = VanillaHudTabIntegration.measure(width, header, footer, objective);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE",
        target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), require = 1)
    private Iterator<?> mellow$skipSizing(List<?> players) {
        return mellow$layout == null ? players.iterator() : Collections.emptyList().iterator();
    }

    // The first null initializes the header lines after vanilla finishes its column layout.
    // Unlike the header field read, VanillaHUD does not redirect this instruction.
    @ModifyVariable(method = "renderPlayerlist", index = 8, at = @At(value = "CONSTANT", args = "nullValue=true", ordinal = 0), require = 1)
    private int mellow$skipRows(int count) { return mellow$layout == null ? count : 0; }

    @ModifyVariable(method = "renderPlayerlist", index = 9, at = @At(value = "CONSTANT", args = "nullValue=true", ordinal = 0), require = 1)
    private int mellow$height(int rows) { return mellow$layout == null ? rows : mellow$layout.bodyHeight() / 9; }

    @ModifyVariable(method = "renderPlayerlist", index = 16, at = @At(value = "STORE"), require = 1)
    private int mellow$width(int width) { return mellow$layout == null ? width : mellow$layout.width(); }

    @Inject(method = "renderPlayerlist", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 1,
        shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, require = 1)
    private void mellow$body(int width, Scoreboard board, ScoreObjective objective, CallbackInfo ci,
                            net.minecraft.client.network.NetHandlerPlayClient handler,
                            List<?> players, int nameWidth, int scoreWidth, int count, int rows,
                            int columns, boolean heads, int objectiveWidth, int columnWidth,
                            int left, int top) {
        if (mellow$layout != null && !VanillaHudTabIntegration.measuring()) {
            Mellow.tabOverlayRouter.getOverlay().drawBody(mellow$layout, width, top,
                (GuiPlayerTabOverlay) (Object) this);
        }
    }
}
