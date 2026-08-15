package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.util.ChatUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

public class ReplayPlaybackSession {

    public static final int REPLAY_VIEWER_ENTITY_ID = -310_001;
    private static final int REPLAY_LOCAL_PLAYER_ENTITY_ID = -310_002;
    private static final double[] SPEEDS = new double[] { 0.25D, 0.5D, 1.0D, 2.0D, 4.0D };
    private static final int DYE_COLOR_GRAY = 8;
    private static final int DYE_COLOR_PINK = 9;
    private static final String UNASSIGNED_TEAM_SORT_KEY = "\uFFFF";
    private static final String MC_COLOR_CODES = "0123456789abcdef";
    private static final String SLOW_HEAD_TEXTURE_URL =
        "http://textures.minecraft.net/texture/c3e4b533e4ba2dff7c0fa90f67e8bef36428b6cb06c45262631b0b25db85b";
    private static final String BACK_HEAD_TEXTURE_URL =
        "http://textures.minecraft.net/texture/c4e490e1658bfde4d4ef1ea7cd646c5353377905a1369b86ee966746ae25ca7";
    private static final String FORWARD_HEAD_TEXTURE_URL =
        "http://textures.minecraft.net/texture/98f293f294980d732f523321c34a4cdcc3e6f9e36c9320e150f1cce31aa5";
    private static final String FAST_HEAD_TEXTURE_URL =
        "http://textures.minecraft.net/texture/60b55f74681c68283a1c1ce51f1c83b52e2971c91ee34efcb598df3990a7e7";

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayLoadedData replay;
    private final Runnable stopCallback;

    private ReplayNetworkManager networkManager;
    private ReplayNetHandlerPlayClient netHandler;
    private EntityOtherPlayerMP localReplayPlayer;
    private ReplayScoreboardFrame currentScoreboard;
    private int packetIndex;
    private int chatIndex;
    private int scoreboardIndex;
    private int localSnapshotIndex;
    private int currentTimeMs;
    private boolean paused;
    private boolean viewerPositionInitialized;
    private boolean reanchorViewerAfterWorldChange;
    private int speedIndex = 2;
    private String lastTeleportedPlayerName = "";
    private boolean stopRequested;
    private boolean bootstrapPacketOpen;
    private boolean worldBootstrapped;
    private boolean suppressTransientEffects;
    private final boolean allowLegacyFallbackPlayers;
    private final boolean usesLegacyLocalSnapshots;

    enum SeekMode {
        NONE,
        FORWARD,
        REBUILD
    }

    enum ControlAction {
        NONE,
        TELEPORT,
        SLOW_DOWN,
        BACKWARD,
        TOGGLE_PAUSE,
        FORWARD,
        SPEED_UP,
        STOP
    }

    static final class TeleportTarget {

        private final String name;
        private final String displayName;
        private final String teamSortKey;
        private final String teamLabel;

        TeleportTarget(
            String name,
            String displayName,
            String teamSortKey,
            String teamLabel
        ) {
            this.name = name;
            this.displayName = displayName;
            this.teamSortKey = teamSortKey;
            this.teamLabel = teamLabel;
        }

        String getName() {
            return name;
        }

        String getDisplayName() {
            return displayName;
        }

        String getTeamSortKey() {
            return teamSortKey;
        }

        String getTeamLabel() {
            return teamLabel;
        }
    }

