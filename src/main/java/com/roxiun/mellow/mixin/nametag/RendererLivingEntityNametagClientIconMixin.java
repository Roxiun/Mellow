package com.roxiun.mellow.mixin.nametag;

import com.roxiun.mellow.util.nametag.NametagClientIconRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(RendererLivingEntity.class)
public class RendererLivingEntityNametagClientIconMixin {

    @WrapOperation(
        method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
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
        method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
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
            y,
            color
        );
        return original.call(fontRenderer, text, adjustedX, y, color);
    }
}
