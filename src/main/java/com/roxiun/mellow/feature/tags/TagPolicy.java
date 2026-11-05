package com.roxiun.mellow.feature.tags;

import com.roxiun.mellow.api.tags.*;
import com.roxiun.mellow.config.MellowOneConfig;
import java.util.*;

/** Shared automatic-warning and display policy. Manual lookups use the unfiltered report. */
public final class TagPolicy {
    private TagPolicy() {}
    public static boolean anyEnabled(MellowOneConfig config) {
        return config != null && (config.isCoralEnabled() || config.xadia || config.watcher);
    }
    public static boolean anyVisibleInTab(MellowOneConfig config) {
        return config != null && ((config.isCoralEnabled() && config.shouldShowCoralTagsInTab())
            || (config.xadia && config.showXadiaTagsInTab) || (config.watcher && config.showWatcherTagsInTab));
    }
    public static long retryDelayMillis(TagReport report) {
        return report != null && report.getFailures().containsKey("Watcher") ? 60_000L : 30_000L;
    }
    public static String settingsKey(MellowOneConfig config) {
        return config.isCoralEnabled() + "|" + config.getCoralApiKey() + "|" + config.xadia
            + "|" + config.xadiaKey + "|" + config.xadiaVerifiedOnly + "|" + config.watcher + "|" + config.watcherKey;
    }
    public static Map<String, String> warnings(TagReport report, boolean enabled, boolean ignored) {
        Map<String, String> messages = new LinkedHashMap<>();
        if (!enabled || ignored) return messages;
        for (PlayerTag tag : report.getTags()) {
            if (tag.isWarning()) messages.merge(tag.getSource(), tag.getText(), (a, b) -> a + ", " + b);
        }
        return messages;
    }
    public static List<PlayerTag> visible(TagReport report, MellowOneConfig config) {
        List<PlayerTag> visible = new ArrayList<>();
        if (config == null) return visible;
        for (PlayerTag tag : report.getTags()) {
            boolean enabled = "Coral".equals(tag.getSource()) ? config.isCoralEnabled() && config.shouldShowCoralTagsInTab()
                : "Xadia".equals(tag.getSource()) ? config.xadia && config.showXadiaTagsInTab
                : "Watcher".equals(tag.getSource()) ? config.watcher && config.showWatcherTagsInTab : true;
            if (enabled) visible.add(tag);
        }
        return visible;
    }
}
