package com.roxiun.mellow.mixin.hitbox;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roxiun.mellow.util.hitbox.HitboxRenderContext;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderManager.class)
public abstract class RenderManagerHitboxContextMixin {
    @WrapMethod(method = "renderDebugBoundingBox")
    private void mellow$hitbox(Entity entity, double x, double y, double z,
                               float entityYaw, float partialTicks, Operation<Void> original) {
        try {
            HitboxRenderContext.setCurrentEntity(entity);
            original.call(entity, x, y, z, entityYaw, partialTicks);
        } finally {
            HitboxRenderContext.clearCurrentEntity();
            HitboxRenderContext.exitReentryGuard();
        }
    }
}
