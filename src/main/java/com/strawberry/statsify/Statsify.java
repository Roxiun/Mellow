package com.strawberry.statsify;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import scala.reflect.internal.Trees;
import net.minecraft.scoreboard.ScorePlayerTeam;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Mod(modid = Statsify.MODID, name = Statsify.NAME, version = Statsify.VERSION)
public class Statsify {
    public static final String MODID = "statsify";
    public static final String NAME = "Stats Mod";
    public static final String VERSION = "1.0";

    private static final String CONFIG_PATH = "config/statsify.json";
    private static final int DEFAULT_MIN_FKDR = -1;
    private static final String DEFAULT_MODE = "bws";
    private Boolean tags = false;
    private Boolean tabstats = true;
    private Boolean urchin = false;
    private Boolean reqUUID = true;
    private Boolean autowho = true;
    private static final Map<String, List<String>> playerSuffixes = new HashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private int minFkdr = DEFAULT_MIN_FKDR;
    private String urchinkey = "";
    private String mode = DEFAULT_MODE;
    private List<String> onlinePlayers = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        loadConfig();
        MinecraftForge.EVENT_BUS.register(this); // Register event listeners
        ClientCommandHandler.instance.registerCommand(new BedwarsCommand());
        ClientCommandHandler.instance.registerCommand(new MinFkdrCommand());
        ClientCommandHandler.instance.registerCommand(new BwModeCommand());
        ClientCommandHandler.instance.registerCommand(new ToggleTagsCommand());
        ClientCommandHandler.instance.registerCommand(new StatsifyCommand());
        ClientCommandHandler.instance.registerCommand(new TablistToggleCommand());
        ClientCommandHandler.instance.registerCommand(new ClearCacheCommand());
        ClientCommandHandler.instance.registerCommand(new SetUrchinKeyCommand());
        ClientCommandHandler.instance.registerCommand(new UrchinTagsToggleCommand());
        ClientCommandHandler.instance.registerCommand(new RequireUUIDToggleCommand());
        ClientCommandHandler.instance.registerCommand(new AutoWhoToggleCommand());
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        catchAndIgnoreNullPointerException(() -> {
            String message = event.message.getUnformattedText();
            if(autowho) {
                if (message.contains("Protect your bed and destroy the enemy beds.") && !(message.contains(":")) && !(message.contains("SHOUT"))) {
                    mc.thePlayer.sendChatMessage("/who");
                }
            }
            if (message.startsWith("ONLINE:")) {
                String playersString = message.substring("ONLINE:".length()).trim();
                String[] players = playersString.split(",\\s*");
                onlinePlayers = new ArrayList<>(Arrays.asList(players));
                if (Objects.equals(mode, "bws")) {

                    checkStatsRatelimitless();
                } else {

                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cPlancke is discontinued, use BWS."));

                }
                if (urchin) {
                    checkUrchinTags();
                }
            }

            if (message.startsWith(" ") && message.contains("Opponent:")) {
                String username = parseUsername(message);
                new Thread(() -> {
                    try {
                        String stats = checkDuels(username);
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] " + stats));
                    } catch (IOException e) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7c" + username + " is possibly nicked.\u00a7r"));

                    }
                }).start();


            }
        });


    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderTabList(RenderGameOverlayEvent.Post event) {
        // Future reference, I have no idea what the FUCK did i do here. It works, but idk how. So I wont be improving this.
        // does not work on viaforge 1.20 / 1.19
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        if (!tabstats) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        Collection<NetworkPlayerInfo> playerInfoList = mc.thePlayer.sendQueue.getPlayerInfoMap();
        if (playerInfoList.isEmpty()) return;

        for (NetworkPlayerInfo playerInfo : playerInfoList) {
            if (playerInfo == null || playerInfo.getGameProfile() == null) continue;

            String playerName = playerInfo.getGameProfile().getName();
            List<String> suffixv = playerSuffixes.get(playerName);

            if (suffixv != null && suffixv.size() >= 2) {
                String[] tabData = getTabDisplayName2(playerName);
                String team = tabData[0], name = tabData[1], suffix = tabData[2];

                if (!name.endsWith("\u30fb" + suffixv.get(1))) {
                    String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
                    String newDisplayName = team + "\u00a77[" + suffixv.get(0) + "\u00a77] " + teamColor + name + "\u30fb" + suffixv.get(1);
                    playerInfo.setDisplayName(new ChatComponentText(newDisplayName));
                }
            }
        }
    }

    public static void sendToTablist(String playerName, String fkdr, String stars) {
        if (playerName != null && fkdr != null && stars != null) {
            playerSuffixes.put(playerName, Arrays.asList(stars, fkdr));
        }
    }

    public static String parseUsername(String str) {
        str = str.trim();
        String[] words = str.split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "";
    }

    private String getTabDisplayName(String playerName) {

        ScorePlayerTeam playerTeam = Minecraft.getMinecraft().theWorld.getScoreboard().getPlayersTeam(playerName);
        if (playerTeam == null) {
            return playerName;
        }


            int length = playerTeam.getColorPrefix().length();

            if (length == 10) {
                return playerTeam.getColorPrefix() + playerName + playerTeam.getColorSuffix();
            }

            if(length == 8) {
                return playerTeam.getColorPrefix() + playerName;

            }

        return playerName;

    }

    private String[] getTabDisplayName2(String playerName) {

        ScorePlayerTeam playerTeam = Minecraft.getMinecraft().theWorld.getScoreboard().getPlayersTeam(playerName);

        if (playerTeam == null) {
            mc.thePlayer.addChatMessage(
                    new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] " + playerName + " |  IS NULL "));

            return new String[]{"", playerName, ""};

        }
            int length = playerTeam.getColorPrefix().length();

        if (length == 10) {
            String val[] = new String[3];
            val[0] = playerTeam.getColorPrefix();
            val[1] = playerName;
            val[2] = playerTeam.getColorSuffix();
            return val;
        }

        if(length == 8)
        {
            String val[] = new String[3];
            val[0] = playerTeam.getColorPrefix();
            val[1] = playerName;
            val[2] = "";
            return val;
        }
        return new String[]{"", playerName, ""};
    }

    private String checkDuels(String playerName) throws IOException {
        String url = "https://plancke.io/hypixel/player/stats/" + playerName;

        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder responseText = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();

        String response = responseText.toString();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return playerName + " is \u00a7cnicked\u00a7r";
        }


        Pattern namePattern = Pattern.compile("(?<=content=\"Plancke\" /><meta property=\"og:locale\" content=\"en_US\" /><meta property=\"og:description\" content=\").+?(?=\")");
        Matcher nameMatcher = namePattern.matcher(response);
        String displayedName = nameMatcher.find() ? nameMatcher.group() : "Unknown";

        String playerrank = ""; // empty if no rank
        String trimmedName = displayedName.trim();


        String[] parts = trimmedName.split("\\s+", 2);
        if (parts.length > 0 && parts[0].startsWith("[") && parts[0].endsWith("]")) {
            String unformattedRank = parts[0];
            playerrank = formatRank(unformattedRank) + " ";
        }
        // Insane Regex Wow
        String regex = "<tr><td>Classic 1v1</td><td>([\\d,]+)</td><td>([\\d,]+)</td><td>([\\d.,]+)</td><td>([\\d,]+)</td><td>([\\d,]+)</td><td>([\\d.,]+)</td><td>([\\d.,]+)</td><td>([\\d.,]+)</td></tr>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String ClassicStats = "\n\u00a7aKills:\u00a7r " + matcher.group(1) + " \u00a7cDeaths:\u00a7r " + matcher.group(2) + " (\u00a7d" + matcher.group(3) + "\u00a7r) " + "\n" + "\u00a7bW:\u00a7r " + matcher.group(4) + " \u00a7cL: \u00a7r" + matcher.group(5) + " (\u00a7d" + matcher.group(6) + "\u00a7r)";
            return playerrank + playerName + "\u00a7r (Classic 1v1)" + ClassicStats;
        } else {
            return playerrank + playerName + " \u00a7chas no Classic Duels stats.\u00a7r";
        }

    }

    private static String formatRank(String rank) {
        String formattedRank = rank.replace("[VIP", "\u00a7a[VIP").replace("[MVP+", "\u00a7b[MVP+").replace("[MVP++", "\u00a76[MVP++");
        return formattedRank;
    }

    private void checkUrchinTags() {
        catchAndIgnoreNullPointerException(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(5);

            for (String playerName : onlinePlayers) {

                executor.submit(() -> {
                    try {
                        String uuid = fetchUUID(playerName);
                        String tags = fetchUrchinTags(playerName).replace("sniper", "\u00a74\u00a7lSniper").replace("blatant_cheater", "\u00a74\u00a7lBlatant Cheater").replace("closet_cheater", "\u00a7e\u00a7lCloset Cheater").replace("confirmed_cheater", "\u00a74\u00a7lConfirmed Cheater");

                        if (!tags.isEmpty()) {
                            mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                                    new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7c\u26a0 \u00a7r" + getTabDisplayName(playerName) + " \u00a7ris \u00a7ctagged\u00a7r for: " + tags)
                            ));
                        }

                    } catch (IOException e) {
                        mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                                new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] Failed to fetch tags for: "
                                        + playerName + " | " + e.getMessage())
                        ));
                    }
                });
            }
            executor.shutdown();

        });
    }

    private void checkStatsRatelimitless() {
        catchAndIgnoreNullPointerException(() -> {
            // Made by The_Bi11iona1re
            final int MAX_THREADS = 20;
            int poolSize = Math.min(onlinePlayers.size(), MAX_THREADS);
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);

            for (String playerName : onlinePlayers) {
                executor.submit(() -> {
                    try {
                        String stats = fetch25KarmaStats(playerName);
                        if (!stats.isEmpty()) {
                            mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                                    new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] " + stats)
                            ));
                        }
                    } catch (IOException e) {
                        mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                                new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] Failed to fetch stats for: "
                                        + playerName + " | [UpstreamCSR] " )
                        ));
                    }
                });
            }

            executor.shutdown();

            new Thread(() -> {
                try {
                    if (executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                                new ChatComponentText("\u00a7r[\u00a7bF\u00a7r]\u00a7a Checks completed.")
                        ));
                    } else {
                        mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                                new ChatComponentText("\u00a7r[\u00a7bF\u00a7r]\u00a7c Timeout waiting for completion.")
                        ));
                    }
                } catch (InterruptedException e) {
                    mc.addScheduledTask(() -> mc.thePlayer.addChatMessage(
                            new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cError while waiting: " + e.getMessage())
                    ));
                }
            }).start();
        });
    }


    public static String fetchUUID(String username) {
        String urlString = "https://api.minecraftservices.com/minecraft/profile/lookup/name/" + username;
        String uuid = null;

        try {
            // Attempt to fetch from the official Minecraft API
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Handle success response from official API
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String responseString = response.toString();
                uuid = extractUUID(responseString);
                if (uuid == "NICKED") {
                    return "NICKED";
                }
            } else if (responseCode == 429) {
                // If the official API returns 429 (Too Many Requests), fall back to minetools API


                urlString = "https://api.minetools.eu/uuid/" + username;
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String responseString = response.toString();

                String[] parts = responseString.split("\"id\"");
                if (parts.length > 1) {

                    String secondPart = parts[1];


                    String[] quotes = secondPart.split("\"");
                    if (quotes.length > 2) {

                        String result = quotes[1];
                        if (!responseString.contains("\"id\": null")) {
                            return result;
                        } else {
                            return username;
                        }
                    }
                }
            } else if (responseCode == 404) {

                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(username + " is possibly nicked."));
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        return uuid;
    }


    private static String extractUUID(String response) {
        String[] parts = response.split("\"");
        if (response.contains("Couldn't find any profile with name")) {
            return "NICKED";
        }

        if (parts.length >= 5) {
            return parts[3];
        }

        return null;
    }

    private String nadeshikoAPI(String uuid) {
        try {
            String urlString = "https://nadeshiko.io/player/" + uuid + "/network";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();


            if (responseCode == HttpURLConnection.HTTP_OK) {

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                String responseString = response.toString();
                Pattern pattern = Pattern.compile("playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)");
                Matcher matcher = pattern.matcher(responseString.toString());

                if (matcher.find()) {
                    String playerDataEncoded = matcher.group(1);
                    String playerData = URLDecoder.decode(playerDataEncoded, "UTF-8");
                    return playerData;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String fetchUrchinTags(String playerName) throws IOException {


        String tagsURL = "https://urchin.ws/player/" + playerName + "?api_key=" + urchinkey;
        URL tagsAPIURL = new URL(tagsURL);
        HttpURLConnection statsConnection = (HttpURLConnection) tagsAPIURL.openConnection();
        statsConnection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0");


        int responseCode = statsConnection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("URCHIN: " + responseCode);
        }

        InputStream inputStream = statsConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder responseText = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();

        String response = responseText.toString();
        if (!response.isEmpty()) {
            try {
                String regex = "\"type\":\"(.*?)\".*?\"reason\":\"(.*?)\".*?\"added_on\":\"(.*?)\"";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(response);

                if (matcher.find()) {
                    String type = matcher.group(1);
                    String reason = matcher.group(2);
                   /* String addedOn = matcher.group(3);
                    LocalDateTime dateTime = LocalDateTime.parse(addedOn.substring(0, 19));
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM HH:mm:ss");
                    String formattedDate = dateTime.format(formatter);*/
                    return "\u00a7r" + type + ". \u00a7rReason: \u00a76" + reason;
                } else {

                }
            } catch (NumberFormatException ignored) {

            }
        }
        return "";
    }

    public String fetch25KarmaStats(String playerName) throws IOException {

        try {
            String statsUrl = "";
            if(reqUUID) {
                String uuid = fetchUUID(playerName);
                if (uuid == "NICKED") {
                    return getTabDisplayName(playerName) + " \u00a7r is nicked.";
                }
                statsUrl = "https://bwstats.shivam.pro/user/" + uuid;
            }
            if(!reqUUID){
                statsUrl = "https://bwstats.shivam.pro/user/" + playerName;
            }
            HttpURLConnection conn = (HttpURLConnection) (new URL(statsUrl)).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            InputStream input = (InputStream) (conn.getContentEncoding() != null && conn.getContentEncoding().equalsIgnoreCase("gzip") ? new GZIPInputStream(conn.getInputStream()) : conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder response = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            String html = response.toString();
            String finalKills = extractValue(html, "<td>Final Kills</td><td>", "</td>").replace(",", "");
            String finalDeaths = extractValue(html, "<td>Final Deaths</td><td>", "</td>").replace(",", "");

            String wlrStr = extractValue(html, "<td>Win/Loss Ratio</td><td>", "</td>");
            String levelStr = extractValue(html, "Level: ", " ");
            String wsStr = extractValue(html, "<td>Winstreak</td><td>", "</td>");

            double wlr = Double.parseDouble(wlrStr);
            int level = Integer.parseInt(levelStr.replaceAll("[^0-9]", ""));
            int ws = Integer.parseInt(wsStr.replaceAll("[^0-9]", ""));
            double fkdrValue;
            try {

                if (finalDeaths.equals("0")) {
                    fkdrValue = Double.parseDouble(finalKills);
                } else {
                    fkdrValue = (double) Integer.parseInt(finalKills) / Integer.parseInt(finalDeaths);
                }

                if (fkdrValue < minFkdr) {
                    return "";
                }

                String fkdrColor = "\u00a77";
                if (fkdrValue >= 1 && fkdrValue < 3) fkdrColor = "\u00a7f";
                if (fkdrValue >= 3 && fkdrValue < 8) fkdrColor = "\u00a7a";
                if (fkdrValue >= 8 && fkdrValue < 16) fkdrColor = "\u00a76";
                if (fkdrValue >= 16 && fkdrValue < 25) fkdrColor = "\u00a7d";
                if (fkdrValue > 25) fkdrColor = "\u00a74";
                DecimalFormat df = new DecimalFormat("#.##");
                String formattedFkdr = df.format(fkdrValue);
                String formattedWinstreak;
                if (ws < 1) {
                    formattedWinstreak = "";
                } else {
                    formattedWinstreak = formatWinstreak(wsStr);
                }
                String formattedStars = formatStars(levelStr);
                String tabfkdr = fkdrColor + formattedFkdr;
                if (tabstats) {
                    sendToTablist(playerName, tabfkdr, formattedStars);
                }
                if (tags) {
                    String tagsValue = buildTags(playerName, playerName, Integer.parseInt(levelStr), fkdrValue, Integer.parseInt(wsStr), Integer.parseInt(finalKills), Integer.parseInt(finalDeaths));
                    if (tagsValue.endsWith(" ")) tagsValue = tagsValue.substring(0, tagsValue.length() - 1);
                    if (formattedWinstreak == "") {
                        return getTabDisplayName(playerName) + " \u00a7r" + formattedStars + "\u00a7r\u00a77 |\u00a7r FKDR: " + fkdrColor + formattedFkdr + " \u00a7r\u00a77|\u00a7r" + " [ " + tagsValue + " ]";
                    } else {
                        return getTabDisplayName(playerName) + " \u00a7r" + formattedStars + "\u00a7r\u00a77 |\u00a7r FKDR: " + fkdrColor + formattedFkdr + " \u00a7r\u00a77|\u00a7r WS: " + formattedWinstreak + "\u00a7r [ " + tagsValue + " ]";
                    }
                } else {
                    if (formattedWinstreak == "")
                        return getTabDisplayName(playerName) + " \u00a7r" + formattedStars + "\u00a7r\u00a77 |\u00a7r FKDR: " + fkdrColor + formattedFkdr + "\u00a7r";
                    else
                        return getTabDisplayName(playerName) + " \u00a7r" + formattedStars + "\u00a7r\u00a77 |\u00a7r FKDR: " + fkdrColor + formattedFkdr + " \u00a7r\u00a77|\u00a7r WS: " + formattedWinstreak + "\u00a7r";

                }
            } catch (Exception e) {

                return "Failed to fetch stats for " + playerName + " [DOWNSTREAM1]";



            }
        } catch (Exception e) {

            return EnumChatFormatting.RED +  playerName + " is possibly nicked.";



        }
    }




/*      Tags are not completed    */

    private String buildTags(String name,String uuid, int stars, double fkdr, int ws, int finals, int fdeaths){
        /*
        N = Suspicious name (kikin, mchk, msmc, 4+ number in name..)
        W = Winstreak while being low star (1+ WS when 1-6 star)
        F = Suspiciously high fkdr while being low star (4+ when 1 - 6 star)
        SK = Default skin
        PN = Prename (mostly XGP accs) [removed]
        NC = Namechanged in last 7 days (localts) [to be done]
        NL = New login (today or yesterday first login)
        0F = 0 finals 0 final deaths
         */



        // Suspicious name
        String totaltags = "";
        String[] suswords = {"msmc", "kikin", "g0ld", "Fxrina_", "MAL_", "fer_","ly_","tzi_","Verse_","uwunova","Anas_","MyloAlt_","rayl_","mchk_","HellAlts_","disruptive","solaralts_","G0LDALTS_","unwilling","predicative"};
        boolean suswordcheck = Arrays.stream(suswords)
                .anyMatch(keyword -> name.toLowerCase().contains(keyword.toLowerCase()));
        if(suswordcheck || Pattern.compile("\\d.*\\d.*\\d.*\\d").matcher(name).find()) totaltags = totaltags + EnumChatFormatting.YELLOW + "N \u00a7r";
        // Suspicious name end

        // Winstreak while low star
        if(stars <= 6 && ws >= 1) totaltags = totaltags + EnumChatFormatting.GREEN + "W \u00a7r";
        // Winstreak while low star end

        // High fkdr when low star
        if(stars <= 6 && fkdr >= 4) totaltags = totaltags + EnumChatFormatting.DARK_RED + "F \u00a7r";
        // High fkdr when low star end

        // Default skin check

        /* All i could collect lol*/ String[] defaultSkinIDS = {"a3bd16079f764cd541e072e888fe43885e711f98658323db0f9a6045da91ee7a ","b66bc80f002b10371e2fa23de6f230dd5e2f3affc2e15786f65bc9be4c6eb71a","e5cdc3243b2153ab28a159861be643a4fc1e3c17d291cdd3e57a7f370ad676f3", "f5dddb41dcafef616e959c2817808e0be741c89ffbfed39134a13e75b811863d" ,"4c05ab9e07b3505dc3ec11370c3bdce5570ad2fb2b562e9b9dd9cf271f81aa44", "31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb", "6ac6ca262d67bcfb3dbc924ba8215a18195497c780058a5749de674217721892", "1abc803022d8300ab7578b189294cce39622d9a404cdc00d3feacfdf45be6981","daf3d88ccb38f11f74814e92053d92f7728ddb1a7955652a60e30cb27ae6659f", "fece7017b1bb13926d1158864b283b8b930271f80a90482f174cca6a17e88236"};
            try {
                String urlString = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Handle success response from official API
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String responseString = response.toString();
                    String[] parts = responseString.split("\"value\" : \"");
                    String value = parts[1].split("\"")[0];
                    byte[] decodedBytes = Base64.getDecoder().decode(value);
                    String valueJson = new String(decodedBytes);
                    boolean skincheck = Arrays.stream(defaultSkinIDS)
                            .anyMatch(id -> valueJson.toLowerCase().contains(id.toLowerCase()));
                    if(skincheck) totaltags = totaltags + EnumChatFormatting.DARK_AQUA + "SK \u00a7r";
                }
            }   catch (Exception e) { e.printStackTrace();}

        // Default skin check end
/*
Prename check

        String apiUrl = "https://laby.net/api/v3/user/" + uuid + "/names";

        try {
            // Make the GET request
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String jsonResponse = response.toString();

            if (jsonResponse.contains("\"name\"")) {

                int nameCount = countOccurrences(jsonResponse, "\"name\":");
                if (nameCount == 1) {
                    totaltags = totaltags + EnumChatFormatting.AQUA + "PN \u00a7r";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

Prename check end
*/
        // New login

        String playerData = nadeshikoAPI(uuid);
        Pattern timestampPattern = Pattern.compile("\"first_login\":(\\d+),");
        Matcher timestampMatcher = timestampPattern.matcher(playerData);
        if (timestampMatcher.find()) {
            long timestamp = Long.parseLong(timestampMatcher.group(1));
            Date loginDate = new Date(timestamp);

            Calendar currentCalendar = Calendar.getInstance();
            Calendar loginCalendar = Calendar.getInstance();


            currentCalendar.setTimeInMillis(System.currentTimeMillis());
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
            currentCalendar.set(Calendar.MINUTE, 0);
            currentCalendar.set(Calendar.SECOND, 0);
            currentCalendar.set(Calendar.MILLISECOND, 0);

            loginCalendar.setTime(loginDate);
            loginCalendar.set(Calendar.HOUR_OF_DAY, 0);
            loginCalendar.set(Calendar.MINUTE, 0);
            loginCalendar.set(Calendar.SECOND, 0);
            loginCalendar.set(Calendar.MILLISECOND, 0);


            long diff = currentCalendar.getTimeInMillis() - loginCalendar.getTimeInMillis();
            long oneDayMillis = 24 * 60 * 60 * 1000;

            if (Math.abs(diff) <= oneDayMillis) {
                totaltags = totaltags + EnumChatFormatting.RED + "NL \u00a7r";
            }
        }

        // 0 finals 0 final deaths
        if(finals == 0 &&  fdeaths == 0) totaltags = totaltags + EnumChatFormatting.RED + "0F \u00a7r";

       return totaltags;
    }
    private String formatWinstreak(String text)
    {
        String color = "\u00a7r";
        int Winstreak = Integer.parseInt(text);
        if (Winstreak >= 5 && Winstreak < 10) {
            color = "\u00a7b";
        }
        if (Winstreak >= 10 && Winstreak < 20) {
            color = "\u00a76";
        }
        if (Winstreak >= 20) {
            color = "\u00a74";
        }
        return color + text;
    }
    private String formatStars(String text)
    {
        String color = "\u00a77";

        int Stars = Integer.parseInt(text);
        if (Stars < 100) {
            color = "\u00a77";
            return color + text + "\u272b";
        }

        if (Stars >= 100 && Stars < 200) {
            color = "\u00a7b";
            return color + text + "\u272b";
        }
        if (Stars >= 200 && Stars < 300) {
            color = "\u00a76";
            return color + text + "\u272b";
        }
        if (Stars >= 300 && Stars < 400) {
            color = "\u00a7b";
            return color + text + "\u272b";
        }
        if (Stars >= 400 && Stars < 500) {
            color = "\u00a72";
            return color + text + "\u272b";
        }
        if (Stars >= 500 && Stars < 600) {
            color = "\u00a73";
            return color + text + "\u272b";
        }
        if (Stars >= 600 && Stars < 700) {
            color = "\u00a74";
            return color + text + "\u272b";
        }
        if (Stars >= 700 && Stars < 800) {
            color = "\u00a7d";
            return color + text + "\u272b";
        }
        if (Stars >= 800 && Stars < 900) {
            color = "\u00a79";
            return color + text + "\u272b";
        }
        if (Stars >= 900 && Stars < 1000) {
            color = "\u00a75";
            return color + text + "\u272b";
        }
        if (Stars >= 1000 && Stars < 1100) {
            String[] digit = text.split("");
            return "\u00a76"+digit[0]+"\u00a7e"+digit[1]+"\u00a7a"+digit[2]+"\u00a7b"+digit[3]+"\u00a7d"+"\u272a";
        }
        if (Stars >= 1100 && Stars < 1200) {
            String[] digit = text.split("");
            return "\u00a7f" + digit[0] + digit[1] + digit[2] + digit[3] + "\u272a";
        }
        if (Stars >= 1200 && Stars < 1300) {
            String[] digit = text.split("");
            return "\u00a7e" + digit[0] + digit[1] + digit[2] + digit[3] + "\u00a76" + "\u272a";
        }
        if (Stars >= 1300 && Stars < 1400) {
            String[] digit = text.split("");
            return "\u00a7b" + digit[0] + digit[1] + digit[2] + digit[3] + "\u00a73"  + "\u272a";
        }
        if (Stars >= 1400 && Stars < 1500) {
            String[] digit = text.split("");
            return "\u00a7a" + digit[0] + digit[1] + digit[2] + digit[3] + "\u00a72"  + "\u272a";
        }
        if (Stars >= 1500 && Stars < 1600) {
            String[] digit = text.split("");
            return "\u00a73" + digit[0] + digit[1] + digit[2] + digit[3]+ "\u00a79"  + "\u272a";
        }
        if (Stars >= 1600 && Stars < 1700) {
            String[] digit = text.split("");
            return "\u00a7c" + digit[0] + digit[1] + digit[2]  + digit[3] + "\u00a74" + "\u272a";
        }
        if (Stars >= 1700 && Stars < 1800) {
            String[] digit = text.split("");
            return "\u00a7d" + digit[0] + digit[1] + digit[2] + digit[3] + "\u00a75" + "\u272a";
        }
        if (Stars >= 1800 && Stars < 1900) {
            String[] digit = text.split("");
            return "\u00a79" + digit[0] + digit[1] + digit[2] + digit[3] + "\u00a71"  + "\u272a";
        }
        if (Stars >= 1900 && Stars < 2000) {
            String[] digit = text.split("");
            return "\u00a75" + digit[0] + digit[1] + digit[2] + digit[3] + "\u00a78"  + "\u272a";
        }
        if (Stars >= 2000 && Stars < 2100) {
            String[] digit = text.split("");
            return "\u00a77" + digit[0] + "\u00a7f" + digit[1] + digit[2] + "\u00a77" + digit[3] + "\u269d";
        }
        if (Stars >= 2100 && Stars < 2200) {
            String[] digit = text.split("");
            return "\u00a7f" + digit[0] + "\u00a7e" + digit[1] + digit[2] + "\u00a76" + digit[3] + "\u269d";
        }
        if (Stars >= 2200 && Stars < 2300) {
            String[] digit = text.split("");
            return "\u00a76" + digit[0] + "\u00a7f" + digit[1] + digit[2] + "\u00a7b" + digit[3] + "\u00a73" + "\u269d";
        }
        if (Stars >= 2300 && Stars < 2400) {
            String[] digit = text.split("");
            return "\u00a75" + digit[0] + "\u00a7d" + digit[1] + digit[2] + "\u00a76" + digit[3] + "\u00a7e" + "\u269d";
        }
        if (Stars >= 2400 && Stars < 2500) {
            String[] digit = text.split("");
            return "\u00a7b" + digit[0] + "\u00a7f" + digit[1] + digit[2] + "\u00a77" + digit[3] + "\u269d";
        }
        if (Stars >= 2500 && Stars < 2600) {
            String[] digit = text.split("");
            return "\u00a7f" + digit[0] + "\u00a7a" + digit[1] + digit[2] + "\u00a72" + digit[3] + "\u269d";
        }
        if (Stars >= 2600 && Stars < 2700) {
            String[] digit = text.split("");
            return "\u00a74" + digit[0] + "\u00a7c" + digit[1] + digit[2] + "\u00a7d" + digit[3] + "\u269d";
        }
        if (Stars >= 2700 && Stars < 2800) {
            String[] digit = text.split("");
            return "\u00a7e" + digit[0] + "\u00a7f" + digit[1] + digit[2] + "\u00a78" + digit[3] + "\u269d";
        }
        if (Stars >= 2800 && Stars < 2900) {
            String[] digit = text.split("");
            return "\u00a7a" + digit[0] + "\u00a72" + digit[1] + digit[2] + "\u00a76" + digit[3] + "\u269d";
        }
        if (Stars >= 2900 && Stars < 3000) {
            String[] digit = text.split("");
            return "\u00a7b" + digit[0] + "\u00a73" + digit[1] + digit[2] + "\u00a79" + digit[3] + "\u269d";
        }
        if (Stars >= 3000) {
            String[] digit = text.split("");
            return "\u00a7e" + digit[0] + "\u00a76" + digit[1] + digit[2] + "\u00a7c" + digit[3] + "\u269d";
        }
        return "NaN";
    }

    private String extractValue(String text, String startDelimiter, String endDelimiter) {
        int startIndex = text.indexOf(startDelimiter);
        if (startIndex == -1) return "N/A"; // Not found
        startIndex += startDelimiter.length();
        int endIndex = text.indexOf(endDelimiter, startIndex);
        if (endIndex == -1) return "N/A"; // Not found
        return text.substring(startIndex, endIndex).trim();
    }
  /*
    private String fetchPlayerStats(String playerName) throws IOException {
        String url = "https://plancke.io/hypixel/player/stats/" + playerName;

        java.net.URL urlObject = new java.net.URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        java.io.InputStream inputStream = connection.getInputStream();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
        StringBuilder responseText = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();

        String response = responseText.toString();


        Pattern namePattern = Pattern.compile("(?<=content=\"Plancke\" /><meta property=\"og:locale\" content=\"en_US\" /><meta property=\"og:description\" content=\").+?(?=\")");
        Matcher nameMatcher = namePattern.matcher(response);
        String displayedName = nameMatcher.find() ? nameMatcher.group() : "Unknown";

        Pattern kdPattern = Pattern.compile("<td style=\"border-right: 1px solid #f3f3f3\">(-|\\d+(\\.\\d+)?)</td>");
        Matcher kdMatcher = kdPattern.matcher(response);
        List<String> kdList = new ArrayList<>();
        while (kdMatcher.find()) {
            kdList.add(kdMatcher.group(1));
        }

        String finalKd = (kdList.size() > 0) ? kdList.get(kdList.size() - 2) : "N/A";
        if (!finalKd.equals("N/A")) {
            try {
                double fkdrValue = Double.parseDouble(finalKd);
                if (minFkdr != DEFAULT_MIN_FKDR && fkdrValue < minFkdr) {
                    return "";
                }



                String fkdrColor = "\u00a7r"; // Default
                if (fkdrValue >= 10 && fkdrValue < 20) {
                    fkdrColor = "\u00a76"; // Gold
                } else if (fkdrValue >= 20) {
                    fkdrColor = "\u00a74"; // Red
                }
                String playerrank = ""; // empty if no rank
                String trimmedName = displayedName.trim();


                String[] parts = trimmedName.split("\\s+", 2);
                if (parts.length > 0 && parts[0].startsWith("[") && parts[0].endsWith("]")) {
                    playerrank = parts[0];
                }
                // above there is insane method to parse rank !!

                return playerrank + " " + getTabDisplayName(playerName) + " \u00a7r| FKDR: " + fkdrColor + finalKd;
            } catch (NumberFormatException ignored) {
            }
        }

        return "";
    }
    */

    public class BwModeCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "bwmode";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/bwmode <plancke/bws>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || (!args[0].equalsIgnoreCase("plancke") && !args[0].equalsIgnoreCase("bws"))) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /bwmode <bws>"));
                return;
            }

            mode = args[0].toLowerCase();
            saveConfig(minFkdr, mode, tags,tabstats,urchin,urchinkey,reqUUID,autowho);
            sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aMode set to: " + mode));
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    private void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            saveConfig(DEFAULT_MIN_FKDR, DEFAULT_MODE, false,true,false,"",true,true);
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            minFkdr = json.has("minFkdr") ? json.get("minFkdr").getAsInt() : DEFAULT_MIN_FKDR;
            mode = json.has("mode") ? json.get("mode").getAsString() : DEFAULT_MODE;
            tags = json.has("tags") ? json.get("tags").getAsBoolean() : false;
            tabstats = json.has("tablist") ? json.get("tablist").getAsBoolean() : true;
            urchin = json.has("urchin") ? json.get("urchin").getAsBoolean() : false;
            reqUUID = json.has("reqUUID") ? json.get("reqUUID").getAsBoolean() : true;
            autowho = json.has("autowho") ? json.get("autowho").getAsBoolean() : true;
            urchinkey = json.has("urchinkey") ? json.get("urchinkey").getAsString() : "";
        } catch (IOException e) {
            e.printStackTrace();
            minFkdr = DEFAULT_MIN_FKDR;
            mode = DEFAULT_MODE;
            tags = false;
            tabstats = true;
            urchin = false;
            urchinkey = "";
            reqUUID = true;
            autowho = true;
        }
    }

    private void saveConfig(int minFkdrValue, String modeValue, boolean TagsConfig, boolean TabstatsConfig, boolean UrchinConfig, String UrchinkeySetting, boolean reqUUID, boolean autowhosetting) {
        File configFile = new File(CONFIG_PATH);
        configFile.getParentFile().mkdirs();

        try (Writer writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();
            json.addProperty("minFkdr", minFkdrValue);
            json.addProperty("mode", modeValue);
            json.addProperty("tags", TagsConfig);
            json.addProperty("tablist", TabstatsConfig);
            json.addProperty("urchin", UrchinConfig);
            json.addProperty("urchinkey", UrchinkeySetting);
            json.addProperty("reqUUID", reqUUID);
            json.addProperty("autowho", autowhosetting);
            new Gson().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class MinFkdrCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "minfkdr";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/minfkdr <number>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /minfkdr <number>\u00a7r"));
                return;
            }

            try {
                int value = Integer.parseInt(args[0]);
                minFkdr = value;
                saveConfig(value, mode,tags,tabstats,urchin,urchinkey,reqUUID,autowho);
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aMinimum FKDR set to: " + value));
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid number! Use an integer value."));
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    public class SetUrchinKeyCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "urchinkey";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/urchinkey <apikey>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /urchinkey <apikey>\u00a7r"));
                return;
            }

            try {
                String value = args[0].toString();
                urchinkey = value;
                saveConfig(minFkdr, mode,tags,tabstats,urchin,urchinkey,reqUUID,autowho);
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aUrchin API Key set to: " + value));
            } catch(Exception e) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cfish."));
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    private String fetchPlayerStatss(String playerName) throws IOException {
        String uuid = fetchUUID(playerName);
        String stjson = nadeshikoAPI(uuid);
        if (stjson == null || stjson.isEmpty()) {
            throw new IOException("Received empty response from API for player: " + playerName);
        }

        JsonObject rootObject = new JsonParser().parse(stjson).getAsJsonObject();

        JsonObject profile = rootObject.getAsJsonObject("profile");
        String displayedName = profile.has("tagged_name") ? profile.get("tagged_name").getAsString() : "[]";
        JsonObject ach = rootObject.getAsJsonObject("achievements");
        String level = ach.has("bedwars_level") ? ach.get("bedwars_level").getAsString() : "0";
        level = formatStars(level);

        JsonObject bedwarsStats = rootObject.getAsJsonObject("stats").getAsJsonObject("Bedwars");
        int finalKills = bedwarsStats.has("final_kills_bedwars") ? bedwarsStats.get("final_kills_bedwars").getAsInt() : 0;
        int finalDeaths = bedwarsStats.has("final_deaths_bedwars") ? bedwarsStats.get("final_deaths_bedwars").getAsInt() : 0;
        double fkdr = (finalDeaths == 0) ? finalKills : (double) finalKills / finalDeaths;
        String fkdrColor = "\u00a77";
        if (fkdr >= 1 && fkdr < 3) fkdrColor = "\u00a7f";
        if (fkdr >= 3 && fkdr < 8) fkdrColor = "\u00a7a";
        if (fkdr >= 8 && fkdr < 16) fkdrColor = "\u00a76";
        if (fkdr >= 16 && fkdr < 25) fkdrColor = "\u00a7d";
        if (fkdr > 25) fkdrColor = "\u00a74";
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedFkdr = df.format(fkdr);
        return displayedName + " \u00a7r" + level +" FKDR: " + fkdrColor + formattedFkdr;
    }

// Command to manually check individual stats
    public class BedwarsCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "bw";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/bw <username>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r]\u00a7cInvalid usage!\u00a7r Use /bw \u00a75<username>\u00a7r"));
                return;
            }

            String username = args[0];
            new Thread(() -> {
                try {
                    String stats = fetchPlayerStatss(username);
                    Minecraft.getMinecraft().addScheduledTask(() ->
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] " + stats))
                    );
                } catch (IOException e) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cFailed to fetch stats for: \u00a7r" + username))
                    );
                }
            }).start();
        }
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream()
                    .map(NetworkPlayerInfo::getGameProfile)
                    .map(GameProfile::getName)
                    .toArray(String[]::new));
        }
        return null;
    }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }



    }
    /* ========================================================= */
    public class StatsifyCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "st";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/st";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {

            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a7b\u00a7lfon\u00a79\u00a7lta\u00a73\u00a7line\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a7bmade by\u00a7e melissalmao\u00a7r"));
            sender.addChatMessage(new ChatComponentText(""));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/bw <username>:\u00a7b Manually check bedwars stats of a player.\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/minfkdr <value>:\u00a7b Set minimum FKDR to show on running /who. Default -1.\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/bwtags <info/on/off>:\u00a7b Toggle tags on /who (on/off) or view information (info). Default off. [indev]\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/tabstats <on/off>:\u00a7b Toggle printing stats on tablist. Default on. (TEAM [STAR] Name\u30fbFKDR)\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/cleartabcache:\u00a7b Clear stats cache of players if you're having issues.\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/reqUUID <on/off>:\u00a7b If the stats check is failing try turning this off.\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/urchin <on/off>:\u00a7b Toggle Urchin API on and off. Default off.\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/urchinkey <key>:\u00a7b Set your urchin API key (discord.gg/urchin)\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/who:\u00a7b Check and print the stats of the players in your lobby.\u00a7r"));
            sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73/bwautowho:\u00a7b Automatically run /who on game start. Default on.\u00a7r"));
            sender.addChatMessage(new ChatComponentText(""));
        }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    /* ========================================================= */
    private void catchAndIgnoreNullPointerException(Runnable runnable) {
        try {
            runnable.run();
        } catch (NullPointerException e) {
           }
    }
    /* debug func */
    private static int countOccurrences(String str, String subStr) {
        int count = 0;
        int idx = 0;

        while ((idx = str.indexOf(subStr, idx)) != -1) {
            count++;
            idx += subStr.length();
        }

        return count;
    }
    public class ToggleTagsCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "bwtags";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/bwtags <info/on/off>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || (!args[0].equalsIgnoreCase("info") && !args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /bwtags <info/on/off>"));
                return;
            }

            String arg = args[0].toLowerCase();
            if(arg.equalsIgnoreCase("info")) {
                sender.addChatMessage(new ChatComponentText("\u00a7r\u00a7b\u00a7lfon\u00a79\u00a7lta\u00a73\u00a7line\u00a7r"));
                sender.addChatMessage(new ChatComponentText("\u00a7r\u00a7bmade by\u00a7e melissalmao\u00a7r"));
                sender.addChatMessage(new ChatComponentText(""));
                sender.addChatMessage(new ChatComponentText("\u00a7r\u00a73Tags are in early development and WILL slow down checking stats.\u00a7r"));
                sender.addChatMessage(new ChatComponentText("\u00a7r        N = Suspicious name (kikin, mchk, msmc, 4+ number in name..)\n" +
                        "        W = Winstreak while being low star (1+ WS when 1-6 star)\n" +
                        "        F = Suspiciously high fkdr while being low star (4+ when 1 - 6 star)\n" +
                        "        SK = Default skin\n" +
                        "        NL = New login (today or yesterday first login)\n" +
                        "        0F = 0 final kills 0 final deaths\u00a7r"));

                sender.addChatMessage(new ChatComponentText(""));


            }
            if(arg.equalsIgnoreCase("on")) {
                saveConfig(minFkdr,mode,true,tabstats,urchin,urchinkey,reqUUID,autowho);
                tags = true;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aTags toggled: " + arg));
            }
            if(arg.equalsIgnoreCase("off")) {
                saveConfig(minFkdr,mode,false,tabstats,urchin,urchinkey,reqUUID,autowho);
                tags = false;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aTags toggled: " + arg));
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }

    public class TablistToggleCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "tabstats";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/tabstats <on/off>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /tabstats <on/off>"));
                return;
            }

            String arg = args[0].toLowerCase();
            if(arg.equalsIgnoreCase("on")) {
                saveConfig(minFkdr,mode,tags,true,urchin,urchinkey,reqUUID,autowho);
                tabstats = true;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aTabstats toggled: " + arg));
            }
            if(arg.equalsIgnoreCase("off")) {
                saveConfig(minFkdr,mode,tags,false,urchin,urchinkey,reqUUID,autowho);
                tabstats = false;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aTabstats toggled: " + arg));
            }
        }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    public class RequireUUIDToggleCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "reqUUID";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/reqUUID <on/off>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /reqUUID <on/off>"));
                return;
            }

            String arg = args[0].toLowerCase();
            if(arg.equalsIgnoreCase("on")) {
                saveConfig(minFkdr,mode,tags,tabstats,urchin,urchinkey,true,autowho);
                reqUUID = true;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7arequire UUID: " + arg));
            }
            if(arg.equalsIgnoreCase("off")) {
                saveConfig(minFkdr,mode,tags,tabstats,urchin,urchinkey,false,autowho);
                reqUUID = false;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7arequire UUID: " + arg));
            }
        }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    public class AutoWhoToggleCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "bwautowho";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/bwautowho <on/off>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /bwautowho <on/off>"));
                return;
            }

            String arg = args[0].toLowerCase();
            if(arg.equalsIgnoreCase("on")) {
                saveConfig(minFkdr,mode,tags,tabstats,urchin,urchinkey,reqUUID,true);
                autowho = true;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aautoWHO: " + arg));
            }
            if(arg.equalsIgnoreCase("off")) {
                saveConfig(minFkdr,mode,tags,tabstats,urchin,urchinkey,reqUUID,false);
                autowho = false;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aautoWHO: " + arg));
            }
        }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    public class UrchinTagsToggleCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "urchin";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/urchin <on/off>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7cInvalid usage! Use /urchin <on/off>"));
                return;
            }

            String arg = args[0].toLowerCase();
            if(arg.equalsIgnoreCase("on")) {
                saveConfig(minFkdr,mode,tags,tabstats,true,urchinkey,reqUUID,autowho);
                urchin = true;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aUrchinAPI toggled: " + arg));
            }
            if(arg.equalsIgnoreCase("off")) {
                saveConfig(minFkdr,mode,tags,tabstats,false,urchinkey,reqUUID,autowho);
                urchin = false;
                sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aUrchinAPI toggled: " + arg));
            }
        }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    public class ClearCacheCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "cleartabcache";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/cleartabcache";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {

            sender.addChatMessage(new ChatComponentText("\u00a7r[\u00a7bF\u00a7r] \u00a7aTab cache has been wiped"));
            playerSuffixes.clear();

        }
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
}
