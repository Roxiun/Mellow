package com.roxiun.mellow.stats;

import com.google.gson.JsonObject;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.data.PlayerProfile;
import java.util.*;

/** Parses cached provider data. Selection and network requests belong to callers. */
public final class PlayerStatsService {
    private PlayerStatsService() {}
    public static ProviderResult<PlayerProfile> parse(String uuid, String name, JsonObject root,
                                                       ProviderId provider, StatsSelection selection) {
        ProviderResult<HypixelPlayerData> decoded = HypixelPlayerData.decode(root, provider);
        if (!decoded.isSuccess()) return ProviderResult.failure(decoded.getFailureReason(), decoded.getError());
        Map<GameDefinition<?>, Object> stats = new LinkedHashMap<>();
        ProviderResult<?> failure = null;
        List<GameDefinition<?>> games = selection == null ? GameRegistry.all() : Collections.singletonList(selection.game());
        for (GameDefinition<?> game : games) {
            ProviderResult<?> result = game.parse(decoded.getValue(), selection == null ? "overall" : selection.mode());
            if (result.isSuccess()) stats.put(game, result.getValue());
            else if (failure == null) failure = result;
        }
        if (stats.isEmpty() && failure != null) return ProviderResult.failure(failure.getFailureReason(), failure.getError());
        return ProviderResult.success(new PlayerProfile(uuid, name, stats, null));
    }
}
