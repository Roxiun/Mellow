package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.stats.ChatStatsFormatter;
import com.roxiun.mellow.stats.StatScope;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class NickUtils {

    private final Set<String> nickedPlayers = new HashSet<>();
    private final Map<String, ResolvedNickProfile> resolvedNickProfiles =
        new HashMap<>();
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
                    if (nickedPlayers.add(normalize(player))) {
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

                                resolveNick(player, realName, ResolutionSource.SKIN);
                            }
                        }
                    }
                }
            }
        }
    }

    public enum ResolutionSource { SKIN, NUMBER }

    // Called on the client thread. Identity is independent of stats availability.
    public boolean resolveNick(String nickName, String realName, ResolutionSource source) {
        return resolveNick(nickName, realName, source, true);
    }

    public boolean resolveNick(String nickName, String realName, ResolutionSource source, boolean automatic) {
        if (nickName == null || realName == null || realName.trim().isEmpty()
            || nickName.equalsIgnoreCase(realName)) return false;
        String key = normalize(nickName);
        ResolvedNickProfile previous = resolvedNickProfiles.get(key);
        if (previous != null && (source == ResolutionSource.NUMBER
            || previous.source == ResolutionSource.SKIN)) return false;

        NetworkPlayerInfo playerInfo = mc.getNetHandler() == null
            ? null : mc.getNetHandler().getPlayerInfo(nickName);
        if (playerInfo == null) return false;
        ResolvedNickProfile resolved = new ResolvedNickProfile(realName, source);
        nickedPlayers.add(key);
        resolvedNickProfiles.put(key, resolved);
        final com.roxiun.mellow.stats.StatsSelection selection = com.roxiun.mellow.stats.GameRegistry.detect(
            HypixelFeatures.getInstance().getGameSnapshot());
        final StatScope scope = selection == null ? null : selection.game().scope();
        AsyncExecutor.getInstance().profileIo(() -> {
            ProfileFetchResult result = playerCache.getSelectedProfileResult(
                realName, selection, ProfileFetchContext.GENERAL, automatic
            );
            MainThreadDispatcher.run(() -> {
                // Clearing the map or replacing this identity invalidates its pending fetch.
                if (resolvedNickProfiles.get(key) != resolved || mc.getNetHandler() == null
                    || mc.getNetHandler().getPlayerInfo(nickName) != playerInfo) return;
                PlayerProfile profile = result.getProfile();
                if (profile == null) {
                    if (!automatic || config.showAutomaticStatsErrors) {
                        ChatUtils.sendMessage("§cFailed to fetch stats for: §r" + realName
                            + "§c (" + StatsFetchFailureFormatter.describe(result) + ")");
                    }
                    return;
                }
                resolved.profile = profile;
                announceProfile(realName, profile, scope, automatic);
            });
        });
        return true;
    }

    private void announceProfile(String realName, PlayerProfile profile, StatScope scope, boolean automatic) {
        String stats = ChatStatsFormatter.format(profile, scope);
        if (!stats.isEmpty()) {
            ChatUtils.sendMessage(stats);
        }
        boolean ignored = automatic && com.roxiun.mellow.Mellow.tagIgnoreManager != null
            && com.roxiun.mellow.Mellow.tagIgnoreManager.isTagIgnored(com.roxiun.mellow.util.UUIDUtils.fromString(profile.getUuid()));
        java.util.Map<String, String> warnings = TagPolicy.warnings(
            profile.getTags(), !automatic || config.printBlacklistTags, ignored);
        for (java.util.Map.Entry<String, String> source : warnings.entrySet())
            ChatUtils.sendMessage("§c" + realName + " is tagged on " + FormattingUtils.formatTagSource(source.getKey(), false) + "§c for: " + source.getValue());
    }

    private static String normalize(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isNicked(String playerName) {
        return playerName != null && nickedPlayers.contains(normalize(playerName));
    }

    public TabStats getResolvedTabStatsForNick(String nickName, StatScope scope) {
        if (nickName == null || scope == null) {
            return null;
        }

        ResolvedNickProfile resolved = resolvedNickProfiles.get(normalize(nickName));
        if (resolved == null || resolved.profile == null) {
            return null;
        }

        return resolved.profile.getTabStats(scope);
    }

    public String getResolvedRealNameForNick(String nickName) {
        if (nickName == null) {
            return null;
        }

        ResolvedNickProfile resolved = resolvedNickProfiles.get(normalize(nickName));
        return resolved == null ? null : resolved.realName;
    }

    public void clearNicks() {
        nickedPlayers.clear();
        resolvedNickProfiles.clear();
    }

    private static class ResolvedNickProfile {

        private final String realName;
        private final ResolutionSource source;
        private PlayerProfile profile;

        private ResolvedNickProfile(String realName, ResolutionSource source) {
            this.realName = realName;
            this.source = source;
        }
    }
}
