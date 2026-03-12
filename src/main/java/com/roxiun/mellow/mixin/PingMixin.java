package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.ping.PingProviderUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetworkPlayerInfo.class)
public class PingMixin {

    @Shadow
    private int responseTime;

    @Inject(method = "getResponseTime", at = @At("HEAD"), cancellable = true)
    private void onGetResponseTime(CallbackInfoReturnable<Integer> cir) {
        if (!Mellow.isEnabled()) {
            return;
        }

        int original = this.responseTime;
        NetworkPlayerInfo info = (NetworkPlayerInfo) (Object) this;

        if (
            info == null ||
            info.getGameProfile() == null ||
            info.getGameProfile().getId() == null ||
            PlayerUtils.isObfuscatedTabEntry(info)
        ) {
            cir.setReturnValue(original);
            return;
        }

        String playerName = info.getGameProfile().getName();
        if (Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName)) {
            cir.setReturnValue(original);
            return;
        }

        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        if (
            Mellow.config == null ||
            !PingProviderUtils.canUseExternalPing(snapshot)
        ) {
            cir.setReturnValue(original);
            return;
        }

        String uuid = info.getGameProfile().getId().toString();
        boolean hasValidVanillaPing = original > 1 && original < 999;

        boolean useLuna = PingProviderUtils.shouldUseLuna(Mellow.config);
        boolean useAurora = PingProviderUtils.shouldUseAurora(Mellow.config);
        boolean useSeraph = PingProviderUtils.shouldUseSeraph(Mellow.config);

        if (!useAurora && !useLuna && !useSeraph) {
            cir.setReturnValue(original);
            return;
        }

        if (useAurora) {
            if (
                Mellow.auroraPingService == null ||
                !PingProviderUtils.hasAuroraApiKey(Mellow.config)
            ) {
                cir.setReturnValue(original);
                return;
            }

            String compactUuid = uuid.replace("-", "");
            int cached = Mellow.auroraPingService.getCachedPing(compactUuid);
            if (cached >= 0 && !hasValidVanillaPing) {
                cir.setReturnValue(cached);
                return;
            }

            cir.setReturnValue(original);
            if (cached < 0 && !hasValidVanillaPing) {
                Mellow.auroraPingService.fetchAsync(
                    compactUuid,
                    Mellow.config.auroraApiKey
                );
            }
            return;
        }

        if (useSeraph) {
            if (
                Mellow.seraphPingService == null ||
                !PingProviderUtils.hasSeraphApiKey(Mellow.config)
            ) {
                cir.setReturnValue(original);
                return;
            }

            int cached = Mellow.seraphPingService.getCachedPing(uuid);
            if (cached >= 0 && !hasValidVanillaPing) {
                cir.setReturnValue(cached);
                return;
            }

            cir.setReturnValue(original);
            if (cached < 0 && !hasValidVanillaPing) {
                Mellow.seraphPingService.fetchAsync(uuid, Mellow.config.seraphKey);
            }
            return;
        }

        if (
            Mellow.lunaPingService == null ||
            !PingProviderUtils.hasLunaApiKey(Mellow.config)
        ) {
            cir.setReturnValue(original);
            return;
        }

        int cached = Mellow.lunaPingService.getCachedPing(uuid);
        if (cached >= 0 && !hasValidVanillaPing) {
            cir.setReturnValue(cached);
            return;
        }

        cir.setReturnValue(original);
        if (cached < 0 && !hasValidVanillaPing) {
            Mellow.lunaPingService.fetchAsync(uuid, Mellow.config.lunaPingApiKey);
        }
    }
}
