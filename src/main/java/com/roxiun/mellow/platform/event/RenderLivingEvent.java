package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public class RenderLivingEvent implements Event {
 public static class Specials {
  public static final class Pre<T extends net.minecraft.entity.EntityLivingBase> implements Event { public final T entity;public Pre(T entity){this.entity=entity;} }
  public static final class Post<T extends net.minecraft.entity.EntityLivingBase> implements Event { public final T entity;public Post(T entity){this.entity=entity;} }
 }
}
