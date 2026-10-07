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

    @Switch(
        name = "Auto Update Check",
        category = "General",
        subcategory = "General"
    )
    public boolean autoUpdateCheck = true;

    @Info(
        text = "Hypixel provider requires an API key from developer.hypixel.net. Configure it in API Keys > Hypixel. Other providers do not require a key.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Stats",
        subcategory = "General"
    )
    public static boolean ignoredHypixelApiInfo;

    @Dropdown(
        description = "Hypixel provider requires an API key from developer.hypixel.net. Configure it in API Keys > Hypixel. Other providers do not require a key.",
        name = "Stats Provider",
        options = { "Hypixel Public API", "Nadeshiko", "Abyss", "Bordic", "Bedlify" },
        category = "Stats",
        subcategory = "General"
    )
    public int statsProvider = 3; // Bordic

    @Switch(
        name = "Auto /who",
        category = "Stats",
        subcategory = "General"
    )
    public boolean autoWho = false;

    @Switch(
        name = "Print Stats to Chat",
        category = "Stats",
        subcategory = "General"
    )
    public boolean printStats = false;

    @Switch(
        description = "Include generated player tags in printed BedWars stats.",
        name = "Include Tags in Chat Stats",
        category = "Stats",
        subcategory = "General"
    )
    public boolean tags = false;

    @Number(
        description = "Filters BedWars stats shown in tab and printed by stats lookups. Set to -1 for no minimum.",
        name = "Minimum BedWars FKDR",
        min = -1,
        max = 500,
        step = 1,
        category = "Stats",
        subcategory = "General"
    )
    public int minFkdr = -1;

    @Switch(
        name = "Auto Pregame Stats",
        category = "Stats",
        subcategory = "Automatic Lookups"
    )
    public boolean pregameStats = true;

    @Switch(
        description = "Show sender stats when they mention your username in BedWars lobby chat.",
        name = "Mention Stats in BedWars Lobbies",
        category = "Stats",
        subcategory = "Automatic Lookups"
    )
    public boolean mentionLobbyStats = false;

    @Switch(
        description = "Show chat errors when background stats, denick stats, or hidden winstreak lookups fail. Manual commands still report lookup failures.",
        name = "Show Automatic Stats Errors",
        category = "Stats",
        subcategory = "Automatic Lookups"
    )
    public boolean showAutomaticStatsErrors = true;

    @Info(
        text = "Shows hidden or zero BedWars winstreaks fetched from the Bordic Aurora API",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public static boolean ignoredWinstreaksDescription;

    @Switch(
        description = "Shows hidden or zero BedWars winstreaks fetched from the Bordic Aurora API. When hidden winstreaks are enabled, Mellow only uses Aurora when the visible BedWars winstreak is missing or hidden. Enabling this sends player UUIDs to Bordic and is subject to their ToS. Hidden winstreak lookups do not require or send an Aurora API key.",
        name = "Show Hidden Winstreaks",
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public boolean showHiddenWinstreaks = true;

    @Info(
        text = "When hidden winstreaks are enabled, Mellow only uses Aurora when the visible BedWars winstreak is missing or hidden.",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public static boolean ignoredWinstreaksVisibleFirstInfo;

    @Info(
        text = "Enabling this sends player UUIDs to Bordic and is subject to their ToS. Hidden winstreak lookups do not require or send an Aurora API key.",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public static boolean ignoredWinstreaksWarning;

    @Dropdown(
        name = "Minimum Stars to Fetch WS",
        options = {
            "None",
            "100",
            "200",
            "300",
            "400",
            "500",
            "600",
            "700",
            "800",
            "900",
            "1000",
            "1100",
            "1200",
            "1300",
            "1400",
            "1500",
            "1600",
            "1700",
            "1800",
            "1900",
            "2000",
            "2100",
            "2200",
            "2300",
            "2400",
            "2500",
            "2600",
            "2700",
            "2800",
            "2900",
            "3000",
            "3100",
            "3200",
            "3300",
            "3400",
            "3500",
            "3600",
            "3700",
            "3800",
            "3900",
            "4000",
            "4100",
            "4200",
            "4300",
            "4400",
            "4500",
            "4600",
            "4700",
            "4800",
            "4900",
            "5000",
        },
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public int winstreakMinStars = 0;

    @Dropdown(
        name = "Minimum FKDR to Fetch WS",
        options = {
            "None",
            "1",
            "2",
            "3",
            "4",
            "5",
            "10",
            "15",
            "20",
            "25",
            "30",
            "40",
            "50",
            "60",
            "70",
            "80",
            "90",
            "100",
        },
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public int winstreakMinFkdr = 0;

    @Dropdown(
        description = "Aurora API provides historical ping averages per player UUID without requiring or sending an API key. Luna's API provides ping averages per player UUID. Configure the key in API Keys > Luna.",
        name = "Ping Provider",
        options = { "None", "Aurora API", "Luna's API" },
        category = "Stats",
        subcategory = "Ping"
    )
    public int pingProvider = 1;

    @Info(
        text = "Aurora API provides historical ping averages per player UUID without requiring or sending an API key.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Stats",
        subcategory = "Ping"
    )
    public static boolean ignoredAuroraPingInfo;

    @Info(
        text = "Luna's API provides ping averages per player UUID. Configure the key in API Keys > Luna.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Stats",
        subcategory = "Ping"
    )
    public static boolean ignoredLunaPingInfo;

    @Switch(
        name = "Show Tab Stats",
        category = "Tab Stats",
        subcategory = "General"
    )
    public boolean tabStats = true;

    @Switch(
        name = "Show Stars with Brackets",
        category = "Tab Stats",
        subcategory = "General"
    )
    public boolean showStarsWithBrackets = true;

    @Switch(
        name = "Show Nick with Brackets",
        category = "Tab Stats",
        subcategory = "General"
    )
    public boolean showNickWithBrackets = true;

    @Switch(
        description = "When enabled, Name stat includes rank prefix during games. Lobbies always show rank.",
        name = "Show Ranks In-Game",
        category = "Tab Stats",
        subcategory = "General"
    )
    public boolean showRanksInGameTabStats = false;

    @Switch(
        name = "Extended Tab Stats View",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean extendedTabStatsView = true;

    @Switch(
        description = "Allows Extended Tab Stats View while in game lobbies.",
        name = "Extended View In Lobbies",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean extendedTabStatsInLobbies = false;

    @Dropdown(
        description = "Style of the extended tab's column headings.",
        name = "Column Headers",
        options = {"Bold", "Normal", "Off"},
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public int extendedTabStatsHeaders = 0;

    @Switch(
        description = "Shows player heads in the Name column when using Extended Tab Stats View.",
        name = "Extended View Player Heads",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean extendedTabStatsShowHeads = true;

    @Dropdown(
        name = "Extended Team Column Mode",
        options = {
            "Combine With Stars",
            "Own Column",
            "Hide Team Header",
            "Combine With Name",
        },
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public int extendedTabStatsTeamColumnMode = 3;

    @Switch(
        description = "Collapses extra whitespace when Team is combined with Name or Stars.",
        name = "Strip Team Padding",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean extendedTabStatsStripCombinedTeamPadding = true;

    @Switch(
        name = "Highlight Tagged Players",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean highlightTaggedPlayers = false;

    @Info(
        text = "Set the order of stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
    )
    public int customStat6 = 11; // HP by default

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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
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
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "BedWars Stat Order"
    )
    public int customStat10 = 10; // None by default

    @Info(
        text = "Set the order of SkyWars stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public static boolean ignoredSkywarsStatsOrderInfo;

    @Dropdown(
        name = "First SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat1 = 0;

    @Dropdown(
        name = "Second SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat2 = 1;

    @Dropdown(
        name = "Third SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat3 = 2;

    @Dropdown(
        name = "Fourth SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat4 = 3;

    @Dropdown(
        name = "Fifth SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat5 = 4;

    @Dropdown(
        name = "Sixth SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat6 = 8; // HP by default

    @Dropdown(
        name = "Seventh SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat7 = 7;

    @Dropdown(
        name = "Eighth SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat8 = 7;

    @Dropdown(
        name = "Ninth SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat9 = 7;

    @Dropdown(
        name = "Tenth SkyWars Stat",
        options = {
            "Team",
            "Level",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Kills",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "SkyWars Stat Order"
    )
    public int skywarsCustomStat10 = 7;

    @Info(
        text = "Set the order of Duels stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public static boolean ignoredDuelsStatsOrderInfo;

    @Dropdown(
        name = "First Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat1 = 0;

    @Dropdown(
        name = "Second Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat2 = 1;

    @Dropdown(
        name = "Third Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat3 = 2;

    @Dropdown(
        name = "Fourth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat4 = 3;

    @Dropdown(
        name = "Fifth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat5 = 4;

    @Dropdown(
        name = "Sixth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat6 = 5;

    @Dropdown(
        name = "Seventh Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat7 = 6;

    @Dropdown(
        name = "Eighth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat8 = 7;

    @Dropdown(
        name = "Ninth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat9 = 8;

    @Dropdown(
        name = "Tenth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
            "Tags",
            "Ping",
        },
        category = "Tab Stats",
        subcategory = "Duels Stat Order"
    )
    public int duelsCustomStat10 = 11; // HP by default

    @Checkbox(
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        name = "Between 1st and 2nd",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot12 = false;

    @Checkbox(
        name = "Between 2nd and 3rd",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot23 = false;

    @Checkbox(
        name = "Between 3rd and 4th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot34 = true;

    @Checkbox(
        name = "Between 4th and 5th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot45 = true;

    @Checkbox(
        name = "Between 5th and 6th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot56 = true;

    @Checkbox(
        name = "Between 6th and 7th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot67 = true;

    @Checkbox(
        name = "Between 7th and 8th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot78 = true;

    @Checkbox(
        name = "Between 8th and 9th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot89 = true;

    @Checkbox(
        name = "Between 9th and 10th",
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public boolean showDot910 = true;

    @Info(
        text = "Separators apply to the standard tab layout, including lobbies where Extended View is off.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "Standard View Separators"
    )
    public static boolean ignoredDotsInfo;

    @Switch(
        description = "Print Coral and Xadia tag alerts during stats lookups. Local blacklist warnings are independent.",
        name = "Print Provider Tag Alerts",
        category = "Tags & Blacklists",
        subcategory = "Chat Warnings"
    )
    public boolean printBlacklistTags = true;

    @Dropdown(
        description = "When blacklisted BedWars opponents are detected in-game, route warning messages to this chat channel.",
        name = "Blacklist Warn Destination",
        options = { "None", "All Chat", "Party Chat" },
        category = "Tags & Blacklists",
        subcategory = "Chat Warnings"
    )
    public int inGameBlacklistWarningDestination = 0;

    @Switch(
        description = "Shows a warning when your Hypixel party contains one or more blacklisted players.",
        name = "Warn for Blacklisted Party Members",
        category = "Tags & Blacklists",
        subcategory = "Party Warnings"
    )
    public boolean partyBlacklistWarning = true;

    @Switch(
        description = "When warning about flagged party members, also print what they are tagged for.",
        name = "Show Party Blacklist Tag Details",
        category = "Tags & Blacklists",
        subcategory = "Party Warnings"
    )
    public boolean partyBlacklistWarningShowTagDetails = false;

    @Switch(
        description = "Requires Stats > Automatic Lookups > Auto Pregame Stats. Automatically runs the configured command in BedWars pregame when a blacklisted chatter is detected and more than 2 seconds remain.",
        name = "Auto Leave on Blacklisted Chat",
        category = "Tags & Blacklists",
        subcategory = "Pregame Auto Leave"
    )
    public boolean autoLeaveBlacklistedPregameChat = false;

    @Text(
        name = "Auto Leave Command",
        multiline = false,
        category = "Tags & Blacklists",
        subcategory = "Pregame Auto Leave"
    )
    public String autoLeaveBlacklistedPregameCommand = "/lobby";

    @Info(
        text = "Coral is a community blacklist, allowing you to see potential cheaters in your game",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Tags & Blacklists",
        subcategory = "Coral"
    )
    public static boolean ignoredCoralDescription;

    @Switch(
        description = "Coral is a community blacklist, allowing you to see potential cheaters in your game. Coral requires an API key. Enabling it sends player identifiers to api.urchin.gg and is subject to their ToS. Configure the key in API Keys > Coral.",
        name = "Enable Coral",
        category = "Tags & Blacklists",
        subcategory = "Coral"
    )
    // Keep the legacy field name so existing OneConfig profiles migrate in place.
    public boolean urchin = false;

    @Switch(
        name = "Show Coral Tags in Tab",
        category = "Tags & Blacklists",
        subcategory = "Coral"
    )
    // Keep the legacy field name so existing OneConfig profiles migrate in place.
    public boolean showUrchinTagsInTab = true;

    @Info(
        text = "Coral requires an API key. Enabling it sends player identifiers to api.urchin.gg and is subject to their ToS. Configure the key in API Keys > Coral.",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Tags & Blacklists",
        subcategory = "Coral"
    )
    public static boolean ignoredCoralWarning;

    @Switch(
        description = "Look up player tags on xadia.sniped.me. Requires a personal key from the Xadia Discord bot (/key generate). Configure it in API Keys > Xadia.",
        name = "Enable Xadia",
        category = "Tags & Blacklists",
        subcategory = "Xadia"
    )
    public boolean xadia = false;

    @Switch(
        name = "Show Xadia Tags in Tab",
        category = "Tags & Blacklists",
        subcategory = "Xadia"
    )
    public boolean showXadiaTagsInTab = true;

    @Switch(
        description = "Hide unverified public reports.",
        name = "Verified Tags Only",
        category = "Tags & Blacklists",
        subcategory = "Xadia"
    )
    public boolean xadiaVerifiedOnly = false;

    @Switch(
        name = "Auto Skin Denicker",
        category = "Denicker",
        subcategory = "Skin Denicker"
    )
    public boolean autoSkinDenick = true;

    @Info(
        text = "This module attempts to denick players based the number of finals and beds broken from chat messages. Configure the Aurora key in API Keys > Aurora.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public static boolean ignoredNumberDenickerInfo; // Useless. Java limitations with @annotation.

    @Switch(
        description = "This module attempts to denick players based on the number of finals and beds broken from chat messages. Configure the Aurora key in API Keys > Aurora.",
        name = "Automatic Number Denicker",
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public boolean numberDenicker = false;

    @Switch(
        description = "When disabled, only print players whose beds and finals both match.",
        name = "Print All Potential Players",
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public boolean numberDenickerFuzzy = true;

    @Info(
        text = "Turning all potential players off, will only print players with both matching beds and finals.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public static boolean ignoredNumberDenickerFuzzyInfo;

    @Number(
        name = "Minimum Finals to Check",
        min = 0,
        max = 500000,
        step = 1000,
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public int minFinalsForDenick = 15000;

    @Dropdown(
        description = "Used by automatic number denicking and the /denick command.",
        name = "Finals Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Denicker",
        subcategory = "Search Settings"
    )
    public int finalsRange = 3; // Index for 200

    @Dropdown(
        description = "Used by automatic number denicking and the /denick command.",
        name = "Beds Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Denicker",
        subcategory = "Search Settings"
    )
    public int bedsRange = 1; // Index for 50

    @Dropdown(
        description = "Used by automatic number denicking and the /denick command.",
        name = "Max Results",
        options = { "5", "10", "20" },
        category = "Denicker",
        subcategory = "Search Settings"
    )
    public int maxResults = 0; // Index for 5

    @Button(
        description = "Use /api view with the Discord bot, then enter the key in API Keys > Aurora.",
        name = "Get an Aurora API Key",
        text = "Discord Bot",
        size = OptionSize.DUAL,
        category = "Denicker",
        subcategory = "Setup"
    )
    Runnable auroraLinkButton = () -> {
        NetworkUtils.browseLink(
            "https://discord.com/oauth2/authorize?client_id=1244205279697174539"
        );
    };

    @Switch(
        description = "Shows accept/deny popups for incoming friend requests and party invites.",
        name = "Request Popups",
        category = "Requests",
        subcategory = "General"
    )
    public boolean requestPopupsEnabled = true;

    @Switch(
        description = "Shows popups for incoming friend requests.",
        name = "Friend Request Popups",
        category = "Requests",
        subcategory = "General"
    )
    public boolean friendRequestPopupsEnabled = true;

    @Switch(
        description = "Shows popups for incoming party invites.",
        name = "Party Invite Popups",
        category = "Requests",
        subcategory = "General"
    )
    public boolean partyInvitePopupsEnabled = true;

    @Switch(
        description = "Play a pling sound when a new request popup is received.",
        name = "Popup Sound",
        category = "Requests",
        subcategory = "Appearance & Sound"
    )
    public boolean requestPopupSoundEnabled = true;

    @Dropdown(
        name = "Popup Position",
        options = { "Top-center", "Top-right", "Bottom-right" },
        category = "Requests",
        subcategory = "Appearance & Sound"
    )
    public int requestPopupPosition = 0;

    @Number(
        name = "Popup Duration (seconds)",
        min = 2,
        max = 30,
        step = 1,
        category = "Requests",
        subcategory = "Appearance & Sound"
    )
    public int requestPopupDurationSeconds = 10;

    @Switch(
        name = "Colored Hitboxes",
        category = "Hitboxes & Nametags",
        subcategory = "Hitboxes"
    )
    public boolean coloredHitboxes = true;

    @Switch(
        name = "Affect Vanilla F3+B",
        category = "Hitboxes & Nametags",
        subcategory = "Hitboxes"
    )
    public boolean coloredHitboxesAffectVanillaDebug = true;

    @Switch(
        name = "Affect PolyHitbox",
        category = "Hitboxes & Nametags",
        subcategory = "Hitboxes"
    )
    public boolean coloredHitboxesAffectPolyHitbox = true;

    @Switch(
        name = "Colored Nametag Backgrounds",
        category = "Hitboxes & Nametags",
        subcategory = "Nametags"
    )
    public boolean coloredNametagBackgrounds = false;

    @Switch(
        name = "Affect PolyNametag",
        category = "Hitboxes & Nametags",
        subcategory = "Nametags"
    )
    public boolean coloredNametagAffectPolyNametag = true;

    @Dropdown(
        name = "Hue Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxHueMode = 0;

    @Number(
        name = "Hue Value",
        min = 0,
        max = 360,
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxHueValue = 0;

    @Number(
        name = "Hue Offset",
        min = -180,
        max = 180,
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxHueOffset = 0;

    @Dropdown(
        name = "Saturation Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxSaturationMode = 0;

    @Number(
        name = "Saturation Value",
        min = 0,
        max = 100,
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxSaturationValue = 100;

    @Number(
        name = "Saturation Offset",
        min = -100,
        max = 100,
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxSaturationOffset = 0;

    @Dropdown(
        name = "Brightness Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxBrightnessMode = 0;

    @Number(
        name = "Brightness Value",
        min = 0,
        max = 100,
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxBrightnessValue = 100;

    @Number(
        name = "Brightness Offset",
        min = -100,
        max = 100,
        category = "Hitboxes & Nametags",
        subcategory = "Colour Adjustments"
    )
    public int hitboxBrightnessOffset = 0;

    @HUD(
        name = "Emerald Counter HUD",
        category = "HUD",
        subcategory = "General"
    )
    public EmeraldCounterHUD emeraldCounterHUD = new EmeraldCounterHUD();

    @HUD(
        name = "Diamond Counter HUD",
        category = "HUD",
        subcategory = "General"
    )
    public DiamondCounterHUD diamondCounterHUD = new DiamondCounterHUD();

    @HUD(
        name = "Upgrades & Traps HUD",
        category = "HUD",
        subcategory = "General"
    )
    public BedwarsUpgradesTrapsHUD upgradesTrapsHUD =
        new BedwarsUpgradesTrapsHUD();

    @Switch(
        description = "Automatically records Hypixel Bedwars sessions into offline replay files.",
        name = "Record Bedwars Replays",
        category = "Replays",
        subcategory = "Recording"
    )
    public boolean enableReplayRecording = false;

    @Switch(
        description = "Persists received chat messages alongside replay packets.",
        name = "Store Chat In Replays",
        category = "Replays",
        subcategory = "Recording"
    )
    public boolean recordChatInReplays = true;

    @Number(
        description = "When a recording is saved, oldest replays are deleted if this limit is exceeded. Set to 0 for unlimited.",
        name = "Max Stored Replays",
        min = 0,
        max = 500,
        step = 1,
        category = "Replays",
        subcategory = "Storage"
    )
    public int maxStoredReplays = 0;

    @Switch(
        name = "Enable Anticheat",
        category = "Anticheat",
        subcategory = "General"
    )
    public boolean anticheatEnabled = false;

    @Switch(
        name = "NoSlow Check",
        category = "Anticheat",
        subcategory = "Checks"
    )
    public boolean noSlowCheckEnabled = true;

    @Switch(
        name = "AutoBlock Check",
        category = "Anticheat",
        subcategory = "Checks"
    )
    public boolean autoBlockCheckEnabled = true;

    @Switch(
        name = "Eagle Check",
        category = "Anticheat",
        subcategory = "Checks"
    )
    public boolean eagleCheckEnabled = false;

    @Switch(
        name = "Scaffold Check",
        category = "Anticheat",
        subcategory = "Checks"
    )
    public boolean scaffoldCheckEnabled = false;

    @Switch(
        description = "Show detailed anticheat info (debug reason + VL) in alerts.",
        name = "Verbose Alerts",
        category = "Anticheat",
        subcategory = "Alert Settings"
    )
    public boolean anticheatVerbose = false;

    @Number(
        name = "Violation Level",
        min = 1,
        max = 100,
        category = "Anticheat",
        subcategory = "Alert Settings"
    )
    public int anticheatVl = 10;

    @Number(
        name = "Cooldown (seconds)",
        min = 1,
        max = 60,
        category = "Anticheat",
        subcategory = "Alert Settings"
    )
    public int anticheatCooldown = 5;

    @Text(
        description = "Used when Stats > General selects Hypixel Public API.",
        name = "Hypixel API Key",
        category = "API Keys",
        subcategory = "Hypixel",
        secure = true,
        multiline = false
    )
    public String hypixelApiKey = "";

    @Text(
        description = "Used by automatic number denicking and /denick. Aurora ping and hidden winstreaks do not need this key.",
        name = "Aurora API Key",
        placeholder = "Required only for player lookups",
        category = "API Keys",
        subcategory = "Aurora",
        secure = true,
        multiline = false
    )
    public String auroraApiKey = "";

    @Text(
        description = "Used when Stats > Ping selects Luna's API.",
        name = "Luna API Key",
        placeholder = "Enter your Luna API key",
        category = "API Keys",
        subcategory = "Luna",
        secure = true,
        multiline = false
    )
    public String lunaPingApiKey = "";

    @Text(
        description = "Used by Tags & Blacklists > Coral.",
        name = "Coral API Key",
        category = "API Keys",
        subcategory = "Coral",
        secure = true,
        multiline = false
    )
    // Keep the legacy field name so existing OneConfig profiles migrate in place.
    public String urchinKey = "";

    @Text(
        description = "Used by Tags & Blacklists > Xadia.",
        name = "Xadia API Key",
        category = "API Keys",
        subcategory = "Xadia",
        secure = true,
        multiline = false
    )
    public String xadiaKey = "";

    @Info(
        text = "Manage all service API keys here. Feature-specific toggles remain in their own categories.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "API Keys"
    )
    public static boolean ignoredApiKeysInfo;

    // Retain saved values for compatibility without exposing inactive features.
    public String seraphKey = "";

    public boolean seraph = false;

    public boolean showSeraphTagsInTab = true;

    public boolean showClientIconsInNametags = true;

    public int nametagClientIconPosition = 0;

    public String getCoralApiKey() {
        return urchinKey;
    }

    public boolean isCoralEnabled() {
        return urchin;
    }

    public boolean shouldShowCoralTagsInTab() {
        return showUrchinTagsInTab;
    }

    public MellowOneConfig() {
        super(new Mod(Mellow.NAME, ModType.HYPIXEL), Mellow.MODID + ".json");
        initialize();
        sanitizeDropdownIndexes();

        configureDependencies();
    }

    @Override
    public void load() {
        super.load();
        // Profile switches use load() too; migrate before the controls read dropdown indices.
        sanitizeDropdownIndexes();
    }

    private void configureDependencies() {
        dependOn("requestPopupsEnabled", "friendRequestPopupsEnabled", "partyInvitePopupsEnabled",
            "requestPopupSoundEnabled", "requestPopupPosition", "requestPopupDurationSeconds");
        dependOn("enableReplayRecording", "recordChatInReplays");
        dependOn("printStats", "tags");
        dependOn("tabStats", "showStarsWithBrackets", "showNickWithBrackets", "showRanksInGameTabStats",
            "extendedTabStatsView", "extendedTabStatsInLobbies", "extendedTabStatsHeaders", "extendedTabStatsShowHeads",
            "extendedTabStatsTeamColumnMode", "extendedTabStatsStripCombinedTeamPadding", "highlightTaggedPlayers",
            "showDot12", "showDot23", "showDot34",
            "showDot45", "showDot56", "showDot67", "showDot78", "showDot89", "showDot910",
            "showUrchinTagsInTab", "showXadiaTagsInTab");
        dependOn("extendedTabStatsView", "extendedTabStatsInLobbies", "extendedTabStatsHeaders",
            "extendedTabStatsShowHeads", "extendedTabStatsTeamColumnMode", "extendedTabStatsStripCombinedTeamPadding",
            "highlightTaggedPlayers");
        hideIf("extendedTabStatsStripCombinedTeamPadding",
            () -> extendedTabStatsTeamColumnMode != 0 && extendedTabStatsTeamColumnMode != 3);
        dependOn("partyBlacklistWarning", "partyBlacklistWarningShowTagDetails");
        dependOn("pregameStats", "autoLeaveBlacklistedPregameChat", "autoLeaveBlacklistedPregameCommand");
        dependOn("autoLeaveBlacklistedPregameChat", "autoLeaveBlacklistedPregameCommand");
        dependOn("urchin", "showUrchinTagsInTab");
        dependOn("xadia", "showXadiaTagsInTab", "xadiaVerifiedOnly");
        dependOn("showHiddenWinstreaks", "winstreakMinStars", "winstreakMinFkdr");
        dependOn("numberDenicker", "numberDenickerFuzzy", "minFinalsForDenick");
        dependOn("coloredHitboxes", "coloredHitboxesAffectVanillaDebug", "coloredHitboxesAffectPolyHitbox");
        dependOn("coloredNametagBackgrounds", "coloredNametagAffectPolyNametag");
        for (String prefix : new String[]{"customStat", "skywarsCustomStat", "duelsCustomStat"}) {
            for (int slot = 1; slot <= 10; slot++) addDependency(prefix + slot, "tabStats");
        }
        for (String component : new String[]{"Hue", "Saturation", "Brightness"}) {
            for (String suffix : new String[]{"Mode", "Value", "Offset"}) {
                addDependency("hitbox" + component + suffix, "Colored Hitboxes or Colored Nametag Backgrounds",
                    () -> coloredHitboxes || coloredNametagBackgrounds);
            }
        }
        hideIf("hitboxHueValue", () -> hitboxHueMode == 0);
        hideIf("hitboxHueOffset", () -> hitboxHueMode != 0);
        hideIf("hitboxSaturationValue", () -> hitboxSaturationMode == 0);
        hideIf("hitboxSaturationOffset", () -> hitboxSaturationMode != 0);
        hideIf("hitboxBrightnessValue", () -> hitboxBrightnessMode == 0);
        hideIf("hitboxBrightnessOffset", () -> hitboxBrightnessMode != 0);
        dependOn("anticheatEnabled", "noSlowCheckEnabled", "autoBlockCheckEnabled", "eagleCheckEnabled",
            "scaffoldCheckEnabled", "anticheatVerbose", "anticheatVl", "anticheatCooldown");
    }

    private void dependOn(String parent, String... children) {
        for (String child : children) addDependency(child, parent);
    }

    private void sanitizeDropdownIndexes() {
        // BedWars tab stats dropdowns: Team..Ping (14 options)
        customStat1 = statIndex(customStat1, 14, 10);
        customStat2 = statIndex(customStat2, 14, 10);
        customStat3 = statIndex(customStat3, 14, 10);
        customStat4 = statIndex(customStat4, 14, 10);
        customStat5 = statIndex(customStat5, 14, 10);
        customStat6 = statIndex(customStat6, 14, 10);
        customStat7 = statIndex(customStat7, 14, 10);
        customStat8 = statIndex(customStat8, 14, 10);
        customStat9 = statIndex(customStat9, 14, 10);
        customStat10 = statIndex(customStat10, 14, 10);

        // SkyWars tab stats dropdowns: Team..Ping (11 options)
        skywarsCustomStat1 = statIndex(skywarsCustomStat1, 11, 7);
        skywarsCustomStat2 = statIndex(skywarsCustomStat2, 11, 7);
        skywarsCustomStat3 = statIndex(skywarsCustomStat3, 11, 7);
        skywarsCustomStat4 = statIndex(skywarsCustomStat4, 11, 7);
        skywarsCustomStat5 = statIndex(skywarsCustomStat5, 11, 7);
        skywarsCustomStat6 = statIndex(skywarsCustomStat6, 11, 7);
        skywarsCustomStat7 = statIndex(skywarsCustomStat7, 11, 7);
        skywarsCustomStat8 = statIndex(skywarsCustomStat8, 11, 7);
        skywarsCustomStat9 = statIndex(skywarsCustomStat9, 11, 7);
        skywarsCustomStat10 = statIndex(skywarsCustomStat10, 11, 7);

        // Duels tab stats dropdowns: Team..Ping (14 options)
        duelsCustomStat1 = statIndex(duelsCustomStat1, 14, 10);
        duelsCustomStat2 = statIndex(duelsCustomStat2, 14, 10);
        duelsCustomStat3 = statIndex(duelsCustomStat3, 14, 10);
        duelsCustomStat4 = statIndex(duelsCustomStat4, 14, 10);
        duelsCustomStat5 = statIndex(duelsCustomStat5, 14, 10);
        duelsCustomStat6 = statIndex(duelsCustomStat6, 14, 10);
        duelsCustomStat7 = statIndex(duelsCustomStat7, 14, 10);
        duelsCustomStat8 = statIndex(duelsCustomStat8, 14, 10);
        duelsCustomStat9 = statIndex(duelsCustomStat9, 14, 10);
        duelsCustomStat10 = statIndex(duelsCustomStat10, 14, 10);

        // Misc dropdowns
        extendedTabStatsHeaders = clampIndex(extendedTabStatsHeaders, 3);
        extendedTabStatsTeamColumnMode = clampIndex(
            extendedTabStatsTeamColumnMode,
            4
        );
        requestPopupPosition = clampIndex(requestPopupPosition, 3);
        statsProvider = clampIndex(statsProvider, 5);
        inGameBlacklistWarningDestination = clampIndex(
            inGameBlacklistWarningDestination,
            3
        );
        pingProvider = pingProvider == 3 ? 0 : clampIndex(pingProvider, 3);
        winstreakMinStars = clampIndex(winstreakMinStars, 51);
        winstreakMinFkdr = clampIndex(winstreakMinFkdr, 18);
        finalsRange = clampIndex(finalsRange, 5);
        bedsRange = clampIndex(bedsRange, 5);
        maxResults = clampIndex(maxResults, 3);
        hitboxHueMode = clampIndex(hitboxHueMode, 2);
        hitboxSaturationMode = clampIndex(hitboxSaturationMode, 2);
        hitboxBrightnessMode = clampIndex(hitboxBrightnessMode, 2);
        nametagClientIconPosition = clampIndex(nametagClientIconPosition, 2);
    }

    private int statIndex(int value, int retiredClientIndex, int noneIndex) {
        return value == retiredClientIndex ? noneIndex : clampIndex(value, retiredClientIndex);
    }

    private int clampIndex(int value, int optionCount) {
        if (optionCount <= 0) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        int maxIndex = optionCount - 1;
        return value > maxIndex ? maxIndex : value;
    }
}
