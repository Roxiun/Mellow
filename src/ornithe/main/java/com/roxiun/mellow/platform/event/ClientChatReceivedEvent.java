package com.roxiun.mellow.platform.event;
import org.polyfrost.oneconfig.api.event.v1.events.Event;
public final class ClientChatReceivedEvent implements Event {
 public net.minecraft.util.IChatComponent message;
 public final byte type;
 private boolean canceled;
 public ClientChatReceivedEvent(byte type, net.minecraft.util.IChatComponent message) {this.type=type;this.message=message;}
 public void setCanceled(boolean value) {canceled=value;}
 public boolean isCanceled() {return canceled;}
}
