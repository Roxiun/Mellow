package com.roxiun.mellow.api.xadia;

/** A Xadia report; null verification means the tag type has no verification. */
public final class XadiaTag {
    private final String type, label, reason;
    private final Boolean verified;

    public XadiaTag(String type, String label, String reason, Boolean verified) {
        this.type = type;
        this.label = label;
        this.reason = reason;
        this.verified = verified;
    }

    public String getType() { return type; }
    public String getLabel() { return label; }
    public String getReason() { return reason; }
    public Boolean getVerified() { return verified; }
}
