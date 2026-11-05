package com.roxiun.mellow.data;

import com.roxiun.mellow.api.tags.TagReport;
import java.util.*;

/** Render-ready values keyed by stat ID. Legacy getters serve the vanilla tab integration. */
public class TabStats {
    private final TagReport tags;
    private final String formattedNameWithRank;
    private final Map<String, String> values = new LinkedHashMap<>();
    public TabStats(TagReport tags, String formattedNameWithRank, Map<String, String> values) {
        this.tags = tags == null ? TagReport.empty() : tags;
        this.formattedNameWithRank = formattedNameWithRank;
        this.values.putAll(values);
    }
    public String value(String id) { return values.get(id); }
    public TagReport getTags() { return tags; }
    public TabStats withTags(TagReport tags) { return new TabStats(tags, formattedNameWithRank, values); }
    public static TabStats tagsOnly(TagReport tags, String playerName) {
        return new TabStats(tags, playerName, Collections.emptyMap());
    }
    public String getFormattedNameWithRank() {
        return formattedNameWithRank;
    }

    public String getStars() {
        return value("stars");
    }

    public String getFkdr() {
        return value("fkdr");
    }

    public String getWinstreak() {
        return value("winstreak");
    }

    public String getWlr() {
        return value("wlr");
    }

    public String getBblr() {
        return value("bblr");
    }

    public String getWins() {
        return value("wins");
    }

    public String getLosses() {
        return value("losses");
    }

    public String getKills() {
        return value("kills");
    }

    public String getDeaths() {
        return value("deaths");
    }

    public String getBeds() {
        return value("beds");
    }

    public String getFinals() {
        return value("finals");
    }

    public String getColoredWlr() {
        return value("wlr");
    }

    public String getColoredBblr() {
        return value("bblr");
    }

    public String getColoredWins() {
        return value("wins");
    }

    public String getColoredBeds() {
        return value("beds");
    }

    public String getColoredFinals() {
        return value("finals");
    }
}
