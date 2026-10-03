package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.entity.EntityPlayerSP.class)
public abstract class ClientCommandsMixin {
 @Inject(method="sendChatMessage",at=@At("HEAD"),cancellable=true)
 private void mellow$command(String message,CallbackInfo ci){if(com.roxiun.mellow.platform.ClientCommands.execute(message))ci.cancel();}
}
