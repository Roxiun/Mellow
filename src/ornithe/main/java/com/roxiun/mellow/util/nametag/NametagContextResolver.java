package com.roxiun.mellow.util.nametag;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.Mellow;
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

        NametagRenderContext.setColor(resolveNametagColor(entity, config));
    }

    private static RgbaColor resolveNametagColor(EntityLivingBase entity, MellowOneConfig config) {
        if (!config.coloredNametagBackgrounds) {
            return null;
        }

        return TeamHitboxColorResolver.resolveTeamHitboxColor(entity, config, 255);
    }

}
