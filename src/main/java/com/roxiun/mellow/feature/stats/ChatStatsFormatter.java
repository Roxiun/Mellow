package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.data.PlayerProfile;

public final class ChatStatsFormatter {

    private ChatStatsFormatter() {}

    public static String format(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return formatSkywarsChatStats(profile);
        }
        if (scope == StatScope.DUELS) {
            return formatDuelsChatStats(profile);
        }
        if (scope == StatScope.BUILD_BATTLE) {
            return formatBuildBattleChatStats(profile);
        }
        if (scope == StatScope.TNT_RUN) {
            return formatTntRunChatStats(profile);
        }
        return formatBedwarsChatStats(profile);
    }

    private static String formatBedwarsChatStats(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        return player == null ? "" : player.getStars() + " §r" + player.getFormattedNameWithRank()
            + " §7|§r FKDR: " + player.getFkdrColor() + player.getFormattedFkdr();
    }

    private static String formatSkywarsChatStats(PlayerProfile profile) {
        SkywarsPlayer player = profile.getSkywarsPlayer();
        if (player == null) {
            return "";
        }

        String base = String.format(
            "%s §r%s§r§7 |§r KDR: %s§r§7 |§r WLR: %s§r",
            player.getFormattedNameWithRank(),
            player.getLevelFormatted(),
            player.getFormattedKdrWithColor(),
            player.getFormattedWlrWithColor()
        );

        return base;
    }

    private static String formatDuelsChatStats(PlayerProfile profile) {
        DuelsPlayer player = profile.getDuelsPlayer();
        if (player == null) {
            return "";
        }

        String modeSuffix = player.getMode() == null || player.getMode().isOverall()
            ? " §7(Overall)"
            : " §7(" + player.getMode().getDisplayName() + ")";

        return String.format(
            "%s §r%s§r%s§7 |§r KDR: %s§r§7 |§r WLR: %s§r§7 |§r WS: %s§r",
            player.getFormattedNameWithRank(),
            player.getDivision(),
            modeSuffix,
            player.getFormattedKdrWithColor(),
            player.getFormattedWlrWithColor(),
            player.getFormattedWinstreakWithColor()
        );
    }

    private static String formatBuildBattleChatStats(PlayerProfile profile) {
        BuildBattlePlayer player = profile.getBuildBattlePlayer();
        if (player == null) {
            return "";
        }

        return String.format(
            "%s §r%s§r§7 |§r WINS: %s§r",
            player.getFormattedNameWithRank(),
            player.getFormattedTitle(),
            player.getFormattedWinsWithColor()
        );
    }

    private static String formatTntRunChatStats(PlayerProfile profile) {
        TntRunPlayer player = profile.getTntRunPlayer();
        if (player == null) {
            return "";
        }

        return String.format(
            "%s §r§7|§r WINS: %s§r§7 |§r RATIO: %s§r",
            player.getFormattedNameWithRank(),
            player.getFormattedWinsWithColor(),
            player.getFormattedRatioWithColor()
        );
    }

}
