package com.roxiun.mellow.stats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameStatsTest {
    private static JsonObject json(String text) { return new JsonParser().parse(text).getAsJsonObject(); }
    private static ProviderResult<PlayerProfile> parse(String text, GameDefinition<?> game, String mode) {
        return PlayerStatsService.parse("uuid", "Player", json(text), ProviderId.BORDIC, new StatsSelection(game, mode));
    }

    @Test public void providerNormalizationPreservesNativeNamesAndRankFormatting() {
        HypixelPlayerData nativeData = HypixelPlayerData.decode(
            "{\"name\":\"OldName\",\"profile\":{\"hypixel_displayname\":\"Player\",\"tagged_name\":\"§b[MVP+] Player\"},\"stats\":{\"Bedwars\":{\"wins_bedwars\":7}}}",
            ProviderId.NADESHIKO).getValue();
        assertEquals("Player", nativeData.name());
        assertEquals("§b[MVP+] Player", nativeData.formattedName());
        assertEquals(7, GameRegistry.BEDWARS.parse(nativeData, "overall").getValue().getWins());
        HypixelPlayerData standard = HypixelPlayerData.decode(
            "{\"success\":true,\"player\":{\"displayname\":\"Player\",\"newPackageRank\":\"MVP_PLUS\",\"rankPlusColor\":\"RED\"}}",
            ProviderId.HYPIXEL_PUBLIC).getValue();
        assertEquals("§b[MVP§c+§b] Player", standard.formattedName());
    }

    @Test public void explicitDuelsSelectionDoesNotDependOnCurrentClientAndDoesNotParseOtherGames() {
        String response = "{\"success\":true,\"player\":{\"displayname\":\"Player\",\"stats\":{\"Duels\":{\"wins\":900,\"classic_duel_wins\":12,\"classic_duel_losses\":3,\"sumo_duel_wins\":8},\"Bedwars\":{\"wins_bedwars\":45}}}}";
        PlayerProfile classic = parse(response, GameRegistry.DUELS, "classic").getValue();
        PlayerProfile sumo = parse(response, GameRegistry.DUELS, "sumo").getValue();
        assertEquals(12, classic.getStats(GameRegistry.DUELS).getWins());
        assertEquals(8, sumo.getStats(GameRegistry.DUELS).getWins());
        assertFalse(classic.hasStats(StatScope.BEDWARS));
        assertEquals(900, parse(response, GameRegistry.DUELS, "overall").getValue().getStats(GameRegistry.DUELS).getWins());
        assertFalse(parse(response, GameRegistry.DUELS, "boxing").isSuccess());
    }

    @Test public void missingGameDoesNotBecomeZeroStatsOrFallBackToBedwars() {
        String response = "{\"success\":true,\"player\":{\"displayname\":\"Player\",\"stats\":{\"Bedwars\":{}}}}";
        assertEquals(FetchFailureReason.NO_PLAYER_DATA, parse(response, GameRegistry.SKYWARS, "overall").getFailureReason());
        PlayerProfile bedwars = parse(response, GameRegistry.BEDWARS, "overall").getValue();
        assertEquals(0, bedwars.getStats(GameRegistry.BEDWARS).getWins());
        assertNull(bedwars.getTabStats(StatScope.SKYWARS).getWins());
        assertEquals("", bedwars.chatStats(StatScope.SKYWARS));
        assertNull(GameRegistry.find(StatScope.NETWORK));
    }

    @Test public void bedwarsSubmodeAggregatesOnlyItsQueues() {
        String response = "{\"success\":true,\"player\":{\"displayname\":\"Player\",\"stats\":{\"Bedwars\":{\"wins_bedwars\":900,\"eight_two_lucky_wins_bedwars\":12,\"four_four_lucky_wins_bedwars\":8,\"eight_two_lucky_final_kills_bedwars\":30,\"eight_two_lucky_final_deaths_bedwars\":3,\"winstreak\":99}}}}";
        assertEquals(20, parse(response, GameRegistry.BEDWARS, "overall_lucky").getValue().getStats(GameRegistry.BEDWARS).getWins());
        assertEquals(12, parse(response, GameRegistry.BEDWARS, "eight_two_lucky").getValue().getStats(GameRegistry.BEDWARS).getWins());
        assertFalse(parse(response, GameRegistry.BEDWARS, "overall_lucky").getValue().getStats(GameRegistry.BEDWARS).hasWinstreakData());
    }

    @Test public void genericTabDefinitionsKeepGameSpecificValuesAndLegacyColumnPositions() {
        String response = "{\"success\":true,\"player\":{\"displayname\":\"Player\",\"stats\":{\"Duels\":{\"wins\":12345,\"losses\":31,\"kills\":24,\"deaths\":6},\"TNTGames\":{\"wins_tntrun\":75,\"deaths_tntrun\":25},\"BuildBattle\":{\"score\":100,\"wins\":8}}}}";
        TabStats duels = parse(response, GameRegistry.DUELS, "overall").getValue().getTabStats(StatScope.DUELS);
        assertEquals("12,345", plain(GameRegistry.DUELS.column(5).value(duels)));
        assertEquals("31", plain(GameRegistry.DUELS.column(6).value(duels)));
        assertEquals("24", plain(GameRegistry.DUELS.column(7).value(duels)));
        assertEquals("6", plain(GameRegistry.DUELS.column(8).value(duels)));
        assertEquals("4", plain(GameRegistry.DUELS.column(3).value(duels)));
        TabStats tnt = parse(response, GameRegistry.TNT_RUN, "overall").getValue().getTabStats(StatScope.TNT_RUN);
        assertEquals("75", plain(GameRegistry.TNT_RUN.column(1).value(tnt)));
        assertEquals("3", plain(GameRegistry.TNT_RUN.column(3).value(tnt)));
        assertEquals("health", GameRegistry.BEDWARS.column(11).id());
        assertEquals("health", GameRegistry.SKYWARS.column(8).id());
        assertArrayEquals(new String[] {"Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP", "Tags", "Ping"}, GameRegistry.SKYWARS.columnOptions());
    }
    private static String plain(String text) { return text.replaceAll("§.", ""); }
}