    private static final class ViewerTransform {

        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        private ViewerTransform(
            double x,
            double y,
            double z,
            float yaw,
            float pitch
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public ReplayPlaybackSession(ReplayLoadedData replay, Runnable stopCallback) {
        this.replay = replay;
        this.stopCallback = stopCallback;
        this.allowLegacyFallbackPlayers = !hasRecordedPlayerSpawns(replay);
        this.usesLegacyLocalSnapshots =
            replay.getMetadata().getRecordedPlayerEntityId() == null &&
            !replay.getLocalSnapshots().isEmpty();
    }

    public void open() {
        paused = false;
        restartFrom(getPlaybackStartMs(), false);
        ChatUtils.sendMessage(
            "§dOpened replay §f" + replay.getMetadata().getReplayId() +
            "§7. Left-click the compass to cycle players or right-click it for the player list."
        );
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        if (stopRequested) {
            stop();
            return;
        }

        if (mc.thePlayer != null) {
            prepareViewer(mc.thePlayer);
            populateHotbar();
        }

        if (!paused) {
            currentTimeMs = Math.min(
                replay.getMetadata().getDurationMs(),
                currentTimeMs + (int) Math.round(50.0D * getSpeed())
            );
        }

        applyPacketsUpTo(currentTimeMs);
        emitChatsUpTo(currentTimeMs);
        advanceScoreboardTo(currentTimeMs);
        updateLocalReplayPlayer(currentTimeMs);
        initializeViewerPosition();
        updateReplayProgressBar();

        if (!paused && isAtEnd(currentTimeMs, replay.getMetadata().getDurationMs())) {
            paused = true;
            ChatUtils.sendMessage("§7Replay reached the end.");
        }
    }

    public boolean isActive() {
        return netHandler != null;
    }

    public void stop() {
        lastTeleportedPlayerName = "";
        stopRequested = false;
        localReplayPlayer = null;
        currentScoreboard = null;
        viewerPositionInitialized = false;
        reanchorViewerAfterWorldChange = false;
        packetIndex = 0;
        chatIndex = 0;
        scoreboardIndex = 0;
        localSnapshotIndex = 0;
        currentTimeMs = 0;
        worldBootstrapped = false;
        netHandler = null;
        networkManager = null;
        unloadReplayWorld();
        if (stopCallback != null) {
            stopCallback.run();
        }
    }

    public void stopFromClientDetach() {
        lastTeleportedPlayerName = "";
        stopRequested = false;
        localReplayPlayer = null;
        currentScoreboard = null;
        viewerPositionInitialized = false;
        reanchorViewerAfterWorldChange = false;
        packetIndex = 0;
        chatIndex = 0;
        scoreboardIndex = 0;
        localSnapshotIndex = 0;
        currentTimeMs = 0;
        worldBootstrapped = false;
        netHandler = null;
        networkManager = null;
        if (stopCallback != null) {
            stopCallback.run();
        }
    }

    public boolean hasLostPlaybackWorld() {
        return isActive() && worldBootstrapped && mc.theWorld == null;
    }

    public void togglePause() {
        if (paused && isAtEnd(currentTimeMs, replay.getMetadata().getDurationMs())) {
            restartFrom(getPlaybackStartMs(), false);
            paused = false;
            ChatUtils.sendMessage("§7Replay restarted.");
            return;
        }

        paused = !paused;
        ChatUtils.sendMessage(paused ? "§7Replay paused." : "§7Replay resumed.");
    }

    public void changeSpeed(int delta) {
        int next = speedIndex + delta;
        if (next < 0 || next >= SPEEDS.length) {
            return;
        }
        speedIndex = next;
        ChatUtils.sendMessage("§7Replay speed set to §f" + speedLabel() + "§7.");
    }

    public void skipBySeconds(int seconds) {
        seekTo(currentTimeMs + (seconds * 1000));
    }

    public void seekTo(int targetMs) {
        int clamped = Math.max(
            getPlaybackStartMs(),
            Math.min(replay.getMetadata().getDurationMs(), targetMs)
        );
        SeekMode seekMode = resolveSeekMode(currentTimeMs, clamped);
        if (seekMode == SeekMode.NONE) {
            ChatUtils.sendMessage("§7Jumped to §f" + formatTime(getVisibleCurrentTimeMs()) + "§7.");
            return;
        }

        boolean wasPaused = paused;
        ViewerTransform preservedViewer = captureViewerTransform();
        String preservedTeleportTarget = lastTeleportedPlayerName;
        int preservedHotbarSlot = captureSelectedHotbarSlot();
        if (seekMode == SeekMode.REBUILD) {
            restartFrom(clamped, true, preservedViewer);
        } else {
            applyPacketsUpTo(clamped);
            advanceChatsTo(clamped, false);
            advanceScoreboardTo(clamped);
            updateLocalReplayPlayer(clamped);
            if (!restoreViewerTransform(preservedViewer)) {
                initializeViewerPosition();
            }
            currentTimeMs = clamped;
        }

        lastTeleportedPlayerName = preservedTeleportTarget;
        restoreSelectedHotbarSlot(preservedHotbarSlot);
        updateReplayProgressBar();
        paused = wasPaused;
        ChatUtils.sendMessage("§7Jumped to §f" + formatTime(getVisibleCurrentTimeMs()) + "§7.");
    }

    public boolean teleportToPlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        Entity entity = findPlayerEntity(playerName.trim());
        if (entity == null) {
            return false;
        }

        teleportToEntity(entity);
        lastTeleportedPlayerName = entity.getName();
        ChatUtils.sendMessage("§7Teleported to §f" + entity.getName() + "§7.");
        return true;
    }

    public List<TeleportTarget> getTeleportTargets() {
        if (mc.theWorld == null) {
            return Collections.emptyList();
        }

        Map<String, TeleportTarget> targetsByName = new LinkedHashMap<>();
        for (Object playerObj : mc.theWorld.playerEntities) {
            if (!(playerObj instanceof EntityPlayer)) {
                continue;
            }
            EntityPlayer player = (EntityPlayer) playerObj;
            if (player == mc.thePlayer) {
                continue;
            }
            addTeleportTarget(targetsByName, player);
        }

        if (localReplayPlayer != null) {
            addTeleportTarget(targetsByName, localReplayPlayer);
        }

        return sortTeleportTargets(new ArrayList<>(targetsByName.values()));
    }

