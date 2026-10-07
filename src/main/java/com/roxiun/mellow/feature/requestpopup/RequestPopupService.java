package com.roxiun.mellow.feature.requestpopup;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.ChatUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestPopupService {

    private static final Pattern FRIEND_REQUEST_PATTERN = Pattern.compile(
        "Friend request from ((?:\\[.+] )?(?<player>\\S{1,16})).*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PARTY_INVITE_PATTERN = Pattern.compile(
        "(?:\\[.*] )?(?<player>\\S{1,16}) has invited you to join (?:their|(?:\\[.*] ?)?\\w{1,16}'s)? party!",
        Pattern.CASE_INSENSITIVE
    );

    private final MellowOneConfig config;
    private final RequestPopupManager popupManager;

    public RequestPopupService(
        MellowOneConfig config,
        RequestPopupManager popupManager
    ) {
        this.config = config;
        this.popupManager = popupManager;
    }

    public void onChatMessage(String message) {
        if (
            config == null ||
            popupManager == null ||
            !config.requestPopupsEnabled ||
            !HypixelUtils.INSTANCE.isHypixel()
        ) {
            return;
        }

        String stripped = ChatUtils.stripFormatting(message).trim();
        if (stripped.isEmpty() || stripped.contains(":")) {
            return;
        }

        String lower = stripped.toLowerCase(Locale.ROOT);

        for (String line : stripped.split("\\r?\\n")) {
            if (
                config.friendRequestPopupsEnabled &&
                lower.contains("friend request") &&
                !lower.contains("has expired")
            ) {
                Matcher friendMatcher = FRIEND_REQUEST_PATTERN.matcher(line.trim());
                if (friendMatcher.matches()) {
                    popupManager.enqueue(
                        RequestType.FRIEND,
                        friendMatcher.group("player")
                    );
                    return;
                }
            }

            if (
                config.partyInvitePopupsEnabled &&
                lower.contains("has invited you to join")
            ) {
                Matcher partyMatcher = PARTY_INVITE_PATTERN.matcher(line.trim());
                if (partyMatcher.matches()) {
                    popupManager.enqueue(
                        RequestType.PARTY,
                        partyMatcher.group("player")
                    );
                    return;
                }
            }
        }
    }
}
