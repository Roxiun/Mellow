package com.strawberry.statsify.mixin;

import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.data.TabStats;
import com.strawberry.statsify.util.player.PlayerUtils;
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
        if (Statsify.config == null || !Statsify.config.tabStats) {
            return;
        }

        String playerName = networkPlayerInfoIn.getGameProfile().getName();
        if (playerName == null) {
            return;
        }

        TabStats stats = Statsify.tabStats.get(playerName);
        boolean isNicked = Statsify.nickUtils.isNicked(playerName);
        String originalDisplayName = getOriginalDisplayName(
            networkPlayerInfoIn
        );

        if (stats != null) {
            handlePlayerWithStats(playerName, stats, originalDisplayName, cir);
        } else if (isNicked && !originalDisplayName.contains("§c[NICK]")) {
            handleNickedPlayer(playerName, originalDisplayName, cir);
        }
    }

    private void handlePlayerWithStats(
        String playerName,
        TabStats stats,
        String originalDisplayName,
        CallbackInfoReturnable<String> cir
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData == null || tabData.length < 2) {
            return;
        }
        String team = tabData[0];
        String name = tabData[1];

        // This is the new feature implementation
        if (stats.isUrchinTagged()) {
            name = "§c⚠" + name;
        }

        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
        String newDisplayName = formatDisplayNameWithStats(
            team,
            name,
            teamColor,
            stats
        );

        if (!originalDisplayName.equals(newDisplayName)) {
            cir.setReturnValue(newDisplayName);
        }
    }

    private String formatDisplayNameWithStats(
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        String newDisplayName;
        String stars = stats.getStars();
        String fkdr = stats.getFkdr();
        String winstreak = stats.getWinstreak();

        switch (Statsify.config.tabFormat) {
            case 1: // [Star] Name · FKDR
                newDisplayName =
                    team +
                    stars +
                    " " +
                    teamColor +
                    name +
                    MIDDLE_DOT +
                    "§r" +
                    fkdr;
                break;
            case 2: // Name · FKDR
                newDisplayName =
                    team + teamColor + name + MIDDLE_DOT + "§r" + fkdr;
                break;
            case 0: // [Star] Name · FKDR
            default:
                newDisplayName =
                    team +
                    "§7[" +
                    stars +
                    "§7] " +
                    teamColor +
                    name +
                    MIDDLE_DOT +
                    "§r" +
                    fkdr;
                break;
        }

        if (winstreak != null && !winstreak.isEmpty()) {
            newDisplayName += MIDDLE_DOT + "§r" + winstreak;
        }

        return newDisplayName;
    }

    private void handleNickedPlayer(
        String playerName,
        String originalDisplayName,
        CallbackInfoReturnable<String> cir
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData != null && tabData.length >= 3) {
            String team = tabData[0] != null ? tabData[0] : "";
            String name = tabData[1] != null ? tabData[1] : "";
            String suffix = tabData[2] != null ? tabData[2] : "";
            String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
            cir.setReturnValue(team + "§c[NICK] " + teamColor + name + suffix);
        } else {
            cir.setReturnValue("§c[NICK] " + originalDisplayName);
        }
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
