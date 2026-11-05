package com.roxiun.mellow.mixin.nametag;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.nametag.NametagContextResolver;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RendererLivingEntity.class)
public abstract class NametagContextMixin {
    @WrapMethod(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V")
    private void mellow$nametag(EntityLivingBase entity, double x, double y, double z,
                                Operation<Void> original) {
        try {
            NametagContextResolver.prepare(entity, Mellow.config);
            original.call(entity, x, y, z);
        } finally {
            NametagRenderContext.clear();
        }
    }
}