    public ReplayPlaybackState getPlaybackState() {
        if (!isActive()) {
            return ReplayPlaybackState.inactive();
        }
        return new ReplayPlaybackState(
            true,
            paused,
            replay.getMetadata().getReplayId(),
            replay.getMetadata().getMap(),
            replay.getMetadata().getMode(),
            getVisibleCurrentTimeMs(),
            replay.getMetadata().getVisibleDurationMs(),
            getSpeed()
        );
    }

    public boolean handleHeldControlClick(int mouseButton) {
        if (!isActive() || mc.thePlayer == null) {
            return false;
        }

        return triggerControlAction(
            resolveControlAction(mc.thePlayer.inventory.currentItem),
            mouseButton
        );
    }

    static ControlAction resolveControlAction(int slot) {
        switch (slot) {
            case 0:
                return ControlAction.TELEPORT;
            case 2:
                return ControlAction.SLOW_DOWN;
            case 3:
                return ControlAction.BACKWARD;
            case 4:
                return ControlAction.TOGGLE_PAUSE;
            case 5:
                return ControlAction.FORWARD;
            case 6:
                return ControlAction.SPEED_UP;
            case 8:
                return ControlAction.STOP;
            default:
                return ControlAction.NONE;
        }
    }

    public List<String> buildHudLines() {
        if (!isActive()) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        lines.add("§5§lReplay");
        lines.add("§7Map: §f" + safe(replay.getMetadata().getMap()));
        lines.add("§7Mode: §f" + safe(replay.getMetadata().getMode()));
        lines.add(
            "§7Time: §f" + formatTime(getVisibleCurrentTimeMs()) + "§7 / §f" +
            formatTime(replay.getMetadata().getVisibleDurationMs())
        );
        lines.add("§7Speed: §f" + speedLabel());
        lines.add("§7State: §f" + (paused ? "Paused" : "Playing"));
        if (currentScoreboard != null) {
            String title = currentScoreboard.getTitle();
            if (!title.isEmpty()) {
                lines.add(" ");
                lines.add(title);
            }
            List<String> boardLines = currentScoreboard.getLines();
            int limit = Math.min(boardLines.size(), 8);
            for (int i = 0; i < limit; i++) {
                lines.add(boardLines.get(i));
            }
        }
        return lines;
    }

    public void prepareViewer(EntityPlayer viewer) {
        viewer.capabilities.allowFlying = true;
        viewer.capabilities.isFlying = true;
        viewer.capabilities.disableDamage = true;
        viewer.noClip = true;
        viewer.fallDistance = 0.0F;
    }

    public void onJoinGame(S01PacketJoinGame packetIn) {
        localReplayPlayer = null;
        viewerPositionInitialized = false;
        reanchorViewerAfterWorldChange = false;
        worldBootstrapped = true;
    }

    public void onRespawn(S07PacketRespawn packetIn) {
        localReplayPlayer = null;
        viewerPositionInitialized = false;
        reanchorViewerAfterWorldChange = true;
    }

    public void onViewerPosition(S08PacketPlayerPosLook packetIn) {
        if (mc.thePlayer == null || viewerPositionInitialized) {
            return;
        }
        mc.thePlayer.setPositionAndRotation(
            packetIn.getX(),
            packetIn.getY() + (reanchorViewerAfterWorldChange ? 2.0D : 0.0D),
            packetIn.getZ(),
            packetIn.getYaw(),
            packetIn.getPitch()
        );
        if (!reanchorViewerAfterWorldChange) {
            viewerPositionInitialized = true;
        }
    }

    private void restartFrom(int targetMs, boolean announceRebuild) {
        restartFrom(targetMs, announceRebuild, null);
    }

