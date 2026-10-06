package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchContext;
import com.roxiun.mellow.cache.ProfileFetchResult;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.stats.StatsFetchFailureFormatter;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.skins.SkinUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class NickUtils {

    private final Set<String> nickedPlayers = new HashSet<>();
    private final Map<String, ResolvedNickProfile> resolvedNickProfiles =
        new ConcurrentHashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final PlayerCache playerCache;
    private final MellowOneConfig config;

    public NickUtils(PlayerCache playerCache, MellowOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    public void updateNickedPlayers(Collection<String> onlinePlayers) {
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        Map<String, NetworkPlayerInfo> playerInfoMap = new HashMap<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info != null && info.getGameProfile() != null) {
                playerInfoMap.put(info.getGameProfile().getName(), info);
            }
        }

        for (String player : onlinePlayers) {
            NetworkPlayerInfo playerInfo = playerInfoMap.get(player);
            if (
                playerInfo != null &&
                playerInfo.getGameProfile().getId() != null
            ) {
                UUID uuid = playerInfo.getGameProfile().getId();
                if (uuid.version() == 1) {
                    if (nickedPlayers.add(player)) {
                        String nickedPlayerDisplay =
                            FormattingUtils.formatNickedPlayerName(player);

                        ChatUtils.sendMessage(
                            nickedPlayerDisplay + " §dis a nicked player!"
                        );

                        if (config.autoSkinDenick) {
                            String realName = SkinUtils.getRealName(playerInfo);
                            if (
                                realName != null &&
                                !realName.equalsIgnoreCase(player)
                            ) {
                                ChatUtils.sendMessage(
                                    nickedPlayerDisplay +
                                        " §ddenicked as §a" +
                                        realName
                                );

                                final String finalRealName = realName;
                                AsyncExecutor.getInstance().profileIo(() -> {
                                    ProfileFetchResult result =
                                        playerCache.getScopedProfileResult(
                                            finalRealName,
                                            StatScope.BEDWARS,
                                            ProfileFetchContext.GENERAL,
                                            true
                                        );
                                    PlayerProfile profile = result.getProfile();

                                    if (profile == null) {
                                        MainThreadDispatcher.run(() ->
                                            ChatUtils.sendMessage(
                                                "§cFailed to fetch stats for: §r" +
                                                    finalRealName +
                                                    "§c (" +
                                                    StatsFetchFailureFormatter.describe(
                                                        result
                                                    ) +
                                                    ")"
                                            )
                                        );
                                        return;
                                    }

                                    resolvedNickProfiles.put(
                                        player,
                                        new ResolvedNickProfile(
                                            finalRealName,
                                            profile
                                        )
                                    );

                                    BedwarsPlayer bwPlayer =
                                        profile.getBedwarsPlayer();
                                    if (bwPlayer != null) {
                                        String statsMessage =
                                            bwPlayer.getStars() +
                                            " §r" +
                                            bwPlayer.getFormattedNameWithRank() +
                                            " §7|§r FKDR: " +
                                            bwPlayer.getFkdrColor() +
                                            bwPlayer.getFormattedFkdr();

                                        MainThreadDispatcher.run(() ->
                                            ChatUtils.sendMessage(statsMessage)
                                        );
                                    }

                                    if (
                                        config.isCoralEnabled() &&
                                        profile.isCoralTagged()
                                    ) {
                                        String tags =
                                            FormattingUtils.formatCoralTags(
                                                profile.getCoralTags()
                                            );
                                        String coralMessage =
                                            "§c" +
                                            finalRealName +
                                            " is tagged on §5Coral§c for: " +
                                            tags;
                                        MainThreadDispatcher.run(() ->
                                            ChatUtils.sendMessage(coralMessage)
                                        );
                                    }

                                    if (
                                        config.xadia &&
                                        profile.isXadiaTagged()
                                    ) {
                                        String tags =
                                            FormattingUtils.formatXadiaTags(
                                                profile.getXadiaTags()
                                            );
                                        String xadiaMessage =
                                            "§c" +
                                            finalRealName +
                                            " is tagged on §5Xadia§c for: " +
                                            tags;
                                        MainThreadDispatcher.run(() ->
                                            ChatUtils.sendMessage(xadiaMessage)
                                        );
                                    }

                                    if (
                                        config.seraph &&
                                        profile.isSeraphTagged()
                                    ) {
                                        String formattedTags =
                                            FormattingUtils.formatSeraphTags(
                                                profile.getSeraphTags()
                                            );
                                        // Split the formatted tags by the newline separator and send as separate messages
                                        String[] tagMessages =
                                            formattedTags.split("\n§c");
                                        if (
                                            tagMessages.length > 0 &&
                                            !tagMessages[0].trim().isEmpty()
                                        ) {
                                            // Send the first tag with the main message
                                            String firstMessage =
                                                "§c" +
                                                finalRealName +
                                                " is tagged on §3Seraph§c for: " +
                                                tagMessages[0];
                                            MainThreadDispatcher.run(() ->
                                                ChatUtils.sendMessage(
                                                    firstMessage
                                                )
                                            );
                                            // Send additional tags as separate messages
                                            for (
                                                int i = 1;
                                                i < tagMessages.length;
                                                i++
                                            ) {
                                                if (
                                                    !tagMessages[i].trim().isEmpty()
                                                ) {
                                                    String additionalMessage =
                                                        "§c" + tagMessages[i];
                                                    MainThreadDispatcher.run(() ->
                                                        ChatUtils.sendMessage(
                                                            additionalMessage
                                                        )
                                                    );
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isNicked(String playerName) {
        return nickedPlayers.contains(playerName);
    }

    public TabStats getResolvedTabStatsForNick(String nickName, StatScope scope) {
        if (nickName == null || scope == null) {
            return null;
        }

        ResolvedNickProfile resolved = resolvedNickProfiles.get(nickName);
        if (resolved == null || resolved.profile == null) {
            return null;
        }

        return resolved.profile.getTabStats(scope);
    }

    public String getResolvedRealNameForNick(String nickName) {
        if (nickName == null) {
            return null;
        }

        ResolvedNickProfile resolved = resolvedNickProfiles.get(nickName);
        return resolved == null ? null : resolved.realName;
    }

    public void clearNicks() {
        nickedPlayers.clear();
        resolvedNickProfiles.clear();
    }

    private static class ResolvedNickProfile {

        private final String realName;
        private final PlayerProfile profile;

        private ResolvedNickProfile(String realName, PlayerProfile profile) {
            this.realName = realName;
            this.profile = profile;
        }
    }
}
