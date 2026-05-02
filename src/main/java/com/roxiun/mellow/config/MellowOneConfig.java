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
    public boolean autoWho = false;

    @Switch(name = "Show Tab Stats", subcategory = "General")
    public boolean tabStats = true;

    @Switch(name = "Show Tags", subcategory = "General")
    public boolean tags = false;

    @Switch(name = "Print Stats to Chat", subcategory = "General")
    public boolean printStats = false;

    @Switch(name = "Auto Update Check", subcategory = "General")
    public boolean autoUpdateCheck = true;

    @Switch(
        name = "Record Bedwars Replays",
        category = "Replays",
        subcategory = "Recording",
        description = "Automatically records Hypixel Bedwars sessions into offline replay files."
    )
    public boolean enableReplayRecording = false;

    @Switch(
        name = "Store Chat In Replays",
        category = "Replays",
        subcategory = "Recording",
        description = "Persists received chat messages alongside replay packets."
    )
    public boolean recordChatInReplays = true;

    @Number(
        name = "Max Stored Replays",
        category = "Replays",
        subcategory = "Recording",
        description = "Oldest replays are deleted once this limit is exceeded. Set to 0 for unlimited.",
        min = 0,
        max = 500,
        step = 1
    )
    public int maxStoredReplays = 0;

    @Switch(
        name = "Request Popups",
        subcategory = "Requests",
        description = "Shows accept/deny popups for incoming friend requests and party invites."
    )
    public boolean requestPopupsEnabled = true;

    @Switch(
        name = "Friend Request Popups",
        subcategory = "Requests",
        description = "Shows popups for incoming friend requests."
    )
    public boolean friendRequestPopupsEnabled = true;

    @Switch(
        name = "Party Invite Popups",
        subcategory = "Requests",
        description = "Shows popups for incoming party invites."
    )
    public boolean partyInvitePopupsEnabled = true;

    @Switch(
        name = "Popup Sound",
        subcategory = "Requests",
        description = "Play a pling sound when a new request popup is received."
    )
    public boolean requestPopupSoundEnabled = true;

    @Dropdown(
        name = "Popup Position",
        subcategory = "Requests",
        options = { "Top-center", "Top-right", "Bottom-right" }
    )
    public int requestPopupPosition = 0;

    @Number(
        name = "Popup Duration (seconds)",
        subcategory = "Requests",
        min = 2,
        max = 30,
        step = 1
    )
    public int requestPopupDurationSeconds = 10;

    // Tab Stats Configuration

    @Switch(name = "Show Stars with Brackets", category = "Tab Stats")
    public boolean showStarsWithBrackets = true;

    @Switch(name = "Show Nick with Brackets", category = "Tab Stats")
    public boolean showNickWithBrackets = true;

    @Switch(name = "Extended Tab Stats View", category = "Tab Stats")
    public boolean extendedTabStatsView = true;

    @Switch(
        name = "Extended View In Lobbies",
        category = "Tab Stats",
        subcategory = "Extended View",
        description = "Allows Extended Tab Stats View while in game lobbies."
    )
    public boolean extendedTabStatsInLobbies = false;

    @Switch(
        name = "Extended View Player Heads",
        category = "Tab Stats",
        subcategory = "Extended View",
        description = "Shows player heads in the Name column when using Extended Tab Stats View."
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
        name = "Strip Team Padding",
        category = "Tab Stats",
        subcategory = "Extended View",
        description = "Collapses extra whitespace when Team is combined with Name or Stars."
    )
    public boolean extendedTabStatsStripCombinedTeamPadding = true;

    @Switch(
        name = "Show Ranks In-Game",
        category = "Tab Stats",
        description = "When enabled, Name stat includes rank prefix during games. Lobbies always show rank."
    )
    public boolean showRanksInGameTabStats = false;

    @Switch(name = "Highlight Tagged Players", category = "Tab Stats")
    public boolean highlightTaggedPlayers = false;

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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
        },
        category = "Tab Stats"
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
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
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
            "HP",
            "Tags",
            "Ping",
            "Client",
        },
        category = "Tab Stats"
    )
    public int customStat10 = 10; // None by default

    @Info(
        text = "Set the order of SkyWars stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat10 = 7;

    @Info(
        text = "Set the order of Duels stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
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
            "Client",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat10 = 11; // HP by default

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
        options = { "Hypixel Public API", "Nadeshiko", "Abyss" },
        subcategory = "Stats"
    )
    public int statsProvider = 2;

    @Info(
        text = "Hypixel provider requires an API key from developer.hypixel.net. Configure it in API Keys > Hypixel. Other providers do not require a key.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        subcategory = "Stats"
    )
    public static boolean ignoredHypixelApiInfo;

    @Info(
        text = "Manage all service API keys here. Feature-specific toggles remain in their own categories.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "API Keys"
    )
    public static boolean ignoredApiKeysInfo;

    @Text(
        name = "Hypixel API Key",
        category = "API Keys",
        subcategory = "Hypixel",
        secure = true,
        multiline = false
    )
    public String hypixelApiKey = "";

    @Text(
        name = "Aurora API Key",
        placeholder = "Enter your Aurora API key",
        category = "API Keys",
        subcategory = "Aurora",
        secure = true,
        multiline = false
    )
    public String auroraApiKey = "";

    @Text(
        name = "Frosty API Key",
        placeholder = "Enter your Frosty API key",
        category = "API Keys",
        subcategory = "Frosty",
        secure = true,
        multiline = false
    )
    public String frostyApiKey = "";

    @Text(
        name = "Luna API Key",
        placeholder = "Enter your Luna API key",
        category = "API Keys",
        subcategory = "Luna",
        secure = true,
        multiline = false
    )
    public String lunaPingApiKey = "";

    @Text(
        name = "Urchin API Key",
        category = "API Keys",
        subcategory = "Urchin",
        secure = true,
        multiline = false
    )
    public String urchinKey = "";

    @Text(
        name = "Seraph API Key",
        category = "API Keys",
        subcategory = "Seraph",
        secure = true,
        multiline = false
    )
    public String seraphKey = "";

    @Switch(name = "Print Blacklist Tags", subcategory = "General")
    public boolean printBlacklistTags = true;

    @Dropdown(
        name = "Blacklist Warn Destination",
        subcategory = "General",
        options = { "None", "All Chat", "Party Chat" },
        description = "When blacklisted BedWars opponents are detected in-game, route warning messages to this chat channel."
    )
    public int inGameBlacklistWarningDestination = 0;

    @Switch(name = "Auto Pregame Stats", subcategory = "Pregame")
    public boolean pregameStats = true;

    @Switch(
        name = "Mention Stats in BedWars Lobbies",
        subcategory = "Pregame",
        description = "Show sender stats when they mention your username in BedWars lobby chat."
    )
    public boolean mentionLobbyStats = false;

    @Switch(
        name = "Warn for Blacklisted Party Members",
        subcategory = "Pregame",
        description = "Shows a warning when your Hypixel party contains one or more blacklisted players."
    )
    public boolean partyBlacklistWarning = true;

    @Switch(
        name = "Show Party Blacklist Tag Details",
        subcategory = "Pregame",
        description = "When warning about flagged party members, also print what they are tagged for."
    )
    public boolean partyBlacklistWarningShowTagDetails = false;

    @Switch(
        name = "Auto Leave on Blacklisted Chat",
        subcategory = "Pregame",
        description = "Automatically runs /lobby in BedWars pregame when a blacklisted chatter is detected and more than 2 seconds remain."
    )
    public boolean autoLeaveBlacklistedPregameChat = false;

    @Text(
        name = "Auto Leave Command",
        subcategory = "Pregame",
        multiline = false
    )
    public String autoLeaveBlacklistedPregameCommand = "/lobby";

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
        text = "Enabling Urchin will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Urchin API Key, Game Info). Configure the key in API Keys > Urchin.",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Urchin"
    )
    public static boolean ignoredUrchinWarning;

    // @Info(
    //     text = "Urchin does not require a key to view tags, these settings are deprecated",
    //     size = OptionSize.DUAL,
    //     type = InfoType.INFO,
    //     category = "Urchin"
    // )
    // public static boolean ignoredUrchinDeprecated;

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
        text = "Enabling Seraph will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Seraph API Key, Game Info). Configure the key in API Keys > Seraph.",
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

    // Winstreaks Configs
    @Info(
        text = "Shows hidden or zero BedWars winstreaks fetched from the Bordic Aurora API",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Winstreaks"
    )
    public static boolean ignoredWinstreaksDescription;

    @Switch(name = "Show Hidden Winstreaks", category = "Winstreaks")
    public boolean showHiddenWinstreaks = false;

    @Info(
        text = "When hidden winstreaks are enabled, Mellow only uses Aurora when the visible BedWars winstreak is missing or hidden.",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Winstreaks"
    )
    public static boolean ignoredWinstreaksVisibleFirstInfo;

    @Info(
        text = "Enabling this will send requests to Bordic and be subject to their ToS, this could enable tracking of your data (IP, Aurora API Key, Game Info). Configure the key in API Keys > Aurora.",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Winstreaks"
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
        category = "Winstreaks"
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
        category = "Winstreaks"
    )
    public int winstreakMinFkdr = 0;

    @Switch(
        name = "Use Luna's API for Ping",
        category = "Winstreaks",
        description = "When hidden winstreaks are enabled, use Luna to keep the Ping column populated."
    )
    public boolean useLunaPingForWinstreaks = false;

    // Ping Configs
    @Dropdown(
        name = "Ping Provider",
        category = "Ping",
        options = { "None", "Aurora API", "Luna's API", "Seraph API" }
    )
    public int pingProvider = 0;

    @Info(
        text = "Aurora API provides historical ping averages per player UUID. Configure the key in API Keys > Aurora.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Ping"
    )
    public static boolean ignoredAuroraPingInfo;

    @Info(
        text = "Luna's API provides ping averages per player UUID. Configure the key in API Keys > Luna.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Ping"
    )
    public static boolean ignoredLunaPingInfo;

    @Info(
        text = "Seraph API provides the latest recorded ping per player UUID. Configure the key in API Keys > Seraph.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Ping"
    )
    public static boolean ignoredSeraphPingInfo;

    @Info(
            text = "This module attempts to denick players based the number of finals and beds broken from chat messages. Configure the keys in API Keys > Aurora / Frosty.",
            type = InfoType.INFO,
            size = OptionSize.DUAL,
            category = "Number Denicker",
            subcategory = "General"
    )
    public static boolean ignoredNumberDenickerInfo;

    @Switch(name = "Enable Number Denicker", category = "Number Denicker", subcategory = "General")
    public boolean numberDenicker = false;

    @Dropdown(
        name = "Provider",
        options = { "Aurora", "Frosty" },
        category = "Number Denicker",
        subcategory = "General"
    )
    public int numberDenickerProvider = 0; // 0 = Aurora, 1 = Frosty

    @Number(
        name = "Minimum Finals to Check",
        category = "Number Denicker",
        subcategory = "General",
        min = 0,
        max = 500000,
        step = 1000
    )
    public int minFinalsForDenick = 15000;

    @Button(
            name = "Run /generate-key on the bot to get your key. You will need to be whitelisted for it.",
            text = "Discord Server",
            size = OptionSize.DUAL,
            category = "Number Denicker",
            subcategory = "Frosty"
    )
    Runnable frostyLinkButton = () -> {
        NetworkUtils.browseLink(
                "https://discord.gg/JwvA3GeDtA"
        );
    };

    @Button(
        name = "Run /api view on the bot to get your key",
        text = "Discord Bot",
        size = OptionSize.DUAL,
        category = "Number Denicker",
        subcategory = "Aurora"
    )
    Runnable auroraLinkButton = () -> {
        NetworkUtils.browseLink(
            "https://discord.com/oauth2/authorize?client_id=1244205279697174539"
        );
    };

    @Switch(name = "Print all potential players", category = "Number Denicker", subcategory = "Aurora")
    public boolean numberDenickerFuzzy = true;

    @Info(
        text = "Turning all potential players off, will only print players with both matching beds and finals.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Number Denicker",
        subcategory = "Aurora"
    )
    public static boolean ignoredNumberDenickerFuzzyInfo;

    @Dropdown(
        name = "Finals Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker",
        subcategory = "Aurora"
    )
    public int finalsRange = 3; // Index for 100

    @Dropdown(
        name = "Beds Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker",
        subcategory = "Aurora"
    )
    public int bedsRange = 1; // Index for 50

    @Dropdown(
        name = "Max Results",
        options = { "5", "10", "20" },
        category = "Number Denicker",
        subcategory = "Aurora"
    )
    public int maxResults = 0; // Index for 5

    // Hitboxes
    @Switch(name = "Colored Hitboxes", category = "Hitboxes")
    public boolean coloredHitboxes = true;

    @Switch(name = "Affect Vanilla F3+B", category = "Hitboxes")
    public boolean coloredHitboxesAffectVanillaDebug = true;

    @Switch(name = "Affect PolyHitbox", category = "Hitboxes")
    public boolean coloredHitboxesAffectPolyHitbox = true;

    @Switch(name = "Colored Nametag Backgrounds", category = "Hitboxes")
    public boolean coloredNametagBackgrounds = false;

    @Switch(name = "Affect PolyNametag", category = "Hitboxes")
    public boolean coloredNametagAffectPolyNametag = true;

    @Switch(
        name = "Show Client Icons In Nametags",
        category = "Hitboxes",
        description = "Shows a Seraph-detected client icon next to in-world player nametags."
    )
    public boolean showClientIconsInNametags = true;

    @Dropdown(
        name = "Nametag Client Icon Position",
        options = { "Left", "Right" },
        category = "Hitboxes"
    )
    public int nametagClientIconPosition = 0;

    @Dropdown(
        name = "Hue Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Hue"
    )
    public int hitboxHueMode = 0;

    @Number(
        name = "Hue Value",
        category = "Hitboxes",
        subcategory = "Hue",
        min = 0,
        max = 360
    )
    public int hitboxHueValue = 0;

    @Number(
        name = "Hue Offset",
        category = "Hitboxes",
        subcategory = "Hue",
        min = -180,
        max = 180
    )
    public int hitboxHueOffset = 0;

    @Dropdown(
        name = "Saturation Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Saturation"
    )
    public int hitboxSaturationMode = 0;

    @Number(
        name = "Saturation Value",
        category = "Hitboxes",
        subcategory = "Saturation",
        min = 0,
        max = 100
    )
    public int hitboxSaturationValue = 100;

    @Number(
        name = "Saturation Offset",
        category = "Hitboxes",
        subcategory = "Saturation",
        min = -100,
        max = 100
    )
    public int hitboxSaturationOffset = 0;

    @Dropdown(
        name = "Brightness Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Brightness"
    )
    public int hitboxBrightnessMode = 0;

    @Number(
        name = "Brightness Value",
        category = "Hitboxes",
        subcategory = "Brightness",
        min = 0,
        max = 100
    )
    public int hitboxBrightnessValue = 100;

    @Number(
        name = "Brightness Offset",
        category = "Hitboxes",
        subcategory = "Brightness",
        min = -100,
        max = 100
    )
    public int hitboxBrightnessOffset = 0;

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

    @Switch(
        name = "Verbose Alerts",
        category = "Anticheat",
        description = "Show detailed anticheat info (debug reason + VL) in alerts."
    )
    public boolean anticheatVerbose = false;

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
        sanitizeDropdownIndexes();

        hideIf("hitboxHueValue", () -> hitboxHueMode == 0);
        hideIf("hitboxHueOffset", () -> hitboxHueMode != 0);
        hideIf("hitboxSaturationValue", () -> hitboxSaturationMode == 0);
        hideIf("hitboxSaturationOffset", () -> hitboxSaturationMode != 0);
        hideIf("hitboxBrightnessValue", () -> hitboxBrightnessMode == 0);
        hideIf("hitboxBrightnessOffset", () -> hitboxBrightnessMode != 0);
    }

    public int getAuroraDenickRange(String type) {
        int[] rangeValues = { 0, 50, 100, 200, 500, 1000 };
        int rangeIndex = "finals".equalsIgnoreCase(type)
                ? finalsRange
                : bedsRange;
        return rangeValues[clampIndex(rangeIndex, rangeValues.length)];
    }

    public int getAuroraDenickMaxResults() {
        int[] maxValues = { 5, 10, 20 };
        return maxValues[clampIndex(maxResults, maxValues.length)];
    }

    private void sanitizeDropdownIndexes() {
        // BedWars tab stats dropdowns: Team..Client (15 options)
        customStat1 = clampIndex(customStat1, 15);
        customStat2 = clampIndex(customStat2, 15);
        customStat3 = clampIndex(customStat3, 15);
        customStat4 = clampIndex(customStat4, 15);
        customStat5 = clampIndex(customStat5, 15);
        customStat6 = clampIndex(customStat6, 15);
        customStat7 = clampIndex(customStat7, 15);
        customStat8 = clampIndex(customStat8, 15);
        customStat9 = clampIndex(customStat9, 15);
        customStat10 = clampIndex(customStat10, 15);

        // SkyWars tab stats dropdowns: Team..Client (12 options)
        skywarsCustomStat1 = clampIndex(skywarsCustomStat1, 12);
        skywarsCustomStat2 = clampIndex(skywarsCustomStat2, 12);
        skywarsCustomStat3 = clampIndex(skywarsCustomStat3, 12);
        skywarsCustomStat4 = clampIndex(skywarsCustomStat4, 12);
        skywarsCustomStat5 = clampIndex(skywarsCustomStat5, 12);
        skywarsCustomStat6 = clampIndex(skywarsCustomStat6, 12);
        skywarsCustomStat7 = clampIndex(skywarsCustomStat7, 12);
        skywarsCustomStat8 = clampIndex(skywarsCustomStat8, 12);
        skywarsCustomStat9 = clampIndex(skywarsCustomStat9, 12);
        skywarsCustomStat10 = clampIndex(skywarsCustomStat10, 12);

        // Duels tab stats dropdowns: Team..Client (15 options)
        duelsCustomStat1 = clampIndex(duelsCustomStat1, 15);
        duelsCustomStat2 = clampIndex(duelsCustomStat2, 15);
        duelsCustomStat3 = clampIndex(duelsCustomStat3, 15);
        duelsCustomStat4 = clampIndex(duelsCustomStat4, 15);
        duelsCustomStat5 = clampIndex(duelsCustomStat5, 15);
        duelsCustomStat6 = clampIndex(duelsCustomStat6, 15);
        duelsCustomStat7 = clampIndex(duelsCustomStat7, 15);
        duelsCustomStat8 = clampIndex(duelsCustomStat8, 15);
        duelsCustomStat9 = clampIndex(duelsCustomStat9, 15);
        duelsCustomStat10 = clampIndex(duelsCustomStat10, 15);

        // Misc dropdowns
        extendedTabStatsTeamColumnMode = clampIndex(
            extendedTabStatsTeamColumnMode,
            4
        );
        requestPopupPosition = clampIndex(requestPopupPosition, 3);
        statsProvider = clampIndex(statsProvider, 3);
        inGameBlacklistWarningDestination = clampIndex(
            inGameBlacklistWarningDestination,
            3
        );
        pingProvider = clampIndex(pingProvider, 4);
        numberDenickerProvider = clampIndex(numberDenickerProvider, 2);
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