    private void restartFrom(
        int targetMs,
        boolean announceRebuild,
        ViewerTransform preservedViewer
    ) {
        unloadReplayWorld();
        networkManager = new ReplayNetworkManager();
        netHandler = new ReplayNetHandlerPlayClient(
            mc,
            null,
            networkManager,
            this,
            replay.getMetadata().getReplayId()
        );
        packetIndex = 0;
        chatIndex = 0;
        scoreboardIndex = 0;
        localSnapshotIndex = 0;
        currentTimeMs = 0;
        localReplayPlayer = null;
        currentScoreboard = null;
        viewerPositionInitialized = false;
        reanchorViewerAfterWorldChange = false;
        lastTeleportedPlayerName = "";
        stopRequested = false;
        bootstrapPacketOpen = false;
        worldBootstrapped = false;
        bootstrapReplayWorld();
        suppressTransientEffects = targetMs > 0;
        try {
            applyPacketsUpTo(targetMs);
        } finally {
            suppressTransientEffects = false;
        }
        advanceChatsBefore(targetMs);
        advanceScoreboardTo(targetMs);
        updateLocalReplayPlayer(targetMs);
        if (!restoreViewerTransform(preservedViewer)) {
            initializeViewerPosition();
        }
        currentTimeMs = targetMs;
        if (announceRebuild && targetMs > 0) {
            ChatUtils.sendMessage(
                "§7Rebuilt replay state at §f" +
                formatTime(Math.max(0, targetMs - getPlaybackStartMs())) +
                "§7."
            );
        }
    }

    private void updateReplayProgressBar() {
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.experience = computeReplayProgress(
            getVisibleCurrentTimeMs(),
            replay.getMetadata().getVisibleDurationMs()
        );
        mc.thePlayer.experienceLevel = 0;
        mc.thePlayer.experienceTotal = 0;
    }

    private void applyPacketsUpTo(int targetMs) {
        while (packetIndex < replay.getPackets().size()) {
            ReplayPacketFrame frame = replay.getPackets().get(packetIndex);
            if (frame.getTimestampMs() > targetMs) {
                break;
            }
            packetIndex++;
            try {
                Packet<?> packet = ReplayPacketCodec.decode(frame);
                processPacket(packet);
            } catch (Exception ignored) {}
        }
    }

    private void emitChatsUpTo(int targetMs) {
        advanceChatsTo(targetMs, true);
    }

    private void advanceChatsTo(int targetMs, boolean emitMessages) {
        while (chatIndex < replay.getChats().size()) {
            ReplayChatEvent event = replay.getChats().get(chatIndex);
            if (event.getTimestampMs() > targetMs) {
                break;
            }
            chatIndex++;
            if (!emitMessages) {
                continue;
            }
            IChatComponent component = IChatComponent.Serializer.jsonToComponent(
                event.getComponentJson()
            );
            if (component == null) {
                component = new ChatComponentText("");
            }
            if (mc.ingameGUI != null) {
                mc.ingameGUI.getChatGUI().printChatMessage(component);
            }
        }
    }

    private void advanceChatsBefore(int targetMs) {
        while (chatIndex < replay.getChats().size()) {
            ReplayChatEvent event = replay.getChats().get(chatIndex);
            if (event.getTimestampMs() >= targetMs) {
                break;
            }
            chatIndex++;
        }
    }

    private void advanceScoreboardTo(int targetMs) {
        while (scoreboardIndex < replay.getScoreboards().size()) {
            ReplayScoreboardFrame frame = replay.getScoreboards().get(scoreboardIndex);
            if (frame.getTimestampMs() > targetMs) {
                break;
            }
            scoreboardIndex++;
            currentScoreboard = frame;
        }
    }

    private void updateLocalReplayPlayer(int targetMs) {
        if (!usesLegacyLocalSnapshots) {
            localReplayPlayer = null;
            return;
        }
        ReplayLocalPlayerSnapshot snapshot = null;
        while (localSnapshotIndex < replay.getLocalSnapshots().size()) {
            ReplayLocalPlayerSnapshot next = replay.getLocalSnapshots().get(localSnapshotIndex);
            if (next.getTimestampMs() > targetMs) {
                break;
            }
            snapshot = next;
            localSnapshotIndex++;
        }
        if (snapshot == null) {
            if (localSnapshotIndex > 0 && localSnapshotIndex <= replay.getLocalSnapshots().size()) {
                snapshot = replay.getLocalSnapshots().get(localSnapshotIndex - 1);
            } else {
                return;
            }
        }
        if (mc.theWorld == null) {
            return;
        }
        if (localReplayPlayer == null) {
            localReplayPlayer = new EntityOtherPlayerMP(
                mc.theWorld,
                new GameProfile(
                    resolveRecordedPlayerUuid(),
                    resolveRecordedPlayerName()
                )
            );
            localReplayPlayer.setEntityId(REPLAY_LOCAL_PLAYER_ENTITY_ID);
            localReplayPlayer.setPositionAndRotation(
                snapshot.getX(),
                snapshot.getY(),
                snapshot.getZ(),
                snapshot.getYaw(),
                snapshot.getPitch()
            );
            mc.theWorld.addEntityToWorld(REPLAY_LOCAL_PLAYER_ENTITY_ID, localReplayPlayer);
        } else {
            localReplayPlayer.setPositionAndRotation2(
                snapshot.getX(),
                snapshot.getY(),
                snapshot.getZ(),
                snapshot.getYaw(),
                snapshot.getPitch(),
                3,
                true
            );
        }
        localReplayPlayer.setSneaking(snapshot.isSneaking());
        localReplayPlayer.setSprinting(snapshot.isSprinting());
        localReplayPlayer.rotationYawHead = snapshot.getYaw();
        localReplayPlayer.renderYawOffset = snapshot.getYaw();
        if (!viewerPositionInitialized && mc.thePlayer != null) {
            mc.thePlayer.setPositionAndRotation(
                snapshot.getX(),
                snapshot.getY() + 2.0D,
                snapshot.getZ(),
                snapshot.getYaw(),
                snapshot.getPitch()
            );
            viewerPositionInitialized = true;
            reanchorViewerAfterWorldChange = false;
        }
    }

