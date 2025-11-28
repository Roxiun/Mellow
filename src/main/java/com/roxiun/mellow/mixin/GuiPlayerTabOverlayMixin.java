package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.UUID;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiPlayerTabOverlay.class)
public class GuiPlayerTabOverlayMixin {

    private static final String MIDDLE_DOT = "\u30fb";

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(
        NetworkPlayerInfo networkPlayerInfoIn,
        CallbackInfoReturnable<String> cir
    ) {
        if (Mellow.config == null || !Mellow.config.tabStats) {
            return;
        }

        String playerName = networkPlayerInfoIn.getGameProfile().getName();
        if (playerName == null) {
            return;
        }

        TabStats stats = Mellow.tabStats.get(playerName);
        boolean isNicked = Mellow.nickUtils.isNicked(playerName);
        String originalDisplayName = getOriginalDisplayName(
            networkPlayerInfoIn
        );
        UUID playerUUID = networkPlayerInfoIn.getGameProfile().getId();

        String newDisplayName;

        if (stats != null) {
            newDisplayName = handlePlayerWithStats(
                playerName,
                stats,
                playerUUID
            );
        } else if (isNicked && !originalDisplayName.contains("§8[§5NICK§8]")) {
            newDisplayName = handleNickedPlayer(
                playerName,
                originalDisplayName
            );
        } else {
            newDisplayName = originalDisplayName;
        }

        newDisplayName = appendBlacklistTag(newDisplayName, playerUUID);

        if (!originalDisplayName.equals(newDisplayName)) {
            cir.setReturnValue(newDisplayName);
        }
    }

    private String handlePlayerWithStats(
        String playerName,
        TabStats stats,
        UUID playerUUID
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData == null || tabData.length < 2) {
            return "";
        }
        String team = tabData[0];
        String name = tabData[1];

        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
        return formatDisplayNameWithStats(team, name, teamColor, stats);
    }

    private String formatDisplayNameWithStats(
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        String newDisplayName = getStatsString(team, name, teamColor, stats);

        if (stats.getWinstreak() != null && !stats.getWinstreak().isEmpty()) {
            newDisplayName += MIDDLE_DOT + "§r" + stats.getWinstreak();
        }

        if (stats.isUrchinTagged()) {
            for (UrchinTag tag : stats.getUrchinTags()) {
                newDisplayName +=
                    " " + FormattingUtils.formatUrchinTagIcon(tag);
            }
        }

        return newDisplayName;
    }

    private String getStatsString(
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        String stars = stats.getStars();
        String fkdr = stats.getFkdr();

        switch (Mellow.config.tabFormat) {
            case 1: // [Star] Name · FKDR
                return (
                    team +
                    stars +
                    " " +
                    teamColor +
                    name +
                    MIDDLE_DOT +
                    "§r" +
                    fkdr
                );
            case 2: // Name · FKDR
                return team + teamColor + name + MIDDLE_DOT + "§r" + fkdr;
            case 0: // [Star] Name · FKDR
            default:
                return (
                    team +
                    "§7[" +
                    stars +
                    "§7] " +
                    teamColor +
                    name +
                    MIDDLE_DOT +
                    "§r" +
                    fkdr
                );
        }
    }

    private String handleNickedPlayer(
        String playerName,
        String originalDisplayName
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData != null && tabData.length >= 3) {
            String team = tabData[0] != null ? tabData[0] : "";
            String name = tabData[1] != null ? tabData[1] : "";
            String suffix = tabData[2] != null ? tabData[2] : "";
            String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
            return team + "§8[§5NICK§8] " + teamColor + name + suffix;
        } else {
            return "§8[§5NICK§8] " + originalDisplayName;
        }
    }

    private String appendBlacklistTag(String displayName, UUID playerUUID) {
        if (
            playerUUID != null &&
            Mellow.blacklistManager.isBlacklisted(playerUUID)
        ) {
            return displayName + " §8[§6BL§8]";
        }
        return displayName;
    }

    private String getOriginalDisplayName(
        NetworkPlayerInfo networkPlayerInfoIn
    ) {
        if (networkPlayerInfoIn.getDisplayName() != null) {
            return networkPlayerInfoIn.getDisplayName().getFormattedText();
        }
        return ScorePlayerTeam.formatPlayerName(
            networkPlayerInfoIn.getPlayerTeam(),
            networkPlayerInfoIn.getGameProfile().getName()
        );
    }
}
