package com.roxiun.mellow.platform;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.hitbox.TeamHitboxColorResolver;
import org.polyfrost.polyhitbox.api.HitboxColors;
import org.polyfrost.polyhitbox.api.HitboxElement;
/** PolyHitbox 1.3.1 / OneConfig v1's public provider API. Loaded only when present. */
public final class PolyHitboxIntegration {
 public static void register() {
  HitboxColors.register((context, argb) -> {
   var config=Mellow.config;
   if(config==null || !config.coloredHitboxes || !config.coloredHitboxesAffectPolyHitbox) return argb;
   if(context.getElement()!=HitboxElement.OUTLINE && context.getElement()!=HitboxElement.SIDE) return argb;
   var color=TeamHitboxColorResolver.resolveTeamHitboxColor(context.getEntity(),config,argb>>>24);
   return color==null?argb:color.getRGB();
  });
 }
}
