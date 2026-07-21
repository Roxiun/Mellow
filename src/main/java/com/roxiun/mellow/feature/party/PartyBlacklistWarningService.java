package com.roxiun.mellow.feature.party;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.feature.alerts.AlertSoundGate;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PartyBlacklistWarningService {

    private enum FlagSource {
        LOCAL("§cLocal"),
        URCHIN("§5Urchin"),
        SERAPH("§3Seraph");

        private final String coloredLabel;

        FlagSource(String coloredLabel) {
            this.coloredLabel = coloredLabel;
        }
    }

    private static final class MemberFlagDetection {

        private final EnumSet<FlagSource> sources = EnumSet.noneOf(
            FlagSource.class
        );
        private final Map<FlagSource, String> detailsBySource = new HashMap<>();

        private void addSource(FlagSource source) {
            sources.add(source);
        }

        private void setDetail(FlagSource source, String detail) {
            if (detail == null || detail.trim().isEmpty()) {
                detailsBySource.remove(source);
                return;
            }
            detailsBySource.put(source, detail);
        }

        private String getDetail(FlagSource source) {
            return detailsBySource.get(source);
        }

        private MemberFlagDetection copy() {
            MemberFlagDetection copy = new MemberFlagDetection();
            copy.sources.addAll(sources);
            copy.detailsBySource.putAll(detailsBySource);
            return copy;
        }
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final BlacklistManager blacklistManager;
    private final MellowOneConfig config;
    private final PlayerCache playerCache;
    private final TagIgnoreManager tagIgnoreManager;
    private final Map<UUID, EnumSet<FlagSource>> warnedSourcesByMember =
        new HashMap<>();
    private final AlertSoundGate partyWarningSoundGate = new AlertSoundGate();
    private long evaluationVersion;

    public PartyBlacklistWarningService(
        BlacklistManager blacklistManager,
        MellowOneConfig config,
        PlayerCache playerCache,
        TagIgnoreManager tagIgnoreManager
    ) {
        this.blacklistManager = blacklistManager;
        this.config = config;
        this.playerCache = playerCache;
        this.tagIgnoreManager = tagIgnoreManager;
    }

    public synchronized void onSnapshotUpdate(GameSnapshot snapshot) {
        if (!Mellow.isEnabled() || config == null || !config.partyBlacklistWarning) {
            resetState();
            return;
        }

        if (snapshot == null || !snapshot.isOnHypixel()) {
            resetState();
            return;
        }

        PartyState partyState = snapshot.getPartyState();
        if (
            partyState == null ||
            !partyState.isInParty() ||
            partyState.getMembers().isEmpty()
        ) {
            resetState();
            return;
        }

        UUID selfUuid = getSelfUuid();
        Set<UUID> partyMemberUuids = new LinkedHashSet<>();
        Map<UUID, String> tabNamesByUuid = new LinkedHashMap<>();
        Map<UUID, MemberFlagDetection> localFlags = new LinkedHashMap<>();

        for (UUID memberUuid : partyState.getMembers().keySet()) {
            if (memberUuid == null) {
                continue;
            }
            if (selfUuid != null && selfUuid.equals(memberUuid)) {
                continue;
            }

            partyMemberUuids.add(memberUuid);
            String tabName = findTabName(memberUuid);
            if (tabName != null && !tabName.isEmpty()) {
                tabNamesByUuid.put(memberUuid, tabName);
            }

            if (blacklistManager.isBlacklisted(memberUuid)) {
                MemberFlagDetection localSource = new MemberFlagDetection();
                localSource.addSource(FlagSource.LOCAL);
                localSource.setDetail(
                    FlagSource.LOCAL,
                    resolveLocalBlacklistReason(memberUuid)
                );
                localFlags.put(memberUuid, localSource);
            }
        }

        if (partyMemberUuids.isEmpty()) {
            resetState();
            return;
        }

        boolean shouldCheckUrchin = config.urchin;
        boolean shouldCheckSeraph = config.seraph;
        long evaluationId = ++evaluationVersion;

        if ((!shouldCheckUrchin && !shouldCheckSeraph) || playerCache == null) {
            applyDetectionResult(evaluationId, localFlags);
            return;
        }

        AsyncExecutor.getInstance().profileIo(() -> {
            Map<UUID, MemberFlagDetection> combined = cloneDetectionMap(
                localFlags
            );

            for (UUID memberUuid : partyMemberUuids) {
                boolean tagsIgnored =
                    tagIgnoreManager != null &&
                    tagIgnoreManager.isTagIgnored(memberUuid);
                if (tagsIgnored) {
                    continue;
                }

                String tabName = tabNamesByUuid.get(memberUuid);
                if (tabName == null || tabName.isEmpty()) {
                    continue;
                }

                PlayerProfile profile = playerCache.getProfile(tabName);
                if (profile == null) {
                    continue;
                }

                MemberFlagDetection detection = combined.get(memberUuid);
                if (detection == null) {
                    detection = new MemberFlagDetection();
                }

                if (shouldCheckUrchin && profile.isUrchinTagged()) {
                    detection.addSource(FlagSource.URCHIN);
                    detection.setDetail(
                        FlagSource.URCHIN,
                        formatUrchinTagDetails(profile)
                    );
                }
                if (shouldCheckSeraph && profile.isSeraphTagged()) {
                    detection.addSource(FlagSource.SERAPH);
                    detection.setDetail(
                        FlagSource.SERAPH,
                        formatSeraphTagDetails(profile)
                    );
                }

                if (detection.sources.isEmpty()) {
                    combined.remove(memberUuid);
                } else {
                    combined.put(memberUuid, detection);
                }
            }

            MainThreadDispatcher.run(() ->
                applyDetectionResult(evaluationId, combined)
            );
        });
    }

    private synchronized void resetState() {
        evaluationVersion++;
        warnedSourcesByMember.clear();
        partyWarningSoundGate.reset();
    }

    private synchronized void applyDetectionResult(
        long evaluationId,
        Map<UUID, MemberFlagDetection> detectedByMember
    ) {
        if (evaluationId != evaluationVersion) {
            return;
        }

        warnedSourcesByMember.keySet().retainAll(detectedByMember.keySet());
        Map<UUID, MemberFlagDetection> changed = new LinkedHashMap<>();

        for (Map.Entry<UUID, MemberFlagDetection> entry : detectedByMember.entrySet()) {
            UUID memberUuid = entry.getKey();
            EnumSet<FlagSource> current = EnumSet.copyOf(entry.getValue().sources);
            EnumSet<FlagSource> previous = warnedSourcesByMember.get(memberUuid);

            if (previous == null || !previous.equals(current)) {
                changed.put(memberUuid, entry.getValue().copy());
            }

            warnedSourcesByMember.put(memberUuid, current);
        }

        if (changed.isEmpty()) {
            return;
        }

        sendWarning(changed);
    }

    private Map<UUID, MemberFlagDetection> cloneDetectionMap(
        Map<UUID, MemberFlagDetection> source
    ) {
        Map<UUID, MemberFlagDetection> copy = new LinkedHashMap<>();
        for (Map.Entry<UUID, MemberFlagDetection> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    private void sendWarning(Map<UUID, MemberFlagDetection> flaggedMembers) {
        MainThreadDispatcher.run(() -> {
            List<String> parts = new ArrayList<>(flaggedMembers.size());
            for (Map.Entry<UUID, MemberFlagDetection> entry : flaggedMembers.entrySet()) {
                String displayName = resolveDisplayName(entry.getKey());
                String labels = formatSourceLabels(entry.getValue().sources);
                parts.add(displayName + " §7[" + labels + "§7]");
            }

            String names = String.join("§7, ", parts);
            String noun = flaggedMembers.size() == 1 ? "member" : "members";

            ChatUtils.sendMessage(
                "§cWarning: flagged party " +
                noun +
                " detected: §c" +
                names +
                "§7. Consider leaving to avoid risk."
            );

            if (config.partyBlacklistWarningShowTagDetails) {
                sendDetailLines(flaggedMembers);
            }

            partyWarningSoundGate.tryPlayPling(mc, 1.0F, 0.8F);
        });
    }

    private void sendDetailLines(Map<UUID, MemberFlagDetection> flaggedMembers) {
        for (Map.Entry<UUID, MemberFlagDetection> entry : flaggedMembers.entrySet()) {
            String displayName = resolveDisplayName(entry.getKey());
            String details = formatTaggedForDetails(entry.getValue());
            ChatUtils.sendMessage(
                "§7- " + displayName + " §7tagged for: " + details
            );
        }
    }

    private String formatTaggedForDetails(MemberFlagDetection detection) {
        List<String> sourceDetails = new ArrayList<>(3);
        if (detection.sources.contains(FlagSource.LOCAL)) {
            String localDetail = detection.getDetail(FlagSource.LOCAL);
            if (BlacklistManager.isExternalFileImportReason(localDetail)) {
                sourceDetails.add(FlagSource.LOCAL.coloredLabel);
            } else {
                sourceDetails.add(
                    FlagSource.LOCAL.coloredLabel +
                    "§7: " +
                    formatDetailWithFallback(localDetail)
                );
            }
        }
        if (detection.sources.contains(FlagSource.URCHIN)) {
            sourceDetails.add(
                FlagSource.URCHIN.coloredLabel +
                "§7: " +
                formatDetailWithFallback(detection.getDetail(FlagSource.URCHIN))
            );
        }
        if (detection.sources.contains(FlagSource.SERAPH)) {
            sourceDetails.add(
                FlagSource.SERAPH.coloredLabel +
                "§7: " +
                formatDetailWithFallback(detection.getDetail(FlagSource.SERAPH))
            );
        }
        return String.join(" §7| ", sourceDetails);
    }

    private String formatDetailWithFallback(String detail) {
        String normalized = normalizeDetailText(detail);
        if (normalized.isEmpty()) {
            return "§7Unknown reason";
        }
        return normalized;
    }

    private String resolveLocalBlacklistReason(UUID memberUuid) {
        BlacklistedPlayer blacklistedPlayer = blacklistManager.getBlacklistedPlayer(
            memberUuid
        );
        if (blacklistedPlayer == null) {
            return null;
        }
        return normalizeDetailText(blacklistedPlayer.getReason());
    }

    private String formatUrchinTagDetails(PlayerProfile profile) {
        return normalizeDetailText(
            FormattingUtils.formatUrchinTags(profile.getUrchinTags())
        );
    }

    private String formatSeraphTagDetails(PlayerProfile profile) {
        String formatted = FormattingUtils.formatSeraphTags(profile.getSeraphTags());
        if (formatted == null || formatted.trim().isEmpty()) {
            return "";
        }

        String[] lines = formatted.split("\n§c");
        List<String> lineParts = new ArrayList<>(lines.length);
        for (String line : lines) {
            String normalized = normalizeDetailText(line);
            if (!normalized.isEmpty()) {
                lineParts.add(normalized);
            }
        }

        return String.join("§7, ", lineParts);
    }

    private String normalizeDetailText(String detail) {
        if (detail == null) {
            return "";
        }

        return detail
            .replace("\r", "")
            .replace("\n", "§7, ")
            .replace("(null)", "(Unknown reason)")
            .trim();
    }

    private String formatSourceLabels(EnumSet<FlagSource> sources) {
        List<String> labels = new ArrayList<>(3);
        if (sources.contains(FlagSource.LOCAL)) {
            labels.add(FlagSource.LOCAL.coloredLabel);
        }
        if (sources.contains(FlagSource.URCHIN)) {
            labels.add(FlagSource.URCHIN.coloredLabel);
        }
        if (sources.contains(FlagSource.SERAPH)) {
            labels.add(FlagSource.SERAPH.coloredLabel);
        }
        return String.join("§7, ", labels);
    }

    private String resolveDisplayName(UUID uuid) {
        String tabName = findTabName(uuid);
        if (tabName != null && !tabName.isEmpty()) {
            if (mc.theWorld != null) {
                return PlayerUtils.getTabDisplayName(tabName);
            }
            return tabName;
        }

        BlacklistedPlayer blacklistedPlayer = blacklistManager.getBlacklistedPlayer(
            uuid
        );
        if (blacklistedPlayer != null) {
            String storedName = blacklistedPlayer.getName();
            if (
                storedName != null &&
                !storedName.trim().isEmpty() &&
                !isUuidLike(storedName)
            ) {
                return storedName;
            }
        }

        return uuid.toString();
    }

    private String findTabName(UUID uuid) {
        if (
            mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null
        ) {
            return null;
        }

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (
                info != null &&
                info.getGameProfile() != null &&
                uuid.equals(info.getGameProfile().getId())
            ) {
                return info.getGameProfile().getName();
            }
        }

        return null;
    }

    private UUID getSelfUuid() {
        if (
            mc.getSession() == null ||
            mc.getSession().getProfile() == null ||
            mc.getSession().getProfile().getId() == null
        ) {
            return null;
        }
        return mc.getSession().getProfile().getId();
    }

    private boolean isUuidLike(String value) {
        String normalized = value.replace("-", "");
        return normalized.matches("(?i)[0-9a-f]{32}");
    }
}
