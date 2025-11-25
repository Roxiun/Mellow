package com.strawberry.statsify.util;

import com.strawberry.statsify.api.UrchinApi;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class UrchinUtils {

    private static String getFormattedTags(
        String username,
        UrchinApi urchinApi,
        String urchinKey
    ) throws IOException {
        return urchinApi
            .fetchUrchinTags(username, urchinKey)
            .replace("sniper", "§4§lSniper")
            .replace("blatant_cheater", "§4§lBlatant Cheater")
            .replace("closet_cheater", "§e§lCloset Cheater")
            .replace("confirmed_cheater", "§4§lConfirmed Cheater")
            .replace("possible_sniper", "§e§l Possible Sniper")
            .replace("legit_sniper", "§e§l Legit Sniper")
            .replace("caution", "§e§l Caution")
            .replace("caution", "§e§l Account")
            .replace("caution", "§f§l Info");
    }

    private static void printMessage(String message) {
        Minecraft.getMinecraft().addScheduledTask(() ->
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(message)
            )
        );
    }

    private static void handleException(String username, Exception e) {
        printMessage(
            "§r[§bF§r] Failed to fetch tags for " +
                username +
                " | " +
                e.getMessage()
        );
    }

    public static void checkAndPrintUrchinTags(
        String username,
        UrchinApi urchinApi,
        String urchinKey,
        boolean withPlayerName
    ) {
        try {
            String tags = getFormattedTags(username, urchinApi, urchinKey);
            if (!tags.isEmpty()) {
                String message;
                if (withPlayerName) {
                    message =
                        "§r[§bF§r] §c⚠ §r" +
                        PlayerUtils.getTabDisplayName(username) +
                        " §ris §ctagged§r for: " +
                        tags;
                } else {
                    message = "§r[§bF§r] §c⚠ §r§cTagged§r for: " + tags;
                }
                printMessage(message);
            }
        } catch (IOException e) {
            handleException(username, e);
        }
    }
}
