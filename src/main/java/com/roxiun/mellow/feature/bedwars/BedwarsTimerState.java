package com.roxiun.mellow.feature.bedwars;

public class BedwarsTimerState {

    private static final BedwarsTimerState EMPTY = new BedwarsTimerState(0, 0, 0, 0);

    private final int emeraldCount;
    private final int emeraldNext;
    private final int diamondCount;
    private final int diamondNext;

    public BedwarsTimerState(
        int emeraldCount,
        int emeraldNext,
        int diamondCount,
        int diamondNext
    ) {
        this.emeraldCount = emeraldCount;
        this.emeraldNext = emeraldNext;
        this.diamondCount = diamondCount;
        this.diamondNext = diamondNext;
    }

    public static BedwarsTimerState empty() {
        return EMPTY;
    }

    public int getEmeraldCount() {
        return emeraldCount;
    }

    public int getEmeraldNext() {
        return emeraldNext;
    }

    public int getDiamondCount() {
        return diamondCount;
    }

    public int getDiamondNext() {
        return diamondNext;
    }

    public String getEmeraldDisplayText() {
        return "§2(§f" + emeraldCount + "§2): §7" + Math.max(0, emeraldNext) + "s";
    }

    public String getDiamondDisplayText() {
        return "§b(§f" + diamondCount + "§b): §7" + Math.max(0, diamondNext) + "s";
    }
}
