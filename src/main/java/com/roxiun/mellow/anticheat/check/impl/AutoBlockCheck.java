package com.roxiun.mellow.anticheat.check.impl;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.anticheat.check.Check;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import com.roxiun.mellow.platform.event.TickEvent;

public class AutoBlockCheck extends Check {

    public AutoBlockCheck() {
        super("AutoBlock", "Checks for attacking while blocking.");
    }

    @Override
    public void onPlayerTick(
        AnticheatManager manager,
        TickEvent.PlayerTickEvent event,
        ACPlayerData data
    ) {
        if (!Mellow.config.autoBlockCheckEnabled) return;
        EntityPlayer player = data.getPlayer();
        if (player == null || player == Minecraft.getMinecraft().thePlayer) {
            // Don't check self
            return;
        }

        long currentTime = System.currentTimeMillis();
        boolean isHoldingSword = isHoldingSword(player);
        boolean isSwinging = data.swingProgress > 0;

        // Add swing to history if swinging and hasn't been detected recently
        if (
            isSwinging &&
            (data.lastSwingDetected == 0 ||
                currentTime - data.lastSwingDetected > 100)
        ) {
            boolean hasBeenBlockingLongEnough =
                data.isBlocking &&
                data.lastBlockStartTime > 0 &&
                (currentTime - data.lastBlockStartTime >= 150);

            data.swingHistory.add(
                new ACPlayerData.SwingData(
                    currentTime,
                    hasBeenBlockingLongEnough
                )
            );
            data.lastSwingDetected = currentTime;

            // Keep only last 20 swings
            if (data.swingHistory.size() > 20) {
                data.swingHistory.remove(0);
            }
        }

        // Update wasBlockingAfter for each swing in history
        for (ACPlayerData.SwingData swing : data.swingHistory) {
            if (swing.wasBlockingAfter == null) {
                long timeSinceSwing = currentTime - swing.time;
                if (timeSinceSwing >= 150 && timeSinceSwing <= 200) {
                    swing.wasBlockingAfter = data.isBlocking;
                } else if (timeSinceSwing > 200) {
                    swing.wasBlockingAfter = false;
                }
            }
        }

        // Get recent swings that have a blocking state after the swing
        java.util.List<ACPlayerData.SwingData> recentSwings =
            new java.util.ArrayList<>();
        for (ACPlayerData.SwingData swing : data.swingHistory) {
            if (
                currentTime - swing.time < 1000 &&
                swing.wasBlockingAfter != null &&
                isHoldingSword
            ) {
                recentSwings.add(swing);
            }
        }

        int autoBlockCount = 0;
        for (ACPlayerData.SwingData swing : recentSwings) {
            boolean wasBlockingBefore = swing.wasBlockingBefore;
            boolean wasBlockingAfter = swing.wasBlockingAfter;

            if (wasBlockingBefore && wasBlockingAfter) {
                autoBlockCount++;
            }
        }

        if (autoBlockCount >= 2) {
            String heldItemName = "nothing";
            if (player.getHeldItem() != null) {
                heldItemName = player.getHeldItem().getDisplayName();
            }

            manager.flag(
                data,
                this,
                "item: " + heldItemName + ", autoblks: " + autoBlockCount,
                1.0
            );
        }
    }

    private boolean isHoldingSword(EntityPlayer player) {
        if (player.getHeldItem() == null) return false;

        int itemId = net.minecraft.item.Item.getIdFromItem(
            player.getHeldItem().getItem()
        );
        // Sword IDs: 267 (iron), 268 (wood), 272 (stone), 276 (diamond), 283 (gold)
        int[] swordIds = { 267, 268, 272, 276, 283 };

        for (int swordId : swordIds) {
            if (itemId == swordId) {
                return true;
            }
        }
        return false;
    }
}
