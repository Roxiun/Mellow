package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.stats.skywars.SkywarsPlayer;
import com.roxiun.mellow.data.PlayerProfile;
import org.junit.Assert;
import org.junit.Test;

public class ChatStatsFormatterTest {

    @Test
    public void skywarsProfileShowsSkywarsStatsWithoutBedwarsData() {
        SkywarsPlayer player = new SkywarsPlayer(
            "Player", "Player", "15✯", "[15✯]", 3.5, 20, 10, 35, 10
        );
        PlayerProfile profile = new PlayerProfile("uuid", "Player", java.util.Collections.singletonMap(com.roxiun.mellow.stats.GameRegistry.SKYWARS, player), null);

        String message = ChatStatsFormatter.format(profile, StatScope.SKYWARS);

        Assert.assertTrue(message.contains("15✯"));
        Assert.assertTrue(message.contains("KDR: " + player.getFormattedKdrWithColor()));
        Assert.assertTrue(message.contains("WLR: " + player.getFormattedWlrWithColor()));
        Assert.assertFalse(message.contains("FKDR"));
    }
}
