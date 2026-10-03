package com.roxiun.mellow;

import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.api.aurora.AuroraPingService;
import com.roxiun.mellow.api.aurora.AuroraWinstreakService;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.luna.LunaPingService;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.AbyssApi;
import com.roxiun.mellow.api.provider.BordicApi;
import com.roxiun.mellow.api.provider.HypixelPublicApi;
import com.roxiun.mellow.api.provider.NadeshikoApi;
import com.roxiun.mellow.api.provider.ProviderManager;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.seraph.SeraphClientCacheService;
import com.roxiun.mellow.api.seraph.SeraphPingService;
import com.roxiun.mellow.api.coral.CoralApi;
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
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.lwjgl.input.Keyboard;

public class Mellow implements net.fabricmc.api.ClientModInitializer {

    public static final String MODID = "mellow";
    public static final String NAME = "Mellow";
    public static final String VERSION = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MODID).map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");

    public static MellowOneConfig config;
    public static final Map<String, TabStats> tabStats = new ConcurrentHashMap<>();
    public static NickUtils nickUtils;

    public static AuroraPingService auroraPingService;
    public static AuroraWinstreakService auroraWinstreakService;
    public static AuroraApi auroraApi;
    public static LunaPingService lunaPingService;
    public static SeraphClientCacheService seraphClientCacheService;
    public static SeraphPingService seraphPingService;
    public static MojangApi mojangApi;
    public static CoralApi coralApi;
    public static SeraphApi seraphApi;
    public static PlayerCache playerCache;
    public static BlacklistManager blacklistManager;
    public static AnnoylistManager annoylistManager;
    public static TagIgnoreManager tagIgnoreManager;
    private static AnticheatManager anticheatManager;

    private ProviderManager providerManager;

    @Override
    public void onInitializeClient() {
        EventManager.register(org.polyfrost.oneconfig.api.event.v1.events.InitializationEvent.class, this::initializeFeatures);
    }

    private void initializeFeatures() {
        config = new MellowOneConfig();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("polyhitbox")) {
            com.roxiun.mellow.platform.PolyHitboxIntegration.register();
        }
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
        providerManager.register(new BordicApi(mojangApi));

        coralApi = new CoralApi();
        seraphApi = new SeraphApi(mojangApi);
        seraphClientCacheService = new SeraphClientCacheService(seraphApi, config);
        auroraApi = new AuroraApi();

        playerCache = new PlayerCache(
            mojangApi,
            providerManager,
            coralApi,
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
            auroraApi
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
        com.roxiun.mellow.platform.ClientBindings.register(requestAcceptKeybind);
        com.roxiun.mellow.platform.ClientBindings.register(requestDenyKeybind);
        net.minecraft.client.Minecraft.getMinecraft().gameSettings.loadOptions();
        EventManager.INSTANCE.register(
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

        EventManager.INSTANCE.register(
            new ChatEventRouter(
                config,
                numberDenicker,
                pregameStats,
                requestPopupService
            )
        );
        EventManager.INSTANCE.register(
            new WorldLifecycleRouter(numberDenicker, pregameStats, nickUtils)
        );
        EventManager.INSTANCE.register(
            new ClientTickRouter(HypixelFeatures.getInstance())
        );
        EventManager.INSTANCE.register(new ReplayHudRouter(replayManager));
        EventManager.INSTANCE.register(new ReplayInputRouter(replayManager));
        EventManager.INSTANCE.register(new NametagColorRouter(config));
        TabOverlayRouter tabOverlayRouter = new TabOverlayRouter(config);
        EventManager.INSTANCE.register(tabOverlayRouter);
        EventManager.INSTANCE.register(
            new TabOverlayInputRouter(tabOverlayRouter)
        );

        com.roxiun.mellow.platform.ClientCommands.register(
            new BedwarsCommand(playerCache, config, blacklistManager)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new SkywarsCommand(playerCache, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new PVCommand(playerCache, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(new MellowCommand());
        com.roxiun.mellow.platform.ClientCommands.register(new DebugStateCommand());
        com.roxiun.mellow.platform.ClientCommands.register(
            new ClearCacheCommand(playerCache, tabStats)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new RefreshCommand(inGameTabStatsSyncService)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new DenickCommand(config, auroraApi)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new SkinDenickCommand(playerCache)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new BlacklistCommand(blacklistManager, mojangApi, seraphApi, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new AnnoylistCommand(annoylistManager, mojangApi)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new TagIgnoreCommand(tagIgnoreManager, mojangApi)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new CoralCommand(coralApi, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new SeraphCommand(seraphApi, mojangApi, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new StatusCommand(mojangApi, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new NameHistoryCommand(mojangApi)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new ClientCommand(seraphApi, mojangApi, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(
            new WinstreakCommand(playerCache, config)
        );
        com.roxiun.mellow.platform.ClientCommands.register(new ReplayCommand(replayManager));
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
