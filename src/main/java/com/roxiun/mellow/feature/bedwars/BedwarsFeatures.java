package com.roxiun.mellow.feature.bedwars;

import com.roxiun.mellow.gamestate.GameSnapshot;

public class BedwarsFeatures {

    private final BedwarsTimerService timerService = new BedwarsTimerService();
    private final BedwarsUpgradesService upgradesService =
        new BedwarsUpgradesService();

    public void onTick(GameSnapshot snapshot) {
        timerService.update(snapshot);
        if (!snapshot.isInBedwarsSession()) {
            upgradesService.reset();
        }
    }

    public void onChat(String message, GameSnapshot snapshot) {
        boolean resetSignal =
            BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
            BedwarsChatSignalParser.isPregameCountdownMessage(message);

        if (resetSignal) {
            upgradesService.reset();
        }

        if (!snapshot.isInBedwarsSession()) {
            return;
        }

        if (BedwarsChatSignalParser.isPurchaseMessage(message)) {
            upgradesService.processPurchaseMessage(message);
        }

        if (BedwarsChatSignalParser.isTrapSignalMessage(message)) {
            upgradesService.processTrapTriggeredMessage(message);
        }
    }

    public void reset() {
        timerService.reset();
        upgradesService.reset();
    }

    public BedwarsTimerState getTimerState() {
        return timerService.getState();
    }

    public BedwarsUpgradesService getUpgradesService() {
        return upgradesService;
    }
}
