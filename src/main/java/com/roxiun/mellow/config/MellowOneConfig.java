package com.roxiun.mellow.config;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Button;
import org.polyfrost.oneconfig.api.config.v1.annotations.Checkbox;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Info;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.hud.BedwarsUpgradesTrapsHUD;
import com.roxiun.mellow.hud.DiamondCounterHUD;
import com.roxiun.mellow.hud.EmeraldCounterHUD;

public class MellowOneConfig extends Config {

    @Switch(title = "Auto /who", subcategory = "General")
    public boolean autoWho = false;

    @Switch(title = "Show Tab Stats", subcategory = "General")
    public boolean tabStats = true;

    @Switch(title = "Show Tags", subcategory = "General")
    public boolean tags = false;

    @Switch(title = "Print Stats to Chat", subcategory = "General")
    public boolean printStats = false;

    @Switch(title = "Auto Update Check", subcategory = "General")
    public boolean autoUpdateCheck = true;

    @Switch(
        title = "Record Bedwars Replays",
        category = "Replays",
        subcategory = "Recording",
        description = "Automatically records Hypixel Bedwars sessions into offline replay files."
    )
    public boolean enableReplayRecording = false;

    @Switch(
        title = "Store Chat In Replays",
        category = "Replays",
        subcategory = "Recording",
        description = "Persists received chat messages alongside replay packets."
    )
    public boolean recordChatInReplays = true;

    @Number(
        title = "Max Stored Replays",
        category = "Replays",
        subcategory = "Recording",
        description = "Oldest replays are deleted once this limit is exceeded. Set to 0 for unlimited.",
        min = 0,
        max = 500
    )
    public int maxStoredReplays = 0;

    @Switch(
        title = "Request Popups",
        subcategory = "Requests",
        description = "Shows accept/deny popups for incoming friend requests and party invites."
    )
    public boolean requestPopupsEnabled = true;

    @Switch(
        title = "Friend Request Popups",
        subcategory = "Requests",
        description = "Shows popups for incoming friend requests."
    )
    public boolean friendRequestPopupsEnabled = true;

    @Switch(
        title = "Party Invite Popups",
        subcategory = "Requests",
        description = "Shows popups for incoming party invites."
    )
    public boolean partyInvitePopupsEnabled = true;

    @Switch(
        title = "Popup Sound",
        subcategory = "Requests",
        description = "Play a pling sound when a new request popup is received."
    )
    public boolean requestPopupSoundEnabled = true;

    @Dropdown(
        title = "Popup Position",
        subcategory = "Requests",
        options = { "Top-center", "Top-right", "Bottom-right" }
    )
    public int requestPopupPosition = 0;

    @Number(
        title = "Popup Duration (seconds)",
        subcategory = "Requests",
        min = 2,
        max = 30
    )
    public int requestPopupDurationSeconds = 10;

    // Tab Stats Configuration

    @Switch(title = "Show Stars with Brackets", category = "Tab Stats")
    public boolean showStarsWithBrackets = true;

    @Switch(title = "Show Nick with Brackets", category = "Tab Stats")
    public boolean showNickWithBrackets = true;

    @Switch(title = "Extended Tab Stats View", category = "Tab Stats")
    public boolean extendedTabStatsView = true;

    @Switch(
        title = "Extended View In Lobbies",
        category = "Tab Stats",
        subcategory = "Extended View",
        description = "Allows Extended Tab Stats View while in game lobbies."
    )
    public boolean extendedTabStatsInLobbies = false;

    @Switch(
        title = "Extended View Player Heads",
        category = "Tab Stats",
        subcategory = "Extended View",
        description = "Shows player heads in the Name column when using Extended Tab Stats View."
    )
    public boolean extendedTabStatsShowHeads = true;

    @Dropdown(
        title = "Extended Team Column Mode",
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
        title = "Strip Team Padding",
        category = "Tab Stats",
        subcategory = "Extended View",
        description = "Collapses extra whitespace when Team is combined with Name or Stars."
    )
    public boolean extendedTabStatsStripCombinedTeamPadding = true;

    @Switch(
        title = "Show Ranks In-Game",
        category = "Tab Stats",
        description = "When enabled, Name stat includes rank prefix during games. Lobbies always show rank."
    )
    public boolean showRanksInGameTabStats = false;

    @Switch(title = "Highlight Tagged Players", category = "Tab Stats")
    public boolean highlightTaggedPlayers = false;

    @Info(
        description = "Set the order of stats in the tab list",
        category = "Tab Stats"
    )
    public static boolean ignoredStatsOrderInfo;

