package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsMode;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.api.util.HypixelApiUtils;
import com.roxiun.mellow.util.cache.TimedValueCache;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;

public class BedlifyApi implements StatsProvider {

    private static final long RAW_DATA_CACHE_TTL_MS = 120_000L;
    private static final String PLAYER_ENDPOINT =
        "https://api.bedlify.xyz/v1/player/cache?uuid=";

    private final MojangApi mojangApi;
    private final TimedValueCache<String, ProviderResult<String>> rawDataCache =
        new TimedValueCache<>(RAW_DATA_CACHE_TTL_MS);

    public BedlifyApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.BEDLIFY;
    }

    @Override
    public String getDisplayName() {
        return "Bedlify";
    }

    @Override
    public String fetchPlayerData(String uuid) {
        ProviderResult<String> result = fetchPlayerDataResult(uuid);
        return result.isSuccess() ? result.getValue() : "";
    }

    @Override
    public ProviderResult<String> fetchPlayerDataResult(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return ProviderResult.failure(
                FetchFailureReason.UUID_UNAVAILABLE,
                "Missing UUID"
            );
        }

        String cacheKey = uuid.trim();
        if (rawDataCache.containsFresh(cacheKey)) {
            ProviderResult<String> cached = rawDataCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        ProviderResult<String> result = HypixelApiUtils.fetchPlayerDataResult(
            PLAYER_ENDPOINT + cacheKey,
            "Mellow/" + Mellow.VERSION
        );
        if (result.isSuccess()) {
            rawDataCache.put(cacheKey, result);
        }
        return result;
    }

    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName)
        throws IOException {
        String json = fetchPlayerJson(playerName);
        return json == null
            ? null
            : HypixelApiUtils.parsePlayerData(json, ProviderId.BEDLIFY);
    }

    @Override
    public SkywarsPlayer fetchSkywarsStats(String playerName)
        throws IOException {
        String json = fetchPlayerJson(playerName);
        return json == null
            ? null
            : HypixelApiUtils.parseSkywarsPlayerData(json, ProviderId.BEDLIFY);
    }

    @Override
    public DuelsPlayer fetchDuelsStats(String playerName) throws IOException {
        return fetchDuelsStats(playerName, DuelsMode.OVERALL);
    }

    @Override
    public DuelsPlayer fetchDuelsStats(String playerName, DuelsMode mode)
        throws IOException {
        String json = fetchPlayerJson(playerName);
        return json == null
            ? null
            : HypixelApiUtils.parseDuelsPlayerData(
                json,
                ProviderId.BEDLIFY,
                mode
            );
    }

    @Override
    public BuildBattlePlayer fetchBuildBattleStats(String playerName)
        throws IOException {
        String json = fetchPlayerJson(playerName);
        return json == null
            ? null
            : HypixelApiUtils.parseBuildBattlePlayerData(
                json,
                ProviderId.BEDLIFY
            );
    }

    @Override
    public TntRunPlayer fetchTntRunStats(String playerName) throws IOException {
        String json = fetchPlayerJson(playerName);
        return json == null
            ? null
            : HypixelApiUtils.parseTntRunPlayerData(json, ProviderId.BEDLIFY);
    }

    private String fetchPlayerJson(String playerName) {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String json = fetchPlayerData(uuid);
        return json == null || json.isEmpty() ? null : json;
    }
}
