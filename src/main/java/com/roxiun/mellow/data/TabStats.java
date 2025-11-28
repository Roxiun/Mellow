package com.roxiun.mellow.data;

import com.roxiun.mellow.api.urchin.UrchinTag;
import java.util.List;

public class TabStats {

    private final List<UrchinTag> urchinTags;
    private final String stars;
    private final String fkdr;
    private final String winstreak;

    public TabStats(
        List<UrchinTag> urchinTags,
        String stars,
        String fkdr,
        String winstreak
    ) {
        this.urchinTags = urchinTags;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
    }

    public boolean isUrchinTagged() {
        return urchinTags != null && !urchinTags.isEmpty();
    }

    public List<UrchinTag> getUrchinTags() {
        return urchinTags;
    }

    public String getStars() {
        return stars;
    }

    public String getFkdr() {
        return fkdr;
    }

    public String getWinstreak() {
        return winstreak;
    }
}
