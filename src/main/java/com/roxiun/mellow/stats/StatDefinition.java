package com.roxiun.mellow.stats;

import com.roxiun.mellow.data.TabStats;

/** Display metadata shared by settings, tab layout and rendering. */
public final class StatDefinition {
    public enum Style { VALUE, BADGE, TITLE, BEDWARS_STARS, BEDWARS_WINSTREAK, TEAM, NAME, NONE, HEALTH, TAGS, PING }
    private final String id, label, option;
    private final int width, maximumWidth;
    private final Style style;

    public StatDefinition(String id, String label, String option, int width, int maximumWidth,
                          Style style) {
        this.id = id;
        this.label = label;
        this.option = option;
        this.width = width;
        this.maximumWidth = maximumWidth;
        this.style = style;
    }
    public String id() { return id; }
    public String label() { return label; }
    public String option() { return option; }
    public int width() { return width; }
    public int maximumWidth() { return maximumWidth; }
    public Style style() { return style; }
    public String value(TabStats stats) { return stats == null ? null : stats.value(id); }

    public static StatDefinition team() { return new StatDefinition("team", "TEAM", "Team", 28, 40, Style.TEAM); }
    public static StatDefinition name() { return new StatDefinition("name", "NAME", "Name", 120, 220, Style.NAME); }
    public static StatDefinition none() { return new StatDefinition("none", "", "None", 36, 72, Style.NONE); }
    public static StatDefinition health() { return new StatDefinition("health", "HP", "HP", 24, 34, Style.HEALTH); }
    public static StatDefinition tags() { return new StatDefinition("tags", "TAGS", "Tags", 36, 110, Style.TAGS); }
    public static StatDefinition ping() { return new StatDefinition("ping", "PING", "Ping", 30, 72, Style.PING); }
}
