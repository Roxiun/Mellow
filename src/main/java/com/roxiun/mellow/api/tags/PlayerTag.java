package com.roxiun.mellow.api.tags;

/** Provider-neutral tag consumed by alerts and overlays. Native adapters retain their own metadata. */
public final class PlayerTag {
    private final String source, type, text, icon;
    private final boolean warning;

    public PlayerTag(String source, String type, String text, String icon, boolean warning) {
        this.source = source;
        this.type = type;
        this.text = text;
        this.icon = icon;
        this.warning = warning;
    }
    public String getSource() { return source; }
    public String getType() { return type; }
    public String getText() { return text; }
    public String getIcon() { return icon; }
    public boolean isWarning() { return warning; }
}
