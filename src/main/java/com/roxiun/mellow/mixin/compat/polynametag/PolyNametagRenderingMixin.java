package com.roxiun.mellow.mixin.compat.polynametag;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(targets = "org.polyfrost.polynametag.render.NametagRenderingKt", remap = false)
public class PolyNametagRenderingMixin {

    @Dynamic
    @ModifyArgs(
        method = "drawBackground",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V"
        ),
        remap = false,
        require = 0
    )
    private static void mellow$setBackgroundColor(Args args) {
        MellowOneConfig config = Mellow.config;
        if (
            config == null ||
            !config.coloredNametagBackgrounds ||
            !config.coloredNametagAffectPolyNametag ||
            !NametagRenderContext.isActive()
        ) {
            return;
        }

        OneColor color = NametagRenderContext.getColor();
        if (color == null) {
            return;
        }

        args.set(0, color.getRed() / 255f);
        args.set(1, color.getGreen() / 255f);
        args.set(2, color.getBlue() / 255f);
    }

}
