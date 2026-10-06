package com.roxiun.mellow.gamestate;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.roxiun.mellow.gamestate.query.GameContext;
import com.roxiun.mellow.util.scoreboard.ScoreboardUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.hypixel.data.type.GameType;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

public class GameStateManager implements GameContext {

    private static final Pattern GENERIC_TIMER_PATTERN = Pattern.compile(
        "(?i).+\\s+(?:in\\s+)?\\d{1,2}:\\d{2}"
    );
    private static final Set<String> BEDWARS_STAGE_EVENTS = new HashSet<>();

    static {
        BEDWARS_STAGE_EVENTS.add("diamond ii");
        BEDWARS_STAGE_EVENTS.add("emerald ii");
        BEDWARS_STAGE_EVENTS.add("diamond iii");
        BEDWARS_STAGE_EVENTS.add("emerald iii");
        BEDWARS_STAGE_EVENTS.add("bed gone");
        BEDWARS_STAGE_EVENTS.add("beds gone");
        BEDWARS_STAGE_EVENTS.add("bed destroyed");
        BEDWARS_STAGE_EVENTS.add("beds destroyed");
        BEDWARS_STAGE_EVENTS.add("sudden death");
        BEDWARS_STAGE_EVENTS.add("game end");
        BEDWARS_STAGE_EVENTS.add("end game");
    }

    private final AtomicReference<GameSnapshot> snapshot =
        new AtomicReference<>(GameSnapshot.empty());
    private final CopyOnWriteArrayList<Consumer<GameSnapshot>> listeners =
        new CopyOnWriteArrayList<>();

    private boolean initialized;
    private int tickCounter;
    private long lastPartyRequestMillis;

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        HypixelModAPI api = HypixelModAPI.getInstance();
        api.createHandler(ClientboundLocationPacket.class, this::handleLocationPacket);
        api.createHandler(ClientboundPartyInfoPacket.class, this::handlePartyInfoPacket);
        api.subscribeToEventPacket(ClientboundLocationPacket.class);

