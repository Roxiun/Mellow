package com.roxiun.mellow.cache;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.urchin.UrchinApi;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;

public class PlayerCache {

    private final Map<String, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final MojangApi mojangApi;
    private final StatsProvider statsProvider;
    private final UrchinApi urchinApi;
    private final String urchinApiKey;
    private final MellowOneConfig config;

    public PlayerCache(
        MojangApi mojangApi,
        StatsProvider statsProvider,
        UrchinApi urchinApi,
        String urchinApiKey,
        MellowOneConfig config
    ) {
        this.mojangApi = mojangApi;
        this.statsProvider = statsProvider;
        this.urchinApi = urchinApi;
        this.urchinApiKey = urchinApiKey;
        this.config = config;
    }

    public PlayerProfile getProfile(String playerName) {
        String lowerCaseName = playerName.toLowerCase();
        PlayerProfile profile = cache.get(lowerCaseName);

        if (profile != null) {
            return profile;
        }

        return fetchAndCachePlayer(playerName);
    }

    private PlayerProfile fetchAndCachePlayer(String playerName) {
        try {
            BedwarsPlayer bedwarsPlayer = statsProvider.fetchPlayerStats(
                playerName
            );
            if (bedwarsPlayer == null) {
                return null;
            }

            String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
            if (uuid == null || uuid.isEmpty()) {
                uuid = mojangApi.fetchUUID(playerName);
            }

            List<UrchinTag> urchinTags = null;
            if (config.urchin) {
                try {
                    urchinTags = urchinApi.fetchUrchinTags(
                        uuid,
                        playerName,
                        urchinApiKey
                    );
                } catch (IOException e) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        ChatUtils.sendMessage(
                            "§cFailed to fetch Urchin tags for " +
                                playerName +
                                "."
                        )
                    );
                }
            }

            PlayerProfile newProfile = new PlayerProfile(
                uuid,
                playerName,
                bedwarsPlayer,
                urchinTags
            );
            cache.put(playerName.toLowerCase(), newProfile);
            return newProfile;
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCache() {
        cache.clear();
    }

    public void clearPlayer(String playerName) {
        cache.remove(playerName.toLowerCase());
    }
}
