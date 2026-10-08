package com.roxiun.mellow.cache;

import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.xadia.XadiaTag;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.roxiun.mellow.util.cache.TimedValueCache;
import com.roxiun.mellow.util.cache.RequestCache;
import com.roxiun.mellow.api.tags.TagReport;
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

    private static final long CACHE_TTL_MS = 300_000L;

    private final RequestCache<String, ProviderResult<JsonObject>> rawDataCache =
        new RequestCache<>(2048, CACHE_TTL_MS, 5_000L, ProviderResult::isSuccess);
    // No timed expiry: cleared with the connection identity cache; still capacity-bounded.
    private final TimedValueCache<String, String> resolvedNames = new TimedValueCache<>(0L);
    private final Map<String, Long> providerCooldown = new ConcurrentHashMap<>();
    private final MojangApi mojangApi;
    private final ProviderManager providerManager;
    private final XadiaApi xadiaApi;
    private final CoralApi coralApi;
    private final MellowOneConfig config;

    private final AtomicBoolean hasWarnedMissingApiKey = new AtomicBoolean(false);

    public PlayerCache(
        MojangApi mojangApi,
        ProviderManager providerManager,
        CoralApi coralApi,
        MellowOneConfig config
    ) {
        this(mojangApi, providerManager, coralApi,
            new XadiaApi(), config);
    }

    public PlayerCache(MojangApi mojangApi, ProviderManager providerManager,
        CoralApi coralApi,
        XadiaApi xadiaApi, MellowOneConfig config) {
        this.xadiaApi = xadiaApi;
        this.mojangApi = mojangApi;
        this.providerManager = providerManager;
        this.coralApi = coralApi;
        this.config = config;
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

    public ProfileFetchResult getIdentityResult(String name, ProfileFetchContext context) {
        ResolvedUuid identity = resolveUuid(name, context);
        return identity.isSuccess() ? ProfileFetchResult.success(PlayerProfile.identity(identity.uuid, name), null)
            : ProfileFetchResult.failure(identity.failureReason, identity.detail, null);
    }

    public ProfileFetchResult getProfileResult(String playerName) {
        return getScopedProfileResult(playerName, null, ProfileFetchContext.GENERAL, true);
    }

    public ProfileFetchResult getScopedProfileResult(String name, StatScope scope,
        ProfileFetchContext context, boolean includeTags) {
        ResolvedUuid identity = resolveUuid(name, context == null ? ProfileFetchContext.GENERAL : context);
        if (!identity.isSuccess()) {
            return ProfileFetchResult.failure(identity.failureReason, identity.detail,
                getSelectedProvider() == null ? null : getSelectedProvider().getDisplayName());
        }
        return getProfileForIdentity(name, identity.uuid, includeTags);
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

        Map<String, String> failures = new java.util.LinkedHashMap<>();
        List<CoralTag> coralTags = java.util.Collections.emptyList();
        if (config.isCoralEnabled()) {
            try {
                coralTags = coralApi.fetchCoralTags(
                    uuid,
                    profile.getName(),
                    normalizeApiKey(config.getCoralApiKey())
                );
            } catch (IOException error) { failures.put("Coral", error.getMessage()); }
        }

        java.util.List<XadiaTag> xadiaTags = java.util.Collections.emptyList();
        if (config.xadia) {
            try {
                xadiaTags = xadiaApi.fetchXadiaTags(
                    uuid, profile.getName(), config.xadiaKey, config.xadiaVerifiedOnly);
            } catch (IOException error) { failures.put("Xadia", error.getMessage()); }
        }
        return profile.withTags(TagReport.nativeTags(coralTags, xadiaTags, failures));
    }

    public void prefetchStats(java.util.Set<String> uuids) {
        StatsProvider provider = getSelectedProvider();
        if (provider == null || !provider.supportsBatch()) return;
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String uuid : uuids) ids.add(normalizeUuidKey(uuid));
        for (int start = 0; start < ids.size(); start += 100) {
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            for (String id : ids.subList(start, Math.min(start + 100, ids.size()))) keys.add(buildRawDataCacheKey(provider, id));
            rawDataCache.getAll(keys, missing -> {
                java.util.Set<String> players = new java.util.LinkedHashSet<>();
                for (String key : missing) players.add(key.substring(key.lastIndexOf(':') + 1));
                Map<String, ProviderResult<String>> fetched = coolingDown(provider) ? java.util.Collections.emptyMap() : provider.fetchBatch(players);
                Map<String, ProviderResult<JsonObject>> result = new java.util.LinkedHashMap<>();
                for (String key : missing) {
                    String id = key.substring(key.lastIndexOf(':') + 1);
                    result.put(key, fetched.isEmpty() ? ProviderResult.failure(FetchFailureReason.RATE_LIMITED, "Provider is cooling down") : parseResponse(provider, fetched.get(id)));
                }
                return result;
            });
        }
    }

    public Map<String, TagReport> fetchTagReports(java.util.Set<String> uuids) {
        Map<String, TagReport> reports = new java.util.LinkedHashMap<>();
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String uuid : uuids) ids.add(normalizeUuidKey(uuid));
        for (int start = 0; start < ids.size(); start += 100) {
            java.util.Set<String> batch = new java.util.LinkedHashSet<>(ids.subList(start, Math.min(ids.size(), start + 100)));
            Map<String, ProviderResult<List<CoralTag>>> coral = config.isCoralEnabled()
                ? coralApi.fetchBatch(batch, config.getCoralApiKey()) : java.util.Collections.emptyMap();
            Map<String, ProviderResult<List<XadiaTag>>> xadia = config.xadia
                ? xadiaApi.fetchBatch(batch, config.xadiaKey, config.xadiaVerifiedOnly) : java.util.Collections.emptyMap();
            for (String id : batch) {
                Map<String, String> errors = new java.util.LinkedHashMap<>();
                ProviderResult<List<CoralTag>> c = coral.get(id);
                ProviderResult<List<XadiaTag>> x = xadia.get(id);
                if (c != null && !c.isSuccess()) errors.put("Coral", c.getError());
                if (x != null && !x.isSuccess()) errors.put("Xadia", x.getError());
                reports.put(id, TagReport.nativeTags(
                    c == null ? null : c.getValue(), x == null ? null : x.getValue(), errors));
            }
        }
        return reports;
    }

    public ProfileFetchResult getProfileForIdentity(String name, String uuid, boolean tags) {
        StatsProvider provider = getSelectedProvider();
        if (provider == null) return ProfileFetchResult.failure(FetchFailureReason.PROVIDER_ERROR, "No provider", null);
        resolvedNames.put(name.toLowerCase(Locale.ROOT), uuid);
        ProviderResult<JsonObject> raw = fetchRaw(provider, uuid);
        ProfileFetchResult result = raw.isSuccess()
            ? buildFullProfileResult(name, uuid, raw.getValue(), provider, resolveActiveDuelsMode(), tags)
            : toProfileFailure(raw, provider.getDisplayName());
        return withAvailableProfile(result, name, uuid, tags);
    }

    public StatsProvider getSelectedProvider() {
        return providerManager.getSelectedProvider(config);
    }

    public String fetchRawPlayerData(String playerName) {
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

        ProviderResult<JsonObject> rawResult = fetchRaw(provider, uuid.uuid);
        if (!rawResult.isSuccess()) return "";
        return rawResult.getValue().toString();
    }

    private ProfileFetchResult buildFullProfileResult(
        String playerName,
        String uuid,
        JsonObject root,
        StatsProvider provider,
        DuelsMode duelsMode,
        boolean includeTags
    ) {
        ProviderResult<BedwarsPlayer> bedwarsResult = HypixelApiUtils.parsePlayerDataResult(
            root,
            provider.getProviderId()
        );
        ProviderResult<SkywarsPlayer> skywarsResult =
            HypixelApiUtils.parseSkywarsPlayerDataResult(
                root,
                provider.getProviderId()
            );
        ProviderResult<DuelsPlayer> duelsResult = HypixelApiUtils.parseDuelsPlayerDataResult(
            root,
            provider.getProviderId(),
            duelsMode
        );
        ProviderResult<BuildBattlePlayer> buildBattleResult =
            HypixelApiUtils.parseBuildBattlePlayerDataResult(
                root,
                provider.getProviderId()
            );
        ProviderResult<TntRunPlayer> tntRunResult =
            HypixelApiUtils.parseTntRunPlayerDataResult(
                root,
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
            null
        );

        if (includeTags) {
            profile = enrichProfileWithTags(profile);
        }

        return ProfileFetchResult.success(profile, provider.getDisplayName());
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
        if (!config.showAutomaticStatsErrors) {
            return;
        }
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

    public void clearIdentityCache() {
        resolvedNames.clear();
        if (mojangApi != null) mojangApi.clearCache();
    }

    public void clearCache() {
        providerCooldown.clear();
        xadiaApi.clearCache();
        clearIdentityCache();
        rawDataCache.clear();
        if (Mellow.coralApi != null) {
            Mellow.coralApi.clearCache();
        }

        if (Mellow.auroraApi != null) {
            Mellow.auroraApi.clearCache();
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

        if (Mellow.coralApi != null) {
            Mellow.coralApi.clearPlayer(fullUuid, playerName);
        }

    }

    /** Clears only stats-provider data, preserving supplemental API gates. */
    public void clearPlayerTags(String uuid, String name) {
        coralApi.clearPlayer(uuid, name);
        xadiaApi.clearPlayer(uuid, name);
    }

    public void clearPlayerStats(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String uuid = resolvedNames.get(playerName.toLowerCase(Locale.ROOT));
        if (uuid == null) {
            UUID tabUuid = PlayerUtils.getTrustedTabUuid(playerName);
            if (tabUuid != null) uuid = tabUuid.toString();
        }
        if (uuid != null) {
            final String suffix = ":" + normalizeUuidKey(uuid);
            rawDataCache.removeMatching(key -> key.endsWith(suffix));
        }
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
                resolvedNames.put(playerName.toLowerCase(Locale.ROOT), uuid);
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

        resolvedNames.put(playerName.toLowerCase(Locale.ROOT), uuid);
                return ResolvedUuid.success(uuid);
    }

    private String buildRawDataCacheKey(StatsProvider provider, String uuid) {
        return provider.getProviderId().name() + ":" + normalizeApiKey(config.hypixelApiKey) + ":" + normalizeUuidKey(uuid);
    }

    private String normalizeUuidKey(String uuid) {
        if (uuid == null) {
            return "";
        }
        return uuid.replace("-", "").toLowerCase(Locale.ROOT).trim();
    }

    private boolean coolingDown(StatsProvider provider) {
        return providerCooldown.getOrDefault(provider.getProviderId().name(), 0L) > System.currentTimeMillis();
    }

    private ProviderResult<JsonObject> fetchRaw(StatsProvider provider, String uuid) {
        return rawDataCache.get(buildRawDataCacheKey(provider, uuid), () -> {
            if (coolingDown(provider)) return ProviderResult.failure(FetchFailureReason.RATE_LIMITED, "Provider is cooling down");
            return parseResponse(provider, provider.fetchPlayerDataResult(uuid));
        });
    }

    private ProviderResult<JsonObject> parseResponse(StatsProvider provider, ProviderResult<String> response) {
        if (response == null) return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "Provider omitted player");
        if (!response.isSuccess()) {
            if (response.getFailureReason() == FetchFailureReason.RATE_LIMITED)
                providerCooldown.put(provider.getProviderId().name(), System.currentTimeMillis() + 30_000L);
            return ProviderResult.failure(response.getFailureReason(), response.getError());
        }
        try {
            JsonObject root = new JsonParser().parse(response.getValue()).getAsJsonObject();
            if (provider.getProviderId() != ProviderId.NADESHIKO) {
                if (!root.has("success") || !root.get("success").getAsBoolean())
                    return ProviderResult.failure(FetchFailureReason.PROVIDER_ERROR,
                        root.has("cause") ? root.get("cause").getAsString() : "Provider returned success=false");
                if (!root.has("player") || root.get("player").isJsonNull())
                    return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No player data");
            }
            return ProviderResult.success(root);
        } catch (RuntimeException error) {
            return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, error.getMessage());
        }
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
