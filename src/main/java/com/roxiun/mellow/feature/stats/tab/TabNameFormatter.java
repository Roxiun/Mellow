package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.stats.GameDefinition;
import com.roxiun.mellow.stats.GameRegistry;
import com.roxiun.mellow.util.player.PlayerUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.world.WorldSettings;

/** Rank decoration never replaces a server identity or gameplay colour. */
public final class TabNameFormatter {
    private TabNameFormatter() {}

    public static String format(NetworkPlayerInfo info, String teamPrefix, String rankedName,
                                GameSnapshot snapshot, boolean enabled, boolean nicked) {
        return format(info.getGameProfile().getName(), PlayerUtils.getRawTabListName(info),
            teamPrefix, rankedName, snapshot, enabled,
            nicked || info.getGameType() == WorldSettings.GameType.SPECTATOR);
    }

    static String format(String name, String serverName, String teamPrefix, String rankedName,
                         GameSnapshot snapshot, boolean enabled, boolean preserveIdentity) {
        String raw = serverName == null ? name : serverName;
        String fallback = raw;
        // The configured Team column already renders this prefix.
        if (teamPrefix != null && !teamPrefix.isEmpty() && raw.startsWith(teamPrefix)) {
            fallback = formattingCodes(teamPrefix) + raw.substring(teamPrefix.length());
        }
        if (!enabled || preserveIdentity || snapshot == null || !snapshot.isOnHypixel()
            || rankedName == null || rankedName.isEmpty()) return fallback + "§r";

        GameDefinition<?> game = snapshot.getStatsGame();
        if (game == null || !game.usesRankNames(snapshot)) return fallback + "§r";

        // Decorate only the visible account name, never an alias or a provider's different identity.
        int start = raw.indexOf(name);
        String plainRank = rankedName.replaceAll("§.", "");
        if (start < 0 || !(plainRank.equals(name) || plainRank.endsWith(" " + name))) return fallback + "§r";
        String visiblePrefix = raw.substring(0, start).replaceAll("§.", "").trim();
        String rankPrefix = plainRank.substring(0, plainRank.length() - name.length()).trim();
        // Keep custom labels and names that merely contain the account name.
        if ((!visiblePrefix.isEmpty() && !visiblePrefix.equals(rankPrefix))
            || (start + name.length() < raw.length()
                && Character.isJavaIdentifierPart(raw.charAt(start + name.length())))) return fallback + "§r";
        if (game == GameRegistry.TNT_TAG) {
            String prefix = raw.substring(0, start);
            String color = lastColor(prefix);
            // Red marks TNT possession. Other special colours/labels remain server-owned too.
            if (!(color.equals("§f") || color.equals("§7")) || !prefix.replaceAll("§.", "").trim().isEmpty()) {
                return fallback + "§r";
            }
        }
        return rankedName + "§r" + raw.substring(start + name.length()) + "§r";
    }

    private static String formattingCodes(String text) {
        StringBuilder codes = new StringBuilder();
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '§') codes.append('§').append(text.charAt(++i));
        }
        return codes.toString();
    }

    private static String lastColor(String text) {
        String color = "§f";
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(++i));
            if ("0123456789abcdef".indexOf(code) >= 0) color = "§" + code;
            else if (code == 'r') color = "§f";
        }
        return color;
    }
}
