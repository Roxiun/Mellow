package com.roxiun.mellow.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Info;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.hud.DiamondCounterHUD;
import com.roxiun.mellow.hud.EmeraldCounterHUD;

public class MellowOneConfig extends Config {

    @Switch(name = "Auto /who", subcategory = "General")
    public boolean autoWho = true;

    @Switch(name = "Show Tab Stats", subcategory = "General")
    public boolean tabStats = true;

    @Switch(name = "Show Tags", subcategory = "General")
    public boolean tags = false;

    @Switch(name = "Print Stats to Chat", subcategory = "General")
    public boolean printStats = false;

    @HUD(name = "Emerald Counter HUD", category = "HUD")
    public EmeraldCounterHUD emeraldCounterHUD = new EmeraldCounterHUD();

    @HUD(name = "Diamond Counter HUD", category = "HUD")
    public DiamondCounterHUD diamondCounterHUD = new DiamondCounterHUD();

    @Number(
        name = "Minimum FKDR to show",
        min = -1,
        max = 500,
        step = 1,
        subcategory = "General"
    )
    public int minFkdr = -1;

    @Dropdown(
        name = "Stats Provider",
        options = { "Nadeshiko", "Abyss" },
        subcategory = "Stats"
    )
    public int statsProvider = 0;

    @Switch(name = "Enable Urchin", subcategory = "Tags")
    public boolean urchin = false;

    @Switch(name = "Print Urchin Tags in /who", subcategory = "Tags")
    public boolean printUrchinTags = true;

    @Switch(name = "Auto Pregame Stats", subcategory = "Pregame")
    public boolean pregameStats = true;

    @Switch(name = "Auto Skin Denicker", subcategory = "Denicker")
    public boolean autoSkinDenick = true;

    @Dropdown(
        name = "Tab Format",
        options = {
            "[Star] Name • FKDR • WS",
            "Star • Name • FKDR • WS",
            "Name • FKDR • WS",
        },
        subcategory = "Stats"
    )
    public int tabFormat = 0;

    // Urchin Configs
    @Info(
        text = "Urchin does not require a key to view tags, these settings are deprecated",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Urchin"
    )
    public static boolean ignored4;

    @Text(
        name = "Urchin API Key",
        category = "Urchin",
        secure = true,
        multiline = false
    )
    public String urchinKey = "";

    // Ping Configs
    @Dropdown(
        name = "Ping Provider",
        category = "Ping",
        options = { "None", "Polsu", "Urchin" }
    )
    public int pingProvider = 0;

    @Info(
        text = "Polsu requires an API key to be able to be used, Urchin does not.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Ping"
    )
    public static boolean ignored3;

    @Button(
        name = "Polsu API Key",
        text = "Get Key",
        category = "Ping",
        subcategory = "Polsu"
    )
    Runnable polsuLinkButton = () -> {
        NetworkUtils.browseLink("https://polsu.xyz/api/apikey");
    };

    @Text(
        name = "Polsu API Key",
        category = "Ping",
        subcategory = "Polsu",
        secure = true,
        multiline = false
    )
    public String polsuApiKey = "";

    // Number denicker
    @Info(
        text = "This module attempts to denick players based the number of finals and beds broken from chat messages.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Number Denicker"
    )
    public static boolean ignored2; // Useless. Java limitations with @annotation.

    @Button(
        name = "Run /api view on the bot to get your key",
        text = "Discord Bot",
        size = OptionSize.DUAL,
        category = "Number Denicker"
    )
    Runnable auroraLinkButton = () -> {
        NetworkUtils.browseLink(
            "https://discord.com/oauth2/authorize?client_id=1244205279697174539"
        );
    };

    @Switch(name = "Enable Number Denicker", category = "Number Denicker")
    public boolean numberDenicker = false;

    @Switch(name = "Print all potential players", category = "Number Denicker")
    public boolean numberDenickerFuzzy = true;

    @Info(
        text = "Turning all potential players off, will only print players with both matching beds and finals.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Number Denicker"
    )
    public static boolean ignored5;

    @Text(
        name = "Aurora API Key",
        placeholder = "Enter your Aurora API key",
        category = "Number Denicker",
        secure = true,
        multiline = false
    )
    public String auroraApiKey = "";

    @Dropdown(
        name = "Finals Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker"
    )
    public int finalsRange = 3; // Index for 100

    @Dropdown(
        name = "Beds Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker"
    )
    public int bedsRange = 1; // Index for 50

    @Number(
        name = "Minimum Finals to Check",
        category = "Number Denicker",
        min = 0,
        max = 500000,
        step = 1000
    )
    public int minFinalsForDenick = 15000;

    @Dropdown(
        name = "Max Results",
        options = { "5", "10", "20" },
        category = "Number Denicker"
    )
    public int maxResults = 0; // Index for 5

    @Switch(name = "Enable Anticheat", category = "Anticheat")
    public boolean anticheatEnabled = true;

    @Switch(name = "NoSlow Check", category = "Anticheat")
    public boolean noSlowCheckEnabled = true;

    @Switch(name = "AutoBlock Check", category = "Anticheat")
    public boolean autoBlockCheckEnabled = true;

    @Switch(name = "Eagle Check", category = "Anticheat")
    public boolean eagleCheckEnabled = true;

    @Switch(name = "Scaffold Check", category = "Anticheat")
    public boolean scaffoldCheckEnabled = true;

    @Number(
        name = "Violation Level",
        category = "Anticheat",
        min = 1,
        max = 100
    )
    public int anticheatVl = 10;

    @Number(
        name = "Cooldown (seconds)",
        category = "Anticheat",
        min = 1,
        max = 60
    )
    public int anticheatCooldown = 5;

    public MellowOneConfig() {
        super(new Mod(Mellow.NAME, ModType.HYPIXEL), Mellow.MODID + ".json");
        initialize();
    }
}
