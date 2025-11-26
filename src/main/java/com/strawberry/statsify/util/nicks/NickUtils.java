package com.strawberry.statsify.util.nicks;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;

public class NickUtils {

    private final Set<String> nickedPlayers = new HashSet<>();
    private final Minecraft mc = Minecraft.getMinecraft();

    public void updateNickedPlayers(Collection<String> onlinePlayers) {
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        Map<String, UUID> playerToUuid = new HashMap<>();
        for (NetworkPlayerInfo networkPlayerInfo : mc.thePlayer.sendQueue.getPlayerInfoMap()) {
            if (
                networkPlayerInfo != null &&
                networkPlayerInfo.getGameProfile() != null &&
                networkPlayerInfo.getGameProfile().getId() != null
            ) {
                playerToUuid.put(
                    networkPlayerInfo.getGameProfile().getName(),
                    networkPlayerInfo.getGameProfile().getId()
                );
            }
        }

        for (String player : onlinePlayers) {
            if (playerToUuid.containsKey(player)) {
                UUID uuid = playerToUuid.get(player);
                if (uuid != null && uuid.version() == 1) {
                    if (nickedPlayers.add(player)) {
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r] §c" + player + " is nicked."
                            )
                        );
                    }
                }
            }
        }
    }

    public boolean isNicked(String playerName) {
        return nickedPlayers.contains(playerName);
    }

    public void clearNicks() {
        nickedPlayers.clear();
    }
}
