package com.roxiun.mellow.feature.stats.tab;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.MathHelper;
import net.minecraft.util.EnumChatFormatting;

public final class TabHealthValueResolver {

    private static final String UNKNOWN_HP = "§7--";

    private TabHealthValueResolver() {}

    public static String getFormattedHealth(
        Minecraft mc,
        NetworkPlayerInfo playerInfo
    ) {
        if (playerInfo == null || playerInfo.getGameProfile() == null) {
            return UNKNOWN_HP;
        }

        return getFormattedHealth(
            mc,
            playerInfo.getGameProfile().getName(),
            playerInfo.getGameProfile().getId()
        );
    }

    public static String getFormattedHealth(
        Minecraft mc,
        String playerName,
        UUID playerUuid
    ) {
        Integer hp = resolveHealth(mc, playerName, playerUuid);
        if (hp == null) {
            return UNKNOWN_HP;
        }
        return colorize(hp);
    }

    private static Integer resolveHealth(
        Minecraft mc,
        String playerName,
        UUID playerUuid
    ) {
        Integer fromScoreboard = resolveFromPlayerListScore(mc, playerName);
        return fromScoreboard != null ? fromScoreboard : resolveFromEntity(mc, playerName, playerUuid);
    }

    /** Shared by value resolution and layout so only health scores replace our HP column. */
    public static boolean isHealthObjective(ScoreObjective objective) {
        if (objective == null) return false;
        if (objective.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) return true;
        String label = EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName());
        if (label == null) return false;
        // Hypixel sends numeric health with a coloured heart as the objective's label.
        label = label.replace("\uFE0F", "").trim().toLowerCase(Locale.ROOT);
        return label.equals("♥") || label.equals("❤") || label.equals("hp") || label.equals("health");
    }

    private static Integer resolveFromEntity(
        Minecraft mc,
        String playerName,
        UUID playerUuid
    ) {
        if (mc == null || mc.theWorld == null) {
            return null;
        }

        EntityPlayer entity = null;
        if (playerUuid != null) {
            entity = mc.theWorld.getPlayerEntityByUUID(playerUuid);
        }
        if (entity == null && playerName != null && !playerName.isEmpty()) {
            entity = mc.theWorld.getPlayerEntityByName(playerName);
        }
        if (entity == null) {
            return null;
        }

        float totalHealth = entity.getHealth() + entity.getAbsorptionAmount();
        return Math.max(0, MathHelper.ceiling_float_int(totalHealth));
    }

    private static Integer resolveFromPlayerListScore(
        Minecraft mc,
        String playerName
    ) {
        if (
            mc == null ||
            mc.theWorld == null ||
            playerName == null ||
            playerName.isEmpty()
        ) {
            return null;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return null;
        }

        ScoreObjective playerListObjective = scoreboard.getObjectiveInDisplaySlot(
            0
        );
        if (!isHealthObjective(playerListObjective)) return null;

        // Read existing scores only: getValueFromObjective would create a zero for missing players.
        Collection<Score> scores = scoreboard.getSortedScores(playerListObjective);
        if (scores == null || scores.isEmpty()) {
            return null;
        }

        for (Score score : scores) {
            if (score == null) {
                continue;
            }
            if (playerName.equalsIgnoreCase(score.getPlayerName())) {
                return Math.max(0, score.getScorePoints());
            }
        }
        return null;
    }

    private static String colorize(int hp) {
        if (hp >= 16) {
            return "§a" + hp;
        }
        if (hp >= 11) {
            return "§e" + hp;
        }
        if (hp >= 6) {
            return "§6" + hp;
        }
        return "§c" + hp;
    }
}
