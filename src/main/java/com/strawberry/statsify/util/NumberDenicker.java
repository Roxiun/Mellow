package com.strawberry.statsify.util;

import com.strawberry.statsify.api.AuroraApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class NumberDenicker {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsifyOneConfig config;
    private final AuroraApi auroraApi;

    private boolean gameStarted = false;
    private final Map<String, PotentialNick> nickToPotentials = new HashMap<>();

    private static final Pattern FINAL_KILL_PATTERN = Pattern.compile(
        "^(\\w+) was ([\\w-]+)'s final #([\\d,]+)\\. FINAL KILL!$"
    );
    private static final Pattern BED_DESTRUCTION_PATTERN = Pattern.compile(
        "^(?:BED DESTRUCTION > )?(\\w+) (?:Bed|bed) was bed #([\\d,]+) destroyed by ([\\w-]+)!$"
    );

    public NumberDenicker(StatsifyOneConfig config) {
        this.config = config;
        this.auroraApi = new AuroraApi();
    }

    public void onWorldChange() {
        this.gameStarted = false;
        this.nickToPotentials.clear();
    }

    public void onChat(ClientChatReceivedEvent event) {
        if (!config.numberDenicker) return;

        String message = event.message.getUnformattedText().trim();

        if (isBedwarsStartMessage(message)) {
            if (!this.gameStarted) {
                this.gameStarted = true;
            }
            return;
        }

        if (!this.gameStarted) return;

        Matcher finalMatcher = FINAL_KILL_PATTERN.matcher(message);
        if (finalMatcher.find()) {
            String nickName = finalMatcher.group(2);
            String finalNumberStr = finalMatcher.group(3).replace(",", "");

            try {
                int finalNumber = Integer.parseInt(finalNumberStr);
                if (finalNumber >= config.minFinalsForDenick) {
                    PotentialNick player = nickToPotentials.computeIfAbsent(
                        nickName,
                        k -> new PotentialNick()
                    );
                    if (isPlayerInGame(nickName) && !player.finalsChecked) {
                        processNumbers("finals", nickName, finalNumberStr);
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore if the number is invalid
            }
            return;
        }

        Matcher bedMatcher = BED_DESTRUCTION_PATTERN.matcher(message);
        if (bedMatcher.find()) {
            String nickName = bedMatcher.group(3);
            String bedNumber = bedMatcher.group(2).replace(",", "");
            PotentialNick player = nickToPotentials.computeIfAbsent(
                nickName,
                k -> new PotentialNick()
            );
            if (isPlayerInGame(nickName) && !player.bedsChecked) {
                processNumbers("beds", nickName, bedNumber);
            }
        }
    }

    private void processNumbers(String type, String nickName, String number) {
        PotentialNick player = nickToPotentials.get(nickName);
        if (player == null) return;

        new Thread(() -> {
            try {
                int[] rangeValues = { 100, 200, 500, 1000 };
                int[] maxValues = { 5, 10, 20 };

                int rangeIndex = type.equals("finals")
                    ? config.finalsRange
                    : config.bedsRange;
                int maxIndex = config.maxResults;

                if (
                    rangeIndex < 0 || rangeIndex >= rangeValues.length
                ) rangeIndex = 1; // Default to 200
                if (maxIndex < 0 || maxIndex >= maxValues.length) maxIndex = 0; // Default to 5

                int range = rangeValues[rangeIndex];
                int max = maxValues[maxIndex];

                AuroraApi.AuroraResponse response = auroraApi.queryStats(
                    type,
                    number,
                    range,
                    max,
                    config.auroraApiKey
                );

                if (response != null && response.success) {
                    List<String> matches = response.data
                        .stream()
                        .filter(p -> p.distance <= 0)
                        .map(p -> p.name)
                        .collect(Collectors.toList());

                    if (matches.isEmpty()) {
                        if (type.equals("finals")) player.finalsChecked = true;
                        if (type.equals("beds")) player.bedsChecked = true;
                        player.setPotentials(new ArrayList<>());
                        return;
                    }

                    if (
                        player.potentials.isEmpty() &&
                        (!player.bedsChecked && !player.finalsChecked)
                    ) {
                        player.setPotentials(matches);
                    } else {
                        player.potentials.retainAll(matches);
                    }

                    if (type.equals("finals")) player.finalsChecked = true;
                    if (type.equals("beds")) player.bedsChecked = true;

                    if (player.finalsChecked && player.bedsChecked) {
                        if (!player.potentials.isEmpty()) {
                            String realName = player.potentials.get(0);
                            mc.addScheduledTask(() -> {
                                sendAlert(nickName, realName);
                                setNickDisplayName(nickName, realName + "?");
                            });
                        } else {
                            mc.addScheduledTask(() ->
                                mc.thePlayer.addChatMessage(
                                    new ChatComponentText(
                                        "§4[ND] §cNo definitive name found for " +
                                            nickName
                                    )
                                )
                            );
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mc.addScheduledTask(() ->
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§4[ND] §cError fetching data from Aurora API."
                        )
                    )
                );
            }
        })
            .start();
    }

    private void sendAlert(String playerName, String realName) {
        String alertMsg =
            "§6" + realName + "§7 might be nicked as " + playerName + "§7.";
        mc.thePlayer.addChatMessage(
            new ChatComponentText("§4[ND] " + alertMsg)
        );
        mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
    }

    private boolean isBedwarsStartMessage(String message) {
        return (
            message.equals("Protect your bed and destroy the enemy beds.") ||
            message.equals("You will respawn because you still have a bed!")
        );
    }

    private boolean isPlayerInGame(String name) {
        return mc
            .getNetHandler()
            .getPlayerInfoMap()
            .stream()
            .anyMatch(info -> info.getGameProfile().getName().equals(name));
    }

    private void setNickDisplayName(String nickName, String realName) {
        for (NetworkPlayerInfo playerInfo : mc
            .getNetHandler()
            .getPlayerInfoMap()) {
            if (playerInfo.getGameProfile().getName().equals(nickName)) {
                playerInfo.setDisplayName(new ChatComponentText(realName));
                break;
            }
        }
    }

    private static class PotentialNick {

        List<String> potentials = new ArrayList<>();
        boolean finalsChecked = false;
        boolean bedsChecked = false;

        void setPotentials(List<String> potentials) {
            this.potentials = potentials;
        }
    }
}
