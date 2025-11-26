package com.strawberry.statsify.data;

import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import com.strawberry.statsify.api.urchin.UrchinTag;
import java.util.List;

public class PlayerProfile {

    private final String uuid;
    private final String name;
    private final BedwarsPlayer bedwarsPlayer;
    private final List<UrchinTag> urchinTags;
    private final long lastUpdated;

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<UrchinTag> urchinTags
    ) {
        this.uuid = uuid;
        this.name = name;
        this.bedwarsPlayer = bedwarsPlayer;
        this.urchinTags = urchinTags;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BedwarsPlayer getBedwarsPlayer() {
        return bedwarsPlayer;
    }

    public List<UrchinTag> getUrchinTags() {
        return urchinTags;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public boolean isUrchinTagged() {
        return urchinTags != null && !urchinTags.isEmpty();
    }
}
