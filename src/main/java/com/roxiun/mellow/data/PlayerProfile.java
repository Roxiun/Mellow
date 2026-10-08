package com.roxiun.mellow.data;

import com.roxiun.mellow.api.tags.TagReport;

import com.roxiun.mellow.stats.*;
import java.util.*;

public class PlayerProfile {
    private final String uuid, name;
    private final Map<GameDefinition<?>, Object> stats = new LinkedHashMap<>();
    private final TagReport tags;
    private final long lastUpdated;

    public static PlayerProfile identity(String uuid, String name) {
        return new PlayerProfile(uuid, name, Collections.emptyMap(), TagReport.empty());
    }
    public PlayerProfile(String uuid, String name, Map<GameDefinition<?>, ?> stats, TagReport tags) {
        this.uuid = uuid;
        this.name = name;
        stats.forEach((game, value) -> this.stats.put(game, game.type().cast(value)));
        this.tags = tags == null ? TagReport.empty() : tags;
        this.lastUpdated = System.currentTimeMillis();
    }
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public TagReport getTags() { return tags; }
    public long getLastUpdated() { return lastUpdated; }
    public PlayerProfile withTags(TagReport tags) {
        return new PlayerProfile(uuid, name, stats, tags);
    }

    public <T> T getStats(GameDefinition<T> game) { return game.type().cast(stats.get(game)); }
    public boolean hasStats(StatScope scope) { return stats.get(GameRegistry.find(scope)) != null; }
    public TabStats getTabStats() { return getTabStats(StatScope.BEDWARS); }
    public TabStats getTabStats(StatScope scope) {
        GameDefinition<?> game = GameRegistry.find(scope);
        return game == null ? TabStats.tagsOnly(tags, name) : tabStats(game);
    }
    private <T> TabStats tabStats(GameDefinition<T> game) {
        T value = getStats(game);
        return value == null ? TabStats.tagsOnly(tags, name) : game.tabStats(value).withTags(tags);
    }
    public String chatStats(StatScope scope) {
        GameDefinition<?> game = GameRegistry.find(scope);
        return game == null ? "" : chatStats(game);
    }
    private <T> String chatStats(GameDefinition<T> game) {
        T value = getStats(game);
        return value == null ? "" : game.chatStats(value);
    }
}
