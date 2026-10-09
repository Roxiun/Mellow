package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.multiplayer.WorldClient.class)
public abstract class WorldEntityMixin {
 @Inject(method="spawnEntityInWorld",at=@At("RETURN")) private void mellow$join(net.minecraft.entity.Entity entity,CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValue())EventManager.INSTANCE.post(new EntityJoinWorldEvent(entity,(net.minecraft.world.World)(Object)this));}
}
