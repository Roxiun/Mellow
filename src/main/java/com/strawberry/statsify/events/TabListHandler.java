package com.strawberry.statsify.events;

import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.util.NickUtils;
import com.strawberry.statsify.util.Utils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TabListHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsifyOneConfig config;
    private final NickUtils nickUtils;
    private final Map<String, List<String>> playerSuffixes;

    public TabListHandler(
        StatsifyOneConfig config,
        NickUtils nickUtils,
        Map<String, List<String>> playerSuffixes
    ) {
        this.config = config;
        this.nickUtils = nickUtils;
        this.playerSuffixes = playerSuffixes;
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
                String team = tabData[0], name = tabData[1];

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
}
