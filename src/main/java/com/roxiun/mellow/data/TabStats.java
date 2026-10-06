package com.roxiun.mellow.data;

import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.coral.CoralTag;
import java.util.List;

public class TabStats {

    private List<XadiaTag> xadiaTags = java.util.Collections.emptyList();

    public TabStats withXadiaTags(List<XadiaTag> tags) {
        TabStats copy = new TabStats(coralTags, seraphTags, formattedNameWithRank, stars, fkdr,
            winstreak, wlr, bblr, wins, losses, kills, deaths, beds, finals);
        copy.xadiaTags = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(tags));
        return copy;
    }

    public List<XadiaTag> getXadiaTags() { return xadiaTags; }
    public boolean isXadiaTagged() { return !xadiaTags.isEmpty(); }

    private final List<CoralTag> coralTags;
    private final List<SeraphTag> seraphTags;
    private final String formattedNameWithRank;
    private final String stars;
    private final String fkdr;
    private final String winstreak;
    private final String wlr;
    private final String bblr;
    private final String wins;
    private final String losses;
    private final String kills;
    private final String deaths;
    private final String beds;
    private final String finals;

    public static TabStats tagsOnly(
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags,
        String playerName
    ) {
        return new TabStats(
            coralTags,
            seraphTags,
            playerName,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public TabStats(
        List<CoralTag> coralTags,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String kills,
        String beds,
        String finals
    ) {
        this(
            coralTags,
            null,
            null,
            stars,
            fkdr,
            winstreak,
            wlr,
            bblr,
            wins,
            null,
            kills,
            null,
            beds,
            finals
        );
    }

    public TabStats(
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags,
        String formattedNameWithRank,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String kills,
        String beds,
        String finals
    ) {
        this(
            coralTags,
            seraphTags,
            formattedNameWithRank,
            stars,
            fkdr,
            winstreak,
            wlr,
            bblr,
            wins,
            null,
            kills,
            null,
            beds,
            finals
        );
    }

    public TabStats(
        List<CoralTag> coralTags,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String beds,
        String finals
    ) {
        this(
            coralTags,
            null,
            null,
            stars,
            fkdr,
            winstreak,
            wlr,
            bblr,
            wins,
            null,
            null,
            null,
            beds,
            finals
        );
    }

    public TabStats(
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags,
        String formattedNameWithRank,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String beds,
        String finals
    ) {
        this(
            coralTags,
            seraphTags,
            formattedNameWithRank,
            stars,
            fkdr,
            winstreak,
            wlr,
            bblr,
            wins,
            null,
            null,
            null,
            beds,
            finals
        );
    }

    public TabStats(
        List<CoralTag> coralTags,
        List<SeraphTag> seraphTags,
        String formattedNameWithRank,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String losses,
        String kills,
        String deaths,
        String beds,
        String finals
    ) {
        this.coralTags = coralTags;
        this.seraphTags = seraphTags;
        this.formattedNameWithRank = formattedNameWithRank;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
        this.wlr = wlr;
        this.bblr = bblr;
        this.wins = wins;
        this.losses = losses;
        this.kills = kills;
        this.deaths = deaths;
        this.beds = beds;
        this.finals = finals;
    }

    public boolean isCoralTagged() {
        return coralTags != null && !coralTags.isEmpty();
    }

    public boolean isSeraphTagged() {
        return seraphTags != null && !seraphTags.isEmpty();
    }

    public List<CoralTag> getCoralTags() {
        return coralTags;
    }

    public List<SeraphTag> getSeraphTags() {
        return seraphTags;
    }

    public String getFormattedNameWithRank() {
        return formattedNameWithRank;
    }

    public String getStars() {
        return stars;
    }

    public String getFkdr() {
        return fkdr;
    }

    public String getWinstreak() {
        return winstreak;
    }

    public String getWlr() {
        return wlr;
    }

    public String getBblr() {
        return bblr;
    }

    public String getWins() {
        return wins;
    }

    public String getLosses() {
        return losses;
    }

    public String getKills() {
        return kills;
    }

    public String getDeaths() {
        return deaths;
    }

    public String getBeds() {
        return beds;
    }

    public String getFinals() {
        return finals;
    }

    public String getColoredWlr() {
        return wlr;
    }

    public String getColoredBblr() {
        return bblr;
    }

    public String getColoredWins() {
        return wins;
    }

    public String getColoredBeds() {
        return beds;
    }

    public String getColoredFinals() {
        return finals;
    }
}
