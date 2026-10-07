package com.roxiun.mellow.mixin;

import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiPlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    @Accessor("header") IChatComponent mellow$getHeader();
    @Accessor("footer") IChatComponent mellow$getFooter();
    @Invoker("drawScoreboardValues")
    void mellow$drawScoreboardValues(ScoreObjective objective, int y, String name,
                                    int left, int right, NetworkPlayerInfo player);
}
