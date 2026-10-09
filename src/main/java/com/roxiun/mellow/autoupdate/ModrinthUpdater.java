package com.roxiun.mellow.autoupdate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.ChatUtils;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
//? if ornithe {
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
//?} else {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
*///?}
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ModrinthUpdater {

    private static final String MODRINTH_API_URL =
        "https://api.modrinth.com/v2/project/statsify/version";
    private static final String MODRINTH_PROJECT_URL =
        "https://modrinth.com/mod/statsify";
    private static final String TARGET_GAME_VERSION = "1.8.9";
    //? if ornithe {
    private static final String TARGET_LOADER = "ornithe";
    //?} else {
    /*private static final String TARGET_LOADER = "forge";
    *///?}
    private static final String RELEASE_TYPE = "release";
    private static final int CHANGELOG_PREVIEW_MAX = 180;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final ModrinthUpdater INSTANCE = new ModrinthUpdater();

    private static volatile boolean isOutdated = false;
    private static volatile boolean hasPromptedThisLaunch = false;
    private static volatile String latestVersion = "";
    private static volatile String downloadUrl = MODRINTH_PROJECT_URL;
    private static volatile String changelog = "";

    private ModrinthUpdater() {}

    public static void init(MellowOneConfig config) {
        if (config == null || !config.autoUpdateCheck) {
            return;
        }
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        //? if ornithe {
        EventManager.INSTANCE.register(INSTANCE);
        //?} else {
        /*MinecraftForge.EVENT_BUS.register(INSTANCE);
        *///?}
        checkForUpdates();
    }

    private static void checkForUpdates() {
        Request request = new Request.Builder()
            .url(MODRINTH_API_URL)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();

        HTTP_CLIENT
            .newCall(request)
            .enqueue(
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logDebug("Modrinth update check failed.", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                        throws IOException {
                        try (Response safeResponse = response) {
                            if (
                                !safeResponse.isSuccessful() ||
                                safeResponse.body() == null
                            ) {
                                return;
                            }

                            String responseBody = safeResponse.body().string();
                            JsonElement parsed = new JsonParser().parse(
                                responseBody
                            );
                            if (!parsed.isJsonArray()) {
                                return;
                            }

                            JsonObject compatibleVersion =
                                findLatestCompatibleVersion(
                                    parsed.getAsJsonArray()
                                );
                            if (compatibleVersion == null) {
                                return;
                            }

                            String remoteVersion = getString(
                                compatibleVersion,
                                "version_number"
                            );
                            if (remoteVersion.isEmpty()) {
                                return;
                            }
                            if (
                                !isRemoteVersionNewer(
                                    Mellow.VERSION,
                                    remoteVersion
                                )
                            ) {
                                return;
                            }

                            latestVersion = remoteVersion;
                            changelog = getString(
                                compatibleVersion,
                                "changelog"
                            );
                            downloadUrl = getDownloadUrl(compatibleVersion);
                            hasPromptedThisLaunch = false;
                            isOutdated = true;
                        } catch (Exception e) {
                            logDebug("Failed to parse Modrinth versions.", e);
                        }
                    }
                }
            );
    }

    private static JsonObject findLatestCompatibleVersion(JsonArray versions) {
        for (JsonElement element : versions) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject version = element.getAsJsonObject();
            if (!RELEASE_TYPE.equalsIgnoreCase(getString(version, "version_type"))) {
                continue;
            }
            if (!containsValue(version, "game_versions", TARGET_GAME_VERSION)) {
                continue;
            }
            if (!containsValue(version, "loaders", TARGET_LOADER)) {
                continue;
            }

            return version;
        }
        return null;
    }

    private static boolean containsValue(
        JsonObject object,
        String key,
        String expected
    ) {
        if (
            object == null ||
            key == null ||
            expected == null ||
            !object.has(key) ||
            !object.get(key).isJsonArray()
        ) {
            return false;
        }

        JsonArray array = object.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) {
                continue;
            }
            String value = element.getAsString();
            if (expected.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String getDownloadUrl(JsonObject version) {
        String versionId = getString(version, "id");
        if (!versionId.isEmpty()) {
            return MODRINTH_PROJECT_URL + "/version/" + versionId;
        }
        return MODRINTH_PROJECT_URL;
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return "";
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    static boolean isRemoteVersionNewer(
        String currentVersion,
        String remoteVersion
    ) {
        if (currentVersion == null || remoteVersion == null) {
            return false;
        }

        String current = currentVersion.trim();
        String remote = remoteVersion.trim();

        if (current.isEmpty() || remote.isEmpty() || current.equals(remote)) {
            return false;
        }

        int[] currentParts = parseNumericVersion(current);
        int[] remoteParts = parseNumericVersion(remote);
        if (currentParts == null || remoteParts == null) {
            return true;
        }

        int maxLength = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ? currentParts[i] : 0;
            int remotePart = i < remoteParts.length ? remoteParts[i] : 0;

            if (remotePart > currentPart) {
                return true;
            }
            if (remotePart < currentPart) {
                return false;
            }
        }

        return false;
    }

    private static int[] parseNumericVersion(String version) {
        if (version == null || !version.matches("\\d+(\\.\\d+)*")) {
            return null;
        }

        String[] tokens = version.split("\\.");
        int[] parts = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                parts[i] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return parts;
    }

    //? if ornithe {
    @Subscribe
    public void onClientTick(TickEvent.End event) {
    //?} else {
    /*@SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
    *///?}
        if (!isOutdated || hasPromptedThisLaunch) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return;
        }

        hasPromptedThisLaunch = true;

        ChatUtils.sendMessage(
            "§7A new version of §dMellow§7 is available: §c" +
            Mellow.VERSION +
            " §7-> §a" +
            latestVersion
        );

        IChatComponent downloadMessage = new ChatComponentText(
            "§7Click §b§nhere§r§7 to open the download page."
        );
        downloadMessage
            .getChatStyle()
            .setChatClickEvent(
                new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl)
            );
        ChatUtils.sendMessage(downloadMessage);

        String changelogPreview = formatChangelogPreview(changelog);
        if (!changelogPreview.isEmpty()) {
            ChatUtils.sendMessage("§aChangelog: §7" + changelogPreview);
        }
    }

    private static String formatChangelogPreview(String rawChangelog) {
        if (rawChangelog == null) {
            return "";
        }

        String normalized = rawChangelog
            .replace("\r", "")
            .replace("\n", " ")
            .trim();

        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.length() <= CHANGELOG_PREVIEW_MAX) {
            return normalized;
        }
        return normalized.substring(0, CHANGELOG_PREVIEW_MAX - 3) + "...";
    }

    private static void logDebug(String message, Exception exception) {
        if (exception == null) {
            System.out.println("[Mellow] " + message);
            return;
        }

        String detail = exception.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = exception.getClass().getSimpleName();
        }
        System.out.println("[Mellow] " + message + " " + detail);
    }
}
