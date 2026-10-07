package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets Forge and VanillaHUD both observe pinning through their existing visibility path. */
@Mixin(KeyBinding.class)
public abstract class PlayerTabVisibilityMixin {
    @Inject(method = "isKeyDown", at = @At("RETURN"), cancellable = true, require = 1)
    private void mellow$pinned(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null && (Object) this == mc.gameSettings.keyBindPlayerList
            && Mellow.tabOverlayRouter != null && Mellow.tabOverlayRouter.isPinned()
            && !VanillaHudTabIntegration.usesToggle()) cir.setReturnValue(true);
    }
}
