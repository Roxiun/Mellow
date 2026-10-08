package com.roxiun.mellow.data;

import com.roxiun.mellow.api.tags.TagReport;

public class TabStats {

    private final TagReport tags;
    public TagReport getTags() { return tags; }
    public TabStats withTags(TagReport tags) {
        return new TabStats(tags, formattedNameWithRank, stars, fkdr, winstreak, wlr,
            bblr, wins, losses, kills, deaths, beds, finals);
    }
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
        TagReport tags,
        String playerName
    ) {
        return new TabStats(
            tags,
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
        TagReport tags,
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
            tags,
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
        TagReport tags,
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
            tags,
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
        TagReport tags,
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
        this.tags = tags == null ? TagReport.empty() : tags;
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
