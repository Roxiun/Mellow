package com.strawberry.statsify.mixin;

import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.util.PlayerUtils;
import java.util.List;
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

        List<String> suffixes = Statsify.playerSuffixes.get(playerName);
        boolean isNicked = Statsify.nickUtils.isNicked(playerName);
        String originalDisplayName = getOriginalDisplayName(
            networkPlayerInfoIn
        );

        if (suffixes != null && suffixes.size() >= 2) {
            handlePlayerWithStats(
                playerName,
                suffixes,
                originalDisplayName,
                cir
            );
        } else if (isNicked && !originalDisplayName.contains("§c[NICK]")) {
            handleNickedPlayer(playerName, originalDisplayName, cir);
        }
    }

    /**
     * Handles modifying the display name for a player with stats.
     */
    private void handlePlayerWithStats(
        String playerName,
        List<String> suffixes,
        String originalDisplayName,
        CallbackInfoReturnable<String> cir
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData == null || tabData.length < 2) {
            return;
        }
        String team = tabData[0];
        String name = tabData[1];

        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";

        String newDisplayName = formatDisplayNameWithStats(
            team,
            name,
            teamColor,
            suffixes
        );

        if (!originalDisplayName.equals(newDisplayName)) {
            cir.setReturnValue(newDisplayName);
        }
    }

    /**
     * Formats the display name for a player with stats based on the config.
     */
    private String formatDisplayNameWithStats(
        String team,
        String name,
        String teamColor,
        List<String> suffixes
    ) {
        String newDisplayName;
        String suffix1 = suffixes.get(0); // Star
        String suffix2 = suffixes.get(1); //FKDR

        switch (Statsify.config.tabFormat) {
            case 1:
                newDisplayName =
                    team +
                    suffix1 +
                    MIDDLE_DOT +
                    teamColor +
                    name +
                    MIDDLE_DOT +
                    suffix2;
                break;
            case 2:
                newDisplayName = team + teamColor + name + MIDDLE_DOT + suffix2;
                break;
            case 0:
            default:
                newDisplayName =
                    team +
                    "§7[" +
                    suffix1 +
                    "§7] " +
                    teamColor +
                    name +
                    MIDDLE_DOT +
                    suffix2;
                break;
        }

        if (suffixes.size() >= 3) {
            newDisplayName += "§7" + MIDDLE_DOT + suffixes.get(2);
        }

        return newDisplayName;
    }

    /**
     * Handles modifying the display name for a nicked player.
     */
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

    /**
     * Gets the original display name of a player.
     */
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
