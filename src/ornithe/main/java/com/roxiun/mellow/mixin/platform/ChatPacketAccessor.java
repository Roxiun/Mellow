package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.event.*;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.network.play.server.S02PacketChat.class)
public interface ChatPacketAccessor {
 @org.spongepowered.asm.mixin.gen.Accessor("chatComponent") void mellow$setMessage(net.minecraft.util.IChatComponent component);
}
