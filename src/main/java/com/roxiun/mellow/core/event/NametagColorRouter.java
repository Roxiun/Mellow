package com.roxiun.mellow.core.event;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.hitbox.TeamHitboxColorResolver;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import net.minecraft.entity.EntityLivingBase;
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

        if (config == null) {
            return;
        }

        NametagRenderContext.setColor(resolveNametagColor(event.entity));
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

}
