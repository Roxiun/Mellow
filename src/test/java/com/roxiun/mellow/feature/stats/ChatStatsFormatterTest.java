package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.data.PlayerProfile;
import org.junit.Assert;
import org.junit.Test;

public class ChatStatsFormatterTest {

    @Test
    public void skywarsProfileShowsSkywarsStatsWithoutBedwarsData() {
        SkywarsPlayer player = new SkywarsPlayer(
            "Player", "Player", "15✯", "[15✯]", 3.5, 20, 10, 35, 10
        );
        PlayerProfile profile = new PlayerProfile("uuid", "Player", null, player, null, null, null, null);

        String message = ChatStatsFormatter.format(profile, StatScope.SKYWARS);

        Assert.assertTrue(message.contains("15✯"));
        Assert.assertTrue(message.contains("KDR: " + player.getFormattedKdrWithColor()));
        Assert.assertTrue(message.contains("WLR: " + player.getFormattedWlrWithColor()));
        Assert.assertFalse(message.contains("FKDR"));
    }
}
