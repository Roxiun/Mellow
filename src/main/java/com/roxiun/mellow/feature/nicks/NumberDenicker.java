package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.util.ChatUtils;
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
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class NumberDenicker {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final AuroraApi auroraApi;
    private final NickUtils nickUtils;

    private boolean gameStarted = false;
    private final Map<String, PotentialNick> nickToPotentials = new HashMap<>();

    private static final Pattern FINAL_KILL_PATTERN = Pattern.compile(
        "^(\\w+) was ([\\w-]+)'s final #([\\d,]+)\\. FINAL KILL!$"
    );
    private static final Pattern BED_DESTRUCTION_PATTERN = Pattern.compile(
        "^(?:BED DESTRUCTION > )?(\\w+) (?:Bed|bed) was bed #([\\d,]+) destroyed by ([\\w-]+)!$"
    );

    public NumberDenicker(
        MellowOneConfig config,
        NickUtils nickUtils,
        AuroraApi auroraApi
    ) {
        this.config = config;
        this.auroraApi = auroraApi;
        this.nickUtils = nickUtils;
    }

    public void onWorldChange() {
        this.gameStarted = false;
        this.nickToPotentials.clear();
    }

    public void onChat(ClientChatReceivedEvent event) {
        if (!config.numberDenicker) return;

        String message = event.message.getUnformattedText().trim();
        message = message.replaceAll("§.", "").trim();

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
                    if (
                        isPlayerInGame(nickName) &&
                        nickUtils.isNicked(nickName) &&
                        nickUtils.getResolvedRealNameForNick(nickName) == null &&
                        !player.finalsInFlight &&
                        (!player.finalsChecked ||
                            player.fuzzy_finals_potentials == null)
                    ) {
                        mc.addScheduledTask(() ->
                            ChatUtils.sendMessage(
                                "§aAttempting to denick " +
                                    nickName +
                                    " with " +
                                    finalNumberStr +
                                    " finals"
                            )
                        );
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
            if (
                isPlayerInGame(nickName) &&
                nickUtils.isNicked(nickName) &&
                nickUtils.getResolvedRealNameForNick(nickName) == null &&
                !player.bedsInFlight &&
                (!player.bedsChecked || player.fuzzy_beds_potentials == null)
            ) {
                mc.addScheduledTask(() ->
                    ChatUtils.sendMessage(
                        "§aAttempting to denick " +
                            nickName +
                            " with " +
                            bedNumber +
                            " beds"
                    )
                );
                processNumbers("beds", nickName, bedNumber);
            }
        }
    }

    private void processNumbers(String type, String nickName, String number) {
        PotentialNick player = nickToPotentials.get(nickName);
        if (player == null || mc.getNetHandler() == null) return;
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(nickName);
        if (playerInfo == null) return;
        if (type.equals("finals")) player.finalsInFlight = true;
        else player.bedsInFlight = true;

        AsyncExecutor.getInstance().profileIo(() -> {
            try {
                int[] rangeValues = { 0, 50, 100, 200, 500, 1000 };
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

                mc.addScheduledTask(() -> {
                    if (nickToPotentials.get(nickName) != player) return;
                    if (type.equals("finals")) player.finalsInFlight = false;
                    else player.bedsInFlight = false;
                    if (mc.getNetHandler() == null
                        || mc.getNetHandler().getPlayerInfo(nickName) != playerInfo
                        || nickUtils.getResolvedRealNameForNick(nickName) != null) return;
                    if (response != null && response.success) {
                        // Fuzzy Matching Logic
                        List<String> fuzzy_matches = response.data
                            .stream()
                            .filter(p -> p.distance <= range)
                            .map(p -> p.name)
                            .collect(Collectors.toList());

                        String fuzzy_players_log = response.data
                            .stream()
                            .filter(p -> p.distance <= range)
                            .map(
                                p ->
                                    "§a" +
                                    p.name +
                                    " §7(distance: " +
                                    p.distance +
                                    ")"
                            )
                            .collect(Collectors.joining(", "));

                        if (config.numberDenickerFuzzy) {
                            ChatUtils.sendMessage(
                                "§aFound potential " +
                                    type +
                                    " players: " +
                                    fuzzy_players_log
                            );
                        }

                        if (type.equals("finals")) {
                            player.fuzzy_finals_potentials = fuzzy_matches;
                        } else if (type.equals("beds")) {
                            player.fuzzy_beds_potentials = fuzzy_matches;
                        }

                        if (
                            player.fuzzy_finals_potentials != null &&
                            player.fuzzy_beds_potentials != null
                        ) {
                            List<String> intersection = new ArrayList<>(
                                player.fuzzy_finals_potentials
                            );
                            intersection.retainAll(player.fuzzy_beds_potentials);

                            if (intersection.isEmpty()) {
                                ChatUtils.sendMessage(
                                    "§cNo fuzzy match found for " + nickName
                                );
                            } else {
                                ChatUtils.sendMessage(
                                    "§aFound fuzzy matches for " +
                                        nickName +
                                        ": " +
                                        String.join(", ", intersection)
                                );
                            }
                        }

                        // Exact Matching Logic
                        List<String> matches = response.data
                            .stream()
                            .filter(p -> p.distance == 0)
                            .map(p -> p.name)
                            .collect(Collectors.toList());

                        player.recordMatches(type, matches);

                        if (player.finalsChecked && player.bedsChecked) {
                            if (player.getMatch() != null) {
                                String realName = player.getMatch();
                                if (nickUtils.resolveNick(nickName, realName, NickUtils.ResolutionSource.NUMBER)) {
                                    sendAlert(nickName, realName);
                                }
                            } else {
                                ChatUtils.sendMessage(
                                    "§cNo definitive name found for " + nickName
                                );
                            }
                        }
                    }
                });
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    if (nickToPotentials.get(nickName) != player
                        || nickUtils.getResolvedRealNameForNick(nickName) != null) return;
                    if (type.equals("finals")) player.finalsInFlight = false;
                    else player.bedsInFlight = false;
                    ChatUtils.sendMessage("§cError fetching data from Aurora API.");
                });
            }
        });
    }

    private void sendAlert(String playerName, String realName) {
        String alertMsg =
            "§6" + realName + "§7 might be nicked as " + playerName + "§7.";
        ChatUtils.sendMessage(alertMsg);
        mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
    }

    private boolean isBedwarsStartMessage(String message) {
        return (
            message.equals("Protect your bed and destroy the enemy beds.") ||
            (message.equals("You will respawn because you still have a bed!") &&
                !(message.contains(":")) &&
                !(message.contains("SHOUT")))
        );
    }

    private boolean isPlayerInGame(String name) {
        if (mc.getNetHandler() == null) return false;
        return mc
            .getNetHandler()
            .getPlayerInfoMap()
            .stream()
            .anyMatch(info -> info.getGameProfile().getName().equals(name));
    }

    static class PotentialNick {

        List<String> potentials = new ArrayList<>();
        boolean finalsInFlight = false;
        boolean bedsInFlight = false;
        boolean finalsChecked = false;
        boolean bedsChecked = false;

        List<String> fuzzy_finals_potentials = null;
        List<String> fuzzy_beds_potentials = null;

        String getMatch() {
            return finalsChecked && bedsChecked && potentials.size() == 1 ? potentials.get(0) : null;
        }

        void recordMatches(String type, List<String> matches) {
            if (!finalsChecked && !bedsChecked) {
                for (String match : matches) {
                    if (potentials.stream().noneMatch(p -> p.equalsIgnoreCase(match))) {
                        potentials.add(match);
                    }
                }
            } else {
                potentials.removeIf(p -> matches.stream().noneMatch(p::equalsIgnoreCase));
            }
            if (type.equals("finals")) finalsChecked = true;
            else bedsChecked = true;
        }
    }
}
