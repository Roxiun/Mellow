package com.roxiun.mellow.mixin.replay;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.replay.ReplayManager;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class NetworkManagerReplayCaptureMixin {

    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void mellow$captureInboundPacket(
        ChannelHandlerContext context,
        Packet<?> packet,
        CallbackInfo ci
    ) {
        if (Mellow.isEnabled()) {
            ReplayManager.getInstance().onInboundPacket(packet);
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"))
    private void mellow$captureOutboundPacket(
        Packet<?> packet,
        CallbackInfo ci
    ) {
        if (Mellow.isEnabled()) {
            ReplayManager.getInstance().onOutboundPacket(packet);
        }
    }
}
