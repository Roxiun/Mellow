package com.roxiun.mellow.core.event;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.seraph.SeraphClientType;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.hitbox.TeamHitboxColorResolver;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NametagColorRouter {

    private final MellowOneConfig config;

    public NametagColorRouter(MellowOneConfig config) {
        this.config = config;
    }

    @SubscribeEvent
    public void onPreRenderNametag(
        RenderLivingEvent.Specials.Pre<EntityLivingBase> event
    ) {
        NametagRenderContext.clear();

        if (!Mellow.isEnabled()) {
            return;
        }

        OneColor color = resolveNametagColor(event.entity);
        SeraphClientType clientType = resolveClientType(event.entity);
        if (color != null || clientType != null) {
            NametagRenderContext.setState(
                color,
                clientType,
                config.nametagClientIconPosition == 0,
                event.entity.getDisplayName().getFormattedText()
            );
        }
    }

    @SubscribeEvent
    public void onPostRenderNametag(
        RenderLivingEvent.Specials.Post<EntityLivingBase> event
    ) {
        NametagRenderContext.clear();
    }

    private OneColor resolveNametagColor(EntityLivingBase entity) {
        if (!config.coloredNametagBackgrounds) {
            return null;
        }

        return TeamHitboxColorResolver.resolveTeamHitboxColor(entity, config, 255);
    }

    private SeraphClientType resolveClientType(EntityLivingBase entity) {
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
