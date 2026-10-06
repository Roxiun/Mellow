package com.roxiun.mellow.cache;

import com.roxiun.mellow.api.xadia.XadiaApi;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsMode;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.ProviderManager;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.api.coral.CoralApi;
import com.roxiun.mellow.api.coral.CoralTag;
import com.roxiun.mellow.api.util.HypixelApiUtils;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;

public class PlayerCache {

    private static final long CACHE_TTL_MS = 120_000L;

    private final Map<String, CachedProfile> cache = new ConcurrentHashMap<>();
    private final Map<String, CachedRawData> rawDataCache =
        new ConcurrentHashMap<>();
    private final MojangApi mojangApi;
    private final ProviderManager providerManager;
    private final XadiaApi xadiaApi;
    private volatile String lastXadiaSettings = "";
    private final CoralApi coralApi;
    private final SeraphApi seraphApi;
    private final MellowOneConfig config;
    private volatile String lastCoralApiKey;
    private volatile String lastSeraphApiKey;

    private final AtomicBoolean hasWarnedMissingApiKey = new AtomicBoolean(false);

    public PlayerCache(
        MojangApi mojangApi,
        ProviderManager providerManager,
        CoralApi coralApi,
        SeraphApi seraphApi,
        MellowOneConfig config
    ) {
        this(mojangApi, providerManager, coralApi, seraphApi,
            new XadiaApi(), config);
    }

    public PlayerCache(MojangApi mojangApi, ProviderManager providerManager,
        CoralApi coralApi, SeraphApi seraphApi,
        XadiaApi xadiaApi, MellowOneConfig config) {
        this.xadiaApi = xadiaApi;
        this.mojangApi = mojangApi;
        this.providerManager = providerManager;
        this.coralApi = coralApi;
        this.seraphApi = seraphApi;
        this.config = config;
        this.lastCoralApiKey = normalizeApiKey(config.getCoralApiKey());
        this.lastSeraphApiKey = normalizeApiKey(config.seraphKey);
    }

    public PlayerProfile getProfile(String playerName) {
        ProfileFetchResult result = getProfileResult(playerName);
        if (
            !result.isSuccess() &&
            result.getFailureReason() == FetchFailureReason.MISSING_API_KEY
        ) {
            maybeWarnMissingApiKey(result.getProviderName());
        }
        return result.getProfile();
    }

    public ProfileFetchResult getProfileResult(String playerName) {
        maybeInvalidateCacheOnApiKeyChange();
        StatsProvider provider = providerManager.getSelectedProvider(config);
        if (provider == null) {
            return ProfileFetchResult.failure(
                FetchFailureReason.PROVIDER_ERROR,
                "No provider selected",
                null
            );
        }

        DuelsMode duelsMode = resolveActiveDuelsMode();
        String cacheKey = buildCacheKey(
            provider,
            playerName,
            null,
            ProfileFetchContext.GENERAL,
            true,
            duelsMode
        );
        CachedProfile cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return ProfileFetchResult.success(
                cached.profile,
                provider.getDisplayName()
            );
        }

        ResolvedUuid uuidResult = resolveUuid(playerName, ProfileFetchContext.GENERAL);
        if (!uuidResult.isSuccess()) {
            return ProfileFetchResult.failure(
                uuidResult.failureReason,
                uuidResult.detail,
                provider.getDisplayName()
            );
        }

        if (provider.requiresApiKey() && !provider.isConfigured()) {
            return withAvailableProfile(
                ProfileFetchResult.failure(
                    FetchFailureReason.MISSING_API_KEY,
                    "Missing API key",
                    provider.getDisplayName()
                ),
                playerName,
                uuidResult.uuid,
                true
            );
        }

        ProviderResult<String> rawResult = provider.fetchPlayerDataResult(uuidResult.uuid);
        if (!rawResult.isSuccess()) {
            return withAvailableProfile(
                toProfileFailure(rawResult, provider.getDisplayName()),
                playerName,
                uuidResult.uuid,
                true
            );
        }
        storeRawData(provider, uuidResult.uuid, rawResult.getValue());

