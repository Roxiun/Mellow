package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public class TickEvent implements Event {
 public enum Phase { START, END }
 public final Phase phase;
 protected TickEvent(Phase phase) { this.phase = phase; }
 public static final class ClientTickEvent extends TickEvent { public ClientTickEvent(Phase p) { super(p); } }
 public static final class PlayerTickEvent extends TickEvent {
  public final net.minecraft.entity.player.EntityPlayer player;
  public PlayerTickEvent(Phase p, net.minecraft.entity.player.EntityPlayer player) { super(p); this.player=player; }
 }
}
