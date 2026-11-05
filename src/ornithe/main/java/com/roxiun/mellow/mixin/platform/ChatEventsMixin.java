package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.client.network.NetHandlerPlayClient.class)
public abstract class ChatEventsMixin {
 @Inject(method="handleChat",at=@At(value="INVOKE",target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V",shift=At.Shift.AFTER),cancellable=true)
 private void mellow$chat(net.minecraft.network.play.server.S02PacketChat packet,CallbackInfo ci){
  ClientChatReceivedEvent event=new ClientChatReceivedEvent(packet.getType(),packet.getChatComponent());
  EventManager.INSTANCE.post(event);
  if(event.isCanceled())ci.cancel();else ((ChatPacketAccessor)packet).mellow$setMessage(event.message);
 }
}