    private void initializeViewerPosition() {
        if (viewerPositionInitialized || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        Entity anchor = null;
        Integer recordedPlayerEntityId = replay.getMetadata().getRecordedPlayerEntityId();
        if (recordedPlayerEntityId != null) {
            anchor = mc.theWorld.getEntityByID(recordedPlayerEntityId.intValue());
        }
        if (anchor == null) {
            anchor = localReplayPlayer;
        }
        if (anchor == null) {
            return;
        }
        mc.thePlayer.setPositionAndRotation(
            anchor.posX,
            anchor.posY + 2.0D,
            anchor.posZ,
            anchor.rotationYaw,
            anchor.rotationPitch
        );
        viewerPositionInitialized = true;
        reanchorViewerAfterWorldChange = false;
    }

    private ViewerTransform captureViewerTransform() {
        if (
            mc.thePlayer == null ||
            mc.theWorld == null ||
            !worldBootstrapped
        ) {
            return null;
        }
        return new ViewerTransform(
            mc.thePlayer.posX,
            mc.thePlayer.posY,
            mc.thePlayer.posZ,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.rotationPitch
        );
    }

    private boolean restoreViewerTransform(ViewerTransform preservedViewer) {
        if (
            preservedViewer == null ||
            mc.thePlayer == null ||
            mc.theWorld == null ||
            reanchorViewerAfterWorldChange
        ) {
            return false;
        }
        mc.thePlayer.setPositionAndRotation(
            preservedViewer.x,
            preservedViewer.y,
            preservedViewer.z,
            preservedViewer.yaw,
            preservedViewer.pitch
        );
        viewerPositionInitialized = true;
        return true;
    }

    private int captureSelectedHotbarSlot() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return 0;
        }
        return normalizeHotbarSlot(mc.thePlayer.inventory.currentItem);
    }

