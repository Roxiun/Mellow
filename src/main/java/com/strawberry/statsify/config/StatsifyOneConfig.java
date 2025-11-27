package com.strawberry.statsify.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Info;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import com.strawberry.statsify.Statsify;

public class StatsifyOneConfig extends Config {

    @Number(name = "Minimum FKDR", min = -1, max = 500, step = 1)
    public int minFkdr = -1;

    @Dropdown(name = "Stats Provider", options = { "Nadeshiko", "Abyss" })
    public int statsProvider = 0;

    @Switch(name = "Show Tags")
    public boolean tags = false;

    @Switch(name = "Show Tab Stats")
    public boolean tabStats = true;

    @Switch(name = "Enable Urchin", category = "Urchin")
    public boolean urchin = false;

    @Info(
        text = "Urchin does not require a key to view tags.",
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

    @Dropdown(
        name = "Ping Provider",
        category = "Ping",
        options = { "None", "Polsu", "Urchin" }
    )
    public int pingProvider = 2;

    @Info(
        text = "Polsu requires an API key to be able to be used.",
        type = InfoType.INFO,
        category = "Ping"
    )
    public static boolean ignored3;

    @Button(name = "Polsu API Key", text = "Get Key", category = "Ping")
    Runnable polsuLinkButton = () -> {
        NetworkUtils.browseLink("https://polsu.xyz/api/apikey");
    };

    @Text(
        name = "Polsu API Key",
        category = "Ping",
        secure = true,
        multiline = false
    )
    public String polsuApiKey = "";

    @Switch(name = "Auto /who")
    public boolean autoWho = true;

    @Switch(name = "Auto Skin Denicker")
    public boolean autoSkinDenick = true;

    @Switch(name = "Auto Pregame Stats")
    public boolean pregameStats = true;

    @Switch(name = "Auto Pregame Tags")
    public boolean pregameTags = true;

    @Switch(name = "Print Stats to Chat")
    public boolean printStats = false;

    @Dropdown(
        name = "Tab Format",
        options = {
            "[Star] Name • FKDR • WS",
            "Star • Name • FKDR • WS",
            "Name • FKDR • WS",
        }
    )
    public int tabFormat = 0;

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

    public StatsifyOneConfig() {
        super(
            new Mod(Statsify.NAME, ModType.HYPIXEL),
            Statsify.MODID + ".json"
        );
        initialize();
    }
}
