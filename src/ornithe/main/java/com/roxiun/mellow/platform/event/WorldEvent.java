package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public class WorldEvent implements Event {
 public final net.minecraft.world.World world;
 protected WorldEvent(net.minecraft.world.World world) { this.world=world; }
 public static final class Disconnected implements Event {}
 public static final class Load extends WorldEvent { public Load(net.minecraft.world.World w) {super(w);} }
 public static final class Unload extends WorldEvent { public Unload(net.minecraft.world.World w) {super(w);} }
}
