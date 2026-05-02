package com.roxiun.mellow;

import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.api.aurora.AuroraPingService;
import com.roxiun.mellow.api.aurora.AuroraWinstreakService;
import com.roxiun.mellow.api.frosty.FrostyApi;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.luna.LunaPingService;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.AbyssApi;
import com.roxiun.mellow.api.provider.HypixelPublicApi;
import com.roxiun.mellow.api.provider.NadeshikoApi;
import com.roxiun.mellow.api.provider.ProviderManager;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.seraph.SeraphClientCacheService;
import com.roxiun.mellow.api.seraph.SeraphPingService;
import com.roxiun.mellow.api.urchin.UrchinApi;
import com.roxiun.mellow.autoupdate.ModrinthUpdater;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.commands.*;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.event.ChatEventRouter;
import com.roxiun.mellow.core.event.ClientTickRouter;
import com.roxiun.mellow.core.event.NametagColorRouter;
import com.roxiun.mellow.core.event.RequestPopupRouter;
import com.roxiun.mellow.core.event.TabOverlayInputRouter;
import com.roxiun.mellow.core.event.TabOverlayRouter;
import com.roxiun.mellow.core.event.WorldLifecycleRouter;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.party.PartyBlacklistWarningService;
import com.roxiun.mellow.feature.requestpopup.RequestPopupManager;
import com.roxiun.mellow.feature.requestpopup.RequestPopupService;
import com.roxiun.mellow.feature.replay.ReplayHudRouter;
import com.roxiun.mellow.feature.replay.ReplayInputRouter;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.stats.InGameTabStatsSyncService;
import com.roxiun.mellow.feature.stats.PregameStats;
import com.roxiun.mellow.feature.stats.ProviderHealthWarningService;
import com.roxiun.mellow.feature.stats.StatsChecker;
import com.roxiun.mellow.feature.tags.TagUtils;
import com.roxiun.mellow.util.annoylist.AnnoylistManager;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = Mellow.MODID, name = Mellow.NAME, version = Mellow.VERSION)
public class Mellow {

    public static final String MODID = "mellow";
    public static final String NAME = "Mellow";
    public static final String VERSION = "6.0.0";

    public static MellowOneConfig config;
    public static final Map<String, TabStats> tabStats = new ConcurrentHashMap<>();
    public static NickUtils nickUtils;

    public static AuroraPingService auroraPingService;
    public static AuroraWinstreakService auroraWinstreakService;
    public static AuroraApi auroraApi;
    public static FrostyApi frostyApi;
    public static LunaPingService lunaPingService;
    public static SeraphClientCacheService seraphClientCacheService;
    public static SeraphPingService seraphPingService;
    public static MojangApi mojangApi;
    public static UrchinApi urchinApi;
    public static SeraphApi seraphApi;
    public static PlayerCache playerCache;
    public static BlacklistManager blacklistManager;
    public static AnnoylistManager annoylistManager;
    public static TagIgnoreManager tagIgnoreManager;
    private static AnticheatManager anticheatManager;

    private ProviderManager providerManager;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        config = new MellowOneConfig();
        ModrinthUpdater.init(config);
        ProviderHealthWarningService.init(config);

        HypixelFeatures.getInstance().initialize();

        anticheatManager = new AnticheatManager(this);
        blacklistManager = new BlacklistManager();
        annoylistManager = new AnnoylistManager();
        tagIgnoreManager = new TagIgnoreManager();

        auroraPingService = new AuroraPingService();
        auroraWinstreakService = new AuroraWinstreakService();
        lunaPingService = new LunaPingService();
        seraphPingService = new SeraphPingService();
        mojangApi = new MojangApi();
        providerManager = new ProviderManager();
        providerManager.register(new HypixelPublicApi(mojangApi, config));
        providerManager.register(new NadeshikoApi(mojangApi));
        providerManager.register(new AbyssApi(mojangApi));

        urchinApi = new UrchinApi(mojangApi);
        seraphApi = new SeraphApi(mojangApi);
        seraphClientCacheService = new SeraphClientCacheService(seraphApi, config);
        auroraApi = new AuroraApi();
        frostyApi = new FrostyApi();

        playerCache = new PlayerCache(
            mojangApi,
            providerManager,
            urchinApi,
            seraphApi,
            config
        );
        PartyBlacklistWarningService partyBlacklistWarningService =
            new PartyBlacklistWarningService(
                blacklistManager,
                config,
                playerCache,
                tagIgnoreManager
            );
        HypixelFeatures
            .getInstance()
            .addGameStateListener(partyBlacklistWarningService::onSnapshotUpdate);

        nickUtils = new NickUtils(playerCache, config);

