package com.roxiun.mellow.mixin;

import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.api.tags.PlayerTag;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.stats.StatDefinition;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.stats.StatScopeResolver;
import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns;
import com.roxiun.mellow.feature.stats.tab.TabHealthValueResolver;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiPlayerTabOverlay.class)
public class GuiPlayerTabOverlayMixin {

    private static final String MIDDLE_DOT = "\u30fb";

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(
        NetworkPlayerInfo networkPlayerInfoIn,
        CallbackInfoReturnable<String> cir
    ) {
        //? if ornithe {
        if (Mellow.config == null || !Mellow.config.tabStats ||
            com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration.active()) {
        //?} else {
        /*if (Mellow.config == null || !Mellow.config.tabStats
            || com.roxiun.mellow.feature.stats.tab.VanillaHudTabIntegration.active()) {
        *///?}
            return;
        }

        String playerName = networkPlayerInfoIn.getGameProfile().getName();
        if (playerName == null) {
            return;
        }

        StatScope scope = resolveTabStatScope();
        if (scope == null) return;
        boolean isNicked =
            Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName);
        TabStats stats = Mellow.tabStats.get(playerName);
        String resolvedRealName = Mellow.nickUtils == null
            ? null
            : Mellow.nickUtils.getResolvedRealNameForNick(playerName);
        if (resolvedRealName != null && Mellow.nickUtils != null) {
            stats = Mellow.nickUtils.getResolvedTabStatsForNick(
                playerName,
                scope
            );
        }
        String originalDisplayName = getOriginalDisplayName(
            networkPlayerInfoIn
        );
        UUID playerUUID = networkPlayerInfoIn.getGameProfile().getId();

        String newDisplayName;

        if (stats != null) {
            newDisplayName = handlePlayerWithStats(
                networkPlayerInfoIn,
                playerName,
                stats,
                scope,
                resolvedRealName
            );
        } else if (isNicked && !originalDisplayName.contains("§8[§5NICK§8]")) {
            // For nicks without stats, still handle them within the dynamic system
            String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
            if (tabData != null && tabData.length >= 2) {
                String team = tabData[0];
                String name = tabData[1];
                String teamColor = PlayerUtils.getTeamColor(team);

                // Create a minimal TabStats object for the nick case
                TabStats emptyStats = TabStats.tagsOnly(null, null);

                newDisplayName = formatDisplayNameWithStats(
                    networkPlayerInfoIn,
                    team,
                    name,
                    teamColor,
                    emptyStats,
                    scope,
                    resolvedRealName
                );
            } else {
                // Fallback: create a basic tab structure from network info
                String team = ScorePlayerTeam.formatPlayerName(
                    networkPlayerInfoIn.getPlayerTeam(),
                    playerName
                );
                String name = playerName;
                String teamColor = PlayerUtils.getTeamColor(team);

                // Create a minimal TabStats object for the nick case
                TabStats emptyStats = TabStats.tagsOnly(null, null);

                newDisplayName = formatDisplayNameWithStats(
                    networkPlayerInfoIn,
                    team,
                    name,
                    teamColor,
                    emptyStats,
                    scope,
                    resolvedRealName
                );
            }
        } else {
            newDisplayName = originalDisplayName;
        }

        newDisplayName = appendListTags(newDisplayName, playerUUID);

