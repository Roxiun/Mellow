package com.roxiun.mellow.mixin.hitbox;

//? if ornithe {
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//?}
import com.roxiun.mellow.util.hitbox.HitboxRenderContext;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
//? if forge {
/*import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(RenderManager.class)
//? if ornithe {
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
//?} else {
/*public class RenderManagerHitboxContextMixin {

    @Inject(method = "renderDebugBoundingBox", at = @At("HEAD"))
    private void mellow$captureHitboxEntity(
        Entity entityIn,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        HitboxRenderContext.setCurrentEntity(entityIn);
    }

    @Inject(method = "renderDebugBoundingBox", at = @At("RETURN"))
    private void mellow$clearHitboxEntity(
        Entity entityIn,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        HitboxRenderContext.clearCurrentEntity();
        HitboxRenderContext.exitReentryGuard();
*///?}
    }
}
