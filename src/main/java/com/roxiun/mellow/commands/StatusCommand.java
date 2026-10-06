package com.roxiun.mellow.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.cache.TimedValueCache;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StatusCommand extends CommandBase {

    private static final long STATUS_CACHE_TTL_MS = 30_000L;

    private final MojangApi mojangApi;
    private final MellowOneConfig config;
    private final String hypixelApiKey;
    private final String lunaPingApiKey;
    private final boolean bordicStatusProvider;
    private final OkHttpClient client;
    private final TimedValueCache<String, JsonObject> responseCache =
        new TimedValueCache<>(STATUS_CACHE_TTL_MS);

    public StatusCommand(MojangApi mojangApi, MellowOneConfig config) {
        this(mojangApi, config, new OkHttpClient());
    }

    StatusCommand(
        MojangApi mojangApi,
        MellowOneConfig config,
        OkHttpClient client
    ) {
        this.mojangApi = mojangApi;
        this.config = config;
        this.hypixelApiKey = null;
        this.lunaPingApiKey = null;
        this.bordicStatusProvider = false;
        this.client = client == null ? new OkHttpClient() : client;
    }

    StatusCommand(
        MojangApi mojangApi,
        String hypixelApiKey,
        String lunaPingApiKey,
        OkHttpClient client
    ) {
        this(mojangApi, hypixelApiKey, lunaPingApiKey, false, client);
    }

    StatusCommand(
        MojangApi mojangApi,
        String hypixelApiKey,
        String lunaPingApiKey,
        boolean bordicStatusProvider,
        OkHttpClient client
    ) {
        this.mojangApi = mojangApi;
        this.config = null;
        this.hypixelApiKey = normalizeApiKey(hypixelApiKey);
        this.lunaPingApiKey = normalizeApiKey(lunaPingApiKey);
        this.bordicStatusProvider = bordicStatusProvider;
        this.client = client == null ? new OkHttpClient() : client;
    }

    @Override
    public String getCommandName() {
        return "mstatus";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mstatus <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /mstatus <username>"
            );
            return;
        }

        boolean hasHypixelKey = hasValue(getHypixelApiKey());
        boolean hasLunaKey = hasValue(getLunaApiKey());
        boolean useCache = useCacheStatusProvider();
        if (!hasHypixelKey && !hasLunaKey && !useCache) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cSet a Hypixel or Luna API key, or select Bordic or Bedlify as your Stats Provider in OneConfig first."
            );
            return;
        }

        String username = args[0];
        ChatUtils.sendCommandMessage(
            sender,
            "§7Fetching status for §f" + username + "§7..."
        );

        AsyncExecutor.getInstance().command(() -> {
            UUID uuid = PlayerUtils.resolveLookupUuid(username, mojangApi);
            if (uuid == null) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cPlayer not found: §r" + username
                    )
                );
                return;
            }

            List<String> lines = getStatusLines(
                username,
                uuid,
                hasHypixelKey,
                hasLunaKey
            );

            MainThreadDispatcher.run(() ->
                ChatUtils.sendMultilineCommandMessage(sender, lines)
            );
        });
    }

    List<String> getStatusLines(
        String username,
        UUID uuid,
        boolean hasHypixelKey,
        boolean hasLunaKey
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("§f" + username);

        if (hasHypixelKey) {
            addHypixelStatus(lines, fetchHypixelStatus(uuid));
        }

        boolean useCache = useCacheStatusProvider();
        JsonObject playerData = null;
        if (useCache) {
            playerData = fetchCachedPlayer(uuid);
        } else if (hasHypixelKey) {
            playerData = fetchHypixelLastLogin(uuid);
        }

        if (useCache) {
            addCachedStatus(lines, playerData, !hasHypixelKey);
        }
        addHypixelLastLogin(lines, playerData);

        if (hasLunaKey) {
            addLunaLobbyMessage(lines, fetchLunaLobbyHistory(uuid));
        }

        if (lines.size() == 1) {
            lines.add("§7No data available.");
        }

        return lines;
    }

    private void addHypixelStatus(List<String> lines, JsonObject json) {
        if (json == null || !isSuccess(json) || !json.has("session")) {
            return;
        }

        JsonObject session = json.getAsJsonObject("session");
        boolean online =
            session.has("online") && session.get("online").getAsBoolean();
        if (!online) {
            lines.add("§rStatus: §7Hidden / Offline");
            return;
        }

        StringBuilder status = new StringBuilder("§aOnline");
        String gameType = getString(session, "gameType");
        String mode = getString(session, "mode");
        if (gameType != null) {
            status.append(" §7- §f").append(formatGameType(gameType));
            if (mode != null && !"LOBBY".equalsIgnoreCase(mode)) {
                status.append(" §7(").append(mode).append("§7)");
            }
        }
        lines.add("§rStatus: " + status);
    }

    private void addHypixelLastLogin(List<String> lines, JsonObject json) {
        if (json == null || !isSuccess(json) || !json.has("player")) {
            return;
        }

        JsonObject player = json.getAsJsonObject("player");
        if (player.has("lastLogin") && !player.get("lastLogin").isJsonNull()) {
            lines.add(
                "§7Last login: §f" +
                    formatDateTime(player.get("lastLogin").getAsLong())
            );
        }
    }

    private void addCachedStatus(
        List<String> lines,
        JsonObject json,
        boolean inferOnlineStatus
    ) {
        if (json == null || !isSuccess(json) || !json.has("player")) {
            return;
        }

        JsonObject player = json.getAsJsonObject("player");
        boolean hasLastLogin =
            player.has("lastLogin") && !player.get("lastLogin").isJsonNull();
        boolean hasLastLogout =
            player.has("lastLogout") && !player.get("lastLogout").isJsonNull();
        if (!hasLastLogin || !hasLastLogout) {
            if (
                json.has("lastUpdated") &&
                !json.get("lastUpdated").isJsonNull()
            ) {
                lines.add(
                    "§7" + cacheProviderName() + " cache updated: §f" +
                        formatDateTime(json.get("lastUpdated").getAsLong())
                );
            }
            return;
        }

        long lastLogin = player.get("lastLogin").getAsLong();
        long lastLogout = player.get("lastLogout").getAsLong();
        if (inferOnlineStatus) {
            lines.add(
                lastLogin > lastLogout
                    ? "§rStatus: §aOnline"
                    : "§rStatus: §7Hidden / Offline"
            );
        }
    }

    private void addLunaLobbyMessage(List<String> lines, JsonObject json) {
        if (
            json == null ||
            !json.has("history") ||
            !json.get("history").isJsonArray()
        ) {
            return;
        }

        JsonArray history = json.getAsJsonArray("history");
        JsonObject entry = firstHistoryEntry(history);
        if (entry == null) {
            return;
        }

        String message = getFirstString(
            entry,
            "message",
            "content",
            "msg",
            "text"
        );
        if (message == null || message.isEmpty()) {
            return;
        }

        long timestamp = getFirstTimestamp(
            entry,
            "timestamp",
            "time",
            "date",
            "sent_at",
            "created_at"
        );
        String line = "§7Last lobby msg: §f\"" + message + "\"";
        if (timestamp > 0L) {
            line += " §7(" + formatRelativeTime(timestamp) + ")";
        }
        lines.add(line);
    }

    private JsonObject fetchHypixelStatus(UUID uuid) {
        String apiKey = getHypixelApiKey();
        if (uuid == null || apiKey.isEmpty()) {
            return null;
        }

        String url =
            "https://api.hypixel.net/status?key=" + apiKey + "&uuid=" + uuid;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();
        return fetchJsonResponse(
            buildResponseCacheKey("hypixel-status", uuid.toString(), apiKey),
            request,
            false
        );
    }

    private JsonObject fetchHypixelLastLogin(UUID uuid) {
        String apiKey = getHypixelApiKey();
        if (uuid == null || apiKey.isEmpty()) {
            return null;
        }

        String url =
            "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();
        return fetchJsonResponse(
            buildResponseCacheKey("hypixel-player", uuid.toString(), apiKey),
            request,
            false
        );
    }

    private JsonObject fetchCachedPlayer(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        String url =
            (useBedlifyStatusProvider()
                ? "https://api.bedlify.xyz/v1/player/cache?uuid="
                : "https://api.bordic.xyz/v3/cache/hypixel?uuid=") + uuid;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();
        return fetchJsonResponse(
            buildResponseCacheKey(cacheProviderName() + "-player", uuid.toString(), ""),
            request,
            true
        );
    }

    private JsonObject fetchLunaLobbyHistory(UUID uuid) {
        String apiKey = getLunaApiKey();
        if (uuid == null || apiKey.isEmpty()) {
            return null;
        }

        String authKey = "LunaAPI-" + apiKey;
        String url = "https://lunaaaa.net/chat/" + uuid + "?key=" + authKey;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .header("Authorization", "Bearer " + authKey)
            .build();
        return fetchJsonResponse(
            buildResponseCacheKey("luna-chat", uuid.toString(), authKey),
            request,
            true
        );
    }

    private JsonObject fetchJsonResponse(
        String cacheKey,
        Request request,
        boolean requireSuccessfulResponse
    ) {
        if (responseCache.containsFresh(cacheKey)) {
            return responseCache.get(cacheKey);
        }

        try (Response response = client.newCall(request).execute()) {
            if (requireSuccessfulResponse && !response.isSuccessful()) {
                return null;
            }

            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }

            JsonObject json = new JsonParser()
                .parse(body.string())
                .getAsJsonObject();
            responseCache.put(cacheKey, json);
            return json;
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject firstHistoryEntry(JsonArray history) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).isJsonObject()) {
                return history.get(i).getAsJsonObject();
            }
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isJsonObject()) {
                return history.get(i).getAsJsonObject();
            }
        }
        return null;
    }

    private String getFirstString(JsonObject object, String... fields) {
        for (String field : fields) {
            String value = getString(object, field);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private long getFirstTimestamp(JsonObject object, String... fields) {
        for (String field : fields) {
            if (!object.has(field) || object.get(field).isJsonNull()) {
                continue;
            }

            try {
                double numeric = object.get(field).getAsDouble();
                long timestamp = (long) numeric;
                if (timestamp > 1_000_000_000_000L) {
                    timestamp /= 1000L;
                }
                if (timestamp > 0L) {
                    return timestamp;
                }
            } catch (Exception ignored) {}

            try {
                String iso = object.get(field).getAsString();
                Date parsed = null;
                try {
                    parsed = isoFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(
                        iso
                    );
                } catch (Exception ignored) {}
                if (parsed == null) {
                    try {
                        parsed = isoFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(
                            iso
                        );
                    } catch (Exception ignored) {}
                }
                if (parsed != null) {
                    return parsed.getTime() / 1000L;
                }
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private boolean isSuccess(JsonObject json) {
        return json.has("success") && json.get("success").getAsBoolean();
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildResponseCacheKey(
        String scope,
        String value,
        String apiKey
    ) {
        return scope + "|" + value + "|" + apiKey;
    }

    private String normalizeApiKey(String value) {
        return value == null ? "" : value.trim();
    }

    private String getHypixelApiKey() {
        return config == null
            ? hypixelApiKey
            : normalizeApiKey(config.hypixelApiKey);
    }

    private String getLunaApiKey() {
        return config == null
            ? lunaPingApiKey
            : normalizeApiKey(config.lunaPingApiKey);
    }

    private boolean useBedlifyStatusProvider() {
        return config != null && config.statsProvider == 4;
    }

    private String cacheProviderName() {
        return useBedlifyStatusProvider() ? "Bedlify" : "Bordic";
    }

    private boolean useCacheStatusProvider() {
        return config == null
            ? bordicStatusProvider
            : config.statsProvider == 3 || config.statsProvider == 4;
    }

    private String getString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private String formatGameType(String gameType) {
        String normalized = gameType.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String formatDateTime(long epochMillis) {
        SimpleDateFormat format = isoFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
        return format.format(new Date(epochMillis));
    }

    private String formatRelativeTime(long unixSeconds) {
        long now = System.currentTimeMillis() / 1000L;
        long diff = Math.max(0L, now - unixSeconds);

        if (diff < 60L) {
            return diff + "s ago";
        }
        if (diff < 3600L) {
            return (diff / 60L) + "m ago";
        }
        if (diff < 86_400L) {
            return (diff / 3600L) + "h ago";
        }
        return (diff / 86_400L) + "d ago";
    }

    private SimpleDateFormat isoFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
