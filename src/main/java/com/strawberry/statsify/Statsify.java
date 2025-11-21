package com.strawberry.statsify;

import com.strawberry.statsify.api.HypixelApi;
import com.strawberry.statsify.api.PlanckeApi;
import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.commands.BedwarsCommand;
import com.strawberry.statsify.commands.ClearCacheCommand;
import com.strawberry.statsify.commands.DenickCommand;
import com.strawberry.statsify.commands.StatsifyCommand;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.util.NickUtils;
import com.strawberry.statsify.util.NumberDenicker;
import com.strawberry.statsify.util.PartyManager;
import com.strawberry.statsify.util.PregameStats;
import com.strawberry.statsify.util.Utils;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = Statsify.MODID, name = Statsify.NAME, version = Statsify.VERSION)
public class Statsify {

    public static final String MODID = "statsify";
    public static final String NAME = "Statsify";
    public static final String VERSION = "4.2.0";

    private final Minecraft mc = Minecraft.getMinecraft();
    public static StatsifyOneConfig config;
    private final HypixelApi hypixelApi;
    private final UrchinApi urchinApi;
    private final PlanckeApi planckeApi;
    private NumberDenicker numberDenicker;
    private PregameStats pregameStats;
    private NickUtils nickUtils;

    private List<String> onlinePlayers = new ArrayList<>();
    private final Map<String, List<String>> playerSuffixes = new HashMap<>();

    public Statsify() {
        this.hypixelApi = new HypixelApi();
        this.urchinApi = new UrchinApi();
        this.planckeApi = new PlanckeApi();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        config = new StatsifyOneConfig();
        nickUtils = new NickUtils();
        numberDenicker = new NumberDenicker(config, nickUtils);
        pregameStats = new PregameStats(config);
        MinecraftForge.EVENT_BUS.register(this);
        PartyManager.getInstance();

        ClientCommandHandler.instance.registerCommand(
            new BedwarsCommand(config)
        );
        ClientCommandHandler.instance.registerCommand(new StatsifyCommand());
        ClientCommandHandler.instance.registerCommand(
            new ClearCacheCommand(playerSuffixes)
        );
        ClientCommandHandler.instance.registerCommand(
            new DenickCommand(config)
        );
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);
        String message = event.message.getUnformattedText();
        if (config.autoWho) {
            if (
                message.contains(
                    "Protect your bed and destroy the enemy beds."
                ) &&
                !(message.contains(":")) &&
                !(message.contains("SHOUT"))
            ) {
                mc.thePlayer.sendChatMessage("/who");
            }
        }
        if (message.startsWith("ONLINE:")) {
            String playersString = message.substring("ONLINE:".length()).trim();
            String[] players = playersString.split(",\\s*");
            onlinePlayers = new ArrayList<>(Arrays.asList(players));
            nickUtils.updateNickedPlayers(onlinePlayers);
            checkStatsRatelimitless();
            if (config.urchin) {
                checkUrchinTags();
            }
        }

