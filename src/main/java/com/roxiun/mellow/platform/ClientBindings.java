package com.roxiun.mellow.platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import java.util.Arrays;
public final class ClientBindings {
 public static void register(KeyBinding binding) {
  var settings=Minecraft.getMinecraft().gameSettings;
  settings.keyBindings=Arrays.copyOf(settings.keyBindings,settings.keyBindings.length+1);
  settings.keyBindings[settings.keyBindings.length-1]=binding;
  KeyBinding.resetKeyBindingArrayAndHash();
 }
}
