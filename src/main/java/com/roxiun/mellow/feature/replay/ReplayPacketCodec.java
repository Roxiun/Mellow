package com.roxiun.mellow.feature.replay;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public final class ReplayPacketCodec {

    private ReplayPacketCodec() {}

    private static final java.util.Properties PACKET_TYPES = new java.util.Properties();
    static {
        try (java.io.InputStream input = ReplayPacketCodec.class.getResourceAsStream("/mellow-packet-types.properties")) {
            if (input == null) throw new IllegalStateException("Missing replay packet mappings");
            PACKET_TYPES.load(input);
        } catch (java.io.IOException e) { throw new ExceptionInInitializerError(e); }
    }

    /** Format v2 stores canonical MCP names, never development/runtime mapping names. */
    public static String typeName(Class<?> packetClass) {
        String intermediary = net.fabricmc.loader.api.FabricLoader.getInstance().getMappingResolver()
            .unmapClassName("intermediary", packetClass.getName());
        for (String canonical : PACKET_TYPES.stringPropertyNames()) {
            if (intermediary.equals(PACKET_TYPES.getProperty(canonical))) return canonical;
        }
        throw new IllegalArgumentException("Unsupported replay packet: " + packetClass.getName());
    }

    private static String runtimeTypeName(String canonical) {
        String intermediary = PACKET_TYPES.getProperty(canonical);
        if (intermediary == null) throw new IllegalArgumentException("Unknown replay packet: " + canonical);
        return net.fabricmc.loader.api.FabricLoader.getInstance().getMappingResolver()
            .mapClassName("intermediary", intermediary);
    }

    public static ReplayPacketFrame encode(int timestampMs, Packet<?> packet)
        throws Exception {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writePacketData(buffer);
            byte[] payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            return new ReplayPacketFrame(timestampMs, typeName(packet.getClass()), payload);
        } finally {
            buffer.release();
        }
    }

    @SuppressWarnings("unchecked")
    public static Packet<?> decode(ReplayPacketFrame frame) throws Exception {
        Class<?> rawClass = Class.forName(runtimeTypeName(frame.getClassName()));
        if (!Packet.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("Not a replay packet: " + frame.getClassName());
        }
        Constructor<?> constructor = rawClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        if (!(instance instanceof Packet)) {
            throw new IllegalStateException(frame.getClassName() + " is not a packet");
        }
        Packet<?> packet = (Packet<?>) instance;
        PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(frame.getPayload()));
        try {
            packet.readPacketData(buffer);
            return packet;
        } finally {
            buffer.release();
        }
    }
}
