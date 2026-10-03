package com.roxiun.mellow.anticheat.check.impl;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.anticheat.check.Check;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSword;
import com.roxiun.mellow.platform.event.TickEvent;

public class NoSlowCheck extends Check {

    private long noSlowStartTime = 0;
    private boolean noSlowActive = false;

    public NoSlowCheck() {
        super("NoSlow", "Checks for NoSlowDown hacks.");
    }

    @Override
    public void onPlayerTick(
        AnticheatManager manager,
        TickEvent.PlayerTickEvent event,
        ACPlayerData data
    ) {
        if (!Mellow.config.noSlowCheckEnabled) return;
        EntityPlayer player = data.getPlayer();
        if (
            player == null ||
            event.phase == TickEvent.Phase.END ||
            player == Minecraft.getMinecraft().thePlayer
        ) {
            return;
        }

        boolean isUsingSlowdownItem = isUsingSlowdownItem(player);
        boolean isSprinting = player.isSprinting();

        boolean isCurrentlyNoSlow =
            isUsingSlowdownItem && isSprinting && !player.isRiding();

        if (isCurrentlyNoSlow && !noSlowActive) {
            noSlowStartTime = System.currentTimeMillis();
            noSlowActive = true;
        } else if (!isCurrentlyNoSlow) {
            noSlowActive = false;
            noSlowStartTime = 0;
        }

        if (noSlowActive) {
            long noSlowDuration = System.currentTimeMillis() - noSlowStartTime;
            if (noSlowDuration > 200) {
                // 200ms threshold from reference
                manager.flag(
                    data,
                    this,
                    "duration: " + noSlowDuration + "ms",
                    1.0
                );
                noSlowActive = false;
                noSlowStartTime = 0;
            }
        }
    }

    private boolean isUsingSlowdownItem(EntityPlayer player) {
        if (
            player.isBlocking() &&
            player.getHeldItem() != null &&
            player.getHeldItem().getItem() instanceof ItemSword
        ) {
            return true;
        }
        if (player.isUsingItem() && player.getHeldItem() != null) {
            if (player.getHeldItem().getItem() instanceof ItemFood) {
                return true;
            }
            if (player.getHeldItem().getItem() instanceof ItemBow) {
                return true;
            }
        }
        return false;
    }
}
