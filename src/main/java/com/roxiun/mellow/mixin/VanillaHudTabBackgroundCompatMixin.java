package com.roxiun.mellow.mixin;
import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The v1 TabListHud owns the entire animated widget, including its background. */
@Pseudo
@Mixin(targets="org.polyfrost.vanillahud.hud.TabListHud",remap=false)
public abstract class VanillaHudTabBackgroundCompatMixin {
 @Inject(method="shouldShow",at=@At("HEAD"),cancellable=true,require=1)
 private void mellow$hideVanillaTab(CallbackInfoReturnable<Boolean> cir) {
  if(ExtendedTabStatsMode.isExtendedModeActiveNow()) cir.setReturnValue(false);
 }
}
