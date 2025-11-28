package com.roxiun.mellow.util.blacklist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;

public class BlacklistManager {

    private final File blacklistFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, BlacklistedPlayer> blacklist = new ConcurrentHashMap<>();

    public BlacklistManager() {
        File configDir = new File(
            Minecraft.getMinecraft().mcDataDir,
            "config/mellow"
        );
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.blacklistFile = new File(configDir, "blacklist.json");
        loadBlacklist();
    }

    public void loadBlacklist() {
        if (blacklistFile.exists()) {
            try (FileReader reader = new FileReader(blacklistFile)) {
                Type type = new TypeToken<
                    ConcurrentHashMap<UUID, BlacklistedPlayer>
                >() {}.getType();
                blacklist = gson.fromJson(reader, type);
                if (blacklist == null) {
                    blacklist = new ConcurrentHashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveBlacklist() {
        try (FileWriter writer = new FileWriter(blacklistFile)) {
            gson.toJson(blacklist, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPlayer(UUID uuid, String name, String reason) {
        blacklist.put(uuid, new BlacklistedPlayer(name, reason));
        saveBlacklist();
    }

    public void removePlayer(UUID uuid) {
        if (blacklist.remove(uuid) != null) {
            saveBlacklist();
        }
    }

    public boolean isBlacklisted(UUID uuid) {
        return blacklist.containsKey(uuid);
    }

    public BlacklistedPlayer getBlacklistedPlayer(UUID uuid) {
        return blacklist.get(uuid);
    }

    public Map<UUID, BlacklistedPlayer> getBlacklist() {
        return blacklist;
    }
}
