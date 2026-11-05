package com.roxiun.mellow.gamestate;

//? if ornithe {
import org.polyfrost.oneconfig.api.hypixel.v1.HypixelUtils;
//?} else {
/*import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
*///?}
import com.roxiun.mellow.util.scoreboard.ScoreboardUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

public class GameStateManager {

    private final AtomicReference<GameSnapshot> snapshot =
        new AtomicReference<>(GameSnapshot.empty());
    private final CopyOnWriteArrayList<Consumer<GameSnapshot>> listeners =
        new CopyOnWriteArrayList<>();

    private long sessionId;
    private boolean awaitingLocation;
    private boolean locationPendingWorld;
    private GameType locationGameType;
    private boolean locationLobby;
    private boolean initialized;
    private int tickCounter;
    private long lastPartyRequestMillis;

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        //? if ornithe {
        // Register OneConfig's hello/disconnect handlers before the first server connection.
        HypixelUtils.getLocation();
        //?}
        HypixelModAPI api = HypixelModAPI.getInstance();
        //? if ornithe {
        api.registerHandler(ClientboundLocationPacket.class, this::handleLocationPacket);
        api.registerHandler(ClientboundPartyInfoPacket.class, this::handlePartyInfoPacket);
        //?} else {
        /*api.createHandler(ClientboundLocationPacket.class, this::handleLocationPacket);
        api.createHandler(ClientboundPartyInfoPacket.class, this::handlePartyInfoPacket);
        *///?}
        api.subscribeToEventPacket(ClientboundLocationPacket.class);

