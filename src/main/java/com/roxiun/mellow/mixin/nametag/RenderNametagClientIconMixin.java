package com.roxiun.mellow.mixin.nametag;

import com.roxiun.mellow.util.nametag.NametagClientIconRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.Render;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Render.class)
public class RenderNametagClientIconMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "renderLivingLabel")
    private void mellow$labelContext(net.minecraft.entity.Entity entity, String text, double x, double y, double z, int distance, Operation<Void> original) {
        com.roxiun.mellow.util.nametag.NametagRenderContext.beginLabel(text);
        try { original.call(entity, text, x, y, z, distance); }
        finally { com.roxiun.mellow.util.nametag.NametagRenderContext.endLabel(); }
    }


    @WrapOperation(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"
        ),
        require = 0
    )
    private int mellow$expandNametagWidth(FontRenderer fontRenderer, String text, Operation<Integer> original) {
        return NametagClientIconRenderer.adjustWidth(
            text,
            original.call(fontRenderer, text)
        );
    }

    @WrapOperation(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"
        ),
        require = 0
    )
    private int mellow$drawNametagWithClientIcon(
        FontRenderer fontRenderer,
        String text,
        int x,
        int y,
        int color,
        Operation<Integer> original
    ) {
        int adjustedX = NametagClientIconRenderer.adjustTextX(text, x);
        NametagClientIconRenderer.drawActiveIcon(
            fontRenderer,
            text,
            adjustedX,
            com.roxiun.mellow.platform.PolyNametagIntegration.textY(y),
            color
        );
        return original.call(fontRenderer, text, adjustedX, y, color);
    }
}
