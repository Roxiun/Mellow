package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

final class ReplayLocalPlayerPacketRecorder {

    interface FrameSink {
        void accept(ReplayPacketFrame frame) throws Exception;
    }

    private static final int EQUIPMENT_SLOT_COUNT = 5;

    private boolean playerListAdded;
    private boolean spawned;
    private int entityId = Integer.MIN_VALUE;
    private UUID playerUuid;
    private String playerName = "";
    private int serverPosX;
    private int serverPosY;
    private int serverPosZ;
    private byte yaw;
    private byte pitch;
    private byte headYaw;
    private boolean swingInProgress;
    private int swingProgressInt;
    private int hurtTime;
    private byte[] metadataPayload;
    private Byte entityFlags;
    private byte[] attributesPayload;
    private final ItemStack[] equipment = new ItemStack[EQUIPMENT_SLOT_COUNT];

    ReplayLocalPlayerPacketRecorder() {}

    ReplayLocalPlayerPacketRecorder(ReplayLocalPlayerPacketRecorder other) {
        copyFrom(other);
    }

    void copyFrom(ReplayLocalPlayerPacketRecorder other) {
        if (other == null) {
            reset();
            return;
        }
        this.playerListAdded = other.playerListAdded;
        this.spawned = other.spawned;
        this.entityId = other.entityId;
        this.playerUuid = other.playerUuid;
        this.playerName = other.playerName;
        this.serverPosX = other.serverPosX;
        this.serverPosY = other.serverPosY;
        this.serverPosZ = other.serverPosZ;
        this.yaw = other.yaw;
        this.pitch = other.pitch;
        this.headYaw = other.headYaw;
        this.swingInProgress = other.swingInProgress;
        this.swingProgressInt = other.swingProgressInt;
        this.hurtTime = other.hurtTime;
        this.metadataPayload = copyBytes(other.metadataPayload);
        this.entityFlags = other.entityFlags;
        this.attributesPayload = copyBytes(other.attributesPayload);
        for (int slot = 0; slot < EQUIPMENT_SLOT_COUNT; slot++) {
            this.equipment[slot] = copyItemStack(other.equipment[slot]);
        }
    }

    public ReplayLocalPlayerPacketRecorder copy() {
        return new ReplayLocalPlayerPacketRecorder(this);
    }

    public void observeOutboundPacket(
        Packet<?> packet,
        EntityPlayerSP player,
        NetworkPlayerInfo playerInfo,
        FrameSink sink
    ) throws Exception {
        if (!(packet instanceof C03PacketPlayer) || player == null) {
            return;
        }

        C03PacketPlayer movementPacket = (C03PacketPlayer) packet;
        GameProfile profile = resolveProfile(player, playerInfo);
        if (
            profile == null ||
            profile.getId() == null ||
            profile.getName() == null ||
            profile.getName().trim().isEmpty()
        ) {
            return;
        }

        ensureInitialized(player, playerInfo, profile, sink);

        int nextServerPosX = movementPacket.isMoving()
            ? toServerCoordinate(movementPacket.getPositionX())
            : serverPosX;
        int nextServerPosY = movementPacket.isMoving()
            ? toServerCoordinate(movementPacket.getPositionY())
            : serverPosY;
        int nextServerPosZ = movementPacket.isMoving()
            ? toServerCoordinate(movementPacket.getPositionZ())
            : serverPosZ;
        byte nextYaw = movementPacket.getRotating()
            ? toAngleByte(movementPacket.getYaw())
            : yaw;
        byte nextPitch = movementPacket.getRotating()
            ? toAngleByte(movementPacket.getPitch())
            : pitch;

        boolean moved =
            movementPacket.isMoving() &&
            (
                nextServerPosX != serverPosX ||
                nextServerPosY != serverPosY ||
                nextServerPosZ != serverPosZ
            );
        boolean looked =
            movementPacket.getRotating() &&
            (nextYaw != yaw || nextPitch != pitch);

        if (!moved && !looked) {
            return;
        }

        ReplayPacketFrame frame = moved
            ? ReplayPacketFactory.encode(
                new S18PacketEntityTeleport(
                    entityId,
                    nextServerPosX,
                    nextServerPosY,
                    nextServerPosZ,
                    nextYaw,
                    nextPitch,
                    movementPacket.isOnGround()
                )
            )
            : ReplayPacketFactory.encode(
                new S14PacketEntity.S16PacketEntityLook(
                    entityId,
                    nextYaw,
                    nextPitch,
                    movementPacket.isOnGround()
                )
            );

        sink.accept(frame);
        serverPosX = nextServerPosX;
        serverPosY = nextServerPosY;
        serverPosZ = nextServerPosZ;
        yaw = nextYaw;
        pitch = nextPitch;
    }

