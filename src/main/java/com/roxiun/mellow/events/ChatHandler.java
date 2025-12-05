package com.roxiun.mellow.events;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.duels.PlanckeApi;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.task.StatsChecker;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.StringUtils;
import com.roxiun.mellow.util.bedwars.BedwarsUpgradesTrapsManager;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.nicks.NickUtils;
import com.roxiun.mellow.util.nicks.NumberDenicker;
import com.roxiun.mellow.util.player.PregameStats;
import com.roxiun.mellow.util.skins.SkinUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NickUtils nickUtils;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final PlanckeApi planckeApi;
    private final StatsChecker statsChecker;
    private final PlayerCache playerCache;

    public ChatHandler(
        MellowOneConfig config,
        NickUtils nickUtils,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        PlanckeApi planckeApi,
        StatsChecker statsChecker,
        PlayerCache playerCache
    ) {
        this.config = config;
        this.nickUtils = nickUtils;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.planckeApi = planckeApi;
        this.statsChecker = statsChecker;
        this.playerCache = playerCache;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);
        String message = event.message.getUnformattedText();

        // Process purchases and traps before other logic
        processBedwarsUpgradesAndTraps(event);

        // Check for Bedwars game start messages
        if (
            message.contains("Protect your bed and destroy the enemy beds.") &&
            !(message.contains(":")) &&
            !(message.contains("SHOUT"))
        ) {
            // Start Bedwars game after a delay to allow scoreboard to update
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Wait 1 second like in the JS version
                    startBedwarsGame();
                    // Reset upgrades and traps when game starts
                    BedwarsUpgradesTrapsManager.getInstance().resetUpgradesAndTraps();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })
                .start();

            if (config.autoWho) {
                mc.thePlayer.sendChatMessage("/who");
            }
        }

        // Check for bed respawn messages which also indicate game is running
        if (
            message.contains(
                "You will respawn because you still have a bed!"
            ) &&
            !(message.contains(":")) &&
            !(message.contains("SHOUT"))
        ) {
            // Start Bedwars game after a delay to allow scoreboard to update
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Wait 1 second like in the JS version
                    startBedwarsGame();
                    // Reset upgrades and traps when game starts
                    BedwarsUpgradesTrapsManager.getInstance().resetUpgradesAndTraps();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })
                .start();
        }

        if (message.startsWith("ONLINE:")) {
            String playersString = message.substring("ONLINE:".length()).trim();
            String[] players = playersString.split(",\\s*");
            List<String> onlinePlayers = new ArrayList<>(
                Arrays.asList(players)
            );
            nickUtils.updateNickedPlayers(onlinePlayers);
            statsChecker.checkPlayerStats(onlinePlayers);
        }

        if (message.startsWith(" ") && message.contains("Opponent:")) {
            String username = StringUtils.parseUsername(message);
            new Thread(() -> {
                try {
                    String stats = planckeApi.checkDuels(username);
                    ChatUtils.sendMessage(stats);
                } catch (IOException e) {
                    ChatUtils.sendMessage(
                        "§cFailed to get stats for " + username
                    );
                }
            })
                .start();
        }

        // Check for pregame start message to reset upgrades and traps
        if (
            message.startsWith("The game starts in ") &&
            message.contains(" seconds!")
        ) {
            BedwarsUpgradesTrapsManager.getInstance().resetUpgradesAndTraps();
        }
    }

    private void processBedwarsUpgradesAndTraps(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();

        // Check for purchase messages
        if (message.toLowerCase().contains("purchased")) {
            BedwarsUpgradesTrapsManager.getInstance().processPurchaseMessage(
                message
            );
        }

        // Check for trap triggered and removed messages
        if (
            message.toLowerCase().contains("trap was set off!") ||
            message.toLowerCase().contains("reveal trap set off") ||
            (message.toLowerCase().contains("removed") &&
                message.toLowerCase().contains("trap from the queue"))
        ) {
            BedwarsUpgradesTrapsManager.getInstance().processTrapTriggeredMessage(
                message
            );
        }
    }

    private void startBedwarsGame() {
        // Check if we're actually in a Bedwars game by looking at the scoreboard
        if (isInBedwarsGame()) {
            String mode = detectBedwarsMode();
            HypixelFeatures.getInstance().startNewGame();
            HypixelFeatures.getInstance().setMode(mode);
        }
    }

    private boolean isInBedwarsGame() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard != null) {
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // Sidebar objective
            if (objective != null) {
                String displayName = objective.getDisplayName();
                String cleanTitle = displayName.replaceAll("§.", ""); // Remove formatting codes
                return cleanTitle.equals("BED WARS");
            }
        }
        return false;
    }

    private String detectBedwarsMode() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard != null) {
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // Sidebar objective
            if (objective != null) {
                // Check all lines in the scoreboard for indicators of doubles vs fours
                java.util.Collection<Score> scores = scoreboard.getSortedScores(
                    objective
                );
                for (Score score : scores) {
                    String scoreName = score.getPlayerName();
                    if (scoreName != null) {
                        String cleanLine = scoreName.replaceAll("§.", ""); // Remove formatting codes
                        // Check for doubles indicators (2s, doubles, etc.)
                        if (
                            cleanLine.toLowerCase().contains("2s") ||
                            cleanLine.toLowerCase().contains("doubles")
                        ) {
                            return "doubles";
                        }
                        // Check for fours indicators (4s, fours, 4v4, etc.)
                        if (
                            cleanLine.toLowerCase().contains("4s") ||
                            cleanLine.toLowerCase().contains("fours")
                        ) {
                            return "fours";
                        }
                        // Check for specific game modes that indicate doubles or fours
                        if (
                            cleanLine.toLowerCase().contains("double") ||
                            cleanLine.contains("Pink")
                        ) {
                            return "doubles";
                        }
                        if (
                            cleanLine.toLowerCase().contains("quad") ||
                            cleanLine.toLowerCase().contains("4v4")
                        ) {
                            return "fours";
                        }
                    }
                }
            }
        }
        // Default to fours if we can't detect the mode
        return "fours";
    }
}