        if (message.startsWith(" ") && message.contains("Opponent:")) {
            String username = Utils.parseUsername(message);
            new Thread(() -> {
                try {
                    String stats = planckeApi.checkDuels(username);
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText("§r[§bF§r] " + stats)
                    );
                } catch (IOException e) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to get stats for " + username
                        )
                    );
                }
            })
                .start();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (numberDenicker != null) {
            numberDenicker.onWorldChange();
        }
        if (pregameStats != null) {
            pregameStats.onWorldChange();
        }
        if (nickUtils != null) {
            nickUtils.clearNicks();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderTabList(RenderGameOverlayEvent.Post event) {
        if (
            event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST
        ) return;
        if (!config.tabStats) return;
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        Collection<NetworkPlayerInfo> playerInfoList =
            mc.thePlayer.sendQueue.getPlayerInfoMap();
        if (playerInfoList.isEmpty()) return;

        for (NetworkPlayerInfo playerInfo : playerInfoList) {
            if (
                playerInfo == null || playerInfo.getGameProfile() == null
            ) continue;

            String playerName = playerInfo.getGameProfile().getName();
            if (playerName == null) continue;
            List<String> suffixv = playerSuffixes.get(playerName);
            boolean isNicked = nickUtils.isNicked(playerName);
            String currentDisplayName;
            if (playerInfo.getDisplayName() != null) {
                currentDisplayName = playerInfo
                    .getDisplayName()
                    .getFormattedText();
            } else {
                currentDisplayName = playerName;
            }

            if (suffixv != null && suffixv.size() >= 2) {
                // Player has stats, so they are not nicked
                String[] tabData = Utils.getTabDisplayName2(playerName);
                String team = tabData[0],
                    name = tabData[1];

                String teamColor = team.length() >= 2
                    ? team.substring(0, 2)
                    : "";
                String newDisplayName;

                switch (config.tabFormat) {
                    case 1:
                        newDisplayName =
                            team +
                            suffixv.get(0) +
                            "\u30fb" +
                            teamColor +
                            name +
                            "\u30fb" +
                            suffixv.get(1);
                        break;
                    case 2:
                        newDisplayName =
                            team + teamColor + name + "\u30fb" + suffixv.get(1);
                        break;
                    case 0:
                    default:
                        newDisplayName =
                            team +
                            "§7[" +
                            suffixv.get(0) +
                            "§7] " +
                            teamColor +
                            name +
                            "\u30fb" +
                            suffixv.get(1);
                        break;
                }

                if (suffixv.size() >= 3) {
                    newDisplayName += "§7\u30fb" + suffixv.get(2);
                }

                if (!currentDisplayName.equals(newDisplayName)) {
                    playerInfo.setDisplayName(
                        new ChatComponentText(newDisplayName)
                    );
                }
            } else if (isNicked && !currentDisplayName.contains("§c[NICK]")) {
                // Player is nicked, does not have stats
                String[] tabData = Utils.getTabDisplayName2(playerName);
                if (tabData != null && tabData.length >= 3) {
                    String team = tabData[0] != null ? tabData[0] : "";
                    String name = tabData[1] != null ? tabData[1] : "";
                    String suffix = tabData[2] != null ? tabData[2] : "";
                    String teamColor = team.length() >= 2
                        ? team.substring(0, 2)
                        : "";
                    playerInfo.setDisplayName(
                        new ChatComponentText(
                            team + "§c[NICK] " + teamColor + name + suffix
                        )
                    );
                } else {
                    playerInfo.setDisplayName(
                        new ChatComponentText("§c[NICK] " + currentDisplayName)
                    );
                }
            }
        }
    }

    private void checkUrchinTags() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;
            executor.submit(() -> {
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
            });
        }
        executor.shutdown();
    }

    private void checkStatsRatelimitless() {
        final int MAX_THREADS = 20;
        int poolSize = Math.min(onlinePlayers.size(), MAX_THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;
            executor.submit(() -> {
                try {
                    String stats = hypixelApi.fetchBedwarsStats(
                        playerName,
                        config.minFkdr,
                        config.tags,
                        config.tabStats,
                        playerSuffixes
                    );
                    if (!stats.isEmpty() && config.printStats) {
                        mc.addScheduledTask(() ->
                            mc.thePlayer.addChatMessage(
                                new ChatComponentText("§r[§bF§r] " + stats)
                            )
                        );
                    }
                } catch (IOException e) {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r] Failed to fetch stats for: " +
                                    playerName +
                                    " | [UpstreamCSR] "
                            )
                        )
                    );
                }
            });
        }

        executor.shutdown();

        new Thread(() -> {
            try {
                if (executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r]§a Checks completed."
                            )
                        )
                    );
                } else {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r]§c Timeout waiting for completion."
                            )
                        )
                    );
                }
            } catch (InterruptedException e) {
                mc.addScheduledTask(() ->
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cError while waiting: " + e.getMessage()
                        )
                    )
                );
            }
        })
            .start();
    }
}
