package com.roxiun.mellow.feature.requestpopup;

//? if ornithe {
import org.polyfrost.oneconfig.api.hypixel.v1.HypixelUtils;
//?} else {
/*import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
*///?}
import com.roxiun.mellow.config.MellowOneConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

public class RequestPopupManager {

    public static final class ActiveRequest {

        private final RequestType requestType;
        private final String fromPlayer;
        private final String displayText;
        private final long startedAtMillis;
        private final long expiresAtMillis;

        private ActiveRequest(
            RequestType requestType,
            String fromPlayer,
            String displayText,
            long startedAtMillis,
            long expiresAtMillis
        ) {
            this.requestType = requestType;
            this.fromPlayer = fromPlayer;
            this.displayText = displayText;
            this.startedAtMillis = startedAtMillis;
            this.expiresAtMillis = expiresAtMillis;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public String getFromPlayer() {
            return fromPlayer;
        }

        public String getDisplayText() {
            return displayText;
        }

        public long getStartedAtMillis() {
            return startedAtMillis;
        }

        public long getExpiresAtMillis() {
            return expiresAtMillis;
        }

        public float getRemainingProgress(long nowMillis) {
            long totalDuration = expiresAtMillis - startedAtMillis;
            if (totalDuration <= 0L) {
                return 0.0F;
            }
            long remaining = expiresAtMillis - nowMillis;
            if (remaining <= 0L) {
                return 0.0F;
            }
            if (remaining >= totalDuration) {
                return 1.0F;
            }
            return (float) remaining / (float) totalDuration;
        }
    }

    private static final class QueuedRequest {

        private final RequestType requestType;
        private final String fromPlayer;

        private QueuedRequest(RequestType requestType, String fromPlayer) {
            this.requestType = requestType;
            this.fromPlayer = fromPlayer;
        }
    }

    private static final int MAX_PENDING_QUEUE = 10;
    private static final int MIN_DURATION_SECONDS = 2;
    private static final int MAX_DURATION_SECONDS = 15;
    private static final int DEFAULT_DURATION_SECONDS = 5;
    private static final ResourceLocation PLING_SOUND = new ResourceLocation(
        "note.pling"
    );

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final Deque<QueuedRequest> queue = new ArrayDeque<>();
    private ActiveRequest activeRequest;

    public RequestPopupManager(MellowOneConfig config) {
        this.config = config;
    }

    public synchronized void enqueue(RequestType requestType, String fromPlayer) {
        if (requestType == null) {
            return;
        }

        String normalizedPlayer = normalizePlayerName(fromPlayer);
        if (normalizedPlayer == null) {
            return;
        }

        if (!isEligibleType(requestType)) {
            return;
        }

        if (activeRequest == null) {
            activeRequest = activate(
                new QueuedRequest(requestType, normalizedPlayer),
                System.currentTimeMillis()
            );
        } else {
            if (queue.size() >= MAX_PENDING_QUEUE) {
                queue.pollFirst();
            }
            queue.addLast(new QueuedRequest(requestType, normalizedPlayer));
        }

        playPingSoundIfEnabled();
    }

    public synchronized ActiveRequest getActiveRequest() {
        if (!isFeatureAvailable()) {
            clear();
            return null;
        }

        long now = System.currentTimeMillis();
        expireActiveIfNeeded(now);
        activateNext(now);
        return activeRequest;
    }

    public synchronized void handleDecision(boolean accept) {
        if (!isFeatureAvailable()) {
            clear();
            return;
        }

        long now = System.currentTimeMillis();
        expireActiveIfNeeded(now);
        activateNext(now);

        if (activeRequest == null) {
            return;
        }

        String command = activeRequest
            .getRequestType()
            .buildCommand(accept, activeRequest.getFromPlayer());
        if (command != null && !command.isEmpty() && mc != null && mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(command);
        }

        activeRequest = null;
        activateNext(now);
    }

    public synchronized void clear() {
        activeRequest = null;
        queue.clear();
    }

    private void expireActiveIfNeeded(long nowMillis) {
        if (
            activeRequest != null && nowMillis >= activeRequest.getExpiresAtMillis()
        ) {
            activeRequest = null;
        }
    }

    private void activateNext(long nowMillis) {
        if (activeRequest != null) {
            return;
        }

        while (!queue.isEmpty()) {
            QueuedRequest next = queue.pollFirst();
            if (next == null || !isEligibleType(next.requestType)) {
                continue;
            }
            activeRequest = activate(next, nowMillis);
            return;
        }
    }

    private ActiveRequest activate(QueuedRequest request, long nowMillis) {
        long durationMillis = resolveDurationSeconds() * 1000L;
        return new ActiveRequest(
            request.requestType,
            request.fromPlayer,
            request.requestType.buildDisplayText(request.fromPlayer),
            nowMillis,
            nowMillis + durationMillis
        );
    }

    private int resolveDurationSeconds() {
        int configured = config == null
            ? DEFAULT_DURATION_SECONDS
            : config.requestPopupDurationSeconds;
        if (configured < MIN_DURATION_SECONDS) {
            return MIN_DURATION_SECONDS;
        }
        if (configured > MAX_DURATION_SECONDS) {
            return MAX_DURATION_SECONDS;
        }
        return configured;
    }

    private boolean isEligibleType(RequestType requestType) {
        if (!isFeatureAvailable()) {
            return false;
        }

        if (requestType == RequestType.FRIEND) {
            return config.friendRequestPopupsEnabled;
        }

        if (requestType == RequestType.PARTY) {
            return config.partyInvitePopupsEnabled;
        }

        return false;
    }

    private boolean isFeatureAvailable() {
        return (
            config != null &&
            config.requestPopupsEnabled &&
            //? if ornithe {
            HypixelUtils.isHypixel()
            //?} else {
            /*HypixelUtils.INSTANCE.isHypixel()
            *///?}
        );
    }

    private String normalizePlayerName(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 16) {
            return null;
        }

        return trimmed;
    }

    private void playPingSoundIfEnabled() {
        if (config == null || !config.requestPopupSoundEnabled) {
            return;
        }
        if (mc == null || mc.thePlayer == null) {
            return;
        }

        SoundHandler soundHandler = mc.getSoundHandler();
        if (soundHandler == null) {
            return;
        }

        soundHandler.playSound(
            new PositionedSoundRecord(
                PLING_SOUND,
                1.0F,
                1.8F,
                (float) mc.thePlayer.posX,
                (float) mc.thePlayer.posY,
                (float) mc.thePlayer.posZ
            )
        );
    }
}