    public void reset() {
        playerListAdded = false;
        spawned = false;
        entityId = Integer.MIN_VALUE;
        playerUuid = null;
        playerName = "";
        serverPosX = 0;
        serverPosY = 0;
        serverPosZ = 0;
        yaw = 0;
        pitch = 0;
        headYaw = 0;
        swingInProgress = false;
        swingProgressInt = 0;
        hurtTime = 0;
        metadataPayload = null;
        entityFlags = null;
        attributesPayload = null;
        Arrays.fill(equipment, null);
    }

    public void capture(
        EntityPlayerSP player,
        NetworkPlayerInfo playerInfo,
        FrameSink sink
    ) throws Exception {
        if (player == null) {
            return;
        }

        GameProfile profile = resolveProfile(player, playerInfo);
        if (
            profile == null ||
            profile.getId() == null ||
            profile.getName() == null ||
            profile.getName().trim().isEmpty()
        ) {
            return;
        }

        ensureInitialized(player, playerInfo, profile, sink);

        if (spawned) {
            syncMovement(player, sink);
            syncMetadata(player, sink, false);
            syncEntityFlags(player, sink, false);
            syncAttributes(player, sink, false);
            syncEquipment(player, sink, false);
            syncHeadLook(player, sink);
        }

        syncAnimations(player, sink);
    }

    private boolean identityChanged(EntityPlayerSP player, GameProfile profile) {
        return
            entityId != Integer.MIN_VALUE &&
            (
                entityId != player.getEntityId() ||
                !profile.getId().equals(playerUuid) ||
                !profile.getName().equals(playerName)
            );
    }

    private void ensureInitialized(
        EntityPlayerSP player,
        NetworkPlayerInfo playerInfo,
        GameProfile profile,
        FrameSink sink
    ) throws Exception {
        if (identityChanged(player, profile)) {
            if (spawned) {
                sink.accept(
                    ReplayPacketFactory.encode(new S13PacketDestroyEntities(entityId))
                );
            }
            reset();
        }

        if (entityId == Integer.MIN_VALUE) {
            entityId = player.getEntityId();
            playerUuid = profile.getId();
            playerName = profile.getName();
        }

        if (!playerListAdded) {
            sink.accept(
                ReplayPacketFactory.playerListAdd(
                    copyProfile(profile),
                    playerInfo == null ? WorldSettings.GameType.SURVIVAL : playerInfo.getGameType(),
                    playerInfo == null ? 0 : playerInfo.getResponseTime(),
                    playerInfo == null ? null : playerInfo.getDisplayName()
                )
            );
            playerListAdded = true;
        }

        if (!spawned) {
            sink.accept(ReplayPacketFactory.encode(new S0CPacketSpawnPlayer(player)));
            serverPosX = toServerCoordinate(player.posX);
            serverPosY = toServerCoordinate(player.posY);
            serverPosZ = toServerCoordinate(player.posZ);
            yaw = toAngleByte(player.rotationYaw);
            pitch = toAngleByte(player.rotationPitch);
            headYaw = toAngleByte(player.rotationYawHead);
            syncMetadata(player, sink, true);
            syncEntityFlags(player, sink, true);
            syncAttributes(player, sink, true);
            syncEquipment(player, sink, true);
            sink.accept(
                ReplayPacketFactory.encode(new S19PacketEntityHeadLook(player, headYaw))
            );
            spawned = true;
        }
    }

    private void syncMovement(EntityPlayerSP player, FrameSink sink) throws Exception {
        int nextServerPosX = toServerCoordinate(player.posX);
        int nextServerPosY = toServerCoordinate(player.posY);
        int nextServerPosZ = toServerCoordinate(player.posZ);
        byte nextYaw = toAngleByte(player.rotationYaw);
        byte nextPitch = toAngleByte(player.rotationPitch);

        boolean moved =
            nextServerPosX != serverPosX ||
            nextServerPosY != serverPosY ||
            nextServerPosZ != serverPosZ;
        boolean looked = nextYaw != yaw || nextPitch != pitch;

        if (!moved && !looked) {
            return;
        }

        ReplayPacketFrame frame;
        if (moved) {
            frame = ReplayPacketFactory.encode(
                new S18PacketEntityTeleport(
                    entityId,
                    nextServerPosX,
                    nextServerPosY,
                    nextServerPosZ,
                    nextYaw,
                    nextPitch,
                    player.onGround
                )
            );
        } else {
            frame = ReplayPacketFactory.encode(
                new S14PacketEntity.S16PacketEntityLook(
                    entityId,
                    nextYaw,
                    nextPitch,
                    player.onGround
                )
            );
        }

        sink.accept(frame);
        serverPosX = nextServerPosX;
        serverPosY = nextServerPosY;
        serverPosZ = nextServerPosZ;
        yaw = nextYaw;
        pitch = nextPitch;
    }

