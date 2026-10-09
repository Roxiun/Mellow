package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings;

public final class ReplayPacketFactory {

    private ReplayPacketFactory() {}

    public static ReplayPacketFrame encode(Packet<?> packet) throws Exception {
        return ReplayPacketCodec.encode(0, packet);
    }

    public static ReplayPacketFrame playerListAdd(
        GameProfile profile,
        WorldSettings.GameType gameMode,
        int ping,
        IChatComponent displayName
    ) throws IOException {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeEnumValue(S38PacketPlayerListItem.Action.ADD_PLAYER);
            buffer.writeVarIntToBuffer(1);
            buffer.writeUuid(profile.getId());
            buffer.writeString(profile.getName());
            buffer.writeVarIntToBuffer(profile.getProperties().size());
            for (Property property : profile.getProperties().values()) {
                buffer.writeString(property.getName());
                buffer.writeString(property.getValue());
                if (property.hasSignature()) {
                    buffer.writeBoolean(true);
                    buffer.writeString(property.getSignature());
                } else {
                    buffer.writeBoolean(false);
                }
            }
            buffer.writeVarIntToBuffer(
                (gameMode == null ? WorldSettings.GameType.SURVIVAL : gameMode).getID()
            );
            buffer.writeVarIntToBuffer(Math.max(0, ping));
            if (displayName == null) {
                buffer.writeBoolean(false);
            } else {
                buffer.writeBoolean(true);
                buffer.writeChatComponent(displayName);
            }
            byte[] payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            return new ReplayPacketFrame(
                0,
                //? if ornithe {
                ReplayPacketCodec.typeName(S38PacketPlayerListItem.class),
                //?} else {
                /*S38PacketPlayerListItem.class.getName(),
                *///?}
                payload
            );
        } finally {
            buffer.release();
        }
    }
}
