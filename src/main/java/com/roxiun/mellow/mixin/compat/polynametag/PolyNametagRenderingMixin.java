package com.roxiun.mellow.mixin.compat.polynametag;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** PolyNametag 1.2.1 uses packed ARGB and its new client renderer. */
@Pseudo
@Mixin(targets="org.polyfrost.polynametag.client.NametagRenderer",remap=false)
public abstract class PolyNametagRenderingMixin {
 @Inject(method={"backgroundColor(I)I","backgroundArgb()I"},at=@At("RETURN"),cancellable=true,require=1)
 private static void mellow$background(CallbackInfoReturnable<Integer> cir) {
  var config=Mellow.config;
  var color=NametagRenderContext.getColor();
  if(config!=null && config.coloredNametagBackgrounds && config.coloredNametagAffectPolyNametag && color!=null)
   cir.setReturnValue((cir.getReturnValue() & 0xff000000) | (color.getRGB() & 0xffffff));
 }

 @ModifyVariable(method="backgroundQuads",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private static float mellow$centerBackground(float x) { return x - mellow$reservedWidth()/2f; }
 @ModifyVariable(method="backgroundQuads",at=@At("HEAD"),argsOnly=true,ordinal=2)
 private static float mellow$expandBackground(float width) { return width + mellow$reservedWidth(); }
 @Unique private static int mellow$reservedWidth() {
  String label=NametagRenderContext.getRenderedLabel();
  return com.roxiun.mellow.util.nametag.NametagClientIconRenderer.adjustWidth(label,0);
 }
}
