package com.strawberry.statsify.util;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.strawberry.statsify.api.MojangApi;
import com.strawberry.statsify.api.NadeshikoApi;
import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PregameStats {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsifyOneConfig config;
    private final NadeshikoApi nadeshikoApi;
    private final UrchinApi urchinApi;
    private final MojangApi mojangApi;
    public static final Logger LOGGER = LogManager.getLogger("Statsify");

    public static boolean bedwars;

    private boolean inPregameLobby = false;

    private static final Pattern BEDWARS_PREGAME_LOBBY_JOIN_PATTERN =
        Pattern.compile("^(\\w+) has joined \\((\\d+)/(\\d+)\\)!$");

    private static final Pattern BEDWARS_PREGAME_LOBBY_CHAT_PATTERN =
        Pattern.compile("^(?:\\[.*?\\]\\s*)*(\\w{3,16})(?::| ») (.*)$");

    public PregameStats(StatsifyOneConfig config) {
        this.config = config;
        this.nadeshikoApi = new NadeshikoApi();
        this.urchinApi = new UrchinApi();
        this.mojangApi = new MojangApi();
    }

    public void onWorldChange() {
        this.inPregameLobby = false;
    }

    private boolean isBedwarsStartMessage(String message) {
        return (
            message.equals("Protect your bed and destroy the enemy beds.") ||
            (message.equals("You will respawn because you still have a bed!") &&
                !(message.contains(":")) &&
                !(message.contains("SHOUT")))
        );
    }

    public void onChat(ClientChatReceivedEvent event) {
        if (!config.pregameStats && !config.pregameTags) return;

        // only denick if bedwars - thanks awruff - https://github.com/awruff/TNTTime
        bedwars = false;

        if (!HypixelUtils.INSTANCE.isHypixel()) {
            return;
        }

        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null || world.getScoreboard() == null) {
            return;
        }

        bedwars = isBedwars(world.getScoreboard());

        if (!bedwars) return;

        String message = event.message.getUnformattedText().trim();
        message = message.replaceAll("§.", "").trim();

        if (isBedwarsStartMessage(message)) {
            inPregameLobby = false;
            return;
        }

        Matcher pregameJoinMatcher = BEDWARS_PREGAME_LOBBY_JOIN_PATTERN.matcher(
            message
        );
        if (pregameJoinMatcher.find()) {
            inPregameLobby = true;
            return;
        }

        if (!inPregameLobby) {
            return;
        }

        Matcher pregameChatMatcher = BEDWARS_PREGAME_LOBBY_CHAT_PATTERN.matcher(
            message
        );
        if (pregameChatMatcher.find()) {
            String username = pregameChatMatcher.group(1);
            LOGGER.info(
                "FORGE: " +
                    Minecraft.getMinecraft().thePlayer.getName() +
                    " PARSED: " +
                    username
            );
            if (username.equals(Minecraft.getMinecraft().thePlayer.getName())) {
                return;
            }
            new Thread(() -> {
                try {
                    if (PartyManager.getInstance().inParty()) {
                        UUID uuid = UUID.fromString(
                            this.mojangApi.getUUIDFromName(username)
                        );
                        if (PartyManager.getInstance().isPartyMember(uuid)) {
                            return;
                        }
                    }
                } catch (Exception e) {
                    // Player is probably nicked, continue.
                }

                if (config.pregameStats) {
                    try {
                        String stats = nadeshikoApi.fetchPlayerStats(username);
                        Minecraft.getMinecraft().addScheduledTask(() ->
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText("§r[§bF§r] " + stats)
                            )
                        );
                    } catch (IOException e) {
                        Minecraft.getMinecraft().addScheduledTask(() ->
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText(
                                    "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                        username +
                                        ", they might be nicked"
                                )
                            )
                        );
                    }
                }
                if (config.pregameTags) {
                    checkUrchinTags(username);
                }
            })
                .start();
        }
    }

    private void checkUrchinTags(String playerName) {
        try {
            String tags = urchinApi
                .fetchUrchinTags(playerName, config.urchinKey)
                .replace("sniper", "§4§lSniper")
                .replace("blatant_cheater", "§4§lBlatant Cheater")
                .replace("closet_cheater", "§e§lCloset Cheater")
                .replace("confirmed_cheater", "§4§lConfirmed Cheater");
            if (!tags.isEmpty()) {
                mc.addScheduledTask(() ->
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §c\u26a0 §r" +
                                Utils.getTabDisplayName(playerName) +
                                " §ris §ctagged§r for: " +
                                tags
                        )
                    )
                );
            }
        } catch (IOException e) {
            mc.addScheduledTask(() ->
                mc.thePlayer.addChatMessage(
                    new ChatComponentText(
                        "§r[§bF§r] Failed to fetch tags for: " +
                            playerName +
                            " | " +
                            e.getMessage()
                    )
                )
            );
        }
    }

    private boolean isBedwars(Scoreboard scoreboard) {
        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(
            1
        );
        if (sidebarObjective == null) return false;
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(
            sidebarObjective.getDisplayName()
        );
        return name.contains("BED WARS");
    }
}
