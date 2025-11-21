package com.strawberry.statsify.mixin;

import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.api.PolsuApi;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetworkPlayerInfo.class)
public class PingMixin {

    private static final PolsuApi polsuApi = new PolsuApi();
    private static final ExecutorService EXECUTOR =
        Executors.newFixedThreadPool(3);

    @Shadow
    private int responseTime;

    @Inject(method = "getResponseTime", at = @At("HEAD"), cancellable = true)
    private void onGetResponseTime(CallbackInfoReturnable<Integer> cir) {
        int original = this.responseTime;

        if (!Statsify.config.polsuPing) {
            cir.setReturnValue(original);
            return;
        }

        String uuid = ((NetworkPlayerInfo) (Object) this).getGameProfile()
            .getId()
            .toString();

        // Use cached ping if exists
        int cached = polsuApi.getCachedPing(uuid);
        if (cached != -1) {
            cir.setReturnValue(cached);
            return;
        }

        // Return vanilla ping while waiting for async fetch
        cir.setReturnValue(original);

        // Prevent duplicate fetch tasks
        if (!polsuApi.tryStartFetch(uuid)) return; // fetch already in progress

        EXECUTOR.submit(() -> {
            try {
                int ping = polsuApi.fetchPingBlocking(uuid);
                // (cache updated inside fetch)
            } finally {
                polsuApi.finishFetch(uuid);
            }
        });
    }
}
