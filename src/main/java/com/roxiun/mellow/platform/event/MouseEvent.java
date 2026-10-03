package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public final class MouseEvent implements Event {
 public final int dwheel,button;
 public final boolean buttonstate;
 private boolean canceled;
 public MouseEvent(int wheel,int button,boolean state){dwheel=wheel;this.button=button;buttonstate=state;}
 public void setCanceled(boolean value){canceled=value;}
 public boolean isCanceled(){return canceled;}
}
