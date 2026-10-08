package com.roxiun.mellow.feature.party;

import com.roxiun.mellow.api.tags.TagReport;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.feature.alerts.AlertSoundGate;
import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

/** Party membership has connection lifetime, independent of the current match. Client-thread owner. */
public final class PartyBlacklistWarningService {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final BlacklistManager blacklist;
    private final MellowOneConfig config;
    private final PlayerCache players;
    private final TagIgnoreManager ignored;
    private final Map<UUID, Set<String>> warned = new HashMap<>();
    private final AlertSoundGate sound = new AlertSoundGate();
    private Set<UUID> members = Collections.emptySet();
    private long generation, nextCheck, lastLocalCheck;
    private boolean fetching;
    private String settings = "";

    public PartyBlacklistWarningService(BlacklistManager blacklist, MellowOneConfig config,
        PlayerCache players, TagIgnoreManager ignored) {
        this.blacklist = blacklist; this.config = config; this.players = players; this.ignored = ignored;
    }

    public void onSnapshotUpdate(GameSnapshot snapshot) {
        Set<UUID> current = new LinkedHashSet<>();
        if (config.partyBlacklistWarning && snapshot != null && snapshot.isOnHypixel())
            current.addAll(snapshot.getPartyState().getMembers().keySet());
        if (mc.getSession() != null && mc.getSession().getProfile() != null)
            current.remove(mc.getSession().getProfile().getId());
        String nextSettings = config.isCoralEnabled() + "|" + config.getCoralApiKey() + "|" + config.xadia
            + "|" + config.xadiaKey + "|" + config.xadiaVerifiedOnly;
        if (!members.equals(current) || !settings.equals(nextSettings)) {
            members = current;
            settings = nextSettings;
            warned.keySet().retainAll(current);
            generation++;
            fetching = false;
            nextCheck = lastLocalCheck = 0;
            if (current.isEmpty()) sound.reset();
        }
        if (current.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastLocalCheck >= 1000) {
            lastLocalCheck = now;
            for (UUID uuid : current) {
                BlacklistedPlayer local = blacklist.getBlacklistedPlayer(uuid);
                if (local != null) warn(uuid, Collections.singletonMap("Local", local.getReason()));
            }
        }
        if (fetching || now < nextCheck || (!config.isCoralEnabled() && !config.xadia)) return;
        Set<String> uuids = new LinkedHashSet<>();
        for (UUID uuid : current) if (ignored == null || !ignored.isTagIgnored(uuid)) uuids.add(uuid.toString());
        nextCheck = now + 120_000L;
        if (uuids.isEmpty()) return;
        fetching = true;
        long requestGeneration = generation;
        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                Map<String, TagReport> reports = players.fetchTagReports(uuids);
                MainThreadDispatcher.run(() -> {
                    if (generation != requestGeneration || !config.partyBlacklistWarning) return;
                    for (UUID uuid : current) {
                        TagReport report = reports.get(uuid.toString().replace("-", ""));
                        if (report == null) continue;
                        if (!report.getFailures().isEmpty()) nextCheck = System.currentTimeMillis() + 10_000L;
                        warn(uuid, TagPolicy.warnings(report, true, ignored != null && ignored.isTagIgnored(uuid)));
                    }
                });
            } catch (RuntimeException error) {
                MainThreadDispatcher.run(() -> { if (generation == requestGeneration) nextCheck = System.currentTimeMillis() + 10_000L; });
                throw error;
            } finally {
                MainThreadDispatcher.run(() -> { if (generation == requestGeneration) fetching = false; });
            }
        });
    }

    private void warn(UUID uuid, Map<String, String> sources) {
        Set<String> seen = warned.computeIfAbsent(uuid, id -> new HashSet<>());
        Map<String, String> fresh = new LinkedHashMap<>();
        for (Map.Entry<String, String> source : sources.entrySet())
            if (seen.add(source.getKey())) fresh.put(source.getKey(), source.getValue());
        if (fresh.isEmpty()) return;
        String name = displayName(uuid);
        ChatUtils.sendMessage("§cWarning: flagged party member detected: " + name + " §7[§d"
            + String.join("§7, §d", fresh.keySet()) + "§7]. Consider leaving to avoid risk.");
        if (config.partyBlacklistWarningShowTagDetails) for (Map.Entry<String, String> source : fresh.entrySet())
            ChatUtils.sendMessage("§7- " + name + " §d" + source.getKey() + "§7: "
                + (source.getValue() == null ? "(none)" : source.getValue()));
        sound.tryPlayPling(mc, 1.0F, 0.8F);
    }

    private String displayName(UUID uuid) {
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(uuid);
            if (info != null && info.getGameProfile() != null) return info.getGameProfile().getName();
        }
        BlacklistedPlayer local = blacklist.getBlacklistedPlayer(uuid);
        return local != null && local.getName() != null ? local.getName() : uuid.toString();
    }
}
