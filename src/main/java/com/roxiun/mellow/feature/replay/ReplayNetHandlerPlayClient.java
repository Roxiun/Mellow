package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.mixin.replay.NetHandlerPlayClientAccessor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

public class ReplayNetHandlerPlayClient extends NetHandlerPlayClient {

    private final Minecraft mc;
    private final ReplayPlaybackSession session;
    private final Deque<GameProfile> fallbackProfiles = new ArrayDeque<>();
    private final Set<UUID> queuedFallbackProfileIds = new HashSet<>();
    private final Set<UUID> claimedFallbackProfileIds = new HashSet<>();
    private final Map<Integer, GameProfile> knownPlayerProfilesByEntityId = new HashMap<>();
    private int fallbackCounter = 1;

    public ReplayNetHandlerPlayClient(
        Minecraft mc,
        GuiScreen parentScreen,
        NetworkManager networkManager,
        ReplayPlaybackSession session,
        String replayId
    ) {
        super(
            mc,
            parentScreen,
            networkManager,
            new GameProfile(
                UUID.nameUUIDFromBytes(
                    ("mellow-replay-viewer-" + safeReplayId(replayId))
                        .getBytes(StandardCharsets.UTF_8)
                ),
                "ReplayViewer"
            )
        );
        this.mc = mc;
        this.session = session;
    }

    @Override
    public void handleJoinGame(S01PacketJoinGame packetIn) {
        if (!session.shouldProcessWorldBootstrapPacket()) {
            return;
        }
        WorldType worldType = packetIn.getWorldType() == null
            ? WorldType.DEFAULT
            : packetIn.getWorldType();
        EnumDifficulty difficulty = packetIn.getDifficulty() == null
            ? EnumDifficulty.NORMAL
            : packetIn.getDifficulty();
        PlayerControllerMP controller = new PlayerControllerMP(mc, this);
        mc.playerController = controller;
        WorldClient world = new WorldClient(
            this,
            new WorldSettings(
                0L,
                WorldSettings.GameType.CREATIVE,
                false,
                packetIn.isHardcoreMode(),
                worldType
            ),
            packetIn.getDimension(),
            difficulty,
            resolveProfiler(mc)
        );
        ((NetHandlerPlayClientAccessor) this).mellow$setClientWorldController(world);
        ((NetHandlerPlayClientAccessor) this).mellow$setDoneLoadingTerrain(true);
        mc.gameSettings.difficulty = difficulty;
        mc.loadWorld(world, "Loading replay");
        if (mc.thePlayer != null) {
            mc.thePlayer.dimension = packetIn.getDimension();
            mc.thePlayer.setEntityId(ReplayPlaybackSession.REPLAY_VIEWER_ENTITY_ID);
            controller.setGameType(WorldSettings.GameType.CREATIVE);
            controller.flipPlayer(mc.thePlayer);
            session.prepareViewer(mc.thePlayer);
        }
        mc.displayGuiScreen(null);
        session.onJoinGame(packetIn);
    }

    @Override
    public void handleRespawn(S07PacketRespawn packetIn) {
        if (
            !session.shouldProcessWorldBootstrapPacket() &&
            !session.hasWorldBootstrapped()
        ) {
            return;
        }
        WorldType worldType = packetIn.getWorldType() == null
            ? WorldType.DEFAULT
            : packetIn.getWorldType();
        EnumDifficulty difficulty = packetIn.getDifficulty() == null
            ? EnumDifficulty.NORMAL
            : packetIn.getDifficulty();
        WorldClient world = new WorldClient(
            this,
            new WorldSettings(
                0L,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                worldType
            ),
            packetIn.getDimensionID(),
            difficulty,
            resolveProfiler(mc)
        );
        ((NetHandlerPlayClientAccessor) this).mellow$setClientWorldController(world);
        mc.gameSettings.difficulty = difficulty;
        mc.loadWorld(world, "Loading replay");
        if (mc.thePlayer != null) {
            mc.thePlayer.dimension = packetIn.getDimensionID();
            mc.thePlayer.setEntityId(ReplayPlaybackSession.REPLAY_VIEWER_ENTITY_ID);
            if (mc.playerController != null) {
                mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
                mc.playerController.flipPlayer(mc.thePlayer);
            }
            session.prepareViewer(mc.thePlayer);
        }
        mc.displayGuiScreen(null);
        session.onRespawn(packetIn);
    }