        TagUtils tagUtils = new TagUtils(this, blacklistManager);
        NumberDenicker numberDenicker = new NumberDenicker(
            config,
            nickUtils,
            auroraApi,
            frostyApi
        );
        PregameStats pregameStats = new PregameStats(
            playerCache,
            config,
            blacklistManager,
            annoylistManager,
            tagIgnoreManager
        );
        RequestPopupManager requestPopupManager = new RequestPopupManager(config);
        RequestPopupService requestPopupService = new RequestPopupService(
            config,
            requestPopupManager
        );
        ReplayManager replayManager = ReplayManager.getInstance();
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        replayManager.onShutdown();
                        AsyncExecutor.getInstance().shutdownReplayIoAndAwait();
                    }
                },
                "Mellow-Shutdown"
            )
        );

        KeyBinding requestAcceptKeybind = new KeyBinding(
            "Accept Request",
            Keyboard.KEY_Y,
            "Mellow Requests"
        );
        KeyBinding requestDenyKeybind = new KeyBinding(
            "Deny Request",
            Keyboard.KEY_N,
            "Mellow Requests"
        );
        ClientRegistry.registerKeyBinding(requestAcceptKeybind);
        ClientRegistry.registerKeyBinding(requestDenyKeybind);
        MinecraftForge.EVENT_BUS.register(
            new RequestPopupRouter(
                config,
                requestPopupManager,
                requestAcceptKeybind,
                requestDenyKeybind
            )
        );

        StatsChecker statsChecker = new StatsChecker(
            playerCache,
            nickUtils,
            config,
            tabStats,
            tagUtils,
            blacklistManager,
            annoylistManager,
            tagIgnoreManager
        );
        InGameTabStatsSyncService inGameTabStatsSyncService =
            new InGameTabStatsSyncService(statsChecker, nickUtils, config, tabStats);
        HypixelFeatures
            .getInstance()
            .addGameStateListener(inGameTabStatsSyncService::onSnapshotUpdate);
        HypixelFeatures.getInstance().addGameStateListener(replayManager::onGameSnapshot);

        MinecraftForge.EVENT_BUS.register(
            new ChatEventRouter(
                config,
                numberDenicker,
                pregameStats,
                requestPopupService
            )
        );
        MinecraftForge.EVENT_BUS.register(
            new WorldLifecycleRouter(numberDenicker, pregameStats, nickUtils)
        );
        MinecraftForge.EVENT_BUS.register(
            new ClientTickRouter(HypixelFeatures.getInstance())
        );
        MinecraftForge.EVENT_BUS.register(new ReplayHudRouter(replayManager));
        MinecraftForge.EVENT_BUS.register(new ReplayInputRouter(replayManager));
        MinecraftForge.EVENT_BUS.register(new NametagColorRouter(config));
        TabOverlayRouter tabOverlayRouter = new TabOverlayRouter(config);
        MinecraftForge.EVENT_BUS.register(tabOverlayRouter);
        MinecraftForge.EVENT_BUS.register(
            new TabOverlayInputRouter(tabOverlayRouter)
        );

        ClientCommandHandler.instance.registerCommand(
            new BedwarsCommand(playerCache, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new SkywarsCommand(playerCache, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new PVCommand(playerCache, config)
        );
        ClientCommandHandler.instance.registerCommand(new MellowCommand());
        ClientCommandHandler.instance.registerCommand(new DebugStateCommand());
        ClientCommandHandler.instance.registerCommand(
            new ClearCacheCommand(playerCache, tabStats)
        );
        ClientCommandHandler.instance.registerCommand(
            new RefreshCommand(inGameTabStatsSyncService)
        );
        ClientCommandHandler.instance.registerCommand(
            new AuroraDenickCommand(config, auroraApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new FrostyDenickCommand(config, frostyApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new SkinDenickCommand(playerCache)
        );
        ClientCommandHandler.instance.registerCommand(
            new BlacklistCommand(blacklistManager, mojangApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new AnnoylistCommand(annoylistManager, mojangApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new TagIgnoreCommand(tagIgnoreManager, mojangApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new UrchinCommand(urchinApi, mojangApi, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new SeraphCommand(seraphApi, mojangApi, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new StatusCommand(mojangApi, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new NameHistoryCommand(mojangApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new ClientCommand(seraphApi, mojangApi, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new WinstreakCommand(playerCache, config)
        );
        ClientCommandHandler.instance.registerCommand(new ReplayCommand(replayManager));
    }

    public StatsProvider getStatsProvider() {
        if (providerManager == null) {
            return null;
        }
        return providerManager.getSelectedProvider(config);
    }

    public static AnticheatManager getAnticheatManager() {
        return anticheatManager;
    }
}
