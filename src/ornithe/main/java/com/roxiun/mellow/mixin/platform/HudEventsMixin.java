package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.gui.GuiIngame.class)
public abstract class HudEventsMixin {
 @Inject(method="renderGameOverlay",at=@At("RETURN")) private void mellow$hud(float partialTicks,CallbackInfo ci){EventManager.INSTANCE.post(new RenderGameOverlayEvent.Post(RenderGameOverlayEvent.ElementType.ALL,partialTicks));}
}
