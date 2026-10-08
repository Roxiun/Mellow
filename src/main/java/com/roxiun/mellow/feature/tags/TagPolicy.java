package com.roxiun.mellow.feature.tags;

import com.roxiun.mellow.api.tags.*;
import com.roxiun.mellow.config.MellowOneConfig;
import java.util.*;

/** Shared automatic-warning and display policy. Manual lookups use the unfiltered report. */
public final class TagPolicy {
    private TagPolicy() {}
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
                : "Xadia".equals(tag.getSource()) ? config.xadia && config.showXadiaTagsInTab : true;
            if (enabled) visible.add(tag);
        }
        return visible;
    }
}
