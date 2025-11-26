package com.strawberry.statsify.util.player;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import com.strawberry.statsify.api.urchin.UrchinTag;
import com.strawberry.statsify.cache.PlayerCache;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.data.PlayerProfile;
import com.strawberry.statsify.util.formatting.FormattingUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class PregameStats {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final PlayerCache playerCache;
    private final StatsifyOneConfig config;

    // runtime state
    private boolean inPregameLobby = false;
    private boolean inBedwars = false;
    private final Set<String> alreadyLookedUp = ConcurrentHashMap.newKeySet();

    // patterns
    private static final Pattern BEDWARS_JOIN_PATTERN = Pattern.compile(
        "^(\\w+) has joined \\((\\d+)/(\\d+)\\)!$"
    );
    private static final Pattern BEDWARS_CHAT_PATTERN = Pattern.compile(
        "^(?:\\[.*?\\]\\s*)*(\\w{3,16})(?::| ») (.*)$"
    );

    public PregameStats(PlayerCache playerCache, StatsifyOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    public void onWorldChange() {
        inPregameLobby = false;
        inBedwars = false;
        alreadyLookedUp.clear();
    }

    public void onChat(ClientChatReceivedEvent event) {
        if (!config.pregameStats && !config.pregameTags) return;
        if (!HypixelUtils.INSTANCE.isHypixel()) return;

        if (!inBedwars) {
            inBedwars = isBedwarsSidebar();
            if (!inBedwars) return;
        }

        String message = event.message.getUnformattedText().trim();

        Matcher joinMatch = BEDWARS_JOIN_PATTERN.matcher(message);
        if (joinMatch.find()) {
            inPregameLobby = true;
            return;
        }

        if (
            message.contains("Protect your bed and destroy the enemy beds.") &&
            !message.contains(":")
        ) {
            inPregameLobby = false;
            return;
        }

        if (!inPregameLobby) return;

        Matcher chatMatch = BEDWARS_CHAT_PATTERN.matcher(message);
        if (!chatMatch.find()) return;

        String username = chatMatch.group(1);

        if (username.equalsIgnoreCase(mc.thePlayer.getName())) return;
        if (!alreadyLookedUp.add(username.toLowerCase())) return;

        new Thread(
            () -> handlePlayer(username),
            "Statsify-PregameThread"
        ).start();
    }

    private void handlePlayer(String username) {
        PlayerProfile profile = playerCache.getProfile(username);

        if (profile == null || profile.getBedwarsPlayer() == null) {
            if (config.pregameStats) {
                mc.addScheduledTask(() ->
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r] §cFailed to fetch stats for: §r" +
                                username +
                                " (possibly nicked)"
                        )
                    )
                );
            }
            return;
        }

        if (config.pregameStats) {
            BedwarsPlayer player = profile.getBedwarsPlayer();
            String stats =
                player.getName() +
                " §r" +
                player.getStars() +
                " §7|§r FKDR: " +
                player.getFkdrColor() +
                player.getFormattedFkdr();
            mc.addScheduledTask(() ->
                mc.thePlayer.addChatMessage(
                    new ChatComponentText("§r[§bStatsify§r] " + stats)
                )
            );
        }

        if (config.pregameTags && profile.isUrchinTagged()) {
            String tags = FormattingUtils.formatUrchinTags(
                profile.getUrchinTags()
            );
            String urchinMessage =
                "§r[§bStatsify§r] §c" + username + " is tagged for: " + tags;
            mc.addScheduledTask(() ->
                mc.thePlayer.addChatMessage(
                    new ChatComponentText(urchinMessage)
                )
            );
        }
    }

    private boolean isBedwarsSidebar() {
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null) return false;

        ScoreObjective obj = board.getObjectiveInDisplaySlot(1);
        if (obj == null) return false;

        String name = EnumChatFormatting.getTextWithoutFormattingCodes(
            obj.getDisplayName()
        );
        return name.contains("BED WARS");
    }
}
