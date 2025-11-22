package com.strawberry.statsify.events;

import com.strawberry.statsify.util.NickUtils;
import com.strawberry.statsify.util.NumberDenicker;
import com.strawberry.statsify.util.PregameStats;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WorldLoadHandler {

    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final NickUtils nickUtils;

    public WorldLoadHandler(
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        NickUtils nickUtils
    ) {
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.nickUtils = nickUtils;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        numberDenicker.onWorldChange();
        pregameStats.onWorldChange();
        nickUtils.clearNicks();
    }
}
