package com.strawberry.statsify.mixin;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.api.ping.PolsuApi;
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

        String playerName =
            ((NetworkPlayerInfo) (Object) this).getGameProfile().getName();
        if (Statsify.nickUtils.isNicked(playerName)) {
            cir.setReturnValue(original);
            return;
        }

        // pingProvider: 0 = None, 1 = Polsu, 2 = Urchin
        if (
            Statsify.config.pingProvider == 0 ||
            !HypixelUtils.INSTANCE.isHypixel()
        ) {
            cir.setReturnValue(original);
            return;
        }

        if (original > 1 && original < 999) {
            cir.setReturnValue(original);
            return;
        }

        String uuid = ((NetworkPlayerInfo) (Object) this).getGameProfile()
            .getId()
            .toString();

        if (Statsify.config.pingProvider == 1) {
            // Polsu
            int cached = polsuApi.getCachedPing(uuid);
            if (cached != -1) {
                cir.setReturnValue(cached);
                return;
            }

            cir.setReturnValue(original);

            if (!polsuApi.tryStartFetch(uuid)) return;

            EXECUTOR.submit(() -> {
                try {
                    polsuApi.fetchPingBlocking(uuid);
                } finally {
                    polsuApi.finishFetch(uuid);
                }
            });
        } else if (Statsify.config.pingProvider == 2) {
            // Urchin
            int cached = Statsify.urchinApi.getCachedPing(uuid);
            if (cached != -1) {
                cir.setReturnValue(cached);
                return;
            }

            cir.setReturnValue(original);

            if (!Statsify.urchinApi.tryStartFetch(uuid)) return;

            EXECUTOR.submit(() -> {
                try {
                    Statsify.urchinApi.fetchPingBlocking(uuid);
                } finally {
                    Statsify.urchinApi.finishFetch(uuid);
                }
            });
        }
    }
}
