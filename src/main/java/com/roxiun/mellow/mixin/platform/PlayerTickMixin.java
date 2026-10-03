package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.entity.player.EntityPlayer.class)
public abstract class PlayerTickMixin {
 @Inject(method="onUpdate",at=@At("HEAD")) private void mellow$start(CallbackInfo ci){if(((net.minecraft.entity.player.EntityPlayer)(Object)this).worldObj.isRemote)EventManager.INSTANCE.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START,(net.minecraft.entity.player.EntityPlayer)(Object)this));}
 @Inject(method="onUpdate",at=@At("RETURN")) private void mellow$end(CallbackInfo ci){if(((net.minecraft.entity.player.EntityPlayer)(Object)this).worldObj.isRemote)EventManager.INSTANCE.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,(net.minecraft.entity.player.EntityPlayer)(Object)this));}
}
