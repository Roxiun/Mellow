package com.roxiun.mellow.data;

import com.roxiun.mellow.api.tags.TagReport;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import java.util.Locale;

public class PlayerProfile {

    private final String uuid;
    private final String name;
    private final BedwarsPlayer bedwarsPlayer;
    private final SkywarsPlayer skywarsPlayer;
    private final DuelsPlayer duelsPlayer;
    private final BuildBattlePlayer buildBattlePlayer;
    private final TntRunPlayer tntRunPlayer;
    private final TagReport tags;
    private final long lastUpdated;

    public static PlayerProfile identity(String uuid, String name) {
        return new PlayerProfile(uuid, name, null, null, null, null, null, null);
    }

    public PlayerProfile(String uuid, String name, BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer, DuelsPlayer duelsPlayer, BuildBattlePlayer buildBattlePlayer,
        TntRunPlayer tntRunPlayer, TagReport tags) {
        this.uuid = uuid;
        this.name = name;
        this.bedwarsPlayer = bedwarsPlayer;
        this.skywarsPlayer = skywarsPlayer;
        this.duelsPlayer = duelsPlayer;
        this.buildBattlePlayer = buildBattlePlayer;
        this.tntRunPlayer = tntRunPlayer;
        this.tags = tags == null ? TagReport.empty() : tags;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BedwarsPlayer getBedwarsPlayer() {
        return bedwarsPlayer;
    }

    public SkywarsPlayer getSkywarsPlayer() {
        return skywarsPlayer;
    }

    public DuelsPlayer getDuelsPlayer() {
        return duelsPlayer;
    }

    public BuildBattlePlayer getBuildBattlePlayer() {
        return buildBattlePlayer;
    }

    public TntRunPlayer getTntRunPlayer() {
        return tntRunPlayer;
    }

    public TagReport getTags() { return tags; }
    public long getLastUpdated() { return lastUpdated; }
    public PlayerProfile withTags(TagReport tags) {
        return new PlayerProfile(uuid, name, bedwarsPlayer, skywarsPlayer, duelsPlayer,
            buildBattlePlayer, tntRunPlayer, tags);
    }

    public TabStats getTabStats() {
        return getTabStats(StatScope.BEDWARS);
    }

    public TabStats getTabStats(StatScope scope) {
        return buildTabStats(scope);
    }

    private TabStats buildTabStats(StatScope scope) {
        if (scope == StatScope.SKYWARS && skywarsPlayer != null) {
            return new TabStats(
                tags,
                skywarsPlayer.getFormattedNameWithRank(),
                skywarsPlayer.getLevelFormattedWithBrackets(),
                skywarsPlayer.getFormattedKdrWithColor(),
                null,
                skywarsPlayer.getFormattedWlrWithColor(),
                null,
                formatTabCountForDisplay(skywarsPlayer.getFormattedWinsWithColor()),
                formatTabCountForDisplay(skywarsPlayer.getFormattedKillsWithColor()),
                null,
                null
            );
        }

        if (scope == StatScope.DUELS && duelsPlayer != null) {
            return new TabStats(
                tags,
                duelsPlayer.getFormattedNameWithRank(),
                duelsPlayer.getDivision(),
                duelsPlayer.getFormattedKdrWithColor(),
                formatTabCountForDisplay(duelsPlayer.getFormattedWinstreakWithColor()),
                duelsPlayer.getFormattedWlrWithColor(),
                null,
                formatTabCountForDisplay(duelsPlayer.getFormattedWinsWithColor()),
                formatTabCountForDisplay(duelsPlayer.getFormattedLossesWithColor()),
                formatTabCountForDisplay(duelsPlayer.getFormattedKillsWithColor()),
                formatTabCountForDisplay(duelsPlayer.getFormattedDeathsWithColor()),
                null,
                null
            );
        }

        if (scope == StatScope.BUILD_BATTLE && buildBattlePlayer != null) {
            return new TabStats(
                tags,
                buildBattlePlayer.getFormattedNameWithRank(),
                buildBattlePlayer.getFormattedTitle(),
                null,
                null,
                null,
                null,
                formatTabCountForDisplay(buildBattlePlayer.getFormattedWinsWithColor()),
                null,
                null,
                null
            );
        }

        if (scope == StatScope.TNT_RUN && tntRunPlayer != null) {
            return new TabStats(
                tags,
                tntRunPlayer.getFormattedNameWithRank(),
                null,
                null,
                null,
                tntRunPlayer.getFormattedRatioWithColor(),
                null,
                formatTabCountForDisplay(tntRunPlayer.getFormattedWinsWithColor()),
                formatTabCountForDisplay(tntRunPlayer.getFormattedDeathsWithColor()),
                null,
                null,
                null,
                null
            );
        }

        if (bedwarsPlayer == null) {
            return TabStats.tagsOnly(tags, name);
        }

        // Format numbers with appropriate formatting including colors
        String formattedWins = formatTabCountForDisplay(
            getBedwarsPlayer().getFormattedWinsWithColor()
        );
        String formattedBeds = formatTabCountForDisplay(
            getBedwarsPlayer().getFormattedBedsWithColor()
        );
        String formattedFinals = formatTabCountForDisplay(
            getBedwarsPlayer().getFormattedFinalsWithColor()
        );
        String formattedFkdr =
            bedwarsPlayer.getFkdrColor() + bedwarsPlayer.getFormattedFkdr();
        String formattedWinstreak = formatTabCountForDisplay(
            getBedwarsPlayer().getFormattedWinstreakWithColor()
        );
        String formattedWLR = getBedwarsPlayer().getFormattedWLRWithColor();
        String formattedBBLR = getBedwarsPlayer().getFormattedBBLRWithColor();

        return new TabStats(
            tags,
            bedwarsPlayer.getFormattedNameWithRank(),
            bedwarsPlayer.getStars(),
            formattedFkdr,
            formattedWinstreak,
            formattedWLR,
            formattedBBLR,
            formattedWins,
            null,
            formattedBeds,
            formattedFinals
        );
    }

    public static String formatTabCountForDisplay(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int index = 0;
        while (index + 1 < value.length() && value.charAt(index) == '§') {
            index += 2;
        }

        String prefix = value.substring(0, index);
        String numericPart = value.substring(index);
        if (numericPart.isEmpty()) {
            return value;
        }

        try {
            long parsed = Long.parseLong(numericPart);
            return prefix + String.format(Locale.US, "%,d", parsed);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }
}
