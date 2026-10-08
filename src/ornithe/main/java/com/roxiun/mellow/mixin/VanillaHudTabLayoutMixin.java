package com.roxiun.mellow.mixin;

import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Measure the same content that is rendered inside VanillaHUD's tab transform. */
@Pseudo
@Mixin(targets = "org.polyfrost.vanillahud.hud.TabListHud", remap = false)
public abstract class VanillaHudTabLayoutMixin {
    @Inject(method = "measuredWidth", at = @At("HEAD"), cancellable = true, require = 1)
    private void mellow$width(CallbackInfoReturnable<Float> cir) {
        ExtendedStatsTabOverlay.Layout layout = VanillaHudTabIntegration.measureCurrent();
        if (layout != null) cir.setReturnValue((float) (layout.width() + 2));
    }
    @Inject(method = "measuredHeight", at = @At("HEAD"), cancellable = true, require = 1)
    private void mellow$height(CallbackInfoReturnable<Float> cir) {
        ExtendedStatsTabOverlay.Layout layout = VanillaHudTabIntegration.measureCurrent();
        if (layout != null) cir.setReturnValue((float) (layout.totalHeight() + 1));
    }
    @Inject(method = "foreignBounds", at = @At("HEAD"), cancellable = true, require = 1)
    private void mellow$clip(CallbackInfoReturnable<org.polyfrost.vanillahud.compat.TabListCompat.Bounds> cir) {
        ExtendedStatsTabOverlay.Layout layout = VanillaHudTabIntegration.measureCurrent();
        if (layout != null) cir.setReturnValue(new org.polyfrost.vanillahud.compat.TabListCompat.Bounds(
            9F, layout.totalHeight() + 1F));
    }
}