    private void syncHeadLook(EntityPlayerSP player, FrameSink sink) throws Exception {
        byte nextHeadYaw = toAngleByte(player.rotationYawHead);
        if (nextHeadYaw == headYaw) {
            return;
        }
        sink.accept(
            ReplayPacketFactory.encode(new S19PacketEntityHeadLook(player, nextHeadYaw))
        );
        headYaw = nextHeadYaw;
    }

    private void syncMetadata(
        EntityPlayerSP player,
        FrameSink sink,
        boolean force
    ) throws Exception {
        ReplayPacketFrame frame = ReplayPacketFactory.encode(
            new S1CPacketEntityMetadata(entityId, player.getDataWatcher(), true)
        );
        if (force || !Arrays.equals(frame.getPayload(), metadataPayload)) {
            sink.accept(frame);
            metadataPayload = copyBytes(frame.getPayload());
        }
    }

    private void syncEntityFlags(
        EntityPlayerSP player,
        FrameSink sink,
        boolean force
    ) throws Exception {
        byte flags = withLocalPlayerPoseFlags(
            player.getDataWatcher().getWatchableObjectByte(0),
            player.isSneaking(),
            player.isSprinting()
        );
        if (!force && entityFlags != null && entityFlags.byteValue() == flags) {
            return;
        }

        DataWatcher watcher = new DataWatcher(player);
        watcher.addObject(0, Byte.valueOf(flags));
        sink.accept(
            ReplayPacketFactory.encode(
                new S1CPacketEntityMetadata(entityId, watcher, true)
            )
        );
        entityFlags = Byte.valueOf(flags);
    }

    static byte withLocalPlayerPoseFlags(
        byte originalFlags,
        boolean sneaking,
        boolean sprinting
    ) {
        int flags = originalFlags & 0xFF;
        flags = sneaking ? flags | (1 << 1) : flags & ~(1 << 1);
        flags = sprinting ? flags | (1 << 3) : flags & ~(1 << 3);
        return (byte) flags;
    }

    private void syncAttributes(
        EntityPlayerSP player,
        FrameSink sink,
        boolean force
    ) throws Exception {
        Collection<IAttributeInstance> attributes =
            player.getAttributeMap().getAllAttributes();
        ReplayPacketFrame frame = ReplayPacketFactory.encode(
            new S20PacketEntityProperties(entityId, attributes)
        );
        if (force || !Arrays.equals(frame.getPayload(), attributesPayload)) {
            sink.accept(frame);
            attributesPayload = copyBytes(frame.getPayload());
        }
    }

    private void syncEquipment(
        EntityPlayerSP player,
        FrameSink sink,
        boolean force
    ) throws Exception {
        for (int slot = 0; slot < EQUIPMENT_SLOT_COUNT; slot++) {
            ItemStack next = player.getEquipmentInSlot(slot);
            if (force || !ItemStack.areItemStacksEqual(next, equipment[slot])) {
                sink.accept(
                    ReplayPacketFactory.encode(
                        new S04PacketEntityEquipment(entityId, slot, next)
                    )
                );
                equipment[slot] = copyItemStack(next);
            }
        }
    }

    private void syncAnimations(EntityPlayerSP player, FrameSink sink) throws Exception {
        if (
            player.isSwingInProgress &&
            (!swingInProgress || player.swingProgressInt < swingProgressInt)
        ) {
            sink.accept(ReplayPacketFactory.encode(new S0BPacketAnimation(player, 0)));
        }
        if (
            player.hurtTime > 0 &&
            player.hurtTime == player.maxHurtTime &&
            player.hurtTime > hurtTime
        ) {
            sink.accept(ReplayPacketFactory.encode(new S0BPacketAnimation(player, 1)));
        }
        swingInProgress = player.isSwingInProgress;
        swingProgressInt = player.swingProgressInt;
        hurtTime = player.hurtTime;
    }

    private static GameProfile resolveProfile(
        EntityPlayerSP player,
        NetworkPlayerInfo playerInfo
    ) {
        if (playerInfo != null && playerInfo.getGameProfile() != null) {
            return copyProfile(playerInfo.getGameProfile());
        }
        if (player.getGameProfile() != null) {
            return copyProfile(player.getGameProfile());
        }
        return null;
    }

    private static GameProfile copyProfile(GameProfile profile) {
        GameProfile copy = new GameProfile(profile.getId(), profile.getName());
        copy.getProperties().putAll(profile.getProperties());
        return copy;
    }

    private static ItemStack copyItemStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private static byte[] copyBytes(byte[] bytes) {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }

    private static int toServerCoordinate(double value) {
        return MathHelper.floor_double(value * 32.0D);
    }

    private static byte toAngleByte(float angle) {
        return (byte) ((int) (angle * 256.0F / 360.0F));
    }
}
