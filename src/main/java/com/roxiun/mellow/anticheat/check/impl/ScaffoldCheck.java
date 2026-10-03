package com.roxiun.mellow.anticheat.check.impl;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.anticheat.check.Check;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.MathHelper;
import com.roxiun.mellow.platform.event.TickEvent;

public class ScaffoldCheck extends Check {

    public ScaffoldCheck() {
        super("Scaffold", "Detects illegal bridging patterns.");
    }

    private double getMoveLookAngleDiff(double dx, double dz, float yaw) {
        double moveAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double lookAngle = MathHelper.wrapAngleTo180_float(yaw);
        double diff = (moveAngle - lookAngle + 360.0) % 360.0;
        if (diff > 180.0) {
            diff -= 360.0;
        }
        return diff;
    }

    private boolean isAlmostZero(double d) {
        return Math.abs(d) < 0.001;
    }

    @Override
    public void onPlayerTick(
        AnticheatManager manager,
        TickEvent.PlayerTickEvent event,
        ACPlayerData data
    ) {
        if (!Mellow.config.scaffoldCheckEnabled) return;
        EntityPlayer player = data.getPlayer();
        if (
            player == null ||
            player == Minecraft.getMinecraft().thePlayer ||
            player.isRiding()
        ) {
            return;
        }

        List<ACPlayerData.PositionSample> samples = data.positionHistory;
        if (samples.size() < 5) {
            // Need at least 5 samples for acceleration
            return;
        }

        ACPlayerData.PositionSample currentSample = samples.get(
            samples.size() - 1
        );
        ACPlayerData.PositionSample prevSample1 = samples.get(
            samples.size() - 2
        );
        ACPlayerData.PositionSample prevSample2 = samples.get(
            samples.size() - 3
        );
        ACPlayerData.PositionSample prevSample3 = samples.get(
            samples.size() - 4
        );
        ACPlayerData.PositionSample prevSample4 = samples.get(
            samples.size() - 5
        );

        long tick = data.currentTick;
        double pitch = player.rotationPitch;

        double dx = (currentSample.pos.xCoord - prevSample1.pos.xCoord) * 20.0;
        double dz = (currentSample.pos.zCoord - prevSample1.pos.zCoord) * 20.0;
        double speedXZSq = dx * dx + dz * dz;
        double speedXZ = Math.sqrt(speedXZSq);

        // Calculate speed and acceleration over a few ticks to smooth out noise
        double speedY =
            (prevSample1.pos.yCoord - prevSample2.pos.yCoord) * 20.0;
        double avgAccelY =
            50.0 *
            (prevSample1.pos.yCoord -
                prevSample2.pos.yCoord -
                (prevSample3.pos.yCoord - prevSample4.pos.yCoord));

        double angleDiff = getMoveLookAngleDiff(
            dx / 20.0,
            dz / 20.0,
            player.rotationYaw
        );

        boolean flagged = false;
        String checkType = "";

        if (
            player.isSwingInProgress &&
            player.hurtTime == 0 &&
            pitch > 50.0 &&
            speedXZSq > 9.0 &&
            player.getHeldItem() != null &&
            player.getHeldItem().getItem() instanceof ItemBlock &&
            Math.abs(angleDiff) > 165.0 &&
            speedXZSq < 100.0 &&
            !isAlmostZero(avgAccelY)
        ) {
            double pitchFactor = Math.max(
                0.0,
                Math.min(1.0, (pitch - 50.0) / 40.0)
            );
            double angleFactor = Math.max(
                0.0,
                Math.min(1.0, (Math.abs(angleDiff) - 165.0) / 15.0)
            );

            double baseVL = 0.0;

            if (speedY >= 4.0 && speedY <= 15.0 && avgAccelY > -25.0) {
                checkType = "tower";
                double ySpeedFactor = Math.max(
                    0.0,
                    Math.min(1.0, (speedY - 4.0) / 11.0)
                );
                double accelFactor = Math.max(
                    0.0,
                    Math.min(1.0, (avgAccelY + 25.0) / 25.0)
                );
                double severity = (pitchFactor * 0.3 +
                    angleFactor * 0.3 +
                    ySpeedFactor * 0.3 +
                    accelFactor * 0.1);
                baseVL = 3.0 + (severity * 3.0);
            } else if (
                speedY >= -1.0 &&
                speedY <= 4.0 &&
                Math.abs(speedY) > 0.005 &&
                speedXZSq > 25.0
            ) {
                checkType = "horizontal";
                double hSpeedFactor = Math.max(
                    0.0,
                    Math.min(1.0, (speedXZ - 5.0) / 5.0)
                );
                double severity = (pitchFactor * 0.3 +
                    angleFactor * 0.3 +
                    hSpeedFactor * 0.4);
                baseVL = 3.0 + (severity * 3.0);
            }

            if (!checkType.isEmpty()) {
                int consecutive = 0;
                if (
                    checkType.equals(data.lastScaffoldViolationType) &&
                    (tick - data.lastScaffoldViolationTime) < 40
                ) {
                    consecutive = data.scaffoldConsecutiveViolations + 1;
                }
                data.scaffoldConsecutiveViolations = consecutive;
                data.lastScaffoldViolationType = checkType;
                data.lastScaffoldViolationTime = tick;

                double consecutiveMultiplier =
                    1.0 + (Math.min(consecutive, 5) * 0.2);
                double finalVL = baseVL * consecutiveMultiplier;

                manager.flag(
                    data,
                    this,
                    String.format(
                        "type: %s, angle: %.1f, speed: %.2f, accelY: %.2f",
                        checkType,
                        angleDiff,
                        speedXZ,
                        avgAccelY
                    ),
                    finalVL
                );
                flagged = true;
            }
        }

        if (!flagged) {
            if (tick - data.lastScaffoldViolationTime > 60) {
                data.scaffoldConsecutiveViolations = 0;
            }
        }
    }
}
