package com.roxiun.mellow.api.hypixel;

import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;

public class HypixelFeatures {

    //
    // thanks yedelo - https://github.com/Yedelo/YedelMod/blob/master/src/main/java/at/yedel/yedelmod/features/major/BedwarsFeatures.java
    private static final HypixelFeatures INSTANCE = new HypixelFeatures();

    private boolean inBedwars;
    private int timeSinceGameStart = 0; // In ticks
    private int secondsSinceGameStart = 0; // In seconds
    private int timeUntilEmerald = -1;
    private int emeraldSpawnCount = 0;
    private int timeUntilDiamond = -1;
    private int diamondSpawnCount = 0;
    private String mode = "";
    private int emeraldStage = 1;
    private int diamondStage = 1;
    private String emeraldCounterTimeText = "";
    private String diamondCounterTimeText = "";
    private int tickCounter = 0; // Counter to track ticks for second conversion

    public static HypixelFeatures getInstance() {
        return INSTANCE;
    }

    private HypixelFeatures() {
        HypixelModAPI.getInstance().registerHandler(
            ClientboundLocationPacket.class,
            this::handleLocationPacket
        );
    }

    private void handleLocationPacket(ClientboundLocationPacket packet) {
        boolean wasInBedwars = inBedwars;
        inBedwars =
            packet.getServerType().isPresent() &&
            packet.getServerType().get() == GameType.BEDWARS &&
            !packet.getLobbyName().isPresent();

        // Reset if we just left a Bedwars game
        if (wasInBedwars && !inBedwars) {
            resetEmeraldTimer();
        }
    }

    // Method to be called every tick to update the emerald timer
    public void updateEmeraldTimer() {
        if (!inBedwars) {
            return;
        }

        // Increment the tick counter every tick
        timeSinceGameStart++;

        // Only update once per second (every 20 ticks)
        tickCounter++;
        if (tickCounter % 20 != 0) {
            // Only update the emerald timer logic once per second
            return;
        }

        if (secondsSinceGameStart == 1) {
            emeraldStage = 1;
            timeUntilEmerald = 30;
        } else if (secondsSinceGameStart == 36) {
            // 36 seconds = 720 ticks
            emeraldStage = 2;
            timeUntilEmerald = 0;
        } else if (secondsSinceGameStart == 72) {
            // 72 seconds = 1440 ticks
            emeraldStage = 3;
            timeUntilEmerald = 0;
        }

        if (timeUntilEmerald == 0) {
            emeraldSpawnCount++;
            if (emeraldStage == 1) {
                if ("doubles".equals(mode)) {
                    timeUntilEmerald = 65;
                } else if ("fours".equals(mode)) {
                    timeUntilEmerald = 55;
                }
            } else if (emeraldStage == 2) {
                if ("doubles".equals(mode)) {
                    timeUntilEmerald = 50;
                } else if ("fours".equals(mode)) {
                    timeUntilEmerald = 40;
                }
            } else if (emeraldStage == 3) {
                if ("doubles".equals(mode)) {
                    timeUntilEmerald = 35;
                } else if ("fours".equals(mode)) {
                    timeUntilEmerald = 27;
                }
            }
        }

        // Update diamond timer logic - diamonds start after 16 seconds (common timing for diamond generation)
        if (secondsSinceGameStart == 16) {
            diamondStage = 1;
            timeUntilDiamond = 30; // Diamond tier 1: 30 seconds (same for doubles/fours)
        } else if (secondsSinceGameStart == 46) {
            // 16 + 30 = 46 (after first diamond spawn)
            if (diamondStage == 1) {
                diamondStage = 2;
                timeUntilDiamond = 23; // Diamond tier 2: 23 seconds
            }
        } else if (secondsSinceGameStart == 69) {
            // 46 + 23 = 69 (after second diamond spawn)
            if (diamondStage == 2) {
                diamondStage = 3;
                timeUntilDiamond = 12; // Diamond tier 3: 12 seconds
            }
        }

        // Check if diamond should spawn
        if (timeUntilDiamond == 0) {
            diamondSpawnCount++;
            if (diamondStage == 1) {
                timeUntilDiamond = 30; // T1: 30 seconds
            } else if (diamondStage == 2) {
                timeUntilDiamond = 23; // T2: 23 seconds
            } else if (diamondStage == 3) {
                timeUntilDiamond = 12; // T3: 12 seconds
            }
        }

        // Update the text for display
        emeraldCounterTimeText = getFormattedEmeraldCountText();
        diamondCounterTimeText = getFormattedDiamondCountText();

        // Only decrease the timer if it's greater than 0 to avoid negative values
        if (timeUntilEmerald > 0) {
            timeUntilEmerald--;
        }
        if (timeUntilDiamond > 0) {
            timeUntilDiamond--;
        }

        // Increment seconds counter after all processing, matching JS behavior
        secondsSinceGameStart++;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void resetEmeraldTimer() {
        timeSinceGameStart = 0;
        secondsSinceGameStart = 0;
        timeUntilEmerald = -1;
        emeraldSpawnCount = 0;
        timeUntilDiamond = -1;
        diamondSpawnCount = 0;
        mode = "";
        emeraldStage = 1;
        diamondStage = 1;
        emeraldCounterTimeText = "";
        diamondCounterTimeText = "";
        tickCounter = 0;
    }

    public void startNewGame() {
        resetEmeraldTimer();
        inBedwars = true;
        // Since the ChatHandler waits 1 second before calling this (like in JS version),
        // we start at 1 second to match the JavaScript behavior
        secondsSinceGameStart = 1; // This will trigger the stage 1 condition on the next update
    }

    public boolean isInBedwars() {
        return inBedwars;
    }

    public int getEmeraldCounterTime() {
        return timeUntilEmerald;
    }

    public int getEmeraldSpawnCount() {
        return emeraldSpawnCount;
    }

    public String getEmeraldCounterText() {
        return emeraldCounterTimeText;
    }

    public String getDiamondCounterText() {
        return diamondCounterTimeText;
    }

    private String getFormattedEmeraldCountText() {
        String timeString;
        if (timeUntilEmerald <= 0) {
            timeString = "0";
        } else {
            timeString = String.valueOf(timeUntilEmerald);
        }

        return "(§f" + emeraldSpawnCount + "§a): §7" + timeString + "s";
    }

    private String getFormattedDiamondCountText() {
        String timeString;
        if (timeUntilDiamond <= 0) {
            timeString = "0";
        } else {
            timeString = String.valueOf(timeUntilDiamond);
        }

        return "(§f" + diamondSpawnCount + "§b): §7" + timeString + "s";
    }
}
