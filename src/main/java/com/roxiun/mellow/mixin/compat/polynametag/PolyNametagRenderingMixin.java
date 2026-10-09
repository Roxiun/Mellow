package com.roxiun.mellow.mixin.compat.polynametag;
//? if forge {
/*
import cc.polyfrost.oneconfig.config.core.OneColor;
*///?}
import com.roxiun.mellow.Mellow;
//? if forge {
/*import com.roxiun.mellow.config.MellowOneConfig;
*///?}
import com.roxiun.mellow.util.nametag.NametagRenderContext;
//? if ornithe {
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** PolyNametag 1.2.1 uses packed ARGB and its new client renderer. */
//?} else {
/*import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

*///?}
@Pseudo
//? if ornithe {
@Mixin(targets="org.polyfrost.polynametag.client.NametagRenderer",remap=false)
public abstract class PolyNametagRenderingMixin {
 @Inject(method={"backgroundColor(I)I","backgroundArgb()I"},at=@At("RETURN"),cancellable=true,require=1)
 private static void mellow$background(CallbackInfoReturnable<Integer> cir) {
  var config=Mellow.config;
  var color=NametagRenderContext.getColor();
  if(config!=null && config.coloredNametagBackgrounds && config.coloredNametagAffectPolyNametag && color!=null)
   cir.setReturnValue((cir.getReturnValue() & 0xff000000) | (color.getRGB() & 0xffffff));
 }
//?} else {
/*@Mixin(targets = "org.polyfrost.polynametag.render.NametagRenderingKt", remap = false)
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
*///?}

}
