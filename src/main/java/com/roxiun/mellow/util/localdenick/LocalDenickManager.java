package com.roxiun.mellow.util.localdenick;

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

public class LocalDenickManager {

    private final File localDenickFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, LocalDenickedPlayer> localDenickList =
        new ConcurrentHashMap<>();

    public LocalDenickManager() {
        this(resolveConfigDir());
    }

    LocalDenickManager(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.localDenickFile = new File(configDir, "localdenick.json");
        loadLocalDenicks();
    }

    public static LocalDenickManager createForTests(File configDir) {
        return new LocalDenickManager(configDir);
    }

    private static File resolveConfigDir() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/mellow");
    }

    public void loadLocalDenicks() {
        if (localDenickFile.exists()) {
            try (FileReader reader = new FileReader(localDenickFile)) {
                Type type = new TypeToken<
                    ConcurrentHashMap<UUID, LocalDenickedPlayer>
                >() {}.getType();
                localDenickList = gson.fromJson(reader, type);
                if (localDenickList == null) {
                    localDenickList = new ConcurrentHashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveLocalDenicks() {
        try (FileWriter writer = new FileWriter(localDenickFile)) {
            gson.toJson(localDenickList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addPlayer(UUID uuid, String name, String nick) {
        if (localDenickList.containsKey(uuid)) {
            return false;
        }
        localDenickList.put(uuid, new LocalDenickedPlayer(name, nick));
        saveLocalDenicks();
        return true;
    }

    public void removePlayer(UUID uuid) {
        if (localDenickList.remove(uuid) != null) {
            saveLocalDenicks();
        }
    }

    public LocalDenickedPlayer getLocalDenickedPlayer(UUID uuid) {
        return localDenickList.get(uuid);
    }

    public Map<UUID, LocalDenickedPlayer> getLocalDenickList() {
        return localDenickList;
    }

    public boolean isNickBlocked(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return false;
        }

        for (LocalDenickedPlayer player : localDenickList.values()) {
            if (player == null || player.getNick() == null) {
                continue;
            }
            if (nick.equalsIgnoreCase(player.getNick())) {
                return true;
            }
        }

        return false;
    }

    public String getPlayerNameForNick(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return null;
        }

        for (LocalDenickedPlayer player : localDenickList.values()) {
            if (player == null || player.getNick() == null) {
                continue;
            }
            if (nick.equalsIgnoreCase(player.getNick())) {
                return player.getName();
            }
        }

        return null;
    }
}