    @Override
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn) {
        session.onViewerPosition(packetIn);
    }

    @Override
    public void handleSpawnPlayer(S0CPacketSpawnPlayer packetIn) {
        claimFallbackProfile(packetIn.getPlayer());
        super.handleSpawnPlayer(packetIn);
        NetworkPlayerInfo info = getPlayerInfo(packetIn.getPlayer());
        if (info != null && info.getGameProfile() != null) {
            knownPlayerProfilesByEntityId.put(
                packetIn.getEntityID(),
                copyProfile(info.getGameProfile())
            );
        }
    }

    @Override
    public void handleDestroyEntities(S13PacketDestroyEntities packetIn) {
        super.handleDestroyEntities(packetIn);
    }

    @Override
    public void handleEntityMovement(S14PacketEntity packetIn) {
        Entity entity = packetIn.getEntity(mc.theWorld);
        if (entity == null) {
            return;
        }
        super.handleEntityMovement(packetIn);
    }

    @Override
    public void handleEntityTeleport(S18PacketEntityTeleport packetIn) {
        ensureReplayPlayer(
            packetIn.getEntityId(),
            packetIn.getX() / 32.0D,
            packetIn.getY() / 32.0D,
            packetIn.getZ() / 32.0D,
            (packetIn.getYaw() * 360.0F) / 256.0F,
            (packetIn.getPitch() * 360.0F) / 256.0F
        );
        super.handleEntityTeleport(packetIn);
    }

    @Override
    public void handleEntityHeadLook(S19PacketEntityHeadLook packetIn) {
        if (packetIn.getEntity(mc.theWorld) != null) {
            super.handleEntityHeadLook(packetIn);
        }
    }

    @Override
    public void handlePlayerListItem(S38PacketPlayerListItem packetIn) {
        if (packetIn.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
            for (S38PacketPlayerListItem.AddPlayerData entry : packetIn.getEntries()) {
                if (entry == null || entry.getProfile() == null) {
                    continue;
                }
                UUID uuid = entry.getProfile().getId();
                String name = entry.getProfile().getName();
                if (uuid == null || name == null || name.trim().isEmpty()) {
                    continue;
                }
                if (
                    mc.thePlayer != null &&
                    (uuid.equals(mc.thePlayer.getUniqueID()) ||
                    name.equalsIgnoreCase(mc.thePlayer.getName()))
                ) {
                    continue;
                }
                if (
                    claimedFallbackProfileIds.contains(uuid) ||
                    queuedFallbackProfileIds.contains(uuid)
                ) {
                    continue;
                }
                fallbackProfiles.addLast(copyProfile(entry.getProfile()));
                queuedFallbackProfileIds.add(uuid);
            }
        }
        super.handlePlayerListItem(packetIn);
    }

    @Override
    public void handleChat(S02PacketChat packetIn) {}

    @Override
    public void handleUpdateHealth(S06PacketUpdateHealth packetIn) {}

    @Override
    public void handleHeldItemChange(S09PacketHeldItemChange packetIn) {}

    @Override
    public void handleSetExperience(S1FPacketSetExperience packetIn) {}

    @Override
    public void handlePlayerAbilities(S39PacketPlayerAbilities packetIn) {}

    @Override
    public void handleOpenWindow(S2DPacketOpenWindow packetIn) {}

    @Override
    public void handleCloseWindow(S2EPacketCloseWindow packetIn) {}

    @Override
    public void handleSetSlot(S2FPacketSetSlot packetIn) {}

    @Override
    public void handleWindowItems(S30PacketWindowItems packetIn) {}

    @Override
    public void handleConfirmTransaction(S32PacketConfirmTransaction packetIn) {}

    @Override
    public void handleStatistics(S37PacketStatistics packetIn) {}

    @Override
    public void handleCamera(S43PacketCamera packetIn) {}

    @Override
    public void handleTitle(S45PacketTitle packetIn) {}

    @Override
    public void handleChangeGameState(S2BPacketChangeGameState packetIn) {
        int state = packetIn.getGameState();
        if (state == 1 || state == 2 || state == 7 || state == 8) {
            super.handleChangeGameState(packetIn);
        }
    }

    private static Profiler resolveProfiler(Minecraft minecraft) {
        return minecraft == null ? new Profiler() : minecraft.mcProfiler;
    }

    private static String safeReplayId(String replayId) {
        return replayId == null || replayId.trim().isEmpty() ? "unknown" : replayId;
    }

    private void ensureReplayPlayer(
        int entityId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        if (mc.theWorld == null) {
            return;
        }
        if (
            entityId == ReplayPlaybackSession.REPLAY_VIEWER_ENTITY_ID ||
            entityId <= 0 ||
            (mc.thePlayer != null && entityId == mc.thePlayer.getEntityId())
        ) {
            return;
        }
        Entity existing = mc.theWorld.getEntityByID(entityId);
        if (existing != null) {
            return;
        }

        GameProfile profile = knownPlayerProfilesByEntityId.get(entityId);
        if (profile == null) {
            if (!session.allowLegacyFallbackPlayers()) {
                return;
            }
            profile = nextFallbackProfile(entityId);
            if (profile == null) {
                return;
            }
            knownPlayerProfilesByEntityId.put(entityId, copyProfile(profile));
        }

        EntityOtherPlayerMP player = new EntityOtherPlayerMP(mc.theWorld, copyProfile(profile));
        player.setEntityId(entityId);
        player.setPositionAndRotation(x, y, z, yaw, pitch);
        mc.theWorld.addEntityToWorld(entityId, player);
    }

    private GameProfile nextFallbackProfile(int entityId) {
        while (!fallbackProfiles.isEmpty()) {
            GameProfile next = fallbackProfiles.removeFirst();
            UUID uuid = next.getId();
            queuedFallbackProfileIds.remove(uuid);
            if (
                next.getName() == null ||
                next.getName().trim().isEmpty() ||
                (uuid != null && claimedFallbackProfileIds.contains(uuid))
            ) {
                continue;
            }
            if (uuid != null) {
                claimedFallbackProfileIds.add(uuid);
            }
            return next;
        }
        return null;
    }

    private void claimFallbackProfile(UUID uuid) {
        if (uuid == null) {
            return;
        }
        queuedFallbackProfileIds.remove(uuid);
        claimedFallbackProfileIds.add(uuid);
    }

    private GameProfile copyProfile(GameProfile profile) {
        GameProfile copy = new GameProfile(profile.getId(), profile.getName());
        copy.getProperties().putAll(profile.getProperties());
        return copy;
    }
}
