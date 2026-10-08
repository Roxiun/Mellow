package com.roxiun.mellow.config;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.annotations.Button;
import org.polyfrost.oneconfig.api.config.v1.annotations.Checkbox;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.DraggableList;
import org.polyfrost.oneconfig.api.config.v1.annotations.Include;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.hud.BedwarsUpgradesTrapsHUD;
import com.roxiun.mellow.hud.DiamondCounterHUD;
import com.roxiun.mellow.hud.EmeraldCounterHUD;

public class MellowOneConfig extends Config {

    @Switch(title = "Auto Update Check", category = "General", subcategory = "General")
    public boolean autoUpdateCheck = true;

    @Dropdown(
        description = "Hypixel provider requires an API key from developer.hypixel.net. Configure it in API Keys > Hypixel. Other providers do not require a key.",
        title = "Stats Provider",
        options = { "Hypixel Public API", "Nadeshiko", "Abyss", "Bordic", "Bedlify" },
        category = "Stats",
        subcategory = "General"
    )
    public int statsProvider = 3; // Bordic

    @Switch(title = "Auto /who", category = "Stats", subcategory = "General")
    public boolean autoWho = false;

    @Switch(title = "Print Stats to Chat", category = "Stats", subcategory = "General")
    public boolean printStats = false;

    @Switch(
        title = "Include Tags in Chat Stats",
        description = "Include generated player tags in printed BedWars stats.",
        category = "Stats",
        subcategory = "General"
    )
    public boolean tags = false;

    @Number(
        title = "Minimum BedWars FKDR",
        description = "Filters BedWars stats shown in tab and printed by stats lookups. Set to -1 for no minimum.",
        min = -1,
        max = 500,
        category = "Stats",
        subcategory = "General"
    )
    public int minFkdr = -1;

    @Switch(title = "Auto Pregame Stats", category = "Stats", subcategory = "Automatic Lookups")
    public boolean pregameStats = true;

    @Switch(
        title = "Mention Stats in BedWars Lobbies",
        description = "Show sender stats when they mention your username in BedWars lobby chat.",
        category = "Stats",
        subcategory = "Automatic Lookups"
    )
    public boolean mentionLobbyStats = false;

    @Switch(
        title = "Show Automatic Stats Errors",
        description = "Show chat errors when background stats, denick stats, or hidden winstreak lookups fail. Manual commands still report lookup failures.",
        category = "Stats",
        subcategory = "Automatic Lookups"
    )
    public boolean showAutomaticStatsErrors = true;

    @Switch(
        description = "Shows hidden or zero BedWars winstreaks fetched from the Bordic Aurora API. When hidden winstreaks are enabled, Mellow only uses Aurora when the visible BedWars winstreak is missing or hidden. Enabling this sends player UUIDs to Bordic and is subject to their ToS. Hidden winstreak lookups do not require or send an Aurora API key.",
        title = "Show Hidden Winstreaks",
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public boolean showHiddenWinstreaks = true;

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
        category = "Stats",
        subcategory = "Hidden Winstreaks"
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
        category = "Stats",
        subcategory = "Hidden Winstreaks"
    )
    public int winstreakMinFkdr = 0;

    @Dropdown(
        description = "Aurora API provides historical ping averages per player UUID without requiring or sending an API key. Luna's API provides ping averages per player UUID. Configure the key in API Keys > Luna.",
        title = "Ping Provider",
        options = { "None", "Aurora API", "Luna's API" },
        category = "Stats",
        subcategory = "Ping"
    )
    public int pingProvider = 1;

    @Switch(title = "Show Tab Stats", category = "Tab Stats", subcategory = "General")
    public boolean tabStats = true;

    @Switch(title = "Show Stars with Brackets", category = "Tab Stats", subcategory = "General")
    public boolean showStarsWithBrackets = true;

    @Switch(title = "Show Nick with Brackets", category = "Tab Stats", subcategory = "General")
    public boolean showNickWithBrackets = true;

