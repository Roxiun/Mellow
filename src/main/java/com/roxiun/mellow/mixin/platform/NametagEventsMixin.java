package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.RenderLivingEvent;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(net.minecraft.client.renderer.entity.RendererLivingEntity.class)
public abstract class NametagEventsMixin {
 @WrapMethod(method="renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V")
 private void mellow$nametag(net.minecraft.entity.EntityLivingBase entity,double x,double y,double z,Operation<Void> original) {
  EventManager.INSTANCE.post(new RenderLivingEvent.Specials.Pre<>(entity));
  try { original.call(entity,x,y,z); }
  finally { EventManager.INSTANCE.post(new RenderLivingEvent.Specials.Post<>(entity)); }
 }
}
