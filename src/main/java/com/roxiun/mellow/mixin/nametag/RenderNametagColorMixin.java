package com.roxiun.mellow.mixin.nametag;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import net.minecraft.client.renderer.entity.Render;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Render.class)
public class RenderNametagColorMixin {

    @ModifyArgs(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;color(FFFF)Lnet/minecraft/client/renderer/WorldRenderer;"
        )
    )
    private void mellow$setBackgroundColor(Args args) {
        if (!NametagRenderContext.isActive()) {
            return;
        }

        RgbaColor color = NametagRenderContext.getColor();
        if (color == null) {
            return;
        }

        args.set(0, color.getRed() / 255f);
        args.set(1, color.getGreen() / 255f);
        args.set(2, color.getBlue() / 255f);
    }
}
