package com.roxiun.mellow.anticheat.check;

import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import com.roxiun.mellow.platform.event.TickEvent;

public abstract class Check {

    private final String name;
    private final String description;

    public Check(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void onPlayerTick(AnticheatManager manager, TickEvent.PlayerTickEvent event, ACPlayerData data) {}
}
