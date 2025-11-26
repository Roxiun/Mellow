package com.strawberry.statsify.api.urchin;

public class UrchinTag {

    private final String type;
    private final String reason;

    public UrchinTag(String type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }
}
