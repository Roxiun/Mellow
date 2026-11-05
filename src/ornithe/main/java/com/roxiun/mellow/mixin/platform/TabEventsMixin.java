package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.gui.GuiPlayerTabOverlay.class)
public abstract class TabEventsMixin {
 @Inject(method="renderPlayerlist",at=@At("HEAD"),cancellable=true)
 private void mellow$tab(int width,net.minecraft.scoreboard.Scoreboard scoreboard,net.minecraft.scoreboard.ScoreObjective objective,CallbackInfo ci){
  RenderGameOverlayEvent.Pre event=new RenderGameOverlayEvent.Pre(RenderGameOverlayEvent.ElementType.PLAYER_LIST,0f);
  EventManager.INSTANCE.post(event);if(event.isCanceled())ci.cancel();
 }
}
