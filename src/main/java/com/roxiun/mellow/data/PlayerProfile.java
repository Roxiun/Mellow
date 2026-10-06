package com.roxiun.mellow.data;

import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.api.coral.CoralTag;
import java.util.List;
import java.util.Locale;

public class PlayerProfile {

    private final String uuid;
    private final String name;
    private final BedwarsPlayer bedwarsPlayer;
    private final SkywarsPlayer skywarsPlayer;
    private final DuelsPlayer duelsPlayer;
    private final BuildBattlePlayer buildBattlePlayer;
    private final TntRunPlayer tntRunPlayer;
    private final List<CoralTag> coralTags;
    private final List<SeraphTag> seraphTags;
    private final List<XadiaTag> xadiaTags;
    private final long lastUpdated;

    public static PlayerProfile identity(String uuid, String name) {
        return new PlayerProfile(
            uuid,
            name,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<CoralTag> coralTags
    ) {
        this(uuid, name, bedwarsPlayer, null, null, coralTags, null);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags
    ) {
        this(uuid, name, bedwarsPlayer, null, null, coralTags, seraphTags);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer,
        List<CoralTag> coralTags
    ) {
        this(uuid, name, bedwarsPlayer, skywarsPlayer, null, coralTags, null);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer,
        DuelsPlayer duelsPlayer,
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags
    ) {
        this(
            uuid,
            name,
            bedwarsPlayer,
            skywarsPlayer,
            duelsPlayer,
            null,
            null,
            coralTags,
            seraphTags
        );
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer,
        DuelsPlayer duelsPlayer,
        BuildBattlePlayer buildBattlePlayer,
        TntRunPlayer tntRunPlayer,
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags
    ) {
        this(uuid, name, bedwarsPlayer, skywarsPlayer, duelsPlayer, buildBattlePlayer,
            tntRunPlayer, coralTags, seraphTags, java.util.Collections.emptyList());
    }

    public PlayerProfile(String uuid, String name, BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer, DuelsPlayer duelsPlayer, BuildBattlePlayer buildBattlePlayer,
        TntRunPlayer tntRunPlayer, List<CoralTag> coralTags, List<SeraphTag> seraphTags,
        List<XadiaTag> xadiaTags) {
        this.xadiaTags = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(xadiaTags));
        this.uuid = uuid;
        this.name = name;
        this.bedwarsPlayer = bedwarsPlayer;
        this.skywarsPlayer = skywarsPlayer;
        this.duelsPlayer = duelsPlayer;
        this.buildBattlePlayer = buildBattlePlayer;
        this.tntRunPlayer = tntRunPlayer;
        this.coralTags = coralTags;
        this.seraphTags = seraphTags;
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

    public List<CoralTag> getCoralTags() {
        return coralTags;
    }

    public List<SeraphTag> getSeraphTags() {
        return seraphTags;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public PlayerProfile withTags(
        List<CoralTag> updatedCoralTags,
        List<SeraphTag> updatedSeraphTags
    ) {
        return new PlayerProfile(
            uuid,
            name,
            bedwarsPlayer,
            skywarsPlayer,
            duelsPlayer,
            buildBattlePlayer,
            tntRunPlayer,
            updatedCoralTags,
            updatedSeraphTags,
            xadiaTags
        );
    }

    public List<XadiaTag> getXadiaTags() { return xadiaTags; }

    public boolean isXadiaTagged() { return !xadiaTags.isEmpty(); }

    public PlayerProfile withXadiaTags(List<XadiaTag> tags) {
        return new PlayerProfile(uuid, name, bedwarsPlayer, skywarsPlayer, duelsPlayer,
            buildBattlePlayer, tntRunPlayer, coralTags, seraphTags, tags);
    }

    public boolean isCoralTagged() {
        return coralTags != null && !coralTags.isEmpty();
    }

    public boolean isSeraphTagged() {
        return seraphTags != null && !seraphTags.isEmpty();
    }

    public TabStats getTabStats() {
        return getTabStats(StatScope.BEDWARS);
    }

    public TabStats getTabStats(StatScope scope) {
        return buildTabStats(scope).withXadiaTags(xadiaTags);
    }

    private TabStats buildTabStats(StatScope scope) {
        if (scope == StatScope.SKYWARS && skywarsPlayer != null) {
            return new TabStats(
                coralTags,
                seraphTags,
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
                coralTags,
                seraphTags,
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
                coralTags,
                seraphTags,
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
                coralTags,
                seraphTags,
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
            return TabStats.tagsOnly(coralTags, seraphTags, name);
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
            coralTags,
            seraphTags,
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
