package com.roxiun.mellow.mixin.platform;

import com.roxiun.mellow.platform.ClientCommands;
import java.util.List;
import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class ClientChatCompletionMixin {
    @Shadow private boolean waitingOnAutocomplete;
    @Shadow public abstract void onAutocompleteResponse(String[] matches);
    @Unique private List<String> mellow$commandCompletions = List.of();

    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void mellow$complete(String input, String ignored, CallbackInfo ci) {
        mellow$commandCompletions = ClientCommands.completeCommandName(input);
        if (!ClientCommands.owns(input) || !mellow$commandCompletions.isEmpty()) return;
        waitingOnAutocomplete = true;
        onAutocompleteResponse(ClientCommands.complete(input).toArray(new String[0]));
        ci.cancel();
    }

    // Keep server suggestions when a local command shares their prefix.
    @ModifyVariable(method = "onAutocompleteResponse", at = @At("HEAD"), argsOnly = true)
    private String[] mellow$mergeCommands(String[] matches) {
        if (mellow$commandCompletions.isEmpty()) return matches;
        String[] merged = java.util.stream.Stream.concat(
            java.util.Arrays.stream(matches), mellow$commandCompletions.stream())
            .distinct().sorted().toArray(String[]::new);
        mellow$commandCompletions = List.of();
        return merged;
    }
}