        ProfileFetchResult result = buildFullProfileResult(
            playerName,
            uuidResult.uuid,
            rawResult.getValue(),
            provider,
            duelsMode,
            true
        );
        if (result.isSuccess()) {
            cache.put(cacheKey, new CachedProfile(result.getProfile()));
        }
        return withAvailableProfile(
            result,
            playerName,
            uuidResult.uuid,
            true
        );
    }

    public ProfileFetchResult getScopedProfileResult(
        String playerName,
        StatScope scope,
        ProfileFetchContext context,
        boolean includeTags
    ) {
        maybeInvalidateCacheOnApiKeyChange();
        StatsProvider provider = providerManager.getSelectedProvider(config);
        if (provider == null) {
            return ProfileFetchResult.failure(
                FetchFailureReason.PROVIDER_ERROR,
                "No provider selected",
                null
            );
        }

        StatScope resolvedScope = scope == null ? StatScope.BEDWARS : scope;
        DuelsMode duelsMode = resolveActiveDuelsMode();
        String cacheKey = buildCacheKey(
            provider,
            playerName,
            resolvedScope,
            context == null ? ProfileFetchContext.GENERAL : context,
            includeTags,
            duelsMode
        );
        CachedProfile cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return ProfileFetchResult.success(
                cached.profile,
                provider.getDisplayName()
            );
        }

        ResolvedUuid uuidResult = resolveUuid(
            playerName,
            context == null ? ProfileFetchContext.GENERAL : context
        );
        if (!uuidResult.isSuccess()) {
            return ProfileFetchResult.failure(
                uuidResult.failureReason,
                uuidResult.detail,
                provider.getDisplayName()
            );
        }

        if (provider.requiresApiKey() && !provider.isConfigured()) {
            return withAvailableProfile(
                ProfileFetchResult.failure(
                    FetchFailureReason.MISSING_API_KEY,
                    "Missing API key",
                    provider.getDisplayName()
                ),
                playerName,
                uuidResult.uuid,
                includeTags
            );
        }

        ProviderResult<String> rawResult = provider.fetchPlayerDataResult(uuidResult.uuid);
        if (!rawResult.isSuccess()) {
            return withAvailableProfile(
                toProfileFailure(rawResult, provider.getDisplayName()),
                playerName,
                uuidResult.uuid,
                includeTags
            );
        }
        storeRawData(provider, uuidResult.uuid, rawResult.getValue());

        ProfileFetchResult result = buildScopedProfileResult(
            playerName,
            uuidResult.uuid,
            rawResult.getValue(),
            provider,
            resolvedScope,
            duelsMode,
            includeTags
        );
        if (result.isSuccess()) {
            cache.put(cacheKey, new CachedProfile(result.getProfile()));
        }
        return withAvailableProfile(
            result,
            playerName,
            uuidResult.uuid,
            includeTags
        );
    }

    private ProfileFetchResult withAvailableProfile(
        ProfileFetchResult result,
        String playerName,
        String uuid,
        boolean includeTags
    ) {
        if (result == null || result.isSuccess() || result.getProfile() != null) {
            return result;
        }
        PlayerProfile profile = PlayerProfile.identity(uuid, playerName);
        if (includeTags) {
            profile = enrichProfileWithTags(profile);
        }
        return result.withProfile(profile);
    }

    public PlayerProfile enrichProfileWithTags(PlayerProfile profile) {
        if (profile == null) {
            return null;
        }

        String uuid = profile.getUuid();
        if (uuid == null || uuid.trim().isEmpty() || "ERROR".equals(uuid)) {
            return profile;
        }

        List<CoralTag> coralTags = profile.getCoralTags();
        if (config.isCoralEnabled()) {
            try {
                coralTags = coralApi.fetchCoralTags(
                    uuid,
                    profile.getName(),
                    normalizeApiKey(config.getCoralApiKey())
                );
            } catch (IOException ignored) {}
        }

        List<SeraphTag> seraphTags = profile.getSeraphTags();
        if (config.seraph) {
            try {
                seraphTags = seraphApi.fetchSeraphTags(
                    uuid,
                    normalizeApiKey(config.seraphKey)
                );
            } catch (IOException ignored) {}
        }

        profile = profile.withTags(coralTags, seraphTags);
        if (config.xadia) {
            try {
                return profile.withXadiaTags(xadiaApi.fetchXadiaTags(
                    uuid, profile.getName(), config.xadiaKey, config.xadiaVerifiedOnly));
            } catch (IOException ignored) {}
        }
        return profile.withXadiaTags(java.util.Collections.emptyList());
    }

    public StatsProvider getSelectedProvider() {
        return providerManager.getSelectedProvider(config);
    }

    public String fetchRawPlayerData(String playerName) {
        maybeInvalidateCacheOnApiKeyChange();
        StatsProvider provider = providerManager.getSelectedProvider(config);
        if (provider == null) {
            return "";
        }
        if (provider.requiresApiKey() && !provider.isConfigured()) {
            maybeWarnMissingApiKey(provider.getDisplayName());
            return "";
        }

        ResolvedUuid uuid = resolveUuid(playerName, ProfileFetchContext.GENERAL);
        if (!uuid.isSuccess()) {
            return "";
        }

        CachedRawData cachedRawData = rawDataCache.get(
            buildRawDataCacheKey(provider, uuid.uuid)
        );
        if (cachedRawData != null && !cachedRawData.isExpired()) {
            return cachedRawData.rawData;
        }

        ProviderResult<String> rawResult = provider.fetchPlayerDataResult(uuid.uuid);
        if (!rawResult.isSuccess()) {
            return "";
        }

        storeRawData(provider, uuid.uuid, rawResult.getValue());
        return rawResult.getValue();
    }

    private ProfileFetchResult buildFullProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider,
        DuelsMode duelsMode,
        boolean includeTags
    ) {
        ProviderResult<BedwarsPlayer> bedwarsResult = HypixelApiUtils.parsePlayerDataResult(
            rawData,
            provider.getProviderId()
        );
        ProviderResult<SkywarsPlayer> skywarsResult =
            HypixelApiUtils.parseSkywarsPlayerDataResult(
                rawData,
                provider.getProviderId()
            );
        ProviderResult<DuelsPlayer> duelsResult = HypixelApiUtils.parseDuelsPlayerDataResult(
            rawData,
            provider.getProviderId(),
            duelsMode
        );
        ProviderResult<BuildBattlePlayer> buildBattleResult =
            HypixelApiUtils.parseBuildBattlePlayerDataResult(
                rawData,
                provider.getProviderId()
            );
        ProviderResult<TntRunPlayer> tntRunResult =
            HypixelApiUtils.parseTntRunPlayerDataResult(
                rawData,
                provider.getProviderId()
            );

        if (
            !bedwarsResult.isSuccess() &&
            !skywarsResult.isSuccess() &&
            !duelsResult.isSuccess() &&
            !buildBattleResult.isSuccess() &&
            !tntRunResult.isSuccess()
        ) {
            return selectProfileFailure(
                provider.getDisplayName(),
                bedwarsResult,
                skywarsResult,
                duelsResult,
                buildBattleResult,
                tntRunResult
            );
        }

        PlayerProfile profile = new PlayerProfile(
            uuid,
            playerName,
            bedwarsResult.getValue(),
            skywarsResult.getValue(),
            duelsResult.getValue(),
            buildBattleResult.getValue(),
            tntRunResult.getValue(),
            null,
            null
        );

        if (includeTags) {
            profile = enrichProfileWithTags(profile);
        }

        return ProfileFetchResult.success(profile, provider.getDisplayName());
    }

    private ProfileFetchResult buildScopedProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider,
        StatScope scope,
        DuelsMode duelsMode,
        boolean includeTags
    ) {
        ProfileFetchResult result;
        switch (scope) {
            case SKYWARS:
                result = createSkywarsProfileResult(
                    playerName,
                    uuid,
                    rawData,
                    provider
                );
                break;
            case DUELS:
                result = createDuelsProfileResult(
                    playerName,
                    uuid,
                    rawData,
                    provider,
                    duelsMode
                );
                break;
            case BUILD_BATTLE:
                result = createBuildBattleProfileResult(
                    playerName,
                    uuid,
                    rawData,
                    provider
                );
                break;
            case TNT_RUN:
                result = createTntRunProfileResult(
                    playerName,
                    uuid,
                    rawData,
                    provider
                );
                break;
            case BEDWARS:
            default:
                result = createBedwarsProfileResult(
                    playerName,
                    uuid,
                    rawData,
                    provider
                );
                break;
        }

        if (!result.isSuccess()) {
            return result;
        }

        PlayerProfile profile = result.getProfile();
        if (includeTags) {
            profile = enrichProfileWithTags(profile);
        }

        return ProfileFetchResult.success(profile, provider.getDisplayName());
    }

    private ProfileFetchResult createBedwarsProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider
    ) {
        ProviderResult<BedwarsPlayer> bedwarsResult = HypixelApiUtils.parsePlayerDataResult(
            rawData,
            provider.getProviderId()
        );
        if (!bedwarsResult.isSuccess()) {
            return toProfileFailure(bedwarsResult, provider.getDisplayName());
        }

        return ProfileFetchResult.success(
            new PlayerProfile(
                uuid,
                playerName,
                bedwarsResult.getValue(),
                null,
                null,
                null,
                null,
                null,
                null
            ),
            provider.getDisplayName()
        );
    }

    private ProfileFetchResult createSkywarsProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider
    ) {
        ProviderResult<SkywarsPlayer> skywarsResult =
            HypixelApiUtils.parseSkywarsPlayerDataResult(
                rawData,
                provider.getProviderId()
            );
        if (!skywarsResult.isSuccess()) {
            return toProfileFailure(skywarsResult, provider.getDisplayName());
        }

        return ProfileFetchResult.success(
            new PlayerProfile(
                uuid,
                playerName,
                null,
                skywarsResult.getValue(),
                null,
                null,
                null,
                null,
                null
            ),
            provider.getDisplayName()
        );
    }

    private ProfileFetchResult createDuelsProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider,
        DuelsMode duelsMode
    ) {
        ProviderResult<DuelsPlayer> duelsResult = HypixelApiUtils.parseDuelsPlayerDataResult(
            rawData,
            provider.getProviderId(),
            duelsMode
        );
        if (!duelsResult.isSuccess()) {
            return toProfileFailure(duelsResult, provider.getDisplayName());
        }

        return ProfileFetchResult.success(
            new PlayerProfile(
                uuid,
                playerName,
                null,
                null,
                duelsResult.getValue(),
                null,
                null,
                null,
                null
            ),
            provider.getDisplayName()
        );
    }

    private ProfileFetchResult createBuildBattleProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider
    ) {
        ProviderResult<BuildBattlePlayer> buildBattleResult =
            HypixelApiUtils.parseBuildBattlePlayerDataResult(
                rawData,
                provider.getProviderId()
            );
        if (!buildBattleResult.isSuccess()) {
            return toProfileFailure(buildBattleResult, provider.getDisplayName());
        }

        return ProfileFetchResult.success(
            new PlayerProfile(
                uuid,
                playerName,
                null,
                null,
                null,
                buildBattleResult.getValue(),
                null,
                null,
                null
            ),
            provider.getDisplayName()
        );
    }

    private ProfileFetchResult createTntRunProfileResult(
        String playerName,
        String uuid,
        String rawData,
        StatsProvider provider
    ) {
        ProviderResult<TntRunPlayer> tntRunResult =
            HypixelApiUtils.parseTntRunPlayerDataResult(
                rawData,
                provider.getProviderId()
            );
        if (!tntRunResult.isSuccess()) {
            return toProfileFailure(tntRunResult, provider.getDisplayName());
        }

        return ProfileFetchResult.success(
            new PlayerProfile(
                uuid,
                playerName,
                null,
                null,
                null,
                null,
                tntRunResult.getValue(),
                null,
                null
            ),
            provider.getDisplayName()
        );
    }

    private ProfileFetchResult selectProfileFailure(
        String providerName,
        ProviderResult<?>... results
    ) {
        if (results == null || results.length == 0) {
            return ProfileFetchResult.failure(
                FetchFailureReason.UNKNOWN,
                "Unknown parse failure",
                providerName
            );
        }

        for (ProviderResult<?> result : results) {
            if (
                result != null &&
                result.getFailureReason() == FetchFailureReason.NO_PLAYER_DATA
            ) {
                return toProfileFailure(result, providerName);
            }
        }

        for (ProviderResult<?> result : results) {
            if (result != null && result.getFailureReason() != null) {
                return toProfileFailure(result, providerName);
            }
        }

        return ProfileFetchResult.failure(
            FetchFailureReason.UNKNOWN,
            "Unknown parse failure",
            providerName
        );
    }

    private ProfileFetchResult toProfileFailure(
        ProviderResult<?> result,
        String providerName
    ) {
        return ProfileFetchResult.failure(
            result == null ? FetchFailureReason.UNKNOWN : result.getFailureReason(),
            result == null ? "Unknown error" : result.getError(),
            providerName
        );
    }

    private void maybeWarnMissingApiKey(String providerName) {
        if (!hasWarnedMissingApiKey.compareAndSet(false, true)) {
            return;
        }

        Minecraft.getMinecraft().addScheduledTask(() ->
            ChatUtils.sendMessage(
                "§e" +
                providerName +
                " is selected but no API key is configured. " +
                "Add a key in §bAPI Keys > Hypixel§e or switch your Stats Provider to §bBordic§e for keyless stats."
            )
        );
    }

    public void clearCache() {
        xadiaApi.clearCache();
        cache.clear();
        rawDataCache.clear();
        if (mojangApi != null) {
            mojangApi.clearCache();
        }
        if (Mellow.coralApi != null) {
            Mellow.coralApi.clearCache();
        }
        if (Mellow.seraphApi != null) {
            Mellow.seraphApi.clearCache();
        }
        if (Mellow.auroraApi != null) {
            Mellow.auroraApi.clearCache();
        }
        if (Mellow.seraphClientCacheService != null) {
            Mellow.seraphClientCacheService.clearCache();
        }
        if (Mellow.auroraPingService != null) {
            Mellow.auroraPingService.clearCache();
        }
        if (Mellow.auroraWinstreakService != null) {
            Mellow.auroraWinstreakService.clearCache();
        }
        if (Mellow.lunaPingService != null) {
            Mellow.lunaPingService.clearCache();
        }
        if (Mellow.seraphPingService != null) {
            Mellow.seraphPingService.clearCache();
        }
    }

    public void clearPlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        xadiaApi.clearPlayer(null, playerName);
        clearPlayerStats(playerName);
        if (mojangApi != null) {
            mojangApi.clearPlayer(playerName);
        }
        if (Mellow.coralApi != null) {
            Mellow.coralApi.clearPlayer(null, playerName);
        }
        if (Mellow.seraphClientCacheService != null) {
            Mellow.seraphClientCacheService.clearPlayer(playerName);
        }

        UUID trustedTabUuid = PlayerUtils.getTrustedTabUuid(playerName);
        if (trustedTabUuid == null) {
            return;
        }

        String fullUuid = trustedTabUuid.toString();
        xadiaApi.clearPlayer(fullUuid, playerName);
        String compactUuid = fullUuid.replace("-", "");
        if (Mellow.auroraPingService != null) {
            Mellow.auroraPingService.clearPlayer(compactUuid);
        }
        if (Mellow.auroraWinstreakService != null) {
            Mellow.auroraWinstreakService.clearPlayer(compactUuid);
        }
        if (Mellow.lunaPingService != null) {
            Mellow.lunaPingService.clearPlayer(fullUuid);
        }
        if (Mellow.seraphPingService != null) {
            Mellow.seraphPingService.clearPlayer(fullUuid);
        }
        if (Mellow.coralApi != null) {
            Mellow.coralApi.clearPlayer(fullUuid, playerName);
        }
        if (Mellow.seraphApi != null) {
            Mellow.seraphApi.clearPlayer(fullUuid);
        }
    }

    /** Clears only stats-provider data, preserving supplemental API gates. */
    public void clearPlayerStats(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String lower = playerName.toLowerCase(Locale.ROOT);
        cache.keySet().removeIf(key -> key.endsWith(":" + lower));

        UUID trustedTabUuid = PlayerUtils.getTrustedTabUuid(playerName);
        if (trustedTabUuid == null) {
            return;
        }

        String compactUuid = trustedTabUuid.toString().replace("-", "");
        rawDataCache.keySet().removeIf(key -> key.endsWith(":" + compactUuid));
    }

    private void maybeInvalidateCacheOnApiKeyChange() {
        String currentCoralApiKey = normalizeApiKey(config.getCoralApiKey());
        String currentSeraphApiKey = normalizeApiKey(config.seraphKey);

        boolean coralChanged = !currentCoralApiKey.equals(lastCoralApiKey);
        boolean seraphChanged = !currentSeraphApiKey.equals(lastSeraphApiKey);
        String xadiaSettings = normalizeApiKey(config.xadiaKey) + "|" + config.xadia + "|" + config.xadiaVerifiedOnly;
        boolean xadiaChanged = !xadiaSettings.equals(lastXadiaSettings);
        lastXadiaSettings = xadiaSettings;
        if (!coralChanged && !seraphChanged && !xadiaChanged) {
            return;
        }

        lastCoralApiKey = currentCoralApiKey;
        lastSeraphApiKey = currentSeraphApiKey;
        clearCache();
    }

    private ResolvedUuid resolveUuid(
        String playerName,
        ProfileFetchContext context
    ) {
        if (
            context == ProfileFetchContext.LIVE_MATCH &&
            PlayerUtils.isNickedOrNpc(playerName)
        ) {
            return ResolvedUuid.failure(
                FetchFailureReason.NO_PLAYER_DATA,
                "Player is nicked or an NPC"
            );
        }

        if (
            context != ProfileFetchContext.PREGAME &&
            PlayerUtils.hasTrustedTabUuid(playerName)
        ) {
            String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
            if (uuid != null && !uuid.trim().isEmpty()) {
                return ResolvedUuid.success(uuid);
            }
        }

        if (context == ProfileFetchContext.LIVE_MATCH) {
            return ResolvedUuid.failure(
                FetchFailureReason.UUID_UNAVAILABLE,
                "No in-game UUID available"
            );
        }

        String uuid = mojangApi.fetchUUID(playerName);
        if (uuid == null || uuid.isEmpty() || "ERROR".equals(uuid)) {
            return ResolvedUuid.failure(
                FetchFailureReason.UUID_UNAVAILABLE,
                "Could not resolve UUID"
            );
        }

        return ResolvedUuid.success(uuid);
    }

    private String buildCacheKey(
        StatsProvider provider,
        String playerName,
        StatScope scope,
        ProfileFetchContext context,
        boolean includeTags,
        DuelsMode duelsMode
    ) {
        return provider.getProviderId().name() +
        ":" +
        (context == null ? ProfileFetchContext.GENERAL.name() : context.name()) +
        ":" +
        (scope == null ? "ALL" : scope.name()) +
        ":" +
        (duelsMode == null ? DuelsMode.OVERALL.name() : duelsMode.name()) +
        ":" +
        (includeTags ? "tags" : "notags") +
        ":" +
        playerName.toLowerCase(Locale.ROOT);
    }

    private String buildRawDataCacheKey(StatsProvider provider, String uuid) {
        return provider.getProviderId().name() +
        ":" +
        normalizeUuidKey(uuid);
    }

    private String normalizeUuidKey(String uuid) {
        if (uuid == null) {
            return "";
        }
        return uuid.replace("-", "").toLowerCase(Locale.ROOT).trim();
    }

    private void storeRawData(StatsProvider provider, String uuid, String rawData) {
        if (provider == null || rawData == null || rawData.isEmpty()) {
            return;
        }

        rawDataCache.put(
            buildRawDataCacheKey(provider, uuid),
            new CachedRawData(rawData)
        );
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

    private DuelsMode resolveActiveDuelsMode() {
        try {
            return DuelsMode.fromSnapshot(
                HypixelFeatures.getInstance().getGameSnapshot()
            );
        } catch (Exception ignored) {
            return DuelsMode.OVERALL;
        }
    }

    private static class CachedProfile {

        private final PlayerProfile profile;
        private final long cachedAt;

        private CachedProfile(PlayerProfile profile) {
            this.profile = profile;
            this.cachedAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
        }
    }

    private static class CachedRawData {

        private final String rawData;
        private final long cachedAt;

        private CachedRawData(String rawData) {
            this.rawData = rawData;
            this.cachedAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
        }
    }

    private static class ResolvedUuid {

        private final String uuid;
        private final FetchFailureReason failureReason;
        private final String detail;

        private ResolvedUuid(
            String uuid,
            FetchFailureReason failureReason,
            String detail
        ) {
            this.uuid = uuid;
            this.failureReason = failureReason;
            this.detail = detail;
        }

        private static ResolvedUuid success(String uuid) {
            return new ResolvedUuid(uuid, null, null);
        }

        private static ResolvedUuid failure(
            FetchFailureReason failureReason,
            String detail
        ) {
            return new ResolvedUuid(null, failureReason, detail);
        }

        private boolean isSuccess() {
            return uuid != null && !uuid.isEmpty();
        }
    }
}
