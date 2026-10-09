package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.IChatComponent;

public class ReplayManager {

    private static final long PREBUFFER_WINDOW_MS = 90_000L;
    private static final int PREBUFFER_MAX_PACKETS = 40_000;
    private static final ReplayManager INSTANCE = new ReplayManager();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayIo io = new ReplayIo();
    private final Deque<PendingFrame> pendingFrames = new ArrayDeque<>();
    private long transitionDeadline;
    private boolean recordingFailed;
    private final ReplayLocalPlayerPacketRecorder pendingLocalPlayerRecorder =
        new ReplayLocalPlayerPacketRecorder();

    private RecordingSession activeRecording;
    private ReplayPlaybackSession activePlayback;
    private GameSnapshot lastSnapshot = GameSnapshot.empty();
    private boolean replayBrowserOpenRequested;

    public static ReplayManager getInstance() {
        return INSTANCE;
    }

    private ReplayManager() {}

    public synchronized void onGameSnapshot(GameSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (isPlaybackActive()) {
            lastSnapshot = snapshot;
            return;
        }
        boolean nowInSession = ReplayRecordingPolicy.isBedwarsSession(snapshot);

        if (!isRecordingEnabled() && activeRecording != null) {
            stopRecording();
        } else if (
            activeRecording == null &&
            !recordingFailed &&
            nowInSession &&
            ReplayRecordingPolicy.isRecordableMatch(snapshot)
        ) {
            startRecording(snapshot);
        } else if (activeRecording != null && !nowInSession) {
            stopRecording();
        }

        if (activeRecording != null) {
            activeRecording.updateSnapshot(snapshot);
        }

        lastSnapshot = snapshot;
        if (!shouldBuffer()) clearPendingFrames();
    }

    private boolean shouldBuffer() {
        return isRecordingEnabled() && !recordingFailed && ReplayRecordingPolicy.shouldBuffer(
            lastSnapshot, transitionDeadline, System.currentTimeMillis());
    }

    private void clearPendingFrames() {
        pendingFrames.clear();
        pendingLocalPlayerRecorder.reset();
    }

