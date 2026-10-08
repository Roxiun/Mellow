package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public class RenderGameOverlayEvent implements Event {
 public enum ElementType { ALL, PLAYER_LIST, TEXT }
 public final ElementType type;
 public final float partialTicks;
 public final net.minecraft.client.gui.ScaledResolution resolution;
 private boolean canceled;
 protected RenderGameOverlayEvent(ElementType type,float partialTicks) {this.type=type;this.partialTicks=partialTicks;resolution=new net.minecraft.client.gui.ScaledResolution(net.minecraft.client.Minecraft.getMinecraft());}
 public void setCanceled(boolean value) {canceled=value;}
 public boolean isCanceled() {return canceled;}
 public static final class Pre extends RenderGameOverlayEvent {public Pre(ElementType t,float p){super(t,p);}}
 public static final class Post extends RenderGameOverlayEvent {public Post(ElementType t,float p){super(t,p);}}
}