    @Switch(
        title = "Smart Rank Styling",
        description = "Show rank prefixes and colours in suitable modes while preserving gameplay colours and identities. Off keeps server styling.",
        category = "Tab Stats",
        subcategory = "General"
    )
    // Keep the saved key so existing preferences survive the smarter name styling.
    public boolean showRanksInGameTabStats = true;

    @Switch(title = "Extended Tab Stats View", category = "Tab Stats", subcategory = "Extended View")
    public boolean extendedTabStatsView = true;

    @Switch(
        title = "Extended View In Lobbies",
        description = "Allows Extended Tab Stats View while in game lobbies.",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean extendedTabStatsInLobbies = false;

    @Dropdown(
        title = "Column Headers",
        options = {"Bold", "Normal", "Off"},
        description = "Style of the extended tab's column headings.",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public int extendedTabStatsHeaders = 0;

    @Switch(
        title = "Extended View Player Heads",
        description = "Shows player heads in the Name column when using Extended Tab Stats View.",
        category = "Tab Stats",
        subcategory = "Extended View"
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
        description = "Collapses extra whitespace when Team is combined with Name or Stars.",
        category = "Tab Stats",
        subcategory = "Extended View"
    )
    public boolean extendedTabStatsStripCombinedTeamPadding = true;

    @Switch(title = "Highlight Tagged Players", category = "Tab Stats", subcategory = "Extended View")
    public boolean highlightTaggedPlayers = false;

    @DraggableList(
        title = "BedWars Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats",
        subcategory = "Stat Order",
        checkable = true,
        options = {"Team", "Stars", "Name", "FKDR", "Winstreak", "WLR", "BBLR", "Wins", "Beds", "Finals", "HP", "Tags", "Ping"}
    )
    public String[] bedwarsStatOrder = {"Team", "Stars", "Name", "FKDR", "Winstreak", "HP"};

    @DraggableList(
        title = "SkyWars Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats",
        subcategory = "Stat Order",
        checkable = true,
        options = {"Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "HP", "Tags", "Ping"}
    )
    public String[] skywarsStatOrder = {"Team", "Level", "Name", "KDR", "WLR", "HP"};

    @DraggableList(
        title = "Duels Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats",
        subcategory = "Stat Order",
        checkable = true,
        options = {"Team", "Division", "Name", "KDR", "WLR", "Wins", "Losses", "Kills", "Deaths", "Winstreak", "HP", "Tags", "Ping"}
    )
    public String[] duelsStatOrder = {"Team", "Division", "Name", "KDR", "WLR", "Wins", "Losses", "Kills", "Deaths", "HP"};

    @DraggableList(
        title = "Build Battle Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats", subcategory = "Stat Order", checkable = true,
        options = {"Team", "Title", "Name", "Wins", "HP", "Tags", "Ping"}
    )
    public String[] buildBattleStatOrder = {"Team", "Title", "Name", "Wins"};

    @DraggableList(
        title = "TNT Run Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats", subcategory = "Stat Order", checkable = true,
        options = {"Team", "Wins", "Name", "Ratio", "HP", "Tags", "Ping"}
    )
    public String[] tntRunStatOrder = {"Team", "Name", "Wins", "Ratio"};

    @DraggableList(
        title = "TNT Tag Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats", subcategory = "Stat Order", checkable = true,
        options = {"Team", "Wins", "Name", "HP", "Tags", "Ping"}
    )
    public String[] tntTagStatOrder = {"Team", "Name", "Wins"};

    @DraggableList(
        title = "Bow Spleef Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats", subcategory = "Stat Order", checkable = true,
        options = {"Team", "Wins", "Name", "Deaths", "Ratio", "HP", "Tags", "Ping"}
    )
    public String[] bowSpleefStatOrder = {"Team", "Name", "Wins", "Deaths", "Ratio"};

    @DraggableList(
        title = "Murder Mystery Stat Order",
        description = "Drag stats to change their order. Uncheck a stat to hide it.",
        category = "Tab Stats", subcategory = "Stat Order", checkable = true,
        options = {"Team", "Wins", "Name", "Kills", "Games", "HP", "Tags", "Ping"}
    )
    public String[] murderMysteryStatOrder = {"Team", "Name", "Wins", "Kills", "Games"};

    @Checkbox(
        description = "Toggle separator between stats",
        title = "Between 1st and 2nd",
        category = "Tab Stats",
        subcategory = "Standard View"
    )
    public boolean showDot12 = false;

    @Checkbox(title = "Between 2nd and 3rd", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot23 = false;

    @Checkbox(title = "Between 3rd and 4th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot34 = true;

    @Checkbox(title = "Between 4th and 5th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot45 = true;

    @Checkbox(title = "Between 5th and 6th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot56 = true;

    @Checkbox(title = "Between 6th and 7th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot67 = true;

    @Checkbox(title = "Between 7th and 8th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot78 = true;

    @Checkbox(title = "Between 8th and 9th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot89 = true;

    @Checkbox(title = "Between 9th and 10th", category = "Tab Stats", subcategory = "Standard View")
    public boolean showDot910 = true;

    @Switch(
        title = "Print Provider Tag Alerts",
        description = "Print Coral and Xadia tag alerts during stats lookups. Local blacklist warnings are independent.",
        category = "Tags & Blacklists",
        subcategory = "Chat Warnings"
    )
    public boolean printBlacklistTags = true;

    @Dropdown(
        title = "Blacklist Warn Destination",
        options = { "None", "All Chat", "Party Chat" },
        description = "When blacklisted BedWars opponents are detected in-game, route warning messages to this chat channel.",
        category = "Tags & Blacklists",
        subcategory = "Chat Warnings"
    )
    public int inGameBlacklistWarningDestination = 0;

    @Switch(
        title = "Warn for Blacklisted Party Members",
        description = "Shows a warning when your Hypixel party contains one or more blacklisted players.",
        category = "Tags & Blacklists",
        subcategory = "Party Warnings"
    )
    public boolean partyBlacklistWarning = true;

    @Switch(
        title = "Show Party Blacklist Tag Details",
        description = "When warning about flagged party members, also print what they are tagged for.",
        category = "Tags & Blacklists",
        subcategory = "Party Warnings"
    )
    public boolean partyBlacklistWarningShowTagDetails = false;

    @Switch(
        title = "Auto Leave on Blacklisted Chat",
        description = "Requires Stats > Automatic Lookups > Auto Pregame Stats. Automatically runs the configured command in BedWars pregame when a blacklisted chatter is detected and more than 2 seconds remain.",
        category = "Tags & Blacklists",
        subcategory = "Pregame Auto Leave"
    )
    public boolean autoLeaveBlacklistedPregameChat = false;

    @Text(
        title = "Auto Leave Command",
        multiline = false,
        category = "Tags & Blacklists",
        subcategory = "Pregame Auto Leave"
    )
    public String autoLeaveBlacklistedPregameCommand = "/lobby";

    @Switch(
        description = "Coral is a community blacklist, allowing you to see potential cheaters in your game. Coral requires an API key. Enabling it sends player identifiers to api.urchin.gg and is subject to their ToS. Configure the key in API Keys > Coral.",
        title = "Enable Coral",
        category = "Tags & Blacklists",
        subcategory = "Coral"
    )
    public boolean urchin = false;

    @Switch(title = "Show Coral Tags in Tab", category = "Tags & Blacklists", subcategory = "Coral")
    public boolean showUrchinTagsInTab = true;

    @Switch(
        description = "Look up player tags on xadia.sniped.me. Requires a personal key from the Xadia Discord bot (/key generate). Configure it in API Keys > Xadia.",
        title = "Enable Xadia",
        category = "Tags & Blacklists",
        subcategory = "Xadia"
    )
    public boolean xadia = false;

    @Switch(title = "Show Xadia Tags in Tab", category = "Tags & Blacklists", subcategory = "Xadia")
    public boolean showXadiaTagsInTab = true;

    @Switch(
        description = "Hide unverified public reports.",
        title = "Verified Tags Only",
        category = "Tags & Blacklists",
        subcategory = "Xadia"
    )
    public boolean xadiaVerifiedOnly = false;

    @Switch(title = "Auto Skin Denicker", category = "Denicker", subcategory = "Skin Denicker")
    public boolean autoSkinDenick = true;

    @Switch(
        description = "This module attempts to denick players based on the number of finals and beds broken from chat messages. Configure the Aurora key in API Keys > Aurora.",
        title = "Automatic Number Denicker",
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public boolean numberDenicker = false;

    @Switch(
        description = "When disabled, only print players whose beds and finals both match.",
        title = "Print All Potential Players",
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public boolean numberDenickerFuzzy = true;

    @Number(
        title = "Minimum Finals to Check",
        min = 0,
        max = 500000,
        category = "Denicker",
        subcategory = "Automatic Number Denicker"
    )
    public int minFinalsForDenick = 15000;

    @Dropdown(
        title = "Finals Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Denicker",
        subcategory = "Search",
        description = "Used by automatic number denicking and the /denick command."
    )
    public int finalsRange = 3; // Index for 200

    @Dropdown(
        title = "Beds Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Denicker",
        subcategory = "Search",
        description = "Used by automatic number denicking and the /denick command."
    )
    public int bedsRange = 1; // Index for 50

    @Dropdown(
        title = "Max Results",
        options = { "5", "10", "20" },
        category = "Denicker",
        subcategory = "Search",
        description = "Used by automatic number denicking and the /denick command."
    )
    public int maxResults = 0; // Index for 5

    @Switch(
        title = "Request Popups",
        description = "Shows accept/deny popups for incoming friend requests and party invites.",
        category = "Requests",
        subcategory = "General"
    )
    public boolean requestPopupsEnabled = true;

    @Switch(
        title = "Friend Request Popups",
        description = "Shows popups for incoming friend requests.",
        category = "Requests",
        subcategory = "General"
    )
    public boolean friendRequestPopupsEnabled = true;

    @Switch(
        title = "Party Invite Popups",
        description = "Shows popups for incoming party invites.",
        category = "Requests",
        subcategory = "General"
    )
    public boolean partyInvitePopupsEnabled = true;

    @Switch(
        title = "Popup Sound",
        description = "Play a pling sound when a new request popup is received.",
        category = "Requests",
        subcategory = "Appearance & Sound"
    )
    public boolean requestPopupSoundEnabled = true;

    @Dropdown(
        title = "Popup Position",
        options = { "Top-center", "Top-right", "Bottom-right" },
        category = "Requests",
        subcategory = "Appearance & Sound"
    )
    public int requestPopupPosition = 0;

    @Number(
        title = "Popup Duration (seconds)",
        min = 2,
        max = 30,
        category = "Requests",
        subcategory = "Appearance & Sound"
    )
    public int requestPopupDurationSeconds = 10;

    @Switch(title = "Colored Hitboxes", category = "Hitboxes & Nametags", subcategory = "Hitboxes")
    public boolean coloredHitboxes = true;

    @Switch(title = "Affect Vanilla F3+B", category = "Hitboxes & Nametags", subcategory = "Hitboxes")
    public boolean coloredHitboxesAffectVanillaDebug = true;

    @Switch(title = "Affect PolyHitbox", category = "Hitboxes & Nametags", subcategory = "Hitboxes")
    public boolean coloredHitboxesAffectPolyHitbox = true;

    @Switch(title = "Colored Nametag Backgrounds", category = "Hitboxes & Nametags", subcategory = "Nametags")
    public boolean coloredNametagBackgrounds = false;

    @Switch(title = "Affect PolyNametag", category = "Hitboxes & Nametags", subcategory = "Nametags")
    public boolean coloredNametagAffectPolyNametag = true;

    @Dropdown(
        title = "Hue Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes & Nametags",
        subcategory = "Colours"
    )
    public int hitboxHueMode = 0;

    @Number(title = "Hue Value", min = 0, max = 360, category = "Hitboxes & Nametags", subcategory = "Colours")
    public int hitboxHueValue = 0;

    @Number(title = "Hue Offset", min = -180, max = 180, category = "Hitboxes & Nametags", subcategory = "Colours")
    public int hitboxHueOffset = 0;

    @Dropdown(
        title = "Saturation Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes & Nametags",
        subcategory = "Colours"
    )
    public int hitboxSaturationMode = 0;

    @Number(title = "Saturation Value", min = 0, max = 100, category = "Hitboxes & Nametags", subcategory = "Colours")
    public int hitboxSaturationValue = 100;

    @Number(title = "Saturation Offset", min = -100, max = 100, category = "Hitboxes & Nametags", subcategory = "Colours")
    public int hitboxSaturationOffset = 0;

    @Dropdown(
        title = "Brightness Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes & Nametags",
        subcategory = "Colours"
    )
    public int hitboxBrightnessMode = 0;

    @Number(title = "Brightness Value", min = 0, max = 100, category = "Hitboxes & Nametags", subcategory = "Colours")
    public int hitboxBrightnessValue = 100;

    @Number(title = "Brightness Offset", min = -100, max = 100, category = "Hitboxes & Nametags", subcategory = "Colours")
    public int hitboxBrightnessOffset = 0;

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
        description = "When a recording is saved, oldest replays are deleted if this limit is exceeded. Set to 0 for unlimited.",
        min = 0,
        max = 500,
        category = "Replays",
        subcategory = "Storage"
    )
    public int maxStoredReplays = 0;

    @Switch(title = "Enable Anticheat", category = "Anticheat", subcategory = "General")
    public boolean anticheatEnabled = false;

    @Switch(title = "NoSlow Check", category = "Anticheat", subcategory = "Checks")
    public boolean noSlowCheckEnabled = true;

    @Switch(title = "AutoBlock Check", category = "Anticheat", subcategory = "Checks")
    public boolean autoBlockCheckEnabled = true;

    @Switch(title = "Eagle Check", category = "Anticheat", subcategory = "Checks")
    public boolean eagleCheckEnabled = false;

    @Switch(title = "Scaffold Check", category = "Anticheat", subcategory = "Checks")
    public boolean scaffoldCheckEnabled = false;

    @Switch(
        title = "Verbose Alerts",
        description = "Show detailed anticheat info (debug reason + VL) in alerts.",
        category = "Anticheat",
        subcategory = "Alerts"
    )
    public boolean anticheatVerbose = false;

    @Number(title = "Violation Level", min = 1, max = 100, category = "Anticheat", subcategory = "Alerts")
    public int anticheatVl = 10;

    @Number(title = "Cooldown (seconds)", min = 1, max = 60, category = "Anticheat", subcategory = "Alerts")
    public int anticheatCooldown = 5;

    // Credentials are edited through masked input screens.

    @Include
    public String hypixelApiKey = "";

    @Include
    public String auroraApiKey = "";

    @Include
    public String lunaPingApiKey = "";

    @Include
    public String urchinKey = "";

    @Include
    public String xadiaKey = "";

    public EmeraldCounterHUD emeraldCounterHUD = new EmeraldCounterHUD();
    public DiamondCounterHUD diamondCounterHUD = new DiamondCounterHUD();
    public BedwarsUpgradesTrapsHUD upgradesTrapsHUD = new BedwarsUpgradesTrapsHUD();

    public String getCoralApiKey() {
        return urchinKey;
    }

    public boolean isCoralEnabled() {
        return urchin;
    }

    public boolean shouldShowCoralTagsInTab() {
        return showUrchinTagsInTab;
    }

    @Button(
        title = "Get an Aurora API Key",
        description = "Use /api view with the Discord bot, then enter the key in API Keys > Aurora.",
        text = "Discord Bot",
        category = "Denicker",
        subcategory = "Setup"
    )
    public void auroraLinkButton() {
        org.polyfrost.oneconfig.utils.v1.NetworkUtils.browseLink(
            "https://discord.com/oauth2/authorize?client_id=1244205279697174539"
        );
    }

    public MellowOneConfig() {
        super("mellow-v1.json", Mellow.NAME, Category.HYPIXEL);
        initialize(false);
        org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.register(emeraldCounterHUD, id);
        org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.register(diamondCounterHUD, id);
        org.polyfrost.oneconfig.api.hud.v1.HudManager.INSTANCE.register(upgradesTrapsHUD, id);
    }

    @Override
    protected Tree makeTree() {
        return PreferenceLayout.arrange(super.makeTree());
    }

    /** Field names remain stable even when their controls move into an accordion. */
    @Override
    public Property<?> getProperty(String option) {
        String group = PreferenceLayout.groupFor(option);
        return super.getProperty(group == null ? option : group + "." + option);
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        // OneConfig rebuilds the tree when switching profiles. Reattach rules to the new properties.
        if (tree != null) return;
        super.initialize(byConfigManager);
        if (ConfigValidation.sanitize(this)) save();
        configureDependencies();
    }

    private void configureDependencies() {
        dependOn("requestPopupsEnabled", "friendRequestPopupsEnabled", "partyInvitePopupsEnabled",
            "requestPopupSoundEnabled", "requestPopupPosition", "requestPopupDurationSeconds");
        dependOn("enableReplayRecording", "recordChatInReplays");
        dependOn("printStats", "tags");
        dependOn("tabStats", "showStarsWithBrackets", "showNickWithBrackets", "showRanksInGameTabStats",
            "extendedTabStatsView", "extendedTabStatsInLobbies", "extendedTabStatsHeaders", "extendedTabStatsShowHeads",
            "extendedTabStatsTeamColumnMode", "extendedTabStatsStripCombinedTeamPadding", "highlightTaggedPlayers",
            "bedwarsStatOrder", "skywarsStatOrder", "duelsStatOrder", "buildBattleStatOrder", "tntRunStatOrder", "tntTagStatOrder", "bowSpleefStatOrder", "murderMysteryStatOrder", "showDot12", "showDot23", "showDot34",
            "showDot45", "showDot56", "showDot67", "showDot78", "showDot89", "showDot910",
            "showUrchinTagsInTab", "showXadiaTagsInTab");
        dependOn("extendedTabStatsView", "extendedTabStatsInLobbies", "extendedTabStatsHeaders",
            "extendedTabStatsShowHeads", "extendedTabStatsTeamColumnMode", "extendedTabStatsStripCombinedTeamPadding",
            "highlightTaggedPlayers");
        displayWhen("extendedTabStatsStripCombinedTeamPadding",
            () -> extendedTabStatsTeamColumnMode == 0 || extendedTabStatsTeamColumnMode == 3
                ? Property.Display.SHOWN : Property.Display.HIDDEN, "extendedTabStatsTeamColumnMode");
        dependOn("partyBlacklistWarning", "partyBlacklistWarningShowTagDetails");
        dependOn("pregameStats", "autoLeaveBlacklistedPregameChat", "autoLeaveBlacklistedPregameCommand");
        dependOn("autoLeaveBlacklistedPregameChat", "autoLeaveBlacklistedPregameCommand");
        dependOn("urchin", "showUrchinTagsInTab");
        dependOn("xadia", "showXadiaTagsInTab", "xadiaVerifiedOnly");
        dependOn("showHiddenWinstreaks", "winstreakMinStars", "winstreakMinFkdr");
        dependOn("numberDenicker", "numberDenickerFuzzy", "minFinalsForDenick");
        dependOn("coloredHitboxes", "coloredHitboxesAffectVanillaDebug", "coloredHitboxesAffectPolyHitbox");
        dependOn("coloredNametagBackgrounds", "coloredNametagAffectPolyNametag");
        for (String component : new String[]{"Hue", "Saturation", "Brightness"}) {
            String mode = "hitbox" + component + "Mode";
            for (String suffix : new String[]{"Mode", "Value", "Offset"}) {
                displayWhen("hitbox" + component + suffix,
                    () -> coloredHitboxes || coloredNametagBackgrounds ? Property.Display.SHOWN : Property.Display.DISABLED,
                    "coloredHitboxes", "coloredNametagBackgrounds");
            }
            displayWhen("hitbox" + component + "Value",
                () -> (Integer) getProperty(mode).get() == 0 ? Property.Display.HIDDEN : Property.Display.SHOWN, mode);
            displayWhen("hitbox" + component + "Offset",
                () -> (Integer) getProperty(mode).get() != 0 ? Property.Display.HIDDEN : Property.Display.SHOWN, mode);
        }
        dependOn("anticheatEnabled", "noSlowCheckEnabled", "autoBlockCheckEnabled", "eagleCheckEnabled",
            "scaffoldCheckEnabled", "anticheatVerbose", "anticheatVl", "anticheatCooldown");
    }

    private void dependOn(String parent, String... children) {
        for (String child : children) addDependency(child, parent);
    }

    /** Listen to the actual parent properties, including those inside accordions. */
    private void displayWhen(String field, java.util.function.Supplier<Property.Display> condition, String... parents) {
        Property<?> child = getProperty(field);
        child.addDisplayCondition(condition);
        for (String parent : parents) {
            getProperty(parent).addCallback(value -> {
                child.revaluateDisplay();
                return false;
            });
        }
    }

    @Button(
        description = "Manage all service API keys here. Feature-specific toggles remain in their own categories.",
        title = "Hypixel API Key",
        text = "Edit Key",
        category = "API Keys",
        subcategory = "Hypixel"
    )
    public void editHypixelKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.displayGuiScreen(new ApiKeyScreen(mc.currentScreen, "Hypixel API Key", hypixelApiKey, value -> {
            hypixelApiKey = value;
            save();
        })));
    }

    @Button(
        description = "Used by Number Denicker and /denick. Hidden winstreaks and Aurora ping do not need this key.",
        title = "Aurora API Key",
        text = "Edit Key",
        category = "API Keys",
        subcategory = "Aurora"
    )
    public void editAuroraKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.displayGuiScreen(new ApiKeyScreen(mc.currentScreen, "Aurora API Key", auroraApiKey, value -> {
            auroraApiKey = value;
            save();
        })));
    }

    @Button(
        description = "Used when Stats > Ping selects Luna's API.",
        title = "Luna API Key",
        text = "Edit Key",
        category = "API Keys",
        subcategory = "Luna"
    )
    public void editLunaKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.displayGuiScreen(new ApiKeyScreen(mc.currentScreen, "Luna API Key", lunaPingApiKey, value -> {
            lunaPingApiKey = value;
            save();
        })));
    }

    @Button(
        description = "Used by Tags & Blacklists > Coral.",
        title = "Coral API Key",
        text = "Edit Key",
        category = "API Keys",
        subcategory = "Coral"
    )
    public void editCoralKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.displayGuiScreen(new ApiKeyScreen(mc.currentScreen, "Coral API Key", urchinKey, value -> {
            urchinKey = value;
            save();
        })));
    }

    @Button(
        description = "Used by Tags & Blacklists > Xadia.",
        title = "Xadia API Key",
        text = "Edit Key",
        category = "API Keys",
        subcategory = "Xadia"
    )
    public void editXadiaKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.displayGuiScreen(new ApiKeyScreen(mc.currentScreen, "Xadia API Key", xadiaKey, value -> {
            xadiaKey = value;
            save();
        })));
    }

}
