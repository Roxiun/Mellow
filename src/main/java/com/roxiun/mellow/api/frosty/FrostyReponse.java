package com.roxiun.mellow.api.frosty;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FrostyReponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<PlayerResult> data;

    public static class PlayerResult {

        @SerializedName("username")
        public String username;

        @SerializedName("uuid")
        public String uuid;

        @SerializedName("game_rank")
        public String gameRank;

        @SerializedName("rank_color")
        public String rankColor;

        @SerializedName("plus_color")
        public String plusColor;

        @SerializedName("network_level")
        public long networkLevel;

        @SerializedName("last_online")
        public String lastOnline;

        @SerializedName("bedwars_index")
        public long bedwarsIndex;

        @SerializedName("bedwars_level")
        public long bedwarsLevel;

        @SerializedName("total_final_kills")
        public long totalFinalKills;

        @SerializedName("total_final_deaths")
        public long totalFinalDeaths;

        @SerializedName("total_beds_broken")
        public long totalBedsBroken;

        @SerializedName("total_beds_lost")
        public long totalBedsLost;

        @SerializedName("total_wins")
        public long totalWins;

        @SerializedName("total_losses")
        public long totalLosses;

        @SerializedName("island_topper")
        public String islandTopper;

        @SerializedName("victory_dance")
        public String victoryDance;

        @SerializedName("kill_effect")
        public String killEffect;

        @SerializedName("projectile_trail")
        public String projectileTrail;

        @SerializedName("kill_message")
        public String killMessage;

        @SerializedName("npc_skin")
        public String npcSkin;

        @SerializedName("death_cry")
        public String deathCry;

        @SerializedName("spray")
        public String spray;

        @SerializedName("bed_destroy")
        public String bedDestroy;

        @SerializedName("glyph")
        public String glyph;

        @SerializedName("wood_type")
        public String woodType;

        @SerializedName("starting_weapon")
        public String startingWeapon;

        @SerializedName("figurine")
        public String figurine;
    }
}

