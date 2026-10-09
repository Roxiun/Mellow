package com.roxiun.mellow.stats;

import java.util.Locale;

/** Resolved selection captured by the caller, never inferred by the cache. */
public final class StatsSelection {
    private final GameDefinition<?> game;
    private final String mode;
    public StatsSelection(GameDefinition<?> game, String mode) {
        if (game == null) throw new IllegalArgumentException("A game is required");
        String selected = mode == null ? "overall" : mode.toLowerCase(Locale.ROOT);
        if (!game.modes().contains(selected)) throw new IllegalArgumentException("Unknown mode: " + mode);
        this.game = game;
        this.mode = selected;
    }
    public GameDefinition<?> game() { return game; }
    public String mode() { return mode; }
    public static StatsSelection overall(StatScope scope) {
        GameDefinition<?> game = GameRegistry.find(scope);
        return game == null ? null : new StatsSelection(game, "overall");
    }
}