        initialized = true;
    }

    @Override
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

        GameSnapshot current = snapshot.get();
        boolean onHypixel = HypixelUtils.INSTANCE.isHypixel();

        if (!onHypixel) {
            if (current.isOnHypixel()) {
                publish(GameSnapshot.empty());
            }
            return;
        }

        ScoreboardState scoreboard = readScoreboard();
        GameType resolvedGameType = resolveGameType(current, scoreboard);
        boolean resolvedLobby = resolveLobby(current, resolvedGameType, scoreboard);
        boolean pregame = detectBedwarsPregame(
            resolvedGameType,
            resolvedLobby,
            scoreboard.lines
        );

        PregameReason pregameReason = pregame &&
        current.getGameType() == null
            ? PregameReason.FALLBACK
            : pregame
            ? PregameReason.PLAYERS_LINE
            : PregameReason.NONE;

        GameSnapshot next = new GameSnapshot(
            true,
            current.getServerName(),
            resolvedGameType,
            current.getMode(),
            current.getMap(),
            resolvedLobby,
            pregame,
            pregameReason,
            scoreboard.title,
            scoreboard.lines,
            current.getPartyState(),
            System.currentTimeMillis(),
            current.getStateVersion() + 1
        );

        publish(next);
        requestPartyInfo(false);
    }

    public void onWorldChange() {
        publish(GameSnapshot.empty());
    }

    private void handleLocationPacket(ClientboundLocationPacket packet) {
        GameSnapshot current = snapshot.get();

        GameType gameType = null;
        if (packet.getServerType().isPresent()) {
            ServerType serverType = packet.getServerType().get();
            if (serverType instanceof GameType) {
                gameType = (GameType) serverType;
            }
        }

        boolean lobby = packet.getLobbyName().isPresent();
        ScoreboardState scoreboard = readScoreboard();
        GameType resolvedGameType = gameType == null
            ? inferGameTypeFromScoreboard(scoreboard)
            : gameType;
        boolean resolvedLobby = lobby;
        if (resolvedGameType == GameType.BEDWARS && gameType == null) {
            resolvedLobby = inferBedwarsLobby(scoreboard.lines);
        }
        boolean pregame = detectBedwarsPregame(
            resolvedGameType,
            resolvedLobby,
            scoreboard.lines
        );

        PregameReason pregameReason = pregame
            ? PregameReason.PLAYERS_LINE
            : (resolvedGameType == GameType.BEDWARS && !resolvedLobby
                ? PregameReason.MODAPI_TRANSITION
                : PregameReason.NONE);

        GameSnapshot next = new GameSnapshot(
            true,
            packet.getServerName(),
            resolvedGameType,
            packet.getMode().orElse(""),
            packet.getMap().orElse(""),
            resolvedLobby,
            pregame,
            pregameReason,
            scoreboard.title,
            scoreboard.lines,
            current.getPartyState(),
            System.currentTimeMillis(),
            current.getStateVersion() + 1
        );

        publish(next);
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

    private boolean detectBedwarsPregame(
        GameType gameType,
        boolean lobby,
        List<String> lines
    ) {
        if (gameType != GameType.BEDWARS || lobby || lines.isEmpty()) {
            return false;
        }

        for (String line : lines) {
            String normalized = line
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
            if (
                normalized.startsWith("players:") ||
                normalized.startsWith("players ") ||
                normalized.equals("players")
            ) {
                return true;
            }
        }

        return false;
    }

    private GameType resolveGameType(
        GameSnapshot current,
        ScoreboardState scoreboard
    ) {
        if (current.getGameType() != null) {
            return current.getGameType();
        }
        return inferGameTypeFromScoreboard(scoreboard);
    }

    private GameType inferGameTypeFromScoreboard(ScoreboardState scoreboard) {
        String title = scoreboard.title == null
            ? ""
            : scoreboard.title.toLowerCase(Locale.ROOT);
        if (title.contains("bed wars")) {
            return GameType.BEDWARS;
        }
        if (title.contains("skywars") || title.contains("sky wars")) {
            return GameType.SKYWARS;
        }
        if (title.contains("duels") || title.contains("duel")) {
            return GameType.DUELS;
        }
        if (title.contains("build battle")) {
            return GameType.BUILD_BATTLE;
        }
        if (title.contains("tnt games") || title.contains("tnt run")) {
            return GameType.TNTGAMES;
        }
        return null;
    }

    private boolean resolveLobby(
        GameSnapshot current,
        GameType resolvedGameType,
        ScoreboardState scoreboard
    ) {
        if (resolvedGameType != GameType.BEDWARS) {
            return current.isLobby();
        }

        boolean hasPlayersLine = hasPlayersLine(scoreboard.lines);
        boolean hasStageTimer = hasBedwarsStageTimer(scoreboard.lines);

        if (hasPlayersLine || hasStageTimer) {
            return false;
        }

        if (current.getGameType() == null) {
            return true;
        }

        return current.isLobby();
    }

    private boolean inferBedwarsLobby(List<String> lines) {
        return !hasPlayersLine(lines) && !hasBedwarsStageTimer(lines);
    }

    private boolean hasPlayersLine(List<String> lines) {
        for (String line : lines) {
            String normalized = line
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
            if (
                normalized.startsWith("players:") ||
                normalized.startsWith("players ") ||
                normalized.equals("players")
            ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBedwarsStageTimer(List<String> lines) {
        for (String line : lines) {
            String normalized = line
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
            if (!GENERIC_TIMER_PATTERN.matcher(normalized).matches()) {
                continue;
            }

            String eventName = normalized;
            int inIndex = normalized.indexOf(" in ");
            if (inIndex >= 0) {
                eventName = normalized.substring(0, inIndex).trim();
            } else {
                int timerStart = normalized.lastIndexOf(' ');
                if (timerStart > 0) {
                    eventName = normalized.substring(0, timerStart).trim();
                }
            }
            if (eventName.startsWith("next event:")) {
                eventName = eventName.substring("next event:".length()).trim();
            }
            if (BEDWARS_STAGE_EVENTS.contains(eventName)) {
                return true;
            }
        }
        return false;
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
            } catch (Exception ignored) {}
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
