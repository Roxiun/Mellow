package com.roxiun.mellow.mixin.platform;
import com.roxiun.mellow.platform.ClientCommands;
import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiChat.class)
public abstract class ClientChatCompletionMixin {
 @Shadow private boolean waitingOnAutocomplete;
 @Shadow public abstract void onAutocompleteResponse(String[] matches);
 @Inject(method="sendAutocompleteRequest",at=@At("HEAD"),cancellable=true)
 private void mellow$complete(String input,String ignored,CallbackInfo ci) {
  if (!ClientCommands.owns(input)) return;
  waitingOnAutocomplete=true;
  onAutocompleteResponse(ClientCommands.complete(input).toArray(new String[0]));
  ci.cancel();
 }
}
