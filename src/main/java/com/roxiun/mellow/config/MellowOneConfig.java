package com.roxiun.mellow.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Checkbox;
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
import com.roxiun.mellow.hud.BedwarsUpgradesTrapsHUD;
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

    // Tab Stats Configuration

    @Switch(name = "Show Stars with Brackets", category = "Tab Stats")
    public boolean showStarsWithBrackets = true;

    @Switch(name = "Show Nick with Brackets", category = "Tab Stats")
    public boolean showNickWithBrackets = true;

    @Info(
        text = "Set the order of stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats"
    )
    public static boolean ignoredStatsOrderInfo;

    @Dropdown(
        name = "First Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat1 = 0;

    @Dropdown(
        name = "Second Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat2 = 1; // Stars

    @Dropdown(
        name = "Third Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat3 = 2; // Name

    @Dropdown(
        name = "Fourth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat4 = 3; // FKDR

    @Dropdown(
        name = "Fifth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat5 = 4; // Winstreak

    @Dropdown(
        name = "Sixth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat6 = 10; // None by default

    @Dropdown(
        name = "Seventh Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat7 = 10; // None by default

    @Dropdown(
        name = "Eighth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat8 = 10; // None by default

    @Dropdown(
        name = "Ninth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat9 = 10; // None by default

    @Dropdown(
        name = "Tenth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
        },
        category = "Tab Stats"
    )
    public int customStat10 = 10; // None by default

    @Info(
        text = "Toggle seperator between stats",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public static boolean ignoredDotsInfo;

    @Checkbox(
        name = "Between 1st and 2nd",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot12 = false;

    @Checkbox(
        name = "Between 2nd and 3rd",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot23 = false;

    @Checkbox(
        name = "Between 3rd and 4th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot34 = true;

    @Checkbox(
        name = "Between 4th and 5th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot45 = true;

    @Checkbox(
        name = "Between 5th and 6th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot56 = true;

    @Checkbox(
        name = "Between 6th and 7th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot67 = true;

    @Checkbox(
        name = "Between 7th and 8th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot78 = true;

    @Checkbox(
        name = "Between 8th and 9th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot89 = true;

    @Checkbox(
        name = "Between 9th and 10th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot910 = true;

    @HUD(name = "Emerald Counter HUD", category = "HUD")
    public EmeraldCounterHUD emeraldCounterHUD = new EmeraldCounterHUD();

    @HUD(name = "Diamond Counter HUD", category = "HUD")
    public DiamondCounterHUD diamondCounterHUD = new DiamondCounterHUD();

    @HUD(name = "Upgrades & Traps HUD", category = "HUD")
    public BedwarsUpgradesTrapsHUD upgradesTrapsHUD =
        new BedwarsUpgradesTrapsHUD();

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

    @Switch(name = "Print Blacklist Tags in /who", subcategory = "General")
    public boolean printBlacklistTags = true;

    @Switch(name = "Auto Pregame Stats", subcategory = "Pregame")
    public boolean pregameStats = true;

    @Switch(name = "Auto Skin Denicker", subcategory = "Denicker")
    public boolean autoSkinDenick = true;

    // Urchin Configs
    @Info(
        text = "Urchin is a community blacklist, allowing you to see potential cheaters in your game",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Urchin"
    )
    public static boolean ignoredUrchinDescription;

    @Switch(name = "Enable Urchin", category = "Urchin")
    public boolean urchin = false;

    @Switch(name = "Show Urchin Tags in Tab", category = "Urchin")
    public boolean showUrchinTagsInTab = true;

    @Info(
        text = "Enabling Urchin will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Urchin API Key, Game Info).",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Urchin"
    )
    public static boolean ignoredUrchinWarning;

    @Info(
        text = "Urchin does not require a key to view tags, these settings are deprecated",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Urchin"
    )
    public static boolean ignoredUrchinDeprecated;

    @Text(
        name = "Urchin API Key",
        category = "Urchin",
        secure = true,
        multiline = false
    )
    public String urchinKey = "";

    // Seraph Configs
    @Info(
        text = "Seraph is a community blacklist, allowing you to see potential cheaters in your game",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Seraph"
    )
    public static boolean ignoredSeraphDescription;

    @Switch(name = "Enable Seraph", category = "Seraph")
    public boolean seraph = false;

    @Switch(name = "Show Seraph Tags in Tab", category = "Seraph")
    public boolean showSeraphTagsInTab = true;

    @Info(
        text = "Enabling Seraph will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Seraph API Key, Game Info).",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Seraph"
    )
    public static boolean ignoredSeraphWarning;

    @Info(
        text = "Seraph does not require a key to view any tags older than 1 week old",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Seraph"
    )
    public static boolean ignoredSeraphInfo;

    @Text(
        name = "Seraph API Key",
        category = "Seraph",
        secure = true,
        multiline = false
    )
    public String seraphKey = "";

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
    public static boolean ignoredPolsuAPI;

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
    public static boolean ignoredNumberDenickerInfo; // Useless. Java limitations with @annotation.

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
    public static boolean ignoredNumberDenickerFuzzyInfo;

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
    public boolean anticheatEnabled = false;

    @Switch(name = "NoSlow Check", category = "Anticheat")
    public boolean noSlowCheckEnabled = true;

    @Switch(name = "AutoBlock Check", category = "Anticheat")
    public boolean autoBlockCheckEnabled = true;

    @Switch(name = "Eagle Check", category = "Anticheat")
    public boolean eagleCheckEnabled = false;

    @Switch(name = "Scaffold Check", category = "Anticheat")
    public boolean scaffoldCheckEnabled = false;

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
