package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.stats.StatScopeResolver;
import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns;
import com.roxiun.mellow.feature.stats.tab.TabHealthValueResolver;
import com.roxiun.mellow.util.formatting.FormattingUtils;
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
        if (!Mellow.isEnabled() || !Mellow.config.tabStats) {
            return;
        }

        String playerName = networkPlayerInfoIn.getGameProfile().getName();
        if (playerName == null) {
            return;
        }

        StatScope scope = resolveTabStatScope();
        boolean isNicked =
            Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName);
        TabStats stats = Mellow.tabStats.get(playerName);
        String resolvedRealName = Mellow.nickUtils == null
            ? null
            : Mellow.nickUtils.getResolvedRealNameForNick(playerName);
        if (stats == null && isNicked && Mellow.nickUtils != null) {
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
                String teamColor = team.length() >= 2
                    ? team.substring(0, 2)
                    : "";

                // Create a minimal TabStats object for the nick case
                TabStats emptyStats = new TabStats(
                    null, // urchinTags
                    null, // seraphTags
                    null, // formattedNameWithRank
                    null, // stars
                    null, // fkdr
                    null, // winstreak
                    null, // wlr
                    null, // bblr
                    null, // wins
                    null, // beds
                    null // finals
                );

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
                String teamColor = team.length() >= 2
                    ? team.substring(0, 2)
                    : "";

                // Create a minimal TabStats object for the nick case
                TabStats emptyStats = new TabStats(
                    null, // urchinTags
                    null, // seraphTags
                    null, // formattedNameWithRank
                    null, // stars
                    null, // fkdr
                    null, // winstreak
                    null, // wlr
                    null, // bblr
                    null, // wins
                    null, // beds
                    null // finals
                );

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

        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
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

        if (Mellow.config.showUrchinTagsInTab && stats.isUrchinTagged()) {
            for (UrchinTag tag : stats.getUrchinTags()) {
                newDisplayName +=
                    " " + FormattingUtils.formatUrchinTagIcon(tag);
            }
        }

        if (Mellow.config.showSeraphTagsInTab && stats.isSeraphTagged()) {
            for (SeraphTag tag : stats.getSeraphTags()) {
                newDisplayName +=
                    " " + FormattingUtils.formatSeraphTagIcon(tag);
            }
        }

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
        if (scope == StatScope.SKYWARS) {
            return processSkywarsDynamicStat(
                statIndex,
                team,
                name,
                teamColor,
                stats,
                resolvedRealName,
                playerInfo
            );
        }
        if (scope == StatScope.DUELS) {
            return processDuelsDynamicStat(
                statIndex,
                team,
                name,
                teamColor,
                stats,
                resolvedRealName,
                playerInfo
            );
        }
        if (scope == StatScope.BUILD_BATTLE) {
            return processBuildBattleDynamicStat(
                statIndex,
                team,
                name,
                teamColor,
                stats,
                resolvedRealName,
                playerInfo
            );
        }
        if (scope == StatScope.TNT_RUN) {
            return processTntRunDynamicStat(
                statIndex,
                team,
                name,
                teamColor,
                stats,
                resolvedRealName,
                playerInfo
            );
        }
        return processBedwarsDynamicStat(
            statIndex,
            team,
            name,
            teamColor,
            stats,
            resolvedRealName,
            playerInfo
        );
    }

    private String[] processBedwarsDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        String stars = stats.getStars();
        String fkdr = stats.getFkdr();

        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Stars (shows Nick instead if player is nicks)
                boolean isNicked =
                    Mellow.nickUtils != null && Mellow.nickUtils.isNicked(name);
                if (isNicked && (stars == null || stars.isEmpty())) {
                    if (Mellow.config.showNickWithBrackets) {
                        return new String[] { "§5[§lNICK§r§5]§r", "false" };
                    } else {
                        return new String[] { "§5§lNICK§r", "false" };
                    }
                } else if (stars != null && !stars.isEmpty()) {
                    return new String[] {
                        formatStarsForTab(
                            stars,
                            Mellow.config.showStarsWithBrackets
                        ),
                        "false",
                    };
                }
                break;
            case 2: // Name
                if (hasResolvedRealName(resolvedRealName)) {
                    return new String[] {
                        buildDenickedName(teamColor, name, resolvedRealName),
                        "false",
                    };
                }
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // FKDR
                if (fkdr != null && !fkdr.isEmpty()) {
                    return new String[] { fkdr, "false" };
                }
                break;
            case 4: // Winstreak
                if (
                    stats.getWinstreak() != null &&
                    !stats.getWinstreak().isEmpty()
                ) {
                    return new String[] { stats.getWinstreak(), "false" };
                }
                break;
            case 5: // WLR
                if (stats.getWlr() != null && !stats.getWlr().isEmpty()) {
                    return new String[] { stats.getWlr(), "false" }; // The color is already included in the string
                }
                break;
            case 6: // BBLR
                if (stats.getBblr() != null && !stats.getBblr().isEmpty()) {
                    return new String[] { stats.getBblr(), "false" }; // The color is already included in the string (if implemented)
                }
                break;
            case 7: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" }; // The color is already included in the string
                }
                break;
            case 8: // Beds
                if (stats.getBeds() != null && !stats.getBeds().isEmpty()) {
                    return new String[] { stats.getBeds(), "false" }; // The color is already included in the string
                }
                break;
            case 9: // Finals
                if (stats.getFinals() != null && !stats.getFinals().isEmpty()) {
                    return new String[] { stats.getFinals(), "false" }; // The color is already included in the string
                }
                break;
            case ExtendedTabStatsColumns.BEDWARS_HP_INDEX: // HP
                return new String[] {
                    TabHealthValueResolver.getFormattedHealth(
                        Minecraft.getMinecraft(),
                        playerInfo
                    ),
                    "false",
                };
            case ExtendedTabStatsColumns.BEDWARS_NONE_INDEX: // None
                return null;
        }
        return null;
    }

    private String[] processSkywarsDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        String level = stats.getStars();
        String kdr = stats.getFkdr();

        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Level (shows Nick instead if player is nicked)
                boolean isNicked =
                    Mellow.nickUtils != null && Mellow.nickUtils.isNicked(name);
                if (isNicked && (level == null || level.isEmpty())) {
                    if (Mellow.config.showNickWithBrackets) {
                        return new String[] { "§5[§lNICK§r§5]§r", "false" };
                    } else {
                        return new String[] { "§5§lNICK§r", "false" };
                    }
                } else if (level != null && !level.isEmpty()) {
                    return new String[] { level + "§r", "false" };
                }
                break;
            case 2: // Name
                if (hasResolvedRealName(resolvedRealName)) {
                    return new String[] {
                        buildDenickedName(teamColor, name, resolvedRealName),
                        "false",
                    };
                }
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // KDR
                if (kdr != null && !kdr.isEmpty()) {
                    return new String[] { kdr, "false" };
                }
                break;
            case 4: // WLR
                if (stats.getWlr() != null && !stats.getWlr().isEmpty()) {
                    return new String[] { stats.getWlr(), "false" };
                }
                break;
            case 5: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" };
                }
                break;
            case 6: // Kills
                if (stats.getKills() != null && !stats.getKills().isEmpty()) {
                    return new String[] { stats.getKills(), "false" };
                }
                break;
            case ExtendedTabStatsColumns.SKYWARS_HP_INDEX: // HP
                return new String[] {
                    TabHealthValueResolver.getFormattedHealth(
                        Minecraft.getMinecraft(),
                        playerInfo
                    ),
                    "false",
                };
            case ExtendedTabStatsColumns.SKYWARS_NONE_INDEX: // None
                return null;
        }

        return null;
    }

    private String[] processDuelsDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        String division = stats.getStars();
        String kdr = stats.getFkdr();

        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Division (shows Nick instead if player is nicked)
                boolean isNicked =
                    Mellow.nickUtils != null && Mellow.nickUtils.isNicked(name);
                if (isNicked && (division == null || division.isEmpty())) {
                    if (Mellow.config.showNickWithBrackets) {
                        return new String[] { "§5[§lNICK§r§5]§r", "false" };
                    } else {
                        return new String[] { "§5§lNICK§r", "false" };
                    }
                } else if (division != null && !division.isEmpty()) {
                    return new String[] { division + "§r", "false" };
                }
                break;
            case 2: // Name
                if (hasResolvedRealName(resolvedRealName)) {
                    return new String[] {
                        buildDenickedName(teamColor, name, resolvedRealName),
                        "false",
                    };
                }
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // KDR
                if (kdr != null && !kdr.isEmpty()) {
                    return new String[] { kdr, "false" };
                }
                break;
            case 4: // WLR
                if (stats.getWlr() != null && !stats.getWlr().isEmpty()) {
                    return new String[] { stats.getWlr(), "false" };
                }
                break;
            case 5: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" };
                }
                break;
            case 6: // Losses
                if (stats.getLosses() != null && !stats.getLosses().isEmpty()) {
                    return new String[] { stats.getLosses(), "false" };
                }
                break;
            case 7: // Kills
                if (stats.getKills() != null && !stats.getKills().isEmpty()) {
                    return new String[] { stats.getKills(), "false" };
                }
                break;
            case 8: // Deaths
                if (stats.getDeaths() != null && !stats.getDeaths().isEmpty()) {
                    return new String[] { stats.getDeaths(), "false" };
                }
                break;
            case 9: // Winstreak
                if (stats.getWinstreak() != null && !stats.getWinstreak().isEmpty()) {
                    return new String[] { stats.getWinstreak(), "false" };
                }
                break;
            case ExtendedTabStatsColumns.DUELS_HP_INDEX: // HP
                return new String[] {
                    TabHealthValueResolver.getFormattedHealth(
                        Minecraft.getMinecraft(),
                        playerInfo
                    ),
                    "false",
                };
            case ExtendedTabStatsColumns.DUELS_NONE_INDEX: // None
                return null;
        }

        return null;
    }

    private String[] processBuildBattleDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        String title = stats.getStars();

        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Title
                boolean isNicked =
                    Mellow.nickUtils != null && Mellow.nickUtils.isNicked(name);
                if (isNicked && (title == null || title.isEmpty())) {
                    if (Mellow.config.showNickWithBrackets) {
                        return new String[] { "§5[§lNICK§r§5]§r", "false" };
                    } else {
                        return new String[] { "§5§lNICK§r", "false" };
                    }
                } else if (title != null && !title.isEmpty()) {
                    return new String[] { title + "§r", "false" };
                }
                break;
            case 2: // Name
                if (hasResolvedRealName(resolvedRealName)) {
                    return new String[] {
                        buildDenickedName(teamColor, name, resolvedRealName),
                        "false",
                    };
                }
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" };
                }
                break;
            case ExtendedTabStatsColumns.BUILD_BATTLE_HP_INDEX: // HP
                return new String[] {
                    TabHealthValueResolver.getFormattedHealth(
                        Minecraft.getMinecraft(),
                        playerInfo
                    ),
                    "false",
                };
            case ExtendedTabStatsColumns.BUILD_BATTLE_NONE_INDEX: // None
                return null;
        }
        return null;
    }

    private String[] processTntRunDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        String resolvedRealName,
        NetworkPlayerInfo playerInfo
    ) {
        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" };
                }
                break;
            case 2: // Name
                if (hasResolvedRealName(resolvedRealName)) {
                    return new String[] {
                        buildDenickedName(teamColor, name, resolvedRealName),
                        "false",
                    };
                }
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // Ratio
                if (stats.getWlr() != null && !stats.getWlr().isEmpty()) {
                    return new String[] { stats.getWlr(), "false" };
                }
                break;
            case ExtendedTabStatsColumns.TNT_RUN_HP_INDEX: // HP
                return new String[] {
                    TabHealthValueResolver.getFormattedHealth(
                        Minecraft.getMinecraft(),
                        playerInfo
                    ),
                    "false",
                };
            case ExtendedTabStatsColumns.TNT_RUN_NONE_INDEX: // None
                return null;
        }
        return null;
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

    private boolean shouldShowRankInTabName() {
        if (HypixelFeatures.getInstance().getGameSnapshot() == null) {
            return false;
        }
        if (HypixelFeatures.getInstance().getGameSnapshot().isLobby()) {
            return true;
        }
        return Mellow.config.showRanksInGameTabStats;
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