        if (!originalDisplayName.equals(newDisplayName)) {
            cir.setReturnValue(newDisplayName);
        }
    }

    private String handlePlayerWithStats(
        NetworkPlayerInfo playerInfo,
        String playerName,
        TabStats stats,
        StatScope scope,
        String resolvedRealName
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData == null || tabData.length < 2) {
            return "";
        }
        String team = tabData[0];
        String name = tabData[1];

        String teamColor = PlayerUtils.getTeamColor(team);
        return formatDisplayNameWithStats(
            playerInfo,
            team,
            name,
            teamColor,
            stats,
            scope,
            resolvedRealName
        );
    }

    private String formatDisplayNameWithStats(
        NetworkPlayerInfo playerInfo,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope,
        String resolvedRealName
    ) {
        String newDisplayName = buildOrderedStatsString(
            playerInfo,
            team,
            name,
            teamColor,
            stats,
            scope,
            resolvedRealName
        );

        for (PlayerTag tag : TagPolicy.visible(stats.getTags(), Mellow.config))
            newDisplayName += " " + tag.getIcon();

        return newDisplayName;
    }

    private String buildOrderedStatsString(
        NetworkPlayerInfo playerInfo,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope,
        String resolvedRealName
    ) {
        return buildDynamicOrderedString(
            playerInfo,
            team,
            name,
            teamColor,
            stats,
            scope,
            resolvedRealName
        );
    }

    private String buildDynamicOrderedString(
        NetworkPlayerInfo playerInfo,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope,
        String resolvedRealName
    ) {
        // Collect all valid stat parts with their type information
        java.util.List<
            java.util.Map.Entry<String, Integer>
        > validPartsWithType = new java.util.ArrayList<>();

        // Process each stat in the configured order with type tracking
        for (int statIndex : getConfiguredStatsForScope(scope)) {
            addValidPartWithConfigStat(
                validPartsWithType,
                statIndex,
                team,
                name,
                teamColor,
                stats,
                scope,
                resolvedRealName,
                playerInfo
            );
        }

        // Build the string with configurable dot separators between positions
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < validPartsWithType.size(); i++) {
            if (i > 0) {
                // Determine which separator to use based on the position and previous element type
                boolean previousIsTeam =
                    validPartsWithType.get(i - 1).getValue() == 0; // Team type is 0

                if (i == 1) {
                    // Between 1st and 2nd (index 0 and 1)
                    if (Mellow.config.showDot12) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 2) {
                    // Between 2nd and 3rd (index 1 and 2)
                    if (Mellow.config.showDot23) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 3) {
                    // Between 3rd and 4th (index 2 and 3)
                    if (Mellow.config.showDot34) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 4) {
                    // Between 4th and 5th (index 3 and 4)
                    if (Mellow.config.showDot45) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 5) {
                    // Between 5th and 6th (index 4 and 5)
                    if (Mellow.config.showDot56) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 6) {
                    // Between 6th and 7th (index 5 and 6)
                    if (Mellow.config.showDot67) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 7) {
                    // Between 7th and 8th (index 6 and 7)
                    if (Mellow.config.showDot78) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 8) {
                    // Between 8th and 9th (index 7 and 8)
                    if (Mellow.config.showDot89) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 9) {
                    // Between 9th and 10th (index 8 and 9)
                    if (Mellow.config.showDot910) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                //? if ornithe {
                } else if (!previousIsTeam) {
                    // The reorderable list can enable more than the old ten slots.
                    result.append(MIDDLE_DOT).append("§r");
                //?}
                }
            }
            result.append(validPartsWithType.get(i).getKey());
        }

        return result.toString();
    }

    private void addValidPartWithConfigStat(
        java.util.List<java.util.Map.Entry<String, Integer>> validPartsWithType,
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        String[] statParts = processDynamicStat(
            statIndex,
            team,
            name,
            teamColor,
            stats,
            scope,
            resolvedRealName,
            playerInfo
        );
        if (statParts != null && !statParts[0].trim().isEmpty()) {
            // Create an entry with the stat value and its type (statIndex)
            validPartsWithType.add(
                new java.util.AbstractMap.SimpleEntry<>(statParts[0], statIndex)
            );
        }
    }

    private String[] processDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        StatDefinition stat = ExtendedTabStatsColumns.definition(scope, statIndex);
        if (stat == null) return null;
        String value = stat.value(stats);
        switch (stat.style()) {
            case TEAM: return new String[] { team, "false" };
            case NAME:
                if (hasResolvedRealName(resolvedRealName)) return new String[] { buildDenickedName(teamColor, name, resolvedRealName), "false" };
                return new String[] { com.roxiun.mellow.feature.stats.tab.TabNameFormatter.format(
                    playerInfo, team, stats == null ? null : stats.getFormattedNameWithRank(),
                    HypixelFeatures.getInstance().getGameSnapshot(), Mellow.config.showRanksInGameTabStats,
                    Mellow.nickUtils != null && Mellow.nickUtils.isNicked(name)), "false" };
            case BEDWARS_STARS: case BADGE: case TITLE:
                boolean nicked = Mellow.nickUtils != null && Mellow.nickUtils.isNicked(name);
                if (nicked && (value == null || value.isEmpty())) {
                    return new String[] { Mellow.config.showNickWithBrackets ? "§5[§lNICK§r§5]§r" : "§5§lNICK§r", "false" };
                }
                if (value == null || value.isEmpty()) return null;
                return new String[] { stat.style() == StatDefinition.Style.BEDWARS_STARS
                    ? formatStarsForTab(value, Mellow.config.showStarsWithBrackets) : value + "§r", "false" };
            case HEALTH:
                return new String[] { TabHealthValueResolver.getFormattedHealth(Minecraft.getMinecraft(), playerInfo), "false" };
            case NONE: case TAGS: case PING: return null;
            default: return value == null || value.isEmpty() ? null : new String[] { value, "false" };
        }
    }

    private int[] getConfiguredStatsForScope(StatScope scope) {
        return ExtendedTabStatsColumns.getConfiguredStatsForScope(
            scope,
            Mellow.config
        );
    }

    private String appendListTags(String displayName, UUID playerUUID) {
        String safeDisplayName = displayName == null ? "" : displayName;
        if (playerUUID == null) {
            return safeDisplayName;
        }
        if (
            Mellow.blacklistManager != null &&
            Mellow.blacklistManager.isBlacklisted(playerUUID)
        ) {
            safeDisplayName += " §8[§4LIST§8]";
        }
        if (
            Mellow.annoylistManager != null &&
            Mellow.annoylistManager.isAnnoylisted(playerUUID)
        ) {
            safeDisplayName += " §8[§3ANNOY§8]";
        }
        return safeDisplayName;
    }

    private StatScope resolveTabStatScope() {
        return StatScopeResolver.resolveInGameScope(
            HypixelFeatures.getInstance().getGameSnapshot()
        );
    }

    private String getOriginalDisplayName(
        NetworkPlayerInfo networkPlayerInfoIn
    ) {
        if (networkPlayerInfoIn.getDisplayName() != null) {
            return networkPlayerInfoIn.getDisplayName().getFormattedText();
        }
        return ScorePlayerTeam.formatPlayerName(
            networkPlayerInfoIn.getPlayerTeam(),
            networkPlayerInfoIn.getGameProfile().getName()
        );
    }

    private String formatStarsForTab(String stars, boolean withBrackets) {
        if (stars == null || stars.isEmpty()) {
            return "";
        }

        String result;
        if (withBrackets) {
            result = hasOuterBrackets(stars) ? stars : "§7[" + stars + "§7]";
        } else {
            result = stripOuterBrackets(stars);
        }
        return result + "§r";
    }

    private String stripOuterBrackets(String value) {
        if (!hasOuterBrackets(value)) {
            return value;
        }

        int open = value.indexOf('[');
        int close = value.lastIndexOf(']');
        if (open >= 0 && close > open) {
            return (
                value.substring(0, open) +
                value.substring(open + 1, close) +
                value.substring(close + 1)
            );
        }
        return value;
    }

    private boolean hasOuterBrackets(String value) {
        String plain = value.replaceAll("§.", "");
        return plain.startsWith("[") && plain.endsWith("]");
    }

    private boolean hasResolvedRealName(String resolvedRealName) {
        return resolvedRealName != null && !resolvedRealName.trim().isEmpty();
    }

    private String buildDenickedName(
        String teamColor,
        String nickedName,
        String resolvedRealName
    ) {
        return (
            "§r" +
            teamColor +
            nickedName +
            " §7(" +
            resolvedRealName +
            "§7)"
        );
    }
}
