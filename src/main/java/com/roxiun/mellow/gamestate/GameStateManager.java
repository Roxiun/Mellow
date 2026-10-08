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
    private long locationBeforeWorldAt;
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
        updateFromScoreboard();
        requestPartyInfo(false);
    }

    public void onWorldChange() {
        if (locationBeforeWorldAt > 0 && System.currentTimeMillis() - locationBeforeWorldAt < 2000) {
            locationBeforeWorldAt = 0;
            return;
        }
        sessionId++;
        awaitingLocation = true;
        locationGameType = null;
        locationLobby = false;
        GameSnapshot current = snapshot.get();
        publish(new GameSnapshot(current.isOnHypixel(), "", null, "", "", GamePhase.UNKNOWN,
            "", java.util.Collections.emptyList(), current.getPartyState(), current.getStateVersion() + 1, sessionId));
    }

    public void onDisconnect() {
        locationBeforeWorldAt = 0;
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
        }
    }

    private void updateFromScoreboard() {
        GameSnapshot current = snapshot.get();
        ScoreboardState board = readScoreboard();
        ScoreboardObservation observation = ScoreboardObservation.parse(board.title, board.lines);
        GameType type = locationGameType != null ? locationGameType : observation.gameType != null ? observation.gameType : current.getGameType();
        GamePhase phase = ScoreboardObservation.resolve(current.getPhase(), locationLobby, observation);
        publish(new GameSnapshot(true, current.getServerName(), type, current.getMode(), current.getMap(), phase,
            board.title, board.lines, current.getPartyState(), current.getStateVersion() + 1, sessionId));
    }

    private void handleLocationPacket(ClientboundLocationPacket packet) {
        GameSnapshot current = snapshot.get();
        boolean changed = !packet.getServerName().equals(current.getServerName());
        if (changed && !awaitingLocation) {
            sessionId++;
            locationBeforeWorldAt = System.currentTimeMillis();
        } else {
            locationBeforeWorldAt = 0;
        }
        awaitingLocation = false;
        locationGameType = packet.getServerType().isPresent() && packet.getServerType().get() instanceof GameType
            ? (GameType) packet.getServerType().get() : null;
        locationLobby = packet.getLobbyName().isPresent();
        // Do not combine a new location with a previous world's sidebar.
        GamePhase phase = locationLobby ? GamePhase.LOBBY : changed ? GamePhase.UNKNOWN : current.getPhase();
        publish(new GameSnapshot(true, packet.getServerName(), locationGameType,
            packet.getMode().orElse(""), packet.getMap().orElse(""), phase,
            changed ? "" : current.getScoreboardTitle(), changed ? java.util.Collections.emptyList() : current.getScoreboardLines(),
            current.getPartyState(), current.getStateVersion() + 1, sessionId));
        requestPartyInfo(true);
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
