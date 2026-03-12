package com.roxiun.mellow.mixin.hitbox;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.hitbox.HitboxRenderContext;
import com.roxiun.mellow.util.hitbox.TeamHitboxColorResolver;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class RenderGlobalHitboxColorMixin {

    @Shadow
    public static void drawOutlinedBoundingBox(
        AxisAlignedBB boundingBox,
        int red,
        int green,
        int blue,
        int alpha
    ) {}

    @Inject(method = "drawOutlinedBoundingBox", at = @At("HEAD"), cancellable = true)
    private static void mellow$applyTeamHitboxColor(
        AxisAlignedBB boundingBox,
        int red,
        int green,
        int blue,
        int alpha,
        CallbackInfo ci
    ) {
        if (!Mellow.isEnabled()) {
            return;
        }
        MellowOneConfig config = Mellow.config;
        if (
            config == null ||
            !config.coloredHitboxes ||
            !config.coloredHitboxesAffectVanillaDebug
        ) {
            return;
        }

        Entity current = HitboxRenderContext.getCurrentEntity();
        OneColor teamColor = TeamHitboxColorResolver.resolveTeamHitboxColor(
            current,
            config,
            alpha
        );
        if (teamColor == null) {
            return;
        }

        if (!HitboxRenderContext.tryEnterReentryGuard()) {
            return;
        }

        try {
            drawOutlinedBoundingBox(
                boundingBox,
                teamColor.getRed(),
                teamColor.getGreen(),
                teamColor.getBlue(),
                teamColor.getAlpha()
            );
            ci.cancel();
        } finally {
            HitboxRenderContext.exitReentryGuard();
        }
    }
}
