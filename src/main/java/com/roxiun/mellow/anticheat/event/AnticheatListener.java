package com.roxiun.mellow.anticheat.event;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AnticheatListener {

    private final AnticheatManager manager;
    private long currentTick = 0;

    public AnticheatListener(AnticheatManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            currentTick++;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!Mellow.isEnabled() || !Mellow.config.anticheatEnabled) return;

        if (event.phase == TickEvent.Phase.START) {
            ACPlayerData data = manager.getPlayerData(event.player);
            if (data == null) {
                // Player might have just joined, or we reloaded.
                manager.registerPlayer(event.player);
                data = manager.getPlayerData(event.player);
            }

            if (data != null) {
                // Update tick
                data.currentTick = this.currentTick;

                // Update positions
                data.lastPosition = new Vec3(
                    event.player.prevPosX,
                    event.player.prevPosY,
                    event.player.prevPosZ
                );
                data.updatePosition(
                    event.player.posX,
                    event.player.posY,
                    event.player.posZ
                );

                // Update swing progress
                data.lastSwingProgress = data.swingProgress;
                data.swingProgress = event.player.swingProgress;

                // Update general player state
                data.wasCrouching = data.isCrouching;
                data.isSprinting = event.player.isSprinting();
                data.isCrouching = event.player.isSneaking();
                data.isUsingItem = event.player.isUsingItem();
                data.isOnGround = event.player.onGround;
                data.isBlocking = event.player.isBlocking();

                // Update blocking state
                if (data.isBlocking && !data.wasBlocking) {
                    data.lastBlockStartTime = System.currentTimeMillis();
                }
                data.wasBlocking = data.isBlocking;

                // Update swinging state
                if (event.player.isSwingInProgress && !data.wasSwinging) {
                    data.lastSwingTime = System.currentTimeMillis();
                    data.lastSwingTick = data.currentTick;
                }
                data.wasSwinging = event.player.isSwingInProgress;

                // Update item use / block place state
                if (
                    data.isUsingItem &&
                    !data.wasUsingItem &&
                    data.isHoldingBlock()
                ) {
                    data.lastBlockPlaceTime = System.currentTimeMillis();
                }
                data.wasUsingItem = data.isUsingItem;

                // Update crouching state for Eagle check
                if (data.isCrouching && !data.wasCrouching) {
                    data.lastCrouchStartTick = data.currentTick;
                } else if (!data.isCrouching && data.wasCrouching) {
                    data.lastCrouchEndTick = data.currentTick;
                    if (data.lastCrouchStartTick > 0) {
                        int crouchDuration = (int) (data.currentTick -
                            data.lastCrouchStartTick);
                        data.crouchDurations.add(0, crouchDuration);
                        if (data.crouchDurations.size() > 10) {
                            data.crouchDurations.remove(
                                data.crouchDurations.size() - 1
                            );
                        }
                    }
                }

                // Run checks
                manager.runChecks(event, data);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!Mellow.isEnabled() || !Mellow.config.anticheatEnabled) return;
        if (event.entity instanceof EntityPlayer) {
            manager.registerPlayer((EntityPlayer) event.entity);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            manager.clearPlayers();
        }
    }
}
