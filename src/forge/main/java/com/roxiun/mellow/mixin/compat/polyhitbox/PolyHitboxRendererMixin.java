package com.roxiun.mellow.mixin.compat.polyhitbox;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.hitbox.HitboxRenderContext;
import com.roxiun.mellow.util.hitbox.TeamHitboxColorResolver;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.polyfrost.polyhitbox.render.HitboxRenderer", remap = false)
public class PolyHitboxRendererMixin {

    @Dynamic
    @Inject(method = "renderHitbox", at = @At("HEAD"), remap = false, require = 0)
    private void mellow$captureEntity(
        @Coerce Object config,
        Entity entity,
        double x,
        double y,
        double z,
        float partialTicks,
        CallbackInfo ci
    ) {
        HitboxRenderContext.setCurrentEntity(entity);
    }

    @Dynamic
    @Inject(method = "renderHitbox", at = @At("RETURN"), remap = false, require = 0)
    private void mellow$clearEntity(
        @Coerce Object config,
        Entity entity,
        double x,
        double y,
        double z,
        float partialTicks,
        CallbackInfo ci
    ) {
        HitboxRenderContext.clearCurrentEntity();
    }

    @Dynamic
    @ModifyArg(
        method = "renderHitbox",
        at = @At(
            value = "INVOKE",
            target = "Lorg/polyfrost/polyhitbox/render/HitboxRenderer;drawBoxOutline(Lorg/polyfrost/polyhitbox/config/HitboxConfig;Lnet/minecraft/util/AxisAlignedBB;Lcc/polyfrost/oneconfig/config/core/OneColor;F)V"
        ),
        index = 2,
        remap = false,
        require = 0
    )
    private OneColor mellow$replaceOutlineColor(OneColor originalColor) {
        return resolveTeamColorOrOriginal(originalColor);
    }

    @Dynamic
    @ModifyArg(
        method = "drawSide",
        at = @At(
            value = "INVOKE",
            target = "Lorg/polyfrost/polyhitbox/render/HitboxRenderer;glColor(Lcc/polyfrost/oneconfig/config/core/OneColor;)V"
        ),
        index = 0,
        remap = false,
        require = 0
    )
    private OneColor mellow$replaceSideColor(OneColor originalColor) {
        return resolveTeamColorOrOriginal(originalColor);
    }

    private OneColor resolveTeamColorOrOriginal(OneColor originalColor) {
        if (originalColor == null) {
            return null;
        }

        MellowOneConfig config = Mellow.config;
        if (
            config == null ||
            !config.coloredHitboxes ||
            !config.coloredHitboxesAffectPolyHitbox
        ) {
            return originalColor;
        }

        OneColor teamColor = TeamHitboxColorResolver.resolveTeamHitboxColor(
            HitboxRenderContext.getCurrentEntity(),
            config,
            originalColor.getAlpha()
        );
        return teamColor == null ? originalColor : teamColor;
    }
}
