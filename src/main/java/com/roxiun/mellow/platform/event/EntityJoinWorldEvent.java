package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public final class EntityJoinWorldEvent implements Event {
 public final net.minecraft.entity.Entity entity;
 public final net.minecraft.world.World world;
 public EntityJoinWorldEvent(net.minecraft.entity.Entity entity, net.minecraft.world.World world) { this.entity=entity;this.world=world; }
}
