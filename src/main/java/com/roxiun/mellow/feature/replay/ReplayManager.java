package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.module.bedwars.BedwarsChatSignalParser;
import com.roxiun.mellow.util.ChatUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S20PacketEntityProperties;
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
    private static final long PENDING_CLIP_JOIN_TIMEOUT_MS = 30_000L;
    private static final ReplayManager INSTANCE = new ReplayManager();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayIo io = new ReplayIo();
    private final List<PendingFrame> pendingFrames = new ArrayList<>();
    private final List<PendingFrame> pendingClipFrames = new ArrayList<>();
    private final ReplayLocalPlayerPacketRecorder pendingLocalPlayerRecorder =
        new ReplayLocalPlayerPacketRecorder();

    private RecordingSession activeRecording;
    private ClipSession activeClip;
    private ReplayPlaybackSession activePlayback;
    private GameSnapshot lastSnapshot = GameSnapshot.empty();
    private boolean replayBrowserOpenRequested;
    private boolean clipCaptureCapped;

    public enum ClipRequestResult {
        ACCEPTED,
        DISABLED,
        NO_MULTIPLAYER_SESSION,
        PLAYBACK_ACTIVE,
        BUFFER_UNAVAILABLE,
        BUFFER_CAPPED,
    }

    public static ReplayManager getInstance() {
        return INSTANCE;
    }

    private ReplayManager() {}

    public synchronized void initialize() {
        io.cleanupStaleClipSpools(mc.mcDataDir);
    }

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
            nowInSession &&
            ReplayRecordingPolicy.hasLiveMatchEvidence(snapshot)
        ) {
            startRecording(snapshot);
        } else if (activeRecording != null && !nowInSession) {
            stopRecording();
        }

        if (activeRecording != null) {
            activeRecording.updateSnapshot(snapshot);
        }

        lastSnapshot = snapshot;
    }

    public synchronized void onInboundPacket(Packet<?> packet) {
        if (
            packet == null ||
            isPlaybackActive() ||
            (!isRecordingEnabled() && !isClippingEnabled()) ||
            shouldSkipPacket(packet)
        ) {
            return;
        }

        long now = System.currentTimeMillis();
        try {
            ReplayPacketFrame frame = ReplayPacketCodec.encode(0, packet);
            if (packet instanceof S01PacketJoinGame) {
                if (activeRecording != null) {
                    stopRecording();
                }
                pendingFrames.clear();
                pendingClipFrames.clear();
                pendingLocalPlayerRecorder.reset();
                discardClipSession(false);
                clipCaptureCapped = false;
                if (isClippingEnabled()) {
                    pendingClipFrames.add(new PendingFrame(now, frame));
                }
                if (isRecordingEnabled()) {
                    pendingFrames.add(new PendingFrame(now, frame));
                }
                return;
            }

            if (activeClip != null) {
                activeClip.addPacket(now, frame);
                activeClip.observeInboundPacket(packet);
                abortClipIfFailed();
            } else if (isClippingEnabled() && !pendingClipFrames.isEmpty()) {
                pendingClipFrames.add(new PendingFrame(now, frame));
                trimPendingClipFrames(now);
            }
            if (activeRecording != null) {
                activeRecording.addPacket(now, frame);
                activeRecording.observeInboundPacket(packet);
                abortRecordingIfFailed();
            } else if (isRecordingEnabled()) {
                pendingFrames.add(new PendingFrame(now, frame));
                trimPendingFrames(now);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void onChatReceived(IChatComponent component, byte type) {
        if (component == null) {
            return;
        }

        if (activeClip != null && recordChatEnabled()) {
            activeClip.addChat(component, type);
        }

        String message = component.getUnformattedText();
        if (
            activeRecording == null &&
            isRecordingEnabled() &&
            ReplayRecordingPolicy.isBedwarsSession(lastSnapshot) &&
            ReplayRecordingPolicy.isChatConfirmedMatchStart(message)
        ) {
            startRecording(lastSnapshot);
        }

        if (activeRecording == null || !recordChatEnabled()) {
            return;
        }
        activeRecording.addChat(component, type);
        abortRecordingIfFailed();
    }

    public synchronized void onOutboundPacket(Packet<?> packet) {
        if (
            packet == null ||
            isPlaybackActive() ||
            (!isRecordingEnabled() && !isClippingEnabled())
        ) {
            return;
        }
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.getNetHandler() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        try {
            if (activeClip != null) {
                activeClip.observeOutboundPacket(now, packet);
                abortClipIfFailed();
            } else if (activeRecording != null) {
                activeRecording.observeOutboundPacket(now, packet);
                abortRecordingIfFailed();
            } else if (isRecordingEnabled()) {
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
        if (!isClippingEnabled()) {
            pendingClipFrames.clear();
            if (activeClip != null) {
                discardClipSession(true);
            }
        } else if (activeClip == null && !pendingClipFrames.isEmpty()) {
            startPendingClipSession();
        } else if (
            activeClip != null &&
            !isPlaybackActive() &&
            !isConnectedToMultiplayer()
        ) {
            discardClipSession(false);
        }
        if (activeClip != null) {
            activeClip.captureTick(snapshot);
            abortClipIfFailed();
        }
        if (activeRecording != null) {
            activeRecording.captureTick(snapshot, activeClip == null);
            abortRecordingIfFailed();
        } else if (
            activeClip == null &&
            !isPlaybackActive() &&
            isRecordingEnabled()
        ) {
            capturePendingLocalPlayer();
        }
        if (activePlayback != null) {
            activePlayback.tick();
        }
    }

    public synchronized void onWorldChange() {
        if (!isPlaybackActive()) {
            pendingLocalPlayerRecorder.reset();
            if (activeRecording != null) {
                stopRecording();
            }
            if (activeClip != null) {
                activeClip.resetPlayerTrackingAfterWorldChange();
            }
        }
    }

    public synchronized void onShutdown() {
        if (activeRecording != null) {
            stopRecording(false);
        }
        discardClipSession(false);
        pendingClipFrames.clear();
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
                pendingClipFrames.clear();
                pendingLocalPlayerRecorder.reset();
                discardClipSession(false);
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

    public synchronized ClipRequestResult requestClip() {
        if (!isClippingEnabled()) {
            return ClipRequestResult.DISABLED;
        }
        if (isPlaybackActive()) {
            return ClipRequestResult.PLAYBACK_ACTIVE;
        }
        if (!isConnectedToMultiplayer()) {
            return ClipRequestResult.NO_MULTIPLAYER_SESSION;
        }
        if (activeClip == null) {
            return clipCaptureCapped
                ? ClipRequestResult.BUFFER_CAPPED
                : ClipRequestResult.BUFFER_UNAVAILABLE;
        }

        final ReplayRecordingSpoolSnapshot snapshot;
        final ReplayMetadata metadata = activeClip.buildMetadata();
        final List<ReplayChatEvent> chats = activeClip.copyVisibleChats(
            metadata.getPlaybackStartMs(),
            metadata.getDurationMs()
        );
        try {
            snapshot = activeClip.snapshot();
        } catch (IOException e) {
            abortClipWithFailure(e);
            return ClipRequestResult.BUFFER_UNAVAILABLE;
        }

        final File mcDataDir = mc.mcDataDir;
        final int maxStoredReplays = maxStoredReplays();
        AsyncExecutor.getInstance().replayIo(new Runnable() {
            @Override
            public void run() {
                File directory = null;
                try {
                    directory = io.createClipDirectory(mcDataDir, metadata);
                    io.saveClipAtomically(directory, metadata, snapshot, chats);
                    io.pruneOldest(mcDataDir, maxStoredReplays);
                    final String replayId = directory.getName();
                    final int durationSeconds = metadata.getVisibleDurationMs() / 1000;
                    MainThreadDispatcher.run(new Runnable() {
                        @Override
                        public void run() {
                            ChatUtils.sendMessage(
                                "§7Saved clip §f" + replayId + "§7 (" +
                                durationSeconds + "s)."
                            );
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (directory != null) {
                        io.deleteReplay(directory);
                    }
                    MainThreadDispatcher.run(new Runnable() {
                        @Override
                        public void run() {
                            ChatUtils.sendMessage(
                                "§cFailed to save clip: §f" + describeException(e)
                            );
                        }
                    });
                } finally {
                    snapshot.close();
                }
            }
        });
        return ClipRequestResult.ACCEPTED;
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
            ChatUtils.sendCommandMessage(sender, "§7No saved replays or clips yet.");
            return;
        }
        ChatUtils.sendCommandMessage(sender, "§d§lMellow Replays & Clips");
        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            ReplayMetadata meta = entries.get(i).getMetadata();
            ChatUtils.sendMultilineCommandMessage(
                sender,
                "§8[" + (i + 1) + "] " + (meta.isClip() ? "§d[Clip] " : "") +
                "§f" + meta.getReplayId() +
                " §7- §f" + safe(meta.getMap().isEmpty() ? meta.getServerName() : meta.getMap()) +
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
                activeClip == null
                    ? pendingLocalPlayerRecorder.copy()
                    : activeClip.copyLocalPlayerRecorder()
            );
            pendingLocalPlayerRecorder.reset();
            ChatUtils.sendMessage(
                "§7Started recording replay for §f" + safe(snapshot.getMap()) + "§7."
            );
        } catch (Exception e) {
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
        final ReplayRecordingSpool spool = session.getSpool();
        final File mcDataDir = mc.mcDataDir;
        final int maxStoredReplays = maxStoredReplays();
        AsyncExecutor.getInstance().replayIo(new Runnable() {
            @Override
            public void run() {
                try {
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
                } finally {
                    session.discard();
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
            pendingFrames.remove(0);
        }
        while (!pendingFrames.isEmpty()) {
            PendingFrame first = pendingFrames.get(0);
            if (now - first.capturedAt <= PREBUFFER_WINDOW_MS) {
                break;
            }
            pendingFrames.remove(0);
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

    private boolean isClippingEnabled() {
        return Mellow.config != null && Mellow.config.enableReplayClipping;
    }

    private int clipLengthMs() {
        int seconds = Mellow.config == null
            ? 60
            : Mellow.config.replayClipLengthSeconds;
        return Math.max(15, Math.min(300, seconds)) * 1000;
    }

    private long clipBufferLimitBytes() {
        int mebibytes = Mellow.config == null
            ? 1024
            : Mellow.config.replayClipBufferMiB;
        int clamped = Math.max(128, Math.min(4096, mebibytes));
        return clamped * 1024L * 1024L;
    }

    private boolean isConnectedToMultiplayer() {
        return
            !mc.isSingleplayer() &&
            mc.getCurrentServerData() != null &&
            mc.getNetHandler() != null;
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

    private void startPendingClipSession() {
        if (pendingClipFrames.isEmpty()) {
            return;
        }
        PendingFrame first = pendingClipFrames.get(0);
        if (isPlaybackActive() || mc.isSingleplayer()) {
            pendingClipFrames.clear();
            return;
        }
        if (!isConnectedToMultiplayer()) {
            if (
                System.currentTimeMillis() - first.capturedAt >=
                PENDING_CLIP_JOIN_TIMEOUT_MS
            ) {
                pendingClipFrames.clear();
            }
            return;
        }
        try {
            activeClip = new ClipSession(first.capturedAt);
            for (PendingFrame pending : pendingClipFrames) {
                activeClip.addPacket(pending.capturedAt, pending.frame);
                activeClip.observeStoredFrame(pending.frame);
                if (activeClip.isFailed()) {
                    break;
                }
            }
            pendingClipFrames.clear();
            abortClipIfFailed();
        } catch (IOException e) {
            pendingClipFrames.clear();
            ChatUtils.sendMessage(
                "§cReplay clipping is unavailable in this world: §f" +
                describeException(e)
            );
        }
    }

    private void discardClipSession(boolean preserveLocalRecorder) {
        ClipSession session = activeClip;
        activeClip = null;
        if (session != null) {
            if (preserveLocalRecorder && isRecordingEnabled()) {
                pendingLocalPlayerRecorder.copyFrom(session.localPlayerRecorder);
            }
            session.discard();
        }
    }

    private void trimPendingClipFrames(long now) {
        long cutoff = now - PREBUFFER_WINDOW_MS;
        while (
            !pendingClipFrames.isEmpty() &&
            (pendingClipFrames.get(0).capturedAt < cutoff ||
                pendingClipFrames.size() > PREBUFFER_MAX_PACKETS)
        ) {
            pendingClipFrames.remove(0);
        }
    }

    private void abortClipIfFailed() {
        ClipSession session = activeClip;
        if (session == null || !session.isFailed()) {
            return;
        }
        activeClip = null;
        if (isRecordingEnabled()) {
            pendingLocalPlayerRecorder.copyFrom(session.localPlayerRecorder);
        }
        clipCaptureCapped = session.isCapped();
        session.discard();
        ChatUtils.sendMessage(
            clipCaptureCapped
                ? "§cReplay clipping paused until the next world because the temporary buffer reached its configured limit."
                : "§cReplay clipping stopped for this world: §f" +
                    safe(session.getFailureMessage())
        );
    }

    private void abortClipWithFailure(Exception e) {
        ClipSession session = activeClip;
        activeClip = null;
        clipCaptureCapped = false;
        if (session != null) {
            if (isRecordingEnabled()) {
                pendingLocalPlayerRecorder.copyFrom(session.localPlayerRecorder);
            }
            session.discard();
        }
        ChatUtils.sendMessage(
            "§cReplay clipping stopped for this world: §f" + describeException(e)
        );
    }

    private static final class PendingFrame {
        private final long capturedAt;
        private final ReplayPacketFrame frame;

        private PendingFrame(long capturedAt, ReplayPacketFrame frame) {
            this.capturedAt = capturedAt;
            this.frame = frame;
        }
    }

    private final class ClipSession {

        private static final long CHAT_HISTORY_MS = 300_000L;

        private final long baseTime;
        private final ReplayRecordingSpool spool;
        private final ReplayLocalPlayerPacketRecorder localPlayerRecorder =
            new ReplayLocalPlayerPacketRecorder();
        private final Set<Integer> knownRemotePlayerEntityIds = new HashSet<>();
        private final Deque<ReplayChatEvent> chats = new ArrayDeque<>();
        private String lastScoreboardTitle = "";
        private List<String> lastScoreboardLines = Collections.emptyList();
        private boolean failed;
        private boolean capped;
        private String failureMessage = "";

        private ClipSession(long baseTime) throws IOException {
            this.baseTime = baseTime;
            this.spool = io.createClipSpool(mc.mcDataDir);
        }

        private void addPacket(long capturedAt, ReplayPacketFrame frame) {
            final ReplayPacketFrame timestamped = new ReplayPacketFrame(
                toRelativeTime(capturedAt),
                frame.getClassName(),
                frame.getPayload()
            );
            long additionalBytes = 12L + timestamped.getPayload().length;
            tryWrite(additionalBytes, new IoRunnable() {
                @Override
                public void run() throws IOException {
                    spool.appendPacket(timestamped);
                }
            });
        }

        private void observeInboundPacket(Packet<?> packet) {
            if (packet instanceof S0CPacketSpawnPlayer) {
                markRemotePlayerEntity(
                    ((S0CPacketSpawnPlayer) packet).getEntityID()
                );
            } else if (packet instanceof S13PacketDestroyEntities) {
                forgetRemotePlayerEntities(
                    ((S13PacketDestroyEntities) packet).getEntityIDs()
                );
            }
        }

        private void observeStoredFrame(ReplayPacketFrame frame) {
            if (
                !S0CPacketSpawnPlayer.class.getName().equals(frame.getClassName()) &&
                !S13PacketDestroyEntities.class.getName().equals(frame.getClassName())
            ) {
                return;
            }
            try {
                observeInboundPacket(ReplayPacketCodec.decode(frame));
            } catch (Exception ignored) {}
        }

        private void addChat(IChatComponent component, byte type) {
            int timestamp = toRelativeTime(System.currentTimeMillis());
            chats.addLast(
                new ReplayChatEvent(
                    timestamp,
                    IChatComponent.Serializer.componentToJson(component),
                    type
                )
            );
            int oldestTimestamp = Math.max(0, timestamp - (int) CHAT_HISTORY_MS);
            while (
                !chats.isEmpty() &&
                chats.peekFirst().getTimestampMs() < oldestTimestamp
            ) {
                chats.removeFirst();
            }
        }

        private void observeOutboundPacket(long capturedAt, Packet<?> packet)
            throws Exception {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null || mc.getNetHandler() == null) {
                return;
            }
            localPlayerRecorder.observeOutboundPacket(
                packet,
                player,
                mc.getNetHandler().getPlayerInfo(player.getUniqueID()),
                new ReplayLocalPlayerPacketRecorder.FrameSink() {
                    @Override
                    public void accept(ReplayPacketFrame frame) {
                        addLocalPlayerPacket(capturedAt, frame);
                    }
                }
            );
        }

        private void captureTick(GameSnapshot snapshot) {
            captureVisiblePlayers(System.currentTimeMillis());
            captureLocalPlayerPackets(System.currentTimeMillis());
            captureScoreboard(snapshot);
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
                    for (
                        ReplayPacketFrame frame :
                        encodeVisiblePlayerSnapshot(player, playerInfo)
                    ) {
                        addPacket(capturedAt, frame);
                    }
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
                            addLocalPlayerPacket(capturedAt, frame);
                        }
                    }
                );
            } catch (Exception ignored) {}
        }

        private void captureScoreboard(GameSnapshot snapshot) {
            String title = snapshot == null ? "" : snapshot.getScoreboardTitle();
            List<String> lines = snapshot == null
                ? Collections.<String>emptyList()
                : new ArrayList<>(snapshot.getScoreboardLines());
            if (safe(title).equals(safe(lastScoreboardTitle)) && lines.equals(lastScoreboardLines)) {
                return;
            }
            final ReplayScoreboardFrame frame = new ReplayScoreboardFrame(
                toRelativeTime(System.currentTimeMillis()),
                title,
                lines
            );
            long additionalBytes = 12L + utf8Length(title);
            for (String line : lines) {
                additionalBytes += 4L + utf8Length(line);
            }
            tryWrite(additionalBytes, new IoRunnable() {
                @Override
                public void run() throws IOException {
                    spool.appendScoreboard(frame);
                }
            });
            lastScoreboardTitle = title;
            lastScoreboardLines = lines;
        }

        private ReplayLocalPlayerPacketRecorder copyLocalPlayerRecorder() {
            return localPlayerRecorder.copy();
        }

        private void resetPlayerTrackingAfterWorldChange() {
            knownRemotePlayerEntityIds.clear();
            localPlayerRecorder.reset();
        }

        private void addLocalPlayerPacket(long capturedAt, ReplayPacketFrame frame) {
            addPacket(capturedAt, frame);
            if (!isRecordingEnabled()) {
                return;
            }
            if (activeRecording != null) {
                activeRecording.addPacket(capturedAt, frame);
                abortRecordingIfFailed();
            } else {
                pendingFrames.add(new PendingFrame(capturedAt, frame));
                trimPendingFrames(capturedAt);
            }
        }

        private ReplayMetadata buildMetadata() {
            long now = System.currentTimeMillis();
            int endMs = toRelativeTime(now);
            ReplayMetadata metadata = new ReplayMetadata();
            metadata.setKind(ReplayMetadata.KIND_CLIP);
            metadata.setStartedAt(baseTime);
            metadata.setEndedAt(now);
            metadata.setSavedAt(now);
            metadata.setDurationMs(endMs);
            metadata.setPlaybackStartMs(Math.max(0, endMs - clipLengthMs()));
            metadata.setMap(lastSnapshot.getMap());
            metadata.setMode(lastSnapshot.getMode());
            metadata.setGameType(
                lastSnapshot.getGameType() == null
                    ? "multiplayer"
                    : lastSnapshot.getGameType().name().toLowerCase(Locale.ROOT)
            );
            String serverName = lastSnapshot.getServerName();
            ServerData serverData = mc.getCurrentServerData();
            if (serverName == null || serverName.trim().isEmpty()) {
                if (serverData != null && serverData.serverName != null && !serverData.serverName.trim().isEmpty()) {
                    serverName = serverData.serverName;
                } else if (serverData != null) {
                    serverName = serverData.serverIP;
                }
            }
            metadata.setServerName(serverName);
            EntityPlayerSP player = mc.thePlayer;
            if (player != null) {
                metadata.setViewerName(player.getName());
                metadata.setViewerUuid(player.getUniqueID());
                metadata.setRecordedPlayerEntityId(Integer.valueOf(player.getEntityId()));
                metadata.setRecordedPlayerUuid(player.getUniqueID());
                metadata.setRecordedPlayerName(player.getName());
            }
            return metadata;
        }

        private List<ReplayChatEvent> copyVisibleChats(int startMs, int endMs) {
            if (!recordChatEnabled() || chats.isEmpty()) {
                return Collections.emptyList();
            }
            List<ReplayChatEvent> visible = new ArrayList<>();
            for (ReplayChatEvent event : chats) {
                if (event.getTimestampMs() >= startMs && event.getTimestampMs() <= endMs) {
                    visible.add(event);
                }
            }
            return visible;
        }

        private ReplayRecordingSpoolSnapshot snapshot() throws IOException {
            return spool.snapshot();
        }

        private int toRelativeTime(long capturedAt) {
            return (int) Math.max(0L, capturedAt - baseTime);
        }

        private void tryWrite(long additionalBytes, IoRunnable action) {
            if (failed) {
                return;
            }
            if (spool.getBytesWritten() + additionalBytes > clipBufferLimitBytes()) {
                failed = true;
                capped = true;
                failureMessage = "Temporary clip buffer limit reached.";
                return;
            }
            try {
                action.run();
            } catch (IOException e) {
                failed = true;
                failureMessage = e.getMessage();
            }
        }

        private boolean isFailed() {
            return failed;
        }

        private boolean isCapped() {
            return capped;
        }

        private String getFailureMessage() {
            return failureMessage;
        }

        private void discard() {
            spool.discard();
            chats.clear();
        }
    }

    private int utf8Length(String value) {
        try {
            return (value == null ? "" : value).getBytes("UTF-8").length;
        } catch (java.io.UnsupportedEncodingException ignored) {
            return value == null ? 0 : value.length();
        }
    }

    private List<ReplayPacketFrame> encodeVisiblePlayerSnapshot(
        EntityPlayer player,
        NetworkPlayerInfo playerInfo
    ) throws Exception {
        List<ReplayPacketFrame> frames = new ArrayList<>();
        frames.add(
            ReplayPacketFactory.playerListAdd(
                player.getGameProfile(),
                playerInfo.getGameType(),
                playerInfo.getResponseTime(),
                playerInfo.getDisplayName()
            )
        );
        frames.add(
            ReplayPacketCodec.encode(0, new S0CPacketSpawnPlayer(player))
        );
        for (int slot = 0; slot < 5; slot++) {
            frames.add(
                ReplayPacketCodec.encode(
                    0,
                    new S04PacketEntityEquipment(
                        player.getEntityId(),
                        slot,
                        player.getEquipmentInSlot(slot)
                    )
                )
            );
        }
        frames.add(
            ReplayPacketCodec.encode(
                0,
                new S20PacketEntityProperties(
                    player.getEntityId(),
                    player.getAttributeMap().getAllAttributes()
                )
            )
        );
        frames.add(
            ReplayPacketCodec.encode(
                0,
                new S19PacketEntityHeadLook(
                    player,
                    (byte) (player.rotationYawHead * 256.0F / 360.0F)
                )
            )
        );
        return frames;
    }

    private final class RecordingSession {

        private final ReplayMetadata metadata = new ReplayMetadata();
        private final Set<Integer> knownRemotePlayerEntityIds = new HashSet<>();
        private final ReplayLocalPlayerPacketRecorder localPlayerRecorder;
        private final ReplayRecordingSpool spool;
        private final long baseTime;
        private String lastScoreboardTitle = "";
        private List<String> lastScoreboardLines = Collections.emptyList();
        private boolean failed;
        private String failureMessage = "";

        private RecordingSession(
            GameSnapshot snapshot,
            List<PendingFrame> pending,
            ReplayLocalPlayerPacketRecorder localPlayerRecorder
        ) throws IOException {
            long now = System.currentTimeMillis();
            this.baseTime = pending.isEmpty() ? now : pending.get(0).capturedAt;
            this.localPlayerRecorder = localPlayerRecorder;
            this.spool = io.createRecordingSpool(mc.mcDataDir);
            metadata.setStartedAt(baseTime);
            metadata.setViewerName(
                mc.thePlayer == null ? "" : mc.thePlayer.getName()
            );
            metadata.setViewerUuid(
                mc.thePlayer == null ? null : mc.thePlayer.getUniqueID()
            );
            configureRecordedPlayerMetadata();
            updateSnapshot(snapshot);
            for (PendingFrame frame : pending) {
                addPacket(frame.capturedAt, frame.frame);
                observeStoredFrame(frame.frame);
            }
            pendingFrames.clear();
            captureVisiblePlayers(now);
            captureLocalPlayerPackets(now);
            if (failed) {
                discard();
                throw new IOException(
                    failureMessage == null || failureMessage.trim().isEmpty()
                        ? "Unknown replay spool error."
                        : failureMessage
                );
            }
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
            tryWrite(new IoRunnable() {
                @Override
                public void run() throws IOException {
                    spool.appendPacket(
                        new ReplayPacketFrame(timestamp, frame.getClassName(), frame.getPayload())
                    );
                }
            });
            metadata.setDurationMs(Math.max(metadata.getDurationMs(), timestamp));
        }

        private void addChat(IChatComponent component, byte type) {
            int timestamp = toRelativeTime(System.currentTimeMillis());
            tryWrite(new IoRunnable() {
                @Override
                public void run() throws IOException {
                    spool.appendChat(
                        new ReplayChatEvent(
                            timestamp,
                            IChatComponent.Serializer.componentToJson(component),
                            type
                        )
                    );
                }
            });
        }

        private void captureTick(
            GameSnapshot snapshot,
            boolean captureLocalPlayer
        ) {
            long now = System.currentTimeMillis();
            captureVisiblePlayers(now);
            if (captureLocalPlayer) {
                captureLocalPlayerPackets(now);
            }
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
            tryWrite(new IoRunnable() {
                @Override
                public void run() throws IOException {
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
                !S0CPacketSpawnPlayer.class.getName().equals(frame.getClassName()) &&
                !S13PacketDestroyEntities.class.getName().equals(frame.getClassName())
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
                    for (
                        ReplayPacketFrame frame :
                        encodeVisiblePlayerSnapshot(player, playerInfo)
                    ) {
                        addPacket(capturedAt, frame);
                    }
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

        private ReplayRecordingSpool getSpool() {
            return spool;
        }

        private boolean hasPackets() {
            return spool.getPacketCount() > 0;
        }

        private boolean isFailed() {
            return failed;
        }

        private String getFailureMessage() {
            return failureMessage;
        }

        private void discard() {
            spool.discard();
        }

        private void tryWrite(IoRunnable action) {
            if (failed) {
                return;
            }
            try {
                action.run();
            } catch (IOException e) {
                failed = true;
                failureMessage = e.getMessage();
            }
        }
    }

    private interface IoRunnable {
        void run() throws IOException;
    }
}
