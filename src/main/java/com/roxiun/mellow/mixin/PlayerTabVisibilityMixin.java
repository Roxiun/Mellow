package com.roxiun.mellow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiIngame.class)
public abstract class PlayerTabVisibilityMixin {
    @WrapOperation(method = "renderGameOverlay", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z"), require = 1)
    private boolean mellow$pinned(KeyBinding key, Operation<Boolean> original) {
        return original.call(key) || (!VanillaHudTabIntegration.usesToggle()
            && Mellow.tabOverlayRouter != null && Mellow.tabOverlayRouter.isPinned());
    }
}
