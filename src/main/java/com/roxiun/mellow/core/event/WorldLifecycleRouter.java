package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.stats.PregameStats;
import com.roxiun.mellow.platform.event.WorldEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;

public class WorldLifecycleRouter {

    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final NickUtils nickUtils;

    public WorldLifecycleRouter(
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        NickUtils nickUtils
    ) {
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.nickUtils = nickUtils;
    }

    @Subscribe
    public void onWorldLoad(WorldEvent.Load event) {
        numberDenicker.onWorldChange();
        pregameStats.onWorldChange();
        nickUtils.clearNicks();

        HypixelFeatures.getInstance().onWorldChange();
        ReplayManager.getInstance().onWorldChange();
    }
}
