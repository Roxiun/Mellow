package com.roxiun.mellow.api.hypixel;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.GameStateManager;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.feature.bedwars.BedwarsFeatures;
import java.util.List;
import java.util.function.Consumer;
import net.hypixel.data.type.GameType;

public class HypixelFeatures {

    private static final HypixelFeatures INSTANCE = new HypixelFeatures();

    private final GameStateManager gameStateManager = new GameStateManager();
    private final BedwarsFeatures bedwarsFeatures = new BedwarsFeatures();

    private boolean initialized;

    public static HypixelFeatures getInstance() {
        return INSTANCE;
    }

    private HypixelFeatures() {

    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        gameStateManager.initialize();
        initialized = true;
    }

    public void onClientTick() {
        initialize();
        gameStateManager.onClientTick();
        bedwarsFeatures.onTick(gameStateManager.getSnapshot());
    }

    public void onChat(String message) {
        gameStateManager.onChat(message);
        bedwarsFeatures.onChat(message, gameStateManager.getSnapshot());
    }

    public void onWorldChange() {
        gameStateManager.onWorldChange();
        bedwarsFeatures.reset();
    }

    public void onDisconnect() { gameStateManager.onDisconnect(); bedwarsFeatures.reset(); }

    public GameSnapshot getGameSnapshot() {
        return gameStateManager.getSnapshot();
    }

    public void addGameStateListener(Consumer<GameSnapshot> listener) {
        gameStateManager.addListener(listener);
    }

    public PartyState getPartyState() {
        return gameStateManager.getSnapshot().getPartyState();
    }

    public boolean isInBedwarsMatch() {
        return gameStateManager.getSnapshot().isInBedwarsMatch();
    }

    public boolean isInBedwarsSession() {
        return gameStateManager.getSnapshot().isInBedwarsSession();
    }

    public boolean isInPregameLobby() {
        GameSnapshot snapshot = gameStateManager.getSnapshot();
        return snapshot.getGameType() == GameType.BEDWARS && snapshot.isPregame();
    }

    public boolean isInGameType(GameType gameType) {
        GameSnapshot snapshot = gameStateManager.getSnapshot();
        return snapshot.getGameType() == gameType && !snapshot.isLobby();
    }

    public String getMode() {
        return gameStateManager.getSnapshot().getMode();
    }

    public String getMap() {
        return gameStateManager.getSnapshot().getMap();
    }

    public String getEmeraldCounterText() {
        return bedwarsFeatures.getTimerState().getEmeraldDisplayText();
    }

    public String getDiamondCounterText() {
        return bedwarsFeatures.getTimerState().getDiamondDisplayText();
    }

    public int getEmeraldCounterTime() {
        return bedwarsFeatures.getTimerState().getEmeraldNext();
    }

    public int getEmeraldSpawnCount() {
        return bedwarsFeatures.getTimerState().getEmeraldCount();
    }

    public int getDiamondCounterTime() {
        return bedwarsFeatures.getTimerState().getDiamondNext();
    }

    public int getDiamondSpawnCount() {
        return bedwarsFeatures.getTimerState().getDiamondCount();
    }

    public List<String> getBedwarsUpgradesDisplayLines(
        boolean useShortNames,
        boolean useRomanNumerals,
        int headingRed,
        int headingGreen,
        int headingBlue,
        int headingAlpha,
        int textRed,
        int textGreen,
        int textBlue,
        int textAlpha
    ) {
        return bedwarsFeatures
            .getUpgradesService()
            .getDisplayLinesWithFormatting(
                useShortNames,
                useRomanNumerals,
                headingRed,
                headingGreen,
                headingBlue,
                headingAlpha,
                textRed,
                textGreen,
                textBlue,
                textAlpha
            );
    }
}
