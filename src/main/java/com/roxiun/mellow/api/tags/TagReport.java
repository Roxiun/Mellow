package com.roxiun.mellow.api.tags;

import com.roxiun.mellow.api.coral.CoralTag;
import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.util.*;

/** Immutable per-source outcomes: an unavailable provider is never an empty successful lookup. */
public final class TagReport {
    private final List<PlayerTag> tags;
    private final Map<String, String> failures;
    public TagReport(List<PlayerTag> tags, Map<String, String> failures) {
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.failures = Collections.unmodifiableMap(new LinkedHashMap<>(failures));
    }
    public static TagReport empty() { return new TagReport(Collections.emptyList(), Collections.emptyMap()); }
    public List<PlayerTag> getTags() { return tags; }
    public Map<String, String> getFailures() { return failures; }
    public List<PlayerTag> from(String source) {
        List<PlayerTag> result = new ArrayList<>();
        for (PlayerTag tag : tags) if (tag.getSource().equals(source)) result.add(tag);
        return Collections.unmodifiableList(result);
    }
    public boolean has(String source) { return !from(source).isEmpty(); }
    public String text(String source) {
        return from(source).stream().map(PlayerTag::getText).collect(java.util.stream.Collectors.joining(", "));
    }
    /** Keep the last known badges only for sources that failed this refresh. */
    public TagReport retainFailedSources(TagReport previous) {
        List<PlayerTag> combined = new ArrayList<>(tags);
        for (PlayerTag tag : previous.tags) if (failures.containsKey(tag.getSource()) || failures.containsKey("Tags")) combined.add(tag);
        return new TagReport(combined, failures);
    }
    public List<String> messages() {
        List<String> lines = new ArrayList<>();
        Set<String> sources = new LinkedHashSet<>();
        for (PlayerTag tag : tags) sources.add(tag.getSource());
        for (String source : sources) {
            boolean first = true;
            for (PlayerTag tag : from(source)) {
                lines.add((first ? FormattingUtils.formatTagSource(source, true) + ": " : "") + tag.getText());
                first = false;
            }
        }
        for (Map.Entry<String, String> failure : failures.entrySet())
            lines.add("§e" + failure.getKey() + " unavailable: " + failure.getValue());
        return lines;
    }
    public static TagReport nativeTags(List<CoralTag> coral, List<XadiaTag> xadia, Map<String, String> failures) {
        List<PlayerTag> tags = new ArrayList<>();
        if (coral != null) for (CoralTag tag : coral) tags.add(new PlayerTag("Coral", tag.getType(),
            FormattingUtils.formatCoralTag(tag), FormattingUtils.formatCoralTagIcon(tag), true));
        if (xadia != null) for (XadiaTag tag : xadia) tags.add(new PlayerTag("Xadia", tag.getType(),
            FormattingUtils.formatXadiaTag(tag), FormattingUtils.formatXadiaTagIcon(tag), true));
        return new TagReport(tags, failures);
    }
}
