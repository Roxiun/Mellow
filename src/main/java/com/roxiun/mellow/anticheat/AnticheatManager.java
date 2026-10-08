package com.roxiun.mellow.anticheat;

//? if ornithe {
import org.polyfrost.oneconfig.api.hypixel.v1.HypixelUtils;
//?} else {
/*import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
*///?}
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.anticheat.check.Check;
import com.roxiun.mellow.anticheat.check.impl.AutoBlockCheck;
import com.roxiun.mellow.anticheat.check.impl.EagleCheck;
import com.roxiun.mellow.anticheat.check.impl.NoSlowCheck;
import com.roxiun.mellow.anticheat.check.impl.ScaffoldCheck;
import com.roxiun.mellow.anticheat.data.ACPlayerData;
import com.roxiun.mellow.anticheat.event.AnticheatListener;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistCommandResolver;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
//? if ornithe {
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import com.roxiun.mellow.platform.event.TickEvent;
//?} else {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
*///?}

public class AnticheatManager {

    private final Mellow mellow;
    private final Map<UUID, ACPlayerData> playerDataMap =
        new ConcurrentHashMap<>();
    private List<Check> checks;

    public AnticheatManager(Mellow mellow) {
        this.mellow = mellow;
        this.checks = loadChecks();
        registerEvents();
    }

    public void registerPlayer(EntityPlayer player) {
        playerDataMap.put(player.getUniqueID(), new ACPlayerData(player));
    }

    public void unregisterPlayer(EntityPlayer player) {
        if (player != null) {
            playerDataMap.remove(player.getUniqueID());
        }
    }

    public ACPlayerData getPlayerData(EntityPlayer player) {
        if (player == null) return null;
        return playerDataMap.get(player.getUniqueID());
    }

    public void clearPlayers() {
        playerDataMap.clear();
    }

    private void registerEvents() {
        //? if ornithe {
        EventManager.INSTANCE.register(new AnticheatListener(this));
        //?} else {
        /*MinecraftForge.EVENT_BUS.register(new AnticheatListener(this));
        *///?}
    }

    public void reloadChecks() {
        this.checks = loadChecks();
    }

    private List<Check> loadChecks() {
        List<Check> loadedChecks = new ArrayList<>();
        if (Mellow.config.noSlowCheckEnabled) {
            loadedChecks.add(new NoSlowCheck());
        }
        if (Mellow.config.autoBlockCheckEnabled) {
            loadedChecks.add(new AutoBlockCheck());
        }
        if (Mellow.config.eagleCheckEnabled) {
            loadedChecks.add(new EagleCheck());
        }
        if (Mellow.config.scaffoldCheckEnabled) {
            loadedChecks.add(new ScaffoldCheck());
        }
        // Add other checks here as they are implemented
        return loadedChecks;
    }

    public void runChecks(TickEvent.PlayerTickEvent event, ACPlayerData data) {
        for (Check check : checks) {
            check.onPlayerTick(this, event, data);
        }
    }

    public void flag(ACPlayerData data, Check check, String info, double vl) {
        EntityPlayer player = data.getPlayer();
        if (player == null) return;

        ACPlayerData.CheckData checkData = data.checkDataMap.computeIfAbsent(
            check.getName(),
            k -> new ACPlayerData.CheckData()
        );
        checkData.violations += vl;

        int threshold = Mellow.config.anticheatVl;
        long cooldown = Mellow.config.anticheatCooldown * 1000L;

        if (checkData.violations >= threshold) {
            long timeSinceLastAlert =
                System.currentTimeMillis() - checkData.lastAlertTime;
            if (timeSinceLastAlert >= cooldown) {
                // Main message component
                IChatComponent mainMessage = new ChatComponentText(
                    buildAlertMessage(player, check, info, checkData.violations)
                );

                // Add WDR button if on Hypixel
                //? if ornithe {
                if (HypixelUtils.isHypixel()) {
                //?} else {
                /*if (HypixelUtils.INSTANCE.isHypixel()) {
                *///?}
                    String plainName = player.getName().replaceAll("§.", "").trim();
                    boolean shouldBlockOnClick =
                        Mellow.nickUtils != null && Mellow.nickUtils.isNicked(plainName);

                    IChatComponent reportButton = new ChatComponentText(
                        " §8[§cWDR§8]"
                    );
                    ChatStyle style = new ChatStyle();
                    style.setChatClickEvent(
                        new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            shouldBlockOnClick
                                ? "/block add " + plainName
                                : "/wdr " + plainName
                        )
                    );
                    style.setChatHoverEvent(
                        new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(
                                shouldBlockOnClick
                                    ? "Click to block " + plainName + " (nicked)"
                                    : "Click to report " + player.getName()
                            )
                        )
                    );
                    reportButton.setChatStyle(style);
                    mainMessage.appendSibling(reportButton);
                }

                appendBlacklistButton(mainMessage, player, check);

                ChatUtils.sendMessage(mainMessage);

                checkData.lastAlertTime = System.currentTimeMillis();
            }
        }
    }

    private String buildAlertMessage(
        EntityPlayer player,
        Check check,
        String info,
        double violations
    ) {
        String formattedName = FormattingUtils.formatNickedPlayerName(
            player.getName()
        );

        if (!Mellow.config.anticheatVerbose) {
            return String.format(
                "§8[§cAC§8] §7%s §ffailed §c%s",
                formattedName,
                check.getName()
            );
        }

        return String.format(
            "§8[§cAC§8] §7%s §ffailed §c%s §7(%s) §c[VL: %.1f]",
            formattedName,
            check.getName(),
            info,
            violations
        );
    }

    private void appendBlacklistButton(
        IChatComponent message,
        EntityPlayer player,
        Check check
    ) {
        if (Mellow.blacklistManager == null || player == null) {
            return;
        }

        if (Mellow.blacklistManager.isBlacklisted(player.getUniqueID())) {
            message.appendSibling(new ChatComponentText(" §8[§4LIST§8]"));
            return;
        }

        String plainName = player.getName().replaceAll("§.", "").trim();
        boolean shouldBlockOnClick =
            Mellow.nickUtils != null && Mellow.nickUtils.isNicked(plainName);
        String reason = "Anticheat flag: " + check.getName();
        String command = shouldBlockOnClick
            ? "/block add " + plainName
            : BlacklistCommandResolver.getCommandPrefix() +
            " add " +
            plainName +
            " " +
            reason;

        IChatComponent blacklistButton = new ChatComponentText(" §8[§cBL§8]");
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        );
        style.setChatHoverEvent(
            new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(
                    shouldBlockOnClick
                        ? "Click to block " + plainName + " (nicked)"
                        : "Click to add " + plainName + " to your local blacklist"
                )
            )
        );
        blacklistButton.setChatStyle(style);
        message.appendSibling(blacklistButton);
    }
}
