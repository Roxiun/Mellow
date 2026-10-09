package com.roxiun.mellow.mixin;

//? if ornithe {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//?}
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration;
//? if ornithe {
import net.minecraft.client.gui.GuiIngame;
//?} else {
/*import net.minecraft.client.Minecraft;
*///?}
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if forge {
/*import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

//? if ornithe {
@Mixin(GuiIngame.class)
//?} else {
/*/^* Lets Forge and VanillaHUD both observe pinning through their existing visibility path. ^/
@Mixin(KeyBinding.class)
*///?}
public abstract class PlayerTabVisibilityMixin {
    //? if ornithe {
    @WrapOperation(method = "renderGameOverlay", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z"), require = 1)
    private boolean mellow$pinned(KeyBinding key, Operation<Boolean> original) {
        return original.call(key) || (!VanillaHudTabIntegration.usesToggle()
            && Mellow.tabOverlayRouter != null && Mellow.tabOverlayRouter.isPinned());
    //?} else {
    /*@Inject(method = "isKeyDown", at = @At("RETURN"), cancellable = true, require = 1)
    private void mellow$pinned(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null && (Object) this == mc.gameSettings.keyBindPlayerList
            && Mellow.tabOverlayRouter != null && Mellow.tabOverlayRouter.isPinned()
            && !VanillaHudTabIntegration.usesToggle()) cir.setReturnValue(true);
    *///?}
    }
}
