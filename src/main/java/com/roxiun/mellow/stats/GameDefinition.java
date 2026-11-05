package com.roxiun.mellow.stats;

import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.*;

/** One game's parsing, selection and presentation contract. No network or UI dependencies. */
public abstract class GameDefinition<T> {
    private final StatScope scope;
    private final String id, displayName;
    private final Class<T> type;
    private final List<StatDefinition> columns;
    private final int[] defaults;

    protected GameDefinition(StatScope scope, String id, String displayName, Class<T> type,
                             int[] defaults, StatDefinition... columns) {
        this.scope = scope;
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.defaults = defaults.clone();
        this.columns = Collections.unmodifiableList(Arrays.asList(columns));
    }
    public StatScope scope() { return scope; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public Class<T> type() { return type; }
    public List<StatDefinition> columns() { return columns; }
    public StatDefinition column(int index) { return index < 0 || index >= columns.size() ? null : columns.get(index); }
    public int columnIndex(String id) {
        for (int i = 0; i < columns.size(); i++) if (columns.get(i).id().equals(id)) return i;
        return -1;
    }
    public String[] columnOptions() { return columns.stream().map(StatDefinition::option).toArray(String[]::new); }
    public int[] defaultColumns() { return defaults.clone(); }
    public List<String> modes() { return Collections.singletonList("overall"); }
    public String modeLabel(String mode) { return "overall".equals(mode) ? "Overall" : mode; }
    public String detectMode(GameSnapshot snapshot) { return "overall"; }
    public net.hypixel.data.type.GameType scoreboardType(String heading) { return null; }
    /** Whether this exact queue permits rank-coloured names; unknown queues preserve server styling. */
    public boolean usesRankNames(GameSnapshot snapshot) { return false; }
    public abstract boolean matches(GameSnapshot snapshot);
    public abstract ProviderResult<T> parse(HypixelPlayerData data, String mode);
    public abstract TabStats tabStats(T stats);
    public abstract String chatStats(T stats);
}