    private void restoreSelectedHotbarSlot(int preservedHotbarSlot) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return;
        }
        mc.thePlayer.inventory.currentItem = normalizeHotbarSlot(preservedHotbarSlot);
    }

    private Entity findPlayerEntity(String name) {
        if (mc.theWorld == null || name == null) {
            return null;
        }
        for (Object playerObj : mc.theWorld.playerEntities) {
            if (!(playerObj instanceof EntityPlayer)) {
                continue;
            }
            EntityPlayer player = (EntityPlayer) playerObj;
            if (player == mc.thePlayer) {
                continue;
            }
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        if (
            localReplayPlayer != null &&
            localReplayPlayer.getName() != null &&
            localReplayPlayer.getName().equalsIgnoreCase(name)
        ) {
            return localReplayPlayer;
        }
        return null;
    }

    private void populateHotbar() {
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.inventory.mainInventory[0] = namedItem(Items.compass, "§dTeleport Players");
        mc.thePlayer.inventory.mainInventory[1] = null;
        mc.thePlayer.inventory.mainInventory[2] = texturedHead(
            SLOW_HEAD_TEXTURE_URL,
            "§dSlow Down"
        );
        mc.thePlayer.inventory.mainInventory[3] = texturedHead(
            BACK_HEAD_TEXTURE_URL,
            "§dBack 5s"
        );
        mc.thePlayer.inventory.mainInventory[4] = namedItem(
            Items.dye,
            paused ? DYE_COLOR_GRAY : DYE_COLOR_PINK,
            paused ? "§dPlay" : "§dPause"
        );
        mc.thePlayer.inventory.mainInventory[5] = texturedHead(
            FORWARD_HEAD_TEXTURE_URL,
            "§dForward 5s"
        );
        mc.thePlayer.inventory.mainInventory[6] = texturedHead(
            FAST_HEAD_TEXTURE_URL,
            "§dSpeed Up"
        );
        mc.thePlayer.inventory.mainInventory[7] = null;
        mc.thePlayer.inventory.mainInventory[8] = namedItem(Items.bed, "§cExit Replay");
    }

    private ItemStack namedItem(net.minecraft.item.Item item, String name) {
        return namedItem(item, 0, name);
    }

    private ItemStack namedItem(net.minecraft.item.Item item, int metadata, String name) {
        ItemStack stack = new ItemStack(item, 1, metadata);
        stack.setStackDisplayName(name);
        return stack;
    }

    private ItemStack texturedHead(String textureUrl, String name) {
        ItemStack stack = namedItem(Items.skull, 3, name);
        stack.setTagCompound(withSkullOwnerTag(stack.getTagCompound(), textureUrl));
        return stack;
    }

    static String buildSkullTextureValue(String textureUrl) {
        String payload = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
        return Base64
            .getEncoder()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    static NBTTagCompound withSkullOwnerTag(NBTTagCompound tag, String textureUrl) {
        NBTTagCompound mergedTag = tag == null ? new NBTTagCompound() : tag;
        String textureValue = buildSkullTextureValue(textureUrl);
        NBTTagCompound skullOwner = new NBTTagCompound();
        skullOwner.setString(
            "Id",
            UUID.nameUUIDFromBytes(textureUrl.getBytes(StandardCharsets.UTF_8)).toString()
        );
        skullOwner.setString("Name", "MellowReplay");

        NBTTagCompound properties = new NBTTagCompound();
        NBTTagList textures = new NBTTagList();
        NBTTagCompound texture = new NBTTagCompound();
        texture.setString("Value", textureValue);
        textures.appendTag(texture);
        properties.setTag("textures", textures);
        skullOwner.setTag("Properties", properties);

        mergedTag.setTag("SkullOwner", skullOwner);
        return mergedTag;
    }

    private boolean triggerControlAction(ControlAction action, int mouseButton) {
        switch (action) {
            case TELEPORT:
                return handleTeleportControl(mouseButton);
            case SLOW_DOWN:
                changeSpeed(-1);
                return true;
            case BACKWARD:
                skipBySeconds(-5);
                return true;
            case TOGGLE_PAUSE:
                togglePause();
                return true;
            case FORWARD:
                skipBySeconds(5);
                return true;
            case SPEED_UP:
                changeSpeed(1);
                return true;
            case STOP:
                requestStop();
                return true;
            default:
                return false;
        }
    }

    private void requestStop() {
        stopRequested = true;
    }

    private boolean handleTeleportControl(int mouseButton) {
        if (mouseButton == 0) {
            return cycleTeleportTarget();
        }
        if (mouseButton == 1) {
            return openTeleportMenu();
        }
        return false;
    }

    private boolean cycleTeleportTarget() {
        List<TeleportTarget> targets = getTeleportTargets();
        if (targets.isEmpty()) {
            ChatUtils.sendMessage("§cNo replay players are currently available to teleport to.");
            return true;
        }

        int nextIndex = 0;
        if (!lastTeleportedPlayerName.isEmpty()) {
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i).getName().equalsIgnoreCase(lastTeleportedPlayerName)) {
                    nextIndex = (i + 1) % targets.size();
                    break;
                }
            }
        }

        TeleportTarget target = targets.get(nextIndex);
        if (!teleportToPlayer(target.getName())) {
            ChatUtils.sendMessage(
                "§cReplay player not available: §f" + target.getName()
            );
        }
        return true;
    }

    private boolean openTeleportMenu() {
        List<TeleportTarget> targets = getTeleportTargets();
        if (targets.isEmpty()) {
            ChatUtils.sendMessage("§cNo replay players are currently available to teleport to.");
            return true;
        }

        mc.displayGuiScreen(new ReplayTeleportPickerGui(this));
        return true;
    }

    private void addTeleportTarget(
        Map<String, TeleportTarget> targetsByName,
        EntityPlayer player
    ) {
        String name = player.getName();
        if (name == null) {
            return;
        }

        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            return;
        }

        String key = trimmedName.toLowerCase(Locale.ROOT);
        if (targetsByName.containsKey(key)) {
            return;
        }

        ScorePlayerTeam team = resolveTeam(trimmedName);
        targetsByName.put(
            key,
            new TeleportTarget(
                trimmedName,
                team == null
                    ? trimmedName
                    : ScorePlayerTeam.formatPlayerName(team, trimmedName),
                resolveTeamSortKey(team),
                resolveTeamLabel(team)
            )
        );
    }

    static List<TeleportTarget> sortTeleportTargets(List<TeleportTarget> targets) {
        List<TeleportTarget> sorted = new ArrayList<>(targets);
        Collections.sort(
            sorted,
            new Comparator<TeleportTarget>() {
                @Override
                public int compare(TeleportTarget first, TeleportTarget second) {
                    int byTeam = first
                        .getTeamSortKey()
                        .compareTo(second.getTeamSortKey());
                    if (byTeam != 0) {
                        return byTeam;
                    }

                    int byName = first
                        .getName()
                        .toLowerCase(Locale.ROOT)
                        .compareTo(second.getName().toLowerCase(Locale.ROOT));
                    if (byName != 0) {
                        return byName;
                    }

                    return first.getName().compareTo(second.getName());
                }
            }
        );
        return sorted;
    }

    private ScorePlayerTeam resolveTeam(String playerName) {
        if (
            mc.theWorld == null ||
            mc.theWorld.getScoreboard() == null ||
            playerName == null ||
            playerName.trim().isEmpty()
        ) {
            return null;
        }
        return mc.theWorld.getScoreboard().getPlayersTeam(playerName);
    }

    private String resolveTeamSortKey(ScorePlayerTeam team) {
        String normalized = resolveGroupedTeamName(team);
        return normalized.isEmpty()
            ? UNASSIGNED_TEAM_SORT_KEY
            : normalized.toLowerCase(Locale.ROOT);
    }

    private String resolveTeamLabel(ScorePlayerTeam team) {
        String normalized = resolveGroupedTeamName(team);
        if (normalized.isEmpty()) {
            normalized = "Unassigned";
        }

        String colorPrefix = team == null ? "" : team.getColorPrefix();
        if (colorPrefix == null || colorPrefix.isEmpty()) {
            colorPrefix = "§7";
        }
        return colorPrefix + normalized;
    }

    private String resolveGroupedTeamName(ScorePlayerTeam team) {
        if (team == null) {
            return "";
        }

        String resolved = normalizeHypixelTeamName(
            team.getRegisteredName(),
            team.getColorPrefix()
        );
        if (!resolved.isEmpty()) {
            return resolved;
        }

        String fallback = normalizeGenericTeamName(team.getRegisteredName());
        if (!fallback.isEmpty()) {
            return fallback;
        }

        return normalizeGenericTeamName(ChatUtils.stripFormatting(team.getColorPrefix()));
    }

    static String normalizeHypixelTeamName(String rawTeamName, String colorPrefix) {
        String normalized = normalizeHypixelTeamToken(rawTeamName);
        if (!normalized.isEmpty()) {
            return normalized;
        }

        normalized = normalizeHypixelTeamToken(ChatUtils.stripFormatting(colorPrefix));
        if (!normalized.isEmpty()) {
            return normalized;
        }

        return mapColorCodeToTeamName(extractMinecraftColorCode(colorPrefix));
    }

    private static String normalizeHypixelTeamToken(String value) {
        if (value == null) {
            return "";
        }

        String normalized = ChatUtils
            .stripFormatting(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z]", "");
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.equals("r") || normalized.contains("red")) {
            return "Red";
        }
        if (normalized.equals("b") || normalized.contains("blue")) {
            return "Blue";
        }
        if (normalized.equals("g") || normalized.contains("green")) {
            return "Green";
        }
        if (normalized.equals("y") || normalized.contains("yellow")) {
            return "Yellow";
        }
        if (
            normalized.equals("a") ||
            normalized.contains("aqua") ||
            normalized.contains("cyan")
        ) {
            return "Aqua";
        }
        if (normalized.equals("w") || normalized.contains("white")) {
            return "White";
        }
        if (
            normalized.equals("p") ||
            normalized.contains("pink") ||
            normalized.contains("lightpurple") ||
            normalized.contains("magenta")
        ) {
            return "Pink";
        }
        if (
            normalized.equals("gr") ||
            normalized.contains("gray") ||
            normalized.contains("grey") ||
            normalized.contains("silver")
        ) {
            return "Gray";
        }
        return "";
    }

    private String normalizeGenericTeamName(String value) {
        String stripped = ChatUtils.stripFormatting(value);
        if (stripped.isEmpty()) {
            return "";
        }
        return toTitleCase(stripped.replace('_', ' ').trim());
    }

    private static String mapColorCodeToTeamName(char colorCode) {
        switch (colorCode) {
            case 'c':
            case '4':
                return "Red";
            case '9':
            case '1':
                return "Blue";
            case 'a':
            case '2':
                return "Green";
            case 'e':
            case '6':
                return "Yellow";
            case 'b':
            case '3':
                return "Aqua";
            case 'f':
                return "White";
            case 'd':
            case '5':
                return "Pink";
            case '7':
            case '8':
                return "Gray";
            default:
                return "";
        }
    }

    private static char extractMinecraftColorCode(String input) {
        if (input == null || input.length() < 2) {
            return '\0';
        }

        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '\u00A7') {
                char maybeColor = Character.toLowerCase(input.charAt(i + 1));
                if (MC_COLOR_CODES.indexOf(maybeColor) >= 0) {
                    return maybeColor;
                }
            }
        }
        return '\0';
    }

    private String toTitleCase(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String[] parts = trimmed.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private void teleportToEntity(Entity entity) {
        if (mc.thePlayer == null || entity == null) {
            return;
        }

        mc.thePlayer.setPositionAndRotation(
            entity.posX,
            entity.posY + 2.0D,
            entity.posZ,
            entity.rotationYaw,
            entity.rotationPitch
        );
        viewerPositionInitialized = true;
        reanchorViewerAfterWorldChange = false;
    }

    private double getSpeed() {
        return SPEEDS[speedIndex];
    }

    private int getPlaybackStartMs() {
        return replay.getMetadata().getPlaybackStartMs();
    }

    private int getVisibleCurrentTimeMs() {
        return Math.max(0, currentTimeMs - getPlaybackStartMs());
    }

    private String speedLabel() {
        double speed = getSpeed();
        if (speed == (int) speed) {
            return ((int) speed) + "x";
        }
        return String.format(Locale.ROOT, "%.2fx", speed);
    }

    private String formatTime(int timeMs) {
        int totalSeconds = Math.max(0, timeMs / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private UUID resolveRecordedPlayerUuid() {
        if (replay.getMetadata().getRecordedPlayerUuid() != null) {
            return replay.getMetadata().getRecordedPlayerUuid();
        }
        return replay.getMetadata().getViewerUuid() == null
            ? UUID.nameUUIDFromBytes(
                ("mellow-replay-" + replay.getMetadata().getReplayId()).getBytes()
            )
            : replay.getMetadata().getViewerUuid();
    }

    public boolean shouldProcessWorldBootstrapPacket() {
        return bootstrapPacketOpen;
    }

    public boolean hasWorldBootstrapped() {
        return worldBootstrapped;
    }

    public boolean allowLegacyFallbackPlayers() {
        return allowLegacyFallbackPlayers;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processPacket(Packet<?> packet) {
        if (
            suppressTransientEffects &&
            (
                packet instanceof S0BPacketAnimation ||
                packet instanceof S28PacketEffect ||
                packet instanceof S29PacketSoundEffect ||
                packet instanceof S2APacketParticles ||
                packet instanceof S2CPacketSpawnGlobalEntity
            )
        ) {
            return;
        }
        ((Packet) packet).processPacket(netHandler);
    }

    private void bootstrapReplayWorld() {
        bootstrapPacketOpen = true;
        try {
            processPacket(
                new S01PacketJoinGame(
                    REPLAY_VIEWER_ENTITY_ID,
                    WorldSettings.GameType.CREATIVE,
                    false,
                    0,
                    EnumDifficulty.NORMAL,
                    20,
                    WorldType.DEFAULT,
                    true
                )
            );
        } finally {
            bootstrapPacketOpen = false;
        }
    }

    private String resolveRecordedPlayerName() {
        String name = replay.getMetadata().getRecordedPlayerName();
        if (name == null || name.trim().isEmpty()) {
            name = replay.getMetadata().getViewerName();
        }
        return name == null || name.trim().isEmpty() ? "ReplayPlayer" : name;
    }

    private void unloadReplayWorld() {
        if (mc.theWorld != null || mc.getNetHandler() != null) {
            if (mc.theWorld != null) {
                mc.theWorld.sendQuittingDisconnectingPacket();
            }
            mc.loadWorld(null);
        }
        mc.displayGuiScreen(null);
    }

    private boolean hasRecordedPlayerSpawns(ReplayLoadedData replayData) {
        for (ReplayPacketFrame frame : replayData.getPackets()) {
            if (S0CPacketSpawnPlayer.class.getName().equals(frame.getClassName())) {
                return true;
            }
        }
        return false;
    }

    static boolean isAtEnd(int currentTimeMs, int durationMs) {
        return currentTimeMs >= durationMs;
    }

    static SeekMode resolveSeekMode(int currentTimeMs, int targetTimeMs) {
        if (targetTimeMs == currentTimeMs) {
            return SeekMode.NONE;
        }
        return targetTimeMs > currentTimeMs
            ? SeekMode.FORWARD
            : SeekMode.REBUILD;
    }

    static int normalizeHotbarSlot(int slot) {
        if (slot < 0) {
            return 0;
        }
        if (slot > 8) {
            return 8;
        }
        return slot;
    }

    static float computeReplayProgress(int currentTimeMs, int durationMs) {
        if (durationMs <= 0) {
            return 0.0F;
        }
        float progress = (float) currentTimeMs / (float) durationMs;
        if (progress < 0.0F) {
            return 0.0F;
        }
        if (progress > 1.0F) {
            return 1.0F;
        }
        return progress;
    }
}
