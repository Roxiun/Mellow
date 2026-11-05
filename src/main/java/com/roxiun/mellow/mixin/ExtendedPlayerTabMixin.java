package com.roxiun.mellow.mixin;

//? if ornithe {
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
//?}
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
//? if forge {
/*import java.util.Collections;
import java.util.Iterator;
import java.util.List;
*///?}
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
//? if ornithe {
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//?} else {
/*import org.spongepowered.asm.mixin.injection.*;
*///?}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if forge {
/*import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
*///?}

//? if ornithe {
/** Retains the vanilla header/footer pass and VanillaHUD's outer transform. */
@Mixin(GuiPlayerTabOverlay.class)
//?} else {
/*/^* Retains the native shell and VanillaHUD's sizing pass. ^/
@Mixin(value = GuiPlayerTabOverlay.class, priority = 1200)
*///?}
public abstract class ExtendedPlayerTabMixin {
    @Shadow private IChatComponent header;
    @Shadow private IChatComponent footer;
    @Unique private ExtendedStatsTabOverlay.Layout mellow$layout;

    @Inject(method = "renderPlayerlist", at = @At("HEAD"), require = 1)
    private void mellow$measure(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo ci) {
        mellow$layout = VanillaHudTabIntegration.measure(width, header, footer, objective);
    }

    //? if ornithe {
    // Our layout already measured the rows. Skip vanilla's redundant sizing loop,
    // which otherwise creates zero-valued scoreboard entries for missing players.
    @WrapOperation(method = "renderPlayerlist", at = @At(value = "INVOKE",
    //?} else {
    /*@Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE",
    *///?}
        target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), require = 1)
    //? if ornithe {
    private java.util.Iterator<?> mellow$measureRows(java.util.List<?> players,
                                                    Operation<java.util.Iterator<?>> original) {
        return mellow$layout != null ? java.util.Collections.emptyIterator() : original.call(players);
    //?} else {
    /*private Iterator<?> mellow$skipSizing(List<?> players) {
        return mellow$layout == null ? players.iterator() : Collections.emptyList().iterator();
    *///?}
    }

    //? if ornithe {
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
    //?} else {
    /*// The first null initializes the header lines after vanilla finishes its column layout.
    // Unlike the header field read, VanillaHUD does not redirect this instruction.
    @ModifyVariable(method = "renderPlayerlist", index = 8, at = @At(value = "CONSTANT", args = "nullValue=true", ordinal = 0), require = 1)
    private int mellow$skipRows(int count) { return mellow$layout == null ? count : 0; }

    @ModifyVariable(method = "renderPlayerlist", index = 9, at = @At(value = "CONSTANT", args = "nullValue=true", ordinal = 0), require = 1)
    private int mellow$height(int rows) { return mellow$layout == null ? rows : mellow$layout.bodyHeight() / 9; }

    @ModifyVariable(method = "renderPlayerlist", index = 16, at = @At(value = "STORE"), require = 1)
    private int mellow$width(int width) { return mellow$layout == null ? width : mellow$layout.width(); }
    *///?}

    @Inject(method = "renderPlayerlist", at = @At(value = "INVOKE",
        //? if ornithe {
        target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V",
        ordinal = 1, shift = At.Shift.AFTER), require = 1)
    private void mellow$body(int width, Scoreboard scoreboard, ScoreObjective objective,
                             CallbackInfo ci, @Local(index = 15) int top) {
        if (mellow$layout != null) {
        //?} else {
        /*target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 1,
        shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, require = 1)
    private void mellow$body(int width, Scoreboard board, ScoreObjective objective, CallbackInfo ci,
                            net.minecraft.client.network.NetHandlerPlayClient handler,
                            List<?> players, int nameWidth, int scoreWidth, int count, int rows,
                            int columns, boolean heads, int objectiveWidth, int columnWidth,
                            int left, int top) {
        if (mellow$layout != null && !VanillaHudTabIntegration.measuring()) {
        *///?}
            Mellow.tabOverlayRouter.getOverlay().drawBody(mellow$layout, width, top,
                (GuiPlayerTabOverlay) (Object) this);
        }
    }
}