    @Dropdown(
        title = "First Stat",
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
        title = "Second Stat",
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
        title = "Third Stat",
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
        title = "Fourth Stat",
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
        title = "Fifth Stat",
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
        title = "Sixth Stat",
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
        title = "Seventh Stat",
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
        title = "Eighth Stat",
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
        title = "Ninth Stat",
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
        title = "Tenth Stat",
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
        description = "Set the order of SkyWars stats in the tab list",
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public static boolean ignoredSkywarsStatsOrderInfo;

    @Dropdown(
        title = "First SkyWars Stat",
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
        title = "Second SkyWars Stat",
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
        title = "Third SkyWars Stat",
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
        title = "Fourth SkyWars Stat",
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
        title = "Fifth SkyWars Stat",
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
        title = "Sixth SkyWars Stat",
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
        title = "Seventh SkyWars Stat",
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
        title = "Eighth SkyWars Stat",
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
        title = "Ninth SkyWars Stat",
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
        title = "Tenth SkyWars Stat",
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
        description = "Set the order of Duels stats in the tab list",
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public static boolean ignoredDuelsStatsOrderInfo;

    @Dropdown(
        title = "First Duels Stat",
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
        title = "Second Duels Stat",
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
        title = "Third Duels Stat",
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
        title = "Fourth Duels Stat",
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
        title = "Fifth Duels Stat",
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
        title = "Sixth Duels Stat",
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
        title = "Seventh Duels Stat",
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
        title = "Eighth Duels Stat",
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
        title = "Ninth Duels Stat",
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
        title = "Tenth Duels Stat",
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
        description = "Toggle seperator between stats",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public static boolean ignoredDotsInfo;

    @Checkbox(
        title = "Between 1st and 2nd",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot12 = false;

    @Checkbox(
        title = "Between 2nd and 3rd",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot23 = false;

    @Checkbox(
        title = "Between 3rd and 4th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot34 = true;

    @Checkbox(
        title = "Between 4th and 5th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot45 = true;

    @Checkbox(
        title = "Between 5th and 6th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot56 = true;

    @Checkbox(
        title = "Between 6th and 7th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot67 = true;

    @Checkbox(
        title = "Between 7th and 8th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot78 = true;

    @Checkbox(
        title = "Between 8th and 9th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot89 = true;

    @Checkbox(
        title = "Between 9th and 10th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot910 = true;

    public EmeraldCounterHUD emeraldCounterHUD = new EmeraldCounterHUD();

    public DiamondCounterHUD diamondCounterHUD = new DiamondCounterHUD();

    public BedwarsUpgradesTrapsHUD upgradesTrapsHUD =
        new BedwarsUpgradesTrapsHUD();

    @Number(
        title = "Minimum FKDR to show",
        min = -1,
        max = 500,
        subcategory = "General"
    )
    public int minFkdr = -1;

    @Dropdown(
        title = "Stats Provider",
        options = { "Hypixel Public API", "Nadeshiko", "Abyss", "Bordic" },
        subcategory = "Stats"
    )
    public int statsProvider = 2;

    @Info(
        description = "Hypixel provider requires an API key from developer.hypixel.net. Configure it in API Keys > Hypixel. Other providers do not require a key.",
        subcategory = "Stats"
    )
    public static boolean ignoredHypixelApiInfo;

    @Info(
        description = "Manage all service API keys here. Feature-specific toggles remain in their own categories.",
        category = "API Keys"
    )
    public static boolean ignoredApiKeysInfo;

    @org.polyfrost.oneconfig.api.config.v1.annotations.Include
    public String hypixelApiKey = "";

    @org.polyfrost.oneconfig.api.config.v1.annotations.Include
    public String auroraApiKey = "";

    @org.polyfrost.oneconfig.api.config.v1.annotations.Include
    public String lunaPingApiKey = "";

    @org.polyfrost.oneconfig.api.config.v1.annotations.Include
    // Keep the legacy field name so existing OneConfig profiles migrate in place.
    public String urchinKey = "";

    @org.polyfrost.oneconfig.api.config.v1.annotations.Include
    public String seraphKey = "";

    @Switch(title = "Print Blacklist Tags", subcategory = "General")
    public boolean printBlacklistTags = true;

    @Dropdown(
        title = "Blacklist Warn Destination",
        subcategory = "General",
        options = { "None", "All Chat", "Party Chat" },
        description = "When blacklisted BedWars opponents are detected in-game, route warning messages to this chat channel."
    )
    public int inGameBlacklistWarningDestination = 0;

    @Switch(title = "Auto Pregame Stats", subcategory = "Pregame")
    public boolean pregameStats = true;

    @Switch(
        title = "Mention Stats in BedWars Lobbies",
        subcategory = "Pregame",
        description = "Show sender stats when they mention your username in BedWars lobby chat."
    )
    public boolean mentionLobbyStats = false;

    @Switch(
        title = "Warn for Blacklisted Party Members",
        subcategory = "Pregame",
        description = "Shows a warning when your Hypixel party contains one or more blacklisted players."
    )
    public boolean partyBlacklistWarning = true;

    @Switch(
        title = "Show Party Blacklist Tag Details",
        subcategory = "Pregame",
        description = "When warning about flagged party members, also print what they are tagged for."
    )
    public boolean partyBlacklistWarningShowTagDetails = false;

    @Switch(
        title = "Auto Leave on Blacklisted Chat",
        subcategory = "Pregame",
        description = "Automatically runs /lobby in BedWars pregame when a blacklisted chatter is detected and more than 2 seconds remain."
    )
    public boolean autoLeaveBlacklistedPregameChat = false;

    @Text(
        title = "Auto Leave Command",
        subcategory = "Pregame",
        multiline = false
    )
    public String autoLeaveBlacklistedPregameCommand = "/lobby";

    @Switch(title = "Auto Skin Denicker", subcategory = "Denicker")
    public boolean autoSkinDenick = true;

    // Coral Configs
    @Info(
        description = "Coral is a community blacklist, allowing you to see potential cheaters in your game",
        category = "Coral"
    )
    public static boolean ignoredCoralDescription;

    @Switch(title = "Enable Coral", category = "Coral")
    // Keep the legacy field name so existing OneConfig profiles migrate in place.
    public boolean urchin = false;

    @Switch(title = "Show Coral Tags in Tab", category = "Coral")
    // Keep the legacy field name so existing OneConfig profiles migrate in place.
    public boolean showUrchinTagsInTab = true;

    @Info(
        description = "Coral requires an API key. Enabling it sends player identifiers to api.urchin.gg and is subject to their ToS. Configure the key in API Keys > Coral.",
        category = "Coral"
    )
    public static boolean ignoredCoralWarning;

    public String getCoralApiKey() {
        return urchinKey;
    }

    public boolean isCoralEnabled() {
        return urchin;
    }

    public boolean shouldShowCoralTagsInTab() {
        return showUrchinTagsInTab;
    }

    // Seraph Configs
    @Info(
        description = "Seraph is a community blacklist, allowing you to see potential cheaters in your game",
        category = "Seraph"
    )
    public static boolean ignoredSeraphDescription;

    @Switch(title = "Enable Seraph", category = "Seraph")
    public boolean seraph = false;

    @Switch(title = "Show Seraph Tags in Tab", category = "Seraph")
    public boolean showSeraphTagsInTab = true;

    @Info(
        description = "Enabling Seraph will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Seraph API Key, Game Info). Configure the key in API Keys > Seraph.",
        category = "Seraph"
    )
    public static boolean ignoredSeraphWarning;

    @Info(
        description = "Seraph does not require a key to view any tags older than 1 week old",
        category = "Seraph"
    )
    public static boolean ignoredSeraphInfo;

    // Winstreaks Configs
    @Info(
        description = "Shows hidden or zero BedWars winstreaks fetched from the Bordic Aurora API",
        category = "Winstreaks"
    )
    public static boolean ignoredWinstreaksDescription;

    @Switch(title = "Show Hidden Winstreaks", category = "Winstreaks")
    public boolean showHiddenWinstreaks = true;

    @Info(
        description = "When hidden winstreaks are enabled, Mellow only uses Aurora when the visible BedWars winstreak is missing or hidden.",
        category = "Winstreaks"
    )
    public static boolean ignoredWinstreaksVisibleFirstInfo;

    @Info(
        description = "Enabling this sends player UUIDs to Bordic and is subject to their ToS. Hidden winstreak lookups do not require or send an Aurora API key.",
        category = "Winstreaks"
    )
    public static boolean ignoredWinstreaksWarning;

    @Dropdown(
        title = "Minimum Stars to Fetch WS",
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
        title = "Minimum FKDR to Fetch WS",
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

    // Ping Configs
    @Dropdown(
        title = "Ping Provider",
        category = "Ping",
        options = { "None", "Aurora API", "Luna's API", "Seraph API" }
    )
    public int pingProvider = 1;

    @Info(
        description = "Aurora API provides historical ping averages per player UUID without requiring or sending an API key.",
        category = "Ping"
    )
    public static boolean ignoredAuroraPingInfo;

    @Info(
        description = "Luna's API provides ping averages per player UUID. Configure the key in API Keys > Luna.",
        category = "Ping"
    )
    public static boolean ignoredLunaPingInfo;

    @Info(
        description = "Seraph API provides the latest recorded ping per player UUID. Configure the key in API Keys > Seraph.",
        category = "Ping"
    )
    public static boolean ignoredSeraphPingInfo;

    // Number denicker
    @Info(
        description = "This module attempts to denick players based the number of finals and beds broken from chat messages. Configure the Aurora key in API Keys > Aurora.",
        category = "Number Denicker"
    )
    public static boolean ignoredNumberDenickerInfo; // Useless. Java limitations with @annotation.

    @Button(
        title = "Run /api view on the bot to get your key",
        text = "Discord Bot",
        category = "Number Denicker"
    )
    public void auroraLinkButton() {
        com.roxiun.mellow.platform.DesktopLinks.open(
            "https://discord.com/oauth2/authorize?client_id=1244205279697174539"
        );
    }

    @Switch(title = "Enable Number Denicker", category = "Number Denicker")
    public boolean numberDenicker = false;

    @Switch(title = "Print all potential players", category = "Number Denicker")
    public boolean numberDenickerFuzzy = true;

    @Info(
        description = "Turning all potential players off, will only print players with both matching beds and finals.",
        category = "Number Denicker"
    )
    public static boolean ignoredNumberDenickerFuzzyInfo;

    @Dropdown(
        title = "Finals Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker"
    )
    public int finalsRange = 3; // Index for 100

    @Dropdown(
        title = "Beds Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker"
    )
    public int bedsRange = 1; // Index for 50

    @Number(
        title = "Minimum Finals to Check",
        category = "Number Denicker",
        min = 0,
        max = 500000
    )
    public int minFinalsForDenick = 15000;

    @Dropdown(
        title = "Max Results",
        options = { "5", "10", "20" },
        category = "Number Denicker"
    )
    public int maxResults = 0; // Index for 5

    // Hitboxes
    @Switch(title = "Colored Hitboxes", category = "Hitboxes")
    public boolean coloredHitboxes = true;

    @Switch(title = "Affect Vanilla F3+B", category = "Hitboxes")
    public boolean coloredHitboxesAffectVanillaDebug = true;

    @Switch(title = "Affect PolyHitbox", category = "Hitboxes")
    public boolean coloredHitboxesAffectPolyHitbox = true;

    @Switch(title = "Colored Nametag Backgrounds", category = "Hitboxes")
    public boolean coloredNametagBackgrounds = false;

    @Switch(title = "Affect PolyNametag", category = "Hitboxes")
    public boolean coloredNametagAffectPolyNametag = true;

    @Switch(
        title = "Show Client Icons In Nametags",
        category = "Hitboxes",
        description = "Shows a Seraph-detected client icon next to in-world player nametags."
    )
    public boolean showClientIconsInNametags = true;

    @Dropdown(
        title = "Nametag Client Icon Position",
        options = { "Left", "Right" },
        category = "Hitboxes"
    )
    public int nametagClientIconPosition = 0;

    @Dropdown(
        title = "Hue Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Hue"
    )
    public int hitboxHueMode = 0;

    @Number(
        title = "Hue Value",
        category = "Hitboxes",
        subcategory = "Hue",
        min = 0,
        max = 360
    )
    public int hitboxHueValue = 0;

    @Number(
        title = "Hue Offset",
        category = "Hitboxes",
        subcategory = "Hue",
        min = -180,
        max = 180
    )
    public int hitboxHueOffset = 0;

    @Dropdown(
        title = "Saturation Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Saturation"
    )
    public int hitboxSaturationMode = 0;

    @Number(
        title = "Saturation Value",
        category = "Hitboxes",
        subcategory = "Saturation",
        min = 0,
        max = 100
    )
    public int hitboxSaturationValue = 100;

    @Number(
        title = "Saturation Offset",
        category = "Hitboxes",
        subcategory = "Saturation",
        min = -100,
        max = 100
    )
    public int hitboxSaturationOffset = 0;

    @Dropdown(
        title = "Brightness Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Brightness"
    )
    public int hitboxBrightnessMode = 0;

    @Number(
        title = "Brightness Value",
        category = "Hitboxes",
        subcategory = "Brightness",
        min = 0,
        max = 100
    )
    public int hitboxBrightnessValue = 100;

    @Number(
        title = "Brightness Offset",
        category = "Hitboxes",
        subcategory = "Brightness",
        min = -100,
        max = 100
    )
    public int hitboxBrightnessOffset = 0;

    @Switch(title = "Enable Anticheat", category = "Anticheat")
    public boolean anticheatEnabled = false;

    @Switch(title = "NoSlow Check", category = "Anticheat")
    public boolean noSlowCheckEnabled = true;

    @Switch(title = "AutoBlock Check", category = "Anticheat")
    public boolean autoBlockCheckEnabled = true;

    @Switch(title = "Eagle Check", category = "Anticheat")
    public boolean eagleCheckEnabled = false;

    @Switch(title = "Scaffold Check", category = "Anticheat")
    public boolean scaffoldCheckEnabled = false;

    @Switch(
        title = "Verbose Alerts",
        category = "Anticheat",
        description = "Show detailed anticheat info (debug reason + VL) in alerts."
    )
    public boolean anticheatVerbose = false;

    @Number(
        title = "Violation Level",
        category = "Anticheat",
        min = 1,
        max = 100
    )
    public int anticheatVl = 10;

    @Number(
        title = "Cooldown (seconds)",
        category = "Anticheat",
        min = 1,
        max = 60
    )
    public int anticheatCooldown = 5;

    public MellowOneConfig() {
        super("mellow-v1.json", Mellow.NAME, Category.HYPIXEL);
        boolean alreadySaved = java.nio.file.Files.isRegularFile(
            org.polyfrost.oneconfig.api.config.v1.ConfigManager.active().getFolder().resolve(id));
        initialize(false);
        boolean migrated = LegacyConfigMigration.importIfNeeded(this, alreadySaved);
        sanitizeDropdownIndexes();
        if (migrated) save();
        org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.register(emeraldCounterHUD, id);
        org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.register(diamondCounterHUD, id);
        org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.register(upgradesTrapsHUD, id);


        hideIf("hitboxHueValue", () -> hitboxHueMode == 0);
        hideIf("hitboxHueOffset", () -> hitboxHueMode != 0);
        hideIf("hitboxSaturationValue", () -> hitboxSaturationMode == 0);
        hideIf("hitboxSaturationOffset", () -> hitboxSaturationMode != 0);
        hideIf("hitboxBrightnessValue", () -> hitboxBrightnessMode == 0);
        hideIf("hitboxBrightnessOffset", () -> hitboxBrightnessMode != 0);
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
        statsProvider = clampIndex(statsProvider, 4);
        inGameBlacklistWarningDestination = clampIndex(
            inGameBlacklistWarningDestination,
            3
        );
        pingProvider = clampIndex(pingProvider, 4);
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
    @Button(title = "Hypixel API Key", text = "Edit Key", category = "API Keys", subcategory = "Hypixel")
    public void editHypixelKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.m_20213497();
        mc.m_78135006(() -> mc.m_52715402(new ApiKeyScreen(mc.f_70816363, "Hypixel API Key", hypixelApiKey, value -> {
            hypixelApiKey = value;
            save();
        })));
    }

    @Button(title = "Aurora API Key", text = "Edit Key", category = "API Keys", subcategory = "Aurora")
    public void editAuroraKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.m_20213497();
        mc.m_78135006(() -> mc.m_52715402(new ApiKeyScreen(mc.f_70816363, "Aurora API Key", auroraApiKey, value -> {
            auroraApiKey = value;
            save();
        })));
    }

    @Button(title = "Luna API Key", text = "Edit Key", category = "API Keys", subcategory = "Luna")
    public void editLunaKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.m_20213497();
        mc.m_78135006(() -> mc.m_52715402(new ApiKeyScreen(mc.f_70816363, "Luna API Key", lunaPingApiKey, value -> {
            lunaPingApiKey = value;
            save();
        })));
    }

    @Button(title = "Coral API Key", text = "Edit Key", category = "API Keys", subcategory = "Coral")
    public void editCoralKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.m_20213497();
        mc.m_78135006(() -> mc.m_52715402(new ApiKeyScreen(mc.f_70816363, "Coral API Key", urchinKey, value -> {
            urchinKey = value;
            save();
        })));
    }

    @Button(title = "Seraph API Key", text = "Edit Key", category = "API Keys", subcategory = "Seraph")
    public void editSeraphKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.m_20213497();
        mc.m_78135006(() -> mc.m_52715402(new ApiKeyScreen(mc.f_70816363, "Seraph API Key", seraphKey, value -> {
            seraphKey = value;
            save();
        })));
    }

}
