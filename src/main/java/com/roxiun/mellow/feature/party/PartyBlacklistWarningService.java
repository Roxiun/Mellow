package com.roxiun.mellow.feature.party;

import com.roxiun.mellow.util.cache.LookupTracker;
import com.roxiun.mellow.util.formatting.FormattingUtils;
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
    private final Map<UUID, TagReport> reportsByMember = new HashMap<>();
    private final Map<UUID, Set<String>> warned = new HashMap<>();
    private final AlertSoundGate sound = new AlertSoundGate();
    private Set<UUID> members = Collections.emptySet();
    private long lastLocalCheck;
    private final LookupTracker<UUID> lookups = new LookupTracker<>();
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
        if (snapshot == null || !snapshot.isOnHypixel()) reset();
        if (!settings.equals(nextSettings)) {
            settings = nextSettings;
            lookups.clear();
            reportsByMember.clear();
        }
        members = current;
        if (current.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastLocalCheck >= 1000) {
            lastLocalCheck = now;
            for (UUID uuid : current) {
                BlacklistedPlayer local = blacklist.getBlacklistedPlayer(uuid);
                if (local != null) warn(uuid, Collections.singletonMap("Local", local.getReason()));
            }
        }
        if (!config.isCoralEnabled() && !config.xadia) return;
        Map<UUID, LookupTracker.Attempt> attempts = new LinkedHashMap<>();
        Set<String> uuids = new LinkedHashSet<>();
        for (UUID uuid : current) {
            if (ignored != null && ignored.isTagIgnored(uuid)) continue;
            TagReport saved = reportsByMember.get(uuid);
            if (saved != null) warn(uuid, TagPolicy.warnings(saved, true, false));
            LookupTracker.Attempt attempt = lookups.begin(uuid);
            if (attempt != null) { attempts.put(uuid, attempt); uuids.add(uuid.toString()); }
        }
        if (uuids.isEmpty()) return;
        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                Map<String, TagReport> reports = players.fetchTagReports(uuids);
                MainThreadDispatcher.run(() -> {
                    for (Map.Entry<UUID, LookupTracker.Attempt> entry : attempts.entrySet()) {
                        UUID uuid = entry.getKey();
                        TagReport report = reports.get(uuid.toString().replace("-", ""));
                        if (!lookups.finish(uuid, entry.getValue(), report != null && report.getFailures().isEmpty())) continue;
                        if (report != null) reportsByMember.put(uuid, report);
                        if (report != null && members.contains(uuid) && config.partyBlacklistWarning)
                            warn(uuid, TagPolicy.warnings(report, true, ignored != null && ignored.isTagIgnored(uuid)));
                    }
                });
            } catch (RuntimeException error) {
                MainThreadDispatcher.run(() -> attempts.forEach((uuid, attempt) -> lookups.finish(uuid, attempt, false)));
            }
        });
    }

    /** Only disconnect/manual refresh starts a new connection lookup lifetime. */
    public void reset() {
        lookups.clear(); warned.clear(); reportsByMember.clear(); members = Collections.emptySet();
        lastLocalCheck = 0; sound.reset();
    }

    private void warn(UUID uuid, Map<String, String> sources) {
        Set<String> seen = warned.computeIfAbsent(uuid, id -> new HashSet<>());
        Map<String, String> fresh = new LinkedHashMap<>();
        for (Map.Entry<String, String> source : sources.entrySet())
            if (seen.add(source.getKey())) fresh.put(source.getKey(), source.getValue());
        if (fresh.isEmpty()) return;
        String name = displayName(uuid);
        ChatUtils.sendMessage("§cWarning: flagged party member detected: " + name + " §7["
            + fresh.keySet().stream().map(source -> FormattingUtils.formatTagSource(source, false))
                .collect(java.util.stream.Collectors.joining("§7, ")) + "§7]. Consider leaving to avoid risk.");
        if (config.partyBlacklistWarningShowTagDetails) for (Map.Entry<String, String> source : fresh.entrySet())
            ChatUtils.sendMessage("§7- " + name + " " + FormattingUtils.formatTagSource(source.getKey(), false) + "§7: "
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