    public synchronized void onInboundPacket(Packet<?> packet) {
        if (
            packet == null ||
            isPlaybackActive() ||
            !isRecordingEnabled() ||
            shouldSkipPacket(packet)
        ) {
            return;
        }

        if (packet instanceof S01PacketJoinGame
            || (packet instanceof S07PacketRespawn && activeRecording == null)) {
            stopRecording();
            recordingFailed = false;
            clearPendingFrames();
            lastSnapshot = GameSnapshot.empty();
            // Location arrives after world-start packets; retain those briefly for a possible match.
            transitionDeadline = System.currentTimeMillis() + 10_000L;
        }
        if (activeRecording == null && !shouldBuffer()) return;
        long now = System.currentTimeMillis();
        try {
            ReplayPacketFrame frame = ReplayPacketCodec.encode(0, packet);
            if (activeRecording != null) {
                activeRecording.addPacket(now, frame);
                activeRecording.observeInboundPacket(packet);
                abortRecordingIfFailed();
            } else {
                pendingFrames.add(new PendingFrame(now, frame));
                trimPendingFrames(now);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void onChatReceived(IChatComponent component, byte type) {
        if (component == null) {
            return;
        }

        if (activeRecording == null || !recordChatEnabled()) {
            return;
        }
        activeRecording.addChat(component, type);
        abortRecordingIfFailed();
    }

    public synchronized void onOutboundPacket(Packet<?> packet) {
        if (packet == null || isPlaybackActive() || !isRecordingEnabled()
            || (activeRecording == null && !shouldBuffer())) {
            return;
        }
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.getNetHandler() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        try {
            if (activeRecording != null) {
                activeRecording.observeOutboundPacket(now, packet);
                abortRecordingIfFailed();
            } else {
                pendingLocalPlayerRecorder.observeOutboundPacket(
                    packet,
                    player,
                    mc.getNetHandler().getPlayerInfo(player.getUniqueID()),
                    new ReplayLocalPlayerPacketRecorder.FrameSink() {
                        @Override
                        public void accept(ReplayPacketFrame frame) {
                            pendingFrames.add(new PendingFrame(now, frame));
                        }
                    }
                );
                trimPendingFrames(now);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void onClientTick(GameSnapshot snapshot) {
        if (replayBrowserOpenRequested && !(mc.currentScreen instanceof GuiChat)) {
            replayBrowserOpenRequested = false;
            mc.displayGuiScreen(new ReplayBrowserGui(this));
        }
        if (activePlayback != null && activePlayback.hasLostPlaybackWorld()) {
            activePlayback.stopFromClientDetach();
        }
        if (activeRecording != null) {
            activeRecording.captureTick(snapshot);
            abortRecordingIfFailed();
        } else if (!isPlaybackActive() && shouldBuffer()) {
            capturePendingLocalPlayer();
        } else {
            clearPendingFrames();
        }
        if (activePlayback != null) {
            activePlayback.tick();
        }
    }

    public synchronized void onWorldChange() {
        if (!isPlaybackActive()) {
            recordingFailed = false;
            // The game-state listener may already have supplied this world's location.
            transitionDeadline = System.currentTimeMillis() + 10_000L;
            if (mc.theWorld == null) {
                clearPendingFrames();
                transitionDeadline = 0;
            }
            pendingLocalPlayerRecorder.reset();
            if (activeRecording != null) {
                stopRecording();
            }
        }
    }

    public synchronized void onShutdown() {
        if (activeRecording != null) {
            stopRecording(false);
        }
    }

    public synchronized boolean isPlaybackActive() {
        return activePlayback != null && activePlayback.isActive();
    }

    public synchronized ReplayPlaybackState getPlaybackState() {
        return activePlayback == null
            ? ReplayPlaybackState.inactive()
            : activePlayback.getPlaybackState();
    }

    public synchronized List<String> getHudLines() {
        return activePlayback == null
            ? Collections.<String>emptyList()
            : activePlayback.buildHudLines();
    }

    public synchronized boolean handlePlaybackControlClick(int mouseButton) {
        return activePlayback != null &&
        activePlayback.handleHeldControlClick(mouseButton);
    }

    public synchronized boolean teleportToPlayer(String name) {
        return activePlayback != null && activePlayback.teleportToPlayer(name);
    }

    public void openReplayBrowser() {
        replayBrowserOpenRequested = true;
    }

    public List<ReplayCatalogEntry> listReplays() {
        return io.listReplays(mc.mcDataDir);
    }

    public boolean openReplay(String token) {
        ReplayCatalogEntry entry = resolveReplayEntry(token);
        if (entry == null) {
            return false;
        }
        try {
            ReplayLoadedData replay = io.loadReplay(entry.getDirectory());
            ReplayPlaybackSession previousPlayback;
            synchronized (this) {
                if (activeRecording != null) {
                    stopRecording();
                }
                pendingFrames.clear();
                pendingLocalPlayerRecorder.reset();
                previousPlayback = activePlayback;
                activePlayback = null;
            }

            if (previousPlayback != null) {
                previousPlayback.stop();
            }

            final ReplayPlaybackSession[] playbackRef =
                new ReplayPlaybackSession[1];
            playbackRef[0] = new ReplayPlaybackSession(
                replay,
                new Runnable() {
                    @Override
                    public void run() {
                        synchronized (ReplayManager.this) {
                            if (activePlayback == playbackRef[0]) {
                                activePlayback = null;
                            }
                        }
                    }
                }
            );
            ReplayPlaybackSession playback = playbackRef[0];

            synchronized (this) {
                activePlayback = playback;
            }
            playback.open();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtils.sendMessage("§cFailed to open replay: §f" + describeException(e));
            return false;
        }
    }

    public boolean deleteReplay(String token) {
        ReplayCatalogEntry entry = resolveReplayEntry(token);
        return entry != null && io.deleteReplay(entry.getDirectory());
    }

    public ReplayCatalogEntry resolveReplayEntry(String token) {
        List<ReplayCatalogEntry> replays = listReplays();
        if (replays.isEmpty()) {
            return null;
        }
        if (token == null || token.trim().isEmpty()) {
            return replays.get(0);
        }
        String trimmed = token.trim();
        try {
            int index = Integer.parseInt(trimmed);
            if (index >= 1 && index <= replays.size()) {
                return replays.get(index - 1);
            }
        } catch (NumberFormatException ignored) {}
        for (ReplayCatalogEntry entry : replays) {
            if (entry.getMetadata().getReplayId().equalsIgnoreCase(trimmed)) {
                return entry;
            }
        }
        return null;
    }

    public void sendReplayList(ICommandSender sender) {
        List<ReplayCatalogEntry> entries = listReplays();
        if (entries.isEmpty()) {
            ChatUtils.sendCommandMessage(sender, "§7No saved replays yet.");
            return;
        }
        ChatUtils.sendCommandMessage(sender, "§d§lMellow Replays");
        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            ReplayMetadata meta = entries.get(i).getMetadata();
            ChatUtils.sendMultilineCommandMessage(
                sender,
                "§8[" + (i + 1) + "] §f" + meta.getReplayId() +
                " §7- §f" + safe(meta.getMap()) +
                " §7(§f" + safe(meta.getMode()) + "§7)"
            );
        }
    }

    private void startRecording(GameSnapshot snapshot) {
        if (!isRecordingEnabled()) {
            return;
        }
        try {
            activeRecording = new RecordingSession(
                snapshot,
                pendingFrames,
                pendingLocalPlayerRecorder.copy()
            );
            pendingLocalPlayerRecorder.reset();
            ChatUtils.sendMessage(
                "§7Started recording replay for §f" + safe(snapshot.getMap()) + "§7."
            );
        } catch (Exception e) {
            recordingFailed = true;
            e.printStackTrace();
            ChatUtils.sendMessage(
                "§cFailed to start replay recording: §f" + describeException(e)
            );
        }
    }

    private void stopRecording() {
        stopRecording(true);
    }

    private void stopRecording(boolean notifyPlayer) {
        RecordingSession session = activeRecording;
        activeRecording = null;
        if (session == null) {
            return;
        }
        if (!session.hasPackets()) {
            session.discard();
            return;
        }

        final ReplayMetadata metadata = session.getMetadata();
        final File mcDataDir = mc.mcDataDir;
        final int maxStoredReplays = maxStoredReplays();
        session.writer.finish((spool, failure) -> {
            try {
                if (failure != null) throw failure;
                File directory = io.createReplayDirectory(mcDataDir, metadata);
                io.saveReplay(directory, metadata, spool);
                io.pruneOldest(mcDataDir, maxStoredReplays);
                if (notifyPlayer) {
                    final String replayId = directory.getName();
                    final int durationSeconds = metadata.getDurationMs() / 1000;
                    MainThreadDispatcher.run(new Runnable() {
                        @Override
                        public void run() {
                            ChatUtils.sendMessage(
                                "§7Saved replay §f" + replayId + "§7 (" +
                                durationSeconds + "s)."
                            );
                        }
                    });
                }
            } catch (final Exception e) {
                e.printStackTrace();
                if (notifyPlayer) {
                    MainThreadDispatcher.run(new Runnable() {
                        @Override
                        public void run() {
                            ChatUtils.sendMessage(
                                "§cFailed to save replay: §f" + describeException(e)
                            );
                        }
                    });
                }
            }
        });
    }

    private void abortRecordingIfFailed() {
        RecordingSession session = activeRecording;
        if (session == null || !session.isFailed()) {
            return;
        }
        activeRecording = null;
        recordingFailed = true;
        clearPendingFrames();
        session.discard();
        ChatUtils.sendMessage(
            "§cStopped replay recording: §f" + safe(session.getFailureMessage())
        );
    }

    private String describeException(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        String message = e.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return e.getClass().getSimpleName() + ": " + message;
        }
        return e.getClass().getSimpleName();
    }

    private void trimPendingFrames(long now) {
        while (pendingFrames.size() > PREBUFFER_MAX_PACKETS) {
            pendingFrames.removeFirst();
        }
        while (!pendingFrames.isEmpty()) {
            PendingFrame first = pendingFrames.peekFirst();
            if (now - first.capturedAt <= PREBUFFER_WINDOW_MS) {
                break;
            }
            pendingFrames.removeFirst();
        }
    }

    private void capturePendingLocalPlayer() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.getNetHandler() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        try {
            pendingLocalPlayerRecorder.capture(
                player,
                mc.getNetHandler().getPlayerInfo(player.getUniqueID()),
                new ReplayLocalPlayerPacketRecorder.FrameSink() {
                    @Override
                    public void accept(ReplayPacketFrame frame) {
                        pendingFrames.add(new PendingFrame(now, frame));
                    }
                }
            );
            trimPendingFrames(now);
        } catch (Exception ignored) {}
    }

    private boolean isRecordingEnabled() {
        return Mellow.config != null && Mellow.config.enableReplayRecording;
    }

    private boolean recordChatEnabled() {
        return Mellow.config == null || Mellow.config.recordChatInReplays;
    }

    private int maxStoredReplays() {
        return Mellow.config == null ? 0 : Mellow.config.maxStoredReplays;
    }

    private boolean shouldSkipPacket(Packet<?> packet) {
        return
            packet instanceof S02PacketChat ||
            packet instanceof S06PacketUpdateHealth ||
            packet instanceof S09PacketHeldItemChange ||
            packet instanceof S1FPacketSetExperience ||
            packet instanceof S2DPacketOpenWindow ||
            packet instanceof S2EPacketCloseWindow ||
            packet instanceof S2FPacketSetSlot ||
            packet instanceof S30PacketWindowItems ||
            packet instanceof S32PacketConfirmTransaction ||
            packet instanceof S37PacketStatistics ||
            packet instanceof S39PacketPlayerAbilities ||
            packet instanceof S43PacketCamera ||
            packet instanceof S45PacketTitle;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private static final class PendingFrame {
        private final long capturedAt;
        private final ReplayPacketFrame frame;

        private PendingFrame(long capturedAt, ReplayPacketFrame frame) {
            this.capturedAt = capturedAt;
            this.frame = frame;
        }
    }

    private final class RecordingSession {

        private final ReplayMetadata metadata = new ReplayMetadata();
        private final Set<Integer> knownRemotePlayerEntityIds = new HashSet<>();
        private final ReplayLocalPlayerPacketRecorder localPlayerRecorder;
        private final ReplayRecordingWriter writer;
        private boolean hasPackets;
        private final long baseTime;
        private String lastScoreboardTitle = "";
        private List<String> lastScoreboardLines = Collections.emptyList();

        private RecordingSession(
            GameSnapshot snapshot,
            Deque<PendingFrame> pending,
            ReplayLocalPlayerPacketRecorder localPlayerRecorder
        ) {
            long now = System.currentTimeMillis();
            this.baseTime = pending.isEmpty() ? now : pending.peekFirst().capturedAt;
            this.localPlayerRecorder = localPlayerRecorder;
            File mcDataDir = mc.mcDataDir;
            this.writer = new ReplayRecordingWriter(
                AsyncExecutor.getInstance()::replayIo, () -> io.createRecordingSpool(mcDataDir));
            metadata.setStartedAt(baseTime);
            metadata.setViewerName(
                mc.thePlayer == null ? "" : mc.thePlayer.getName()
            );
            metadata.setViewerUuid(
                mc.thePlayer == null ? null : mc.thePlayer.getUniqueID()
            );
            configureRecordedPlayerMetadata();
            updateSnapshot(snapshot);
            List<ReplayPacketFrame> initialFrames = new ArrayList<>(pending.size());
            for (PendingFrame frame : pending) {
                int timestamp = toRelativeTime(frame.capturedAt);
                initialFrames.add(new ReplayPacketFrame(timestamp, frame.frame.getClassName(), frame.frame.getPayload()));
                metadata.setDurationMs(Math.max(metadata.getDurationMs(), timestamp));
                observeStoredFrame(frame.frame);
            }
            hasPackets = !initialFrames.isEmpty();
            writer.write(spool -> {
                for (ReplayPacketFrame frame : initialFrames) spool.appendPacket(frame);
            });
            pendingFrames.clear();
            captureVisiblePlayers(now);
            captureLocalPlayerPackets(now);
        }

        private void updateSnapshot(GameSnapshot snapshot) {
            metadata.setMap(snapshot.getMap());
            metadata.setMode(snapshot.getMode());
            metadata.setServerName(snapshot.getServerName());
            metadata.setGameType(
                snapshot.getGameType() == null
                    ? ""
                    : snapshot.getGameType().name().toLowerCase(Locale.ROOT)
            );
        }

        private void addPacket(long capturedAt, ReplayPacketFrame frame) {
            int timestamp = toRelativeTime(capturedAt);
            hasPackets = true;
            writer.write(new ReplayRecordingWriter.Write() {
                @Override
                public void run(ReplayRecordingSpool spool) throws IOException {
                    spool.appendPacket(
                        new ReplayPacketFrame(timestamp, frame.getClassName(), frame.getPayload())
                    );
                }
            });
            metadata.setDurationMs(Math.max(metadata.getDurationMs(), timestamp));
        }

        private void addChat(IChatComponent component, byte type) {
            int timestamp = toRelativeTime(System.currentTimeMillis());
            ReplayChatEvent event = new ReplayChatEvent(
                timestamp, IChatComponent.Serializer.componentToJson(component), type);
            writer.write(spool -> spool.appendChat(event));
        }

        private void captureTick(GameSnapshot snapshot) {
            long now = System.currentTimeMillis();
            captureVisiblePlayers(now);
            captureLocalPlayerPackets(now);
            captureScoreboard(snapshot);
            metadata.setEndedAt(now);
            metadata.setDurationMs(toRelativeTime(now));
        }

        private void captureScoreboard(GameSnapshot snapshot) {
            String title = snapshot == null ? "" : snapshot.getScoreboardTitle();
            List<String> lines = snapshot == null
                ? Collections.<String>emptyList()
                : new ArrayList<>(snapshot.getScoreboardLines());
            boolean changed = !safe(title).equals(safe(lastScoreboardTitle)) ||
            !lines.equals(lastScoreboardLines);
            if (!changed) {
                return;
            }
            long now = System.currentTimeMillis();
            final ReplayScoreboardFrame frame = new ReplayScoreboardFrame(
                toRelativeTime(now),
                title,
                lines
            );
            writer.write(new ReplayRecordingWriter.Write() {
                @Override
                public void run(ReplayRecordingSpool spool) throws IOException {
                    spool.appendScoreboard(frame);
                }
            });
            lastScoreboardTitle = title;
            lastScoreboardLines = lines;
        }

        private void configureRecordedPlayerMetadata() {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null) {
                return;
            }
            metadata.setRecordedPlayerEntityId(Integer.valueOf(player.getEntityId()));
            metadata.setRecordedPlayerUuid(player.getUniqueID());
            metadata.setRecordedPlayerName(player.getName());
        }

        private void captureLocalPlayerPackets(long capturedAt) {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null || mc.getNetHandler() == null) {
                return;
            }
            try {
                localPlayerRecorder.capture(
                    player,
                    mc.getNetHandler().getPlayerInfo(player.getUniqueID()),
                    new ReplayLocalPlayerPacketRecorder.FrameSink() {
                        @Override
                        public void accept(ReplayPacketFrame frame) {
                            addPacket(capturedAt, frame);
                        }
                    }
                );
            } catch (Exception ignored) {}
        }

        private void observeInboundPacket(Packet<?> packet) {
            if (packet instanceof S0CPacketSpawnPlayer) {
                markRemotePlayerEntity(((S0CPacketSpawnPlayer) packet).getEntityID());
            } else if (packet instanceof S13PacketDestroyEntities) {
                forgetRemotePlayerEntities(((S13PacketDestroyEntities) packet).getEntityIDs());
            }
        }

        private void observeOutboundPacket(long capturedAt, Packet<?> packet) {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null || mc.getNetHandler() == null) {
                return;
            }
            try {
                localPlayerRecorder.observeOutboundPacket(
                    packet,
                    player,
                    mc.getNetHandler().getPlayerInfo(player.getUniqueID()),
                    new ReplayLocalPlayerPacketRecorder.FrameSink() {
                        @Override
                        public void accept(ReplayPacketFrame frame) {
                            addPacket(capturedAt, frame);
                        }
                    }
                );
            } catch (Exception ignored) {}
        }

        private void observeStoredFrame(ReplayPacketFrame frame) {
            if (
                //? if ornithe {
                !ReplayPacketCodec.typeName(S0CPacketSpawnPlayer.class).equals(frame.getClassName()) &&
                !ReplayPacketCodec.typeName(S13PacketDestroyEntities.class).equals(frame.getClassName())
                //?} else {
                /*!S0CPacketSpawnPlayer.class.getName().equals(frame.getClassName()) &&
                !S13PacketDestroyEntities.class.getName().equals(frame.getClassName())
                *///?}
            ) {
                return;
            }
            try {
                Packet<?> packet = ReplayPacketCodec.decode(frame);
                observeInboundPacket(packet);
            } catch (Exception ignored) {}
        }

        private void captureVisiblePlayers(long capturedAt) {
            if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) {
                return;
            }

            for (Object playerObj : mc.theWorld.playerEntities) {
                if (!(playerObj instanceof EntityPlayer)) {
                    continue;
                }

                EntityPlayer player = (EntityPlayer) playerObj;
                if (player == mc.thePlayer) {
                    continue;
                }

                int entityId = player.getEntityId();
                if (knownRemotePlayerEntityIds.contains(entityId)) {
                    continue;
                }

                if (
                    player.getGameProfile() == null ||
                    player.getGameProfile().getId() == null ||
                    player.getGameProfile().getName() == null ||
                    player.getGameProfile().getName().trim().isEmpty()
                ) {
                    continue;
                }

                NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(
                    player.getUniqueID()
                );
                if (playerInfo == null) {
                    continue;
                }

                try {
                    addPacket(capturedAt, ReplayPacketCodec.encode(0, new S0CPacketSpawnPlayer(player)));
                    markRemotePlayerEntity(entityId);
                } catch (Exception ignored) {}
            }
        }

        private void markRemotePlayerEntity(int entityId) {
            if (mc.thePlayer == null || entityId != mc.thePlayer.getEntityId()) {
                knownRemotePlayerEntityIds.add(entityId);
            }
        }

        private void forgetRemotePlayerEntities(int[] entityIds) {
            if (entityIds == null || entityIds.length == 0) {
                return;
            }
            for (int entityId : entityIds) {
                knownRemotePlayerEntityIds.remove(entityId);
            }
        }

        private int toRelativeTime(long capturedAt) {
            return (int) Math.max(0L, capturedAt - baseTime);
        }

        private ReplayMetadata getMetadata() {
            metadata.setEndedAt(Math.max(metadata.getEndedAt(), System.currentTimeMillis()));
            return metadata;
        }

        private boolean hasPackets() {
            return hasPackets;
        }

        private boolean isFailed() {
            return writer.getFailure() != null;
        }

        private String getFailureMessage() {
            return writer.getFailure() == null ? "" : writer.getFailure().getMessage();
        }

        private void discard() {
            writer.finish((spool, failure) -> {});
        }
    }
}
