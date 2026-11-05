package com.roxiun.mellow.anticheat.check.impl;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.anticheat.check.Check;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.MathHelper;
//? if ornithe {
import com.roxiun.mellow.platform.event.TickEvent;
//?} else {
/*import net.minecraftforge.fml.common.gameevent.TickEvent;
*///?}

public class EagleCheck extends Check {

    public EagleCheck() {
        super("Eagle", "Detects mechanical eagle patterns.");
    }

    private double getRelativeMoveAngle(double dx, double dz, float yaw) {
        double moveAngle = Math.toDegrees(Math.atan2(-dx, dz));
        return MathHelper.wrapAngleTo180_double(moveAngle - yaw);
    }

    @Override
    public void onPlayerTick(
        AnticheatManager manager,
        TickEvent.PlayerTickEvent event,
        ACPlayerData data
    ) {
        if (!Mellow.config.eagleCheckEnabled) return;
        EntityPlayer player = data.getPlayer();
        if (
            player == null ||
            player == Minecraft.getMinecraft().thePlayer ||
            data.lastPosition == null
        ) {
            return;
        }

        long tick = data.currentTick;

        double deltaX = player.posX - data.lastPosition.xCoord;
        double deltaZ = player.posZ - data.lastPosition.zCoord;
        double moveYaw = getRelativeMoveAngle(
            deltaX,
            deltaZ,
            player.rotationYaw
        );

        long crouchDuration = data.lastCrouchEndTick - data.lastCrouchStartTick;

        boolean quickCrouch = crouchDuration >= 1 && crouchDuration <= 2;
        boolean swingOnCrouch =
            data.lastSwingTick >= data.lastCrouchEndTick &&
            data.lastSwingTick <= data.lastCrouchEndTick + 1;
        boolean holdingBlock =
            player.getHeldItem() != null &&
            player.getHeldItem().getItem() instanceof ItemBlock;
        boolean lookingDown = player.rotationPitch >= 70f;
        boolean extremelyLookingDown = player.rotationPitch >= 85f;
        boolean movingBackwards = Math.abs(moveYaw) >= 90f;
        boolean movingDirectlyBackwards = Math.abs(moveYaw) >= 160f;

        boolean flagged = false;

        if (lookingDown && holdingBlock) {
            // Mechanical Pattern Detection
            if (quickCrouch && swingOnCrouch) {
                boolean hasConsistentPattern =
                    data.crouchDurations.size() >= 3 &&
                    data.crouchDurations
                        .stream()
                        .limit(3)
                        .allMatch(d -> d <= 2);

                double consistencyScore = 0.0;
                if (data.crouchDurations.size() >= 3) {
                    double mean = data.crouchDurations
                        .stream()
                        .limit(3)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                    double variance = data.crouchDurations
                        .stream()
                        .limit(3)
                        .mapToDouble(d -> Math.pow(d - mean, 2))
                        .average()
                        .orElse(0.0);
                    consistencyScore = Math.max(
                        0.0,
                        Math.min(1.0, 1.0 - (variance / 4.0))
                    );
                }

                double vlAmount = 2.0;
                double pitchMultiplier = extremelyLookingDown ? 1.5 : 1.0;
                double swingTimingMultiplier = data.lastSwingTick ==
                    data.lastCrouchEndTick
                    ? 1.4
                    : 1.0;
                double movementMultiplier = movingDirectlyBackwards
                    ? 1.8
                    : (movingBackwards ? 1.5 : 1.0);
                double consistencyMultiplier = 1.0 + (consistencyScore * 0.5);

                if (tick - data.lastEaglePatternTick > 15) {
                    data.eaglePatternCount = 0;
                }
                data.eaglePatternCount++;
                data.lastEaglePatternTick = tick;

                if (data.eaglePatternCount >= 2) {
                    data.eagleConsecutiveViolations++;
                    double consecutiveMultiplier =
                        1.0 +
                        (Math.min(data.eagleConsecutiveViolations, 5) * 0.15);
                    double finalVL =
                        vlAmount *
                        pitchMultiplier *
                        swingTimingMultiplier *
                        movementMultiplier *
                        consistencyMultiplier *
                        consecutiveMultiplier;

                    String checkType = movingBackwards
                        ? "backwards-bridging"
                        : (hasConsistentPattern
                              ? "consistent-pattern"
                              : "mechanical-pattern");
                    String patternInfo = hasConsistentPattern
                        ? data.crouchDurations
                              .stream()
                              .limit(3)
                              .map(String::valueOf)
                              .collect(Collectors.joining(",")) +
                          "t"
                        : "n/a";

                    manager.flag(
                        data,
                        this,
                        String.format(
                            "type: %s, angle: %.1f, crouch: %dt, consistency: %.2f",
                            checkType,
                            moveYaw,
                            crouchDuration,
                            consistencyScore
                        ),
                        finalVL
                    );
                    flagged = true;
                    data.eaglePatternCount = 0;
                }
            }
            // Instant Sequence Detection
            else if (
                data.lastSwingTick == tick &&
                data.lastCrouchEndTick >= tick - 1 &&
                data.lastCrouchStartTick == data.lastCrouchEndTick - 1
            ) {
                double pitchMultiplier = extremelyLookingDown
                    ? 1.6
                    : (player.rotationPitch >= 80f ? 1.3 : 1.0);
                double movementMultiplier = movingDirectlyBackwards
                    ? 1.5
                    : (movingBackwards ? 1.2 : 1.0);

                data.eagleConsecutiveViolations++;
                double consecutiveMultiplier =
                    1.0 + (Math.min(data.eagleConsecutiveViolations, 5) * 0.15);
                double vlAmount =
                    3.5 *
                    pitchMultiplier *
                    movementMultiplier *
                    consecutiveMultiplier;

                manager.flag(
                    data,
                    this,
                    String.format(
                        "type: instant-sequence, angle: %.1f, pitch: %.1f",
                        moveYaw,
                        player.rotationPitch
                    ),
                    vlAmount
                );
                flagged = true;
                data.eaglePatternCount = 0;
            }
        }

        if (!flagged) {
            if ((tick - data.lastEaglePatternTick) > 15) {
                data.eaglePatternCount = 0;
            }
            // Simple decay for consecutive violations if not flagged
            if (data.eagleConsecutiveViolations > 0) {
                data.eagleConsecutiveViolations--;
            }
        }
    }
}
