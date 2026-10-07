package com.roxiun.mellow.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains the vanilla header/footer pass and VanillaHUD's outer transform. */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class ExtendedPlayerTabMixin {
    @Shadow private IChatComponent header;
    @Shadow private IChatComponent footer;
    @Unique private ExtendedStatsTabOverlay.Layout mellow$layout;

    @Inject(method = "renderPlayerlist", at = @At("HEAD"), require = 1)
    private void mellow$measure(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo ci) {
        mellow$layout = VanillaHudTabIntegration.measure(width, header, footer, objective);
    }

    // Our layout already measured the rows. Skip vanilla's redundant sizing loop,
    // which otherwise creates zero-valued scoreboard entries for missing players.
    @WrapOperation(method = "renderPlayerlist", at = @At(value = "INVOKE",
        target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), require = 1)
    private java.util.Iterator<?> mellow$measureRows(java.util.List<?> players,
                                                    Operation<java.util.Iterator<?>> original) {
        return mellow$layout != null ? java.util.Collections.emptyIterator() : original.call(players);
    }

    // 1.8.9 locals: m (8) = player count, n (9) = rows, s (16) = shell width.
    // At the first header read the vanilla multi-column calculation has finished.
    @Inject(method = "renderPlayerlist", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;header:Lnet/minecraft/util/IChatComponent;",
        ordinal = 0), require = 1)
    private void mellow$bodyDimensions(int width, Scoreboard scoreboard, ScoreObjective objective,
                                      CallbackInfo ci, @Local(index = 8) LocalIntRef count,
                                      @Local(index = 9) LocalIntRef rows,
                                      @Local(index = 16) LocalIntRef shellWidth) {
        if (mellow$layout == null) return;
        count.set(0); // Only skip vanilla player rows; header/footer continue normally.
        rows.set(mellow$layout.bodyHeight() / 9);
        shellWidth.set(mellow$layout.width());
    }

    @Inject(method = "renderPlayerlist", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V",
        ordinal = 1, shift = At.Shift.AFTER), require = 1)
    private void mellow$body(int width, Scoreboard scoreboard, ScoreObjective objective,
                             CallbackInfo ci, @Local(index = 15) int top) {
        if (mellow$layout != null) {
            Mellow.tabOverlayRouter.getOverlay().drawBody(mellow$layout, width, top,
                (GuiPlayerTabOverlay) (Object) this);
        }
    }
}
