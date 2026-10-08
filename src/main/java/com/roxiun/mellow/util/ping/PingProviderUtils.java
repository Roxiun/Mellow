package com.roxiun.mellow.util.ping;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.gamestate.GameSnapshot;

public final class PingProviderUtils {

    public static final int PROVIDER_NONE = 0;
    public static final int PROVIDER_AURORA = 1;
    public static final int PROVIDER_LUNA = 2;

    private PingProviderUtils() {}

    public static boolean shouldUseAurora(MellowOneConfig config) {
        return (
            config != null &&
            config.pingProvider == PROVIDER_AURORA
        );
    }

    public static boolean shouldUseLuna(MellowOneConfig config) {
        return config != null && config.pingProvider == PROVIDER_LUNA;
    }

    public static boolean hasLunaApiKey(MellowOneConfig config) {
        return hasValue(config == null ? null : config.lunaPingApiKey);
    }

    public static boolean canUseExternalPing(GameSnapshot snapshot) {
        return snapshot != null && snapshot.isOnHypixel() && !snapshot.isLobby();
    }

    /** Called once per second on the client thread; render hooks only read cached values. */
    public static void warmTab(GameSnapshot snapshot, MellowOneConfig config) {
        if (!canUseExternalPing(snapshot) || config == null) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;
        for (net.minecraft.client.network.NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null
                || info.getGameProfile().getId().version() != 4
                || com.roxiun.mellow.util.player.PlayerUtils.isObfuscatedTabEntry(info)) continue;
            if (com.roxiun.mellow.Mellow.nickUtils != null
                && com.roxiun.mellow.Mellow.nickUtils.isNicked(info.getGameProfile().getName())) continue;
            int ping = info.getResponseTime();
            if (ping > 1 && ping < 999) continue;
            String uuid = info.getGameProfile().getId().toString();
            if (shouldUseAurora(config) && com.roxiun.mellow.Mellow.auroraPingService != null)
                com.roxiun.mellow.Mellow.auroraPingService.fetchAsync(uuid.replace("-", ""));
            else if (shouldUseLuna(config) && hasLunaApiKey(config) && com.roxiun.mellow.Mellow.lunaPingService != null)
                com.roxiun.mellow.Mellow.lunaPingService.fetchAsync(uuid, config.lunaPingApiKey);
        }
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