        initialized = true;
    }

    public GameSnapshot getSnapshot() {
        return snapshot.get();
    }

    public void addListener(Consumer<GameSnapshot> listener) {
        listeners.add(listener);
    }

    public void onClientTick() {
        initialize();

        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }

        //? if ornithe {
        if (!HypixelUtils.isHypixel()) {
        //?} else {
        /*if (!HypixelUtils.INSTANCE.isHypixel()) {
        *///?}
            if (snapshot.get().isOnHypixel() && net.minecraft.client.Minecraft.getMinecraft().getNetHandler() == null) onDisconnect();
            return;
        }
        if (needsScoreboard(snapshot.get())) updateFromScoreboard();
        requestPartyInfo(false);
    }

    public void onWorldChange() {
        // A location notification may precede world load. Consume it once, never skip the world reset.
        boolean keepLocation = locationPendingWorld;
        locationPendingWorld = false;
        sessionId++;
        awaitingLocation = !keepLocation;
        GameSnapshot current = snapshot.get();
        if (!keepLocation) { locationGameType = null; locationLobby = false; }
        publish(new GameSnapshot(current.isOnHypixel(), keepLocation ? current.getServerName() : "",
            locationGameType, keepLocation ? current.getMode() : "", keepLocation ? current.getMap() : "",
            locationLobby ? GamePhase.LOBBY : GamePhase.UNKNOWN, "", java.util.Collections.emptyList(),
            current.getPartyState(), current.getStateVersion() + 1, sessionId));
    }

    public void onDisconnect() {
        locationPendingWorld = false;
        sessionId++;
        locationGameType = null;
        locationLobby = false;
        awaitingLocation = true;
        lastPartyRequestMillis = 0;
        publish(new GameSnapshot(false, "", null, "", "", GamePhase.UNKNOWN, "",
            java.util.Collections.emptyList(), PartyState.empty(), snapshot.get().getStateVersion() + 1, sessionId));
    }

    public void onChat(String message) {
        GameSnapshot current = snapshot.get();
        if (current.isOnHypixel() && current.getGameType() == GameType.BEDWARS
            && current.getPhase() != GamePhase.LOBBY
            && (com.roxiun.mellow.feature.bedwars.BedwarsChatSignalParser.isBedwarsStartMessage(message)
                || com.roxiun.mellow.feature.bedwars.BedwarsChatSignalParser.isBedwarsRespawnMessage(message))) {
            publish(current.withPhase(GamePhase.LIVE));
        } else if (current.isOnHypixel() && current.getGameType() == GameType.BEDWARS
            && current.getPhase() != GamePhase.LOBBY
            && com.roxiun.mellow.feature.bedwars.BedwarsChatSignalParser.isPregameCountdownMessage(message)) {
            if (current.getPhase() == GamePhase.LIVE) sessionId++;
            publish(current.withPhase(GamePhase.PREGAME, sessionId));
        }
    }

    /** Location packets settle ordinary games; only unresolved context and Bed Wars need polling. */
    static boolean needsScoreboard(GameSnapshot current) {
        if (current.isLobby()) return false;
        if (current.getGameType() == null || current.getGameType() == GameType.BEDWARS) return true;
        if (current.getMode() != null && !current.getMode().isEmpty()) return false;
        return current.getGameType() == GameType.TNTGAMES && current.getStatsGame() == null
            || current.getGameType() == GameType.DUELS && "overall".equals(current.getStatsMode());
    }

    private void updateFromScoreboard() {
        ScoreboardState board = readScoreboard();
        updateFromScoreboard(board.title, board.lines);
    }

    void updateFromScoreboard(String title, List<String> lines) {
        GameSnapshot current = snapshot.get();
        ScoreboardObservation observation = ScoreboardObservation.parse(title, lines,
            current.getPhase() != GamePhase.LIVE);
        GameType type = locationGameType != null ? locationGameType : observation.gameType != null ? observation.gameType : current.getGameType();
        GamePhase phase = type == GameType.BEDWARS
            ? ScoreboardObservation.resolve(current.getPhase(), locationLobby, observation) : current.getPhase();
        publish(new GameSnapshot(true, current.getServerName(), type, current.getMode(), current.getMap(), phase,
            title, lines, current.getPartyState(), current.getStateVersion() + 1, sessionId,
            observation, System.currentTimeMillis()));
    }

    private void handleLocationPacket(ClientboundLocationPacket packet) {
        acceptLocation(packet);
        requestPartyInfo(true);
    }

    void acceptLocation(ClientboundLocationPacket packet) {
        GameSnapshot current = snapshot.get();
        boolean changed = !packet.getServerName().equals(current.getServerName());
        if (changed && !awaitingLocation) {
            locationPendingWorld = true;
            sessionId++;
        }
        awaitingLocation = false;
        locationGameType = packet.getServerType().isPresent() && packet.getServerType().get() instanceof GameType
            ? (GameType) packet.getServerType().get() : null;
        locationLobby = packet.getLobbyName().isPresent();
        // Do not combine a new location with a previous world's sidebar.
        boolean resetBoard = changed || locationGameType != current.getGameType()
            || !packet.getMode().orElse("").equals(current.getMode()) || locationLobby;
        GamePhase phase = locationLobby ? GamePhase.LOBBY : resetBoard ? GamePhase.UNKNOWN : current.getPhase();
        publish(new GameSnapshot(true, packet.getServerName(), locationGameType,
            packet.getMode().orElse(""), packet.getMap().orElse(""), phase,
            resetBoard ? "" : current.getScoreboardTitle(), resetBoard ? java.util.Collections.emptyList() : current.getScoreboardLines(),
            current.getPartyState(), current.getStateVersion() + 1, sessionId,
            resetBoard ? ScoreboardObservation.parse("", java.util.Collections.emptyList()) : current.getObservation(),
            resetBoard ? System.currentTimeMillis() : current.getScoreboardObservedAt()));
    }

    private void handlePartyInfoPacket(ClientboundPartyInfoPacket packet) {
        PartyState partyState;

        if (!packet.isInParty()) {
            partyState = PartyState.empty();
        } else {
            Map<UUID, PartyState.PartyRole> members = new HashMap<>();
            UUID leader = null;

            for (Map.Entry<UUID, ClientboundPartyInfoPacket.PartyMember> entry : packet
                .getMemberMap()
                .entrySet()) {
                PartyState.PartyRole role = toRole(entry.getValue().getRole());
                if (role == PartyState.PartyRole.LEADER) {
                    leader = entry.getKey();
                }
                members.put(entry.getKey(), role);
            }

            partyState = new PartyState(true, leader, members);
        }

        publish(snapshot.get().withPartyState(partyState));
    }

    private PartyState.PartyRole toRole(
        ClientboundPartyInfoPacket.PartyRole role
    ) {
        switch (role) {
            case LEADER:
                return PartyState.PartyRole.LEADER;
            case MOD:
                return PartyState.PartyRole.MOD;
            case MEMBER:
            default:
                return PartyState.PartyRole.MEMBER;
        }
    }

    private void requestPartyInfo(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastPartyRequestMillis < 5000) {
            return;
        }

        try {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
            lastPartyRequestMillis = now;
        } catch (Exception ignored) {}
    }

    private ScoreboardState readScoreboard() {
        return new ScoreboardState(
            ScoreboardUtils.getSidebarTitle(),
            ScoreboardUtils.getSidebarLines()
        );
    }

    private void publish(GameSnapshot next) {
        GameSnapshot current = snapshot.get();
        if (current.hasSameState(next)) {
            return;
        }

        snapshot.set(next);
        for (Consumer<GameSnapshot> listener : listeners) {
            try {
                listener.accept(next);
            } catch (Exception error) {
                org.apache.logging.log4j.LogManager.getLogger("Mellow").warn("Game-state listener failed", error);
            }
        }
    }

    private static class ScoreboardState {

        private final String title;
        private final List<String> lines;

        private ScoreboardState(String title, List<String> lines) {
            this.title = title == null ? "" : title;
            this.lines = lines;
        }
    }
}
