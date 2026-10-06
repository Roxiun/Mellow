package com.roxiun.mellow.util.nametag;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.seraph.SeraphClientType;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.hitbox.TeamHitboxColorResolver;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public final class NametagContextResolver {
    private NametagContextResolver() {}

    public static void prepare(EntityLivingBase entity, MellowOneConfig config) {
        NametagRenderContext.clear();

        if (config == null) {
            return;
        }

        RgbaColor color = resolveNametagColor(entity, config);
        SeraphClientType clientType = resolveClientType(entity, config);
        if (color != null || clientType != null) {
            NametagRenderContext.setState(
                color,
                clientType,
                config.nametagClientIconPosition == 0,
                entity.getDisplayName().getFormattedText()
            );
        }
    }

    private static RgbaColor resolveNametagColor(EntityLivingBase entity, MellowOneConfig config) {
        if (!config.coloredNametagBackgrounds) {
            return null;
        }

        return TeamHitboxColorResolver.resolveTeamHitboxColor(entity, config, 255);
    }

    private static SeraphClientType resolveClientType(EntityLivingBase entity, MellowOneConfig config) {
        if (
            !config.showClientIconsInNametags ||
            !config.seraph ||
            Mellow.seraphClientCacheService == null ||
            !(entity instanceof EntityPlayer)
        ) {
            return null;
        }

        EntityPlayer player = (EntityPlayer) entity;
        if (
            player.getGameProfile() == null ||
            player.getGameProfile().getName() == null ||
            player.getGameProfile().getName().trim().isEmpty()
        ) {
            return null;
        }

        String playerName = player.getGameProfile().getName();
        SeraphClientType cachedClient = Mellow.seraphClientCacheService.getCachedClient(
            playerName
        );
        if (cachedClient != null) {
            return cachedClient;
        }

        if (player.getUniqueID() == null || player.getUniqueID().version() != 4) {
            return null;
        }

        Mellow.seraphClientCacheService.refreshClientAsync(
            playerName,
            player.getUniqueID().toString().replace("-", "")
        );
        return null;
    }
}
