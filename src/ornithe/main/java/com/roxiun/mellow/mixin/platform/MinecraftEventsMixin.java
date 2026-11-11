package com.roxiun.mellow.mixin.platform;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftEventsMixin {
 @Shadow public net.minecraft.client.multiplayer.WorldClient theWorld;
 // Wrapped rather than redirected so it chains with other mods that also hook this Mouse.next() call.
 @WrapOperation(method="runTick",at=@At(value="INVOKE",target="Lorg/lwjgl/input/Mouse;next()Z"))
 private boolean mellow$mouse(Operation<Boolean> original){
  while(original.call()){
   MouseEvent event=new MouseEvent(org.lwjgl.input.Mouse.getEventDWheel(),org.lwjgl.input.Mouse.getEventButton(),org.lwjgl.input.Mouse.getEventButtonState());
   EventManager.INSTANCE.post(event);
   if(!event.isCanceled()) return true;
  }
  return false;
 }
 @Inject(method="loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",at=@At("HEAD"))
 private void mellow$unload(net.minecraft.client.multiplayer.WorldClient world,String message,CallbackInfo ci){if(theWorld!=null)EventManager.INSTANCE.post(new WorldEvent.Unload(theWorld));}
 @Inject(method="loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",at=@At("RETURN"))
 private void mellow$load(net.minecraft.client.multiplayer.WorldClient world,String message,CallbackInfo ci){if(world!=null)EventManager.INSTANCE.post(new WorldEvent.Load(world));else EventManager.INSTANCE.post(new WorldEvent.Disconnected());}
}
