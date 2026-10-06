package com.roxiun.mellow.util.blacklist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

public class BlacklistManager {

    private static final String EXTERNAL_FILE_IMPORT_REASON =
        "Added from external file";
    private final File blacklistFile;
    private final File blacklistTxtFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, BlacklistedPlayer> blacklist = new ConcurrentHashMap<>();

    // Regex pattern to validate UUID format
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    public BlacklistManager() {
        this(new File(Minecraft.getMinecraft().mcDataDir, "config/mellow"));
    }

    BlacklistManager(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.blacklistFile = new File(configDir, "blacklist.json");
        this.blacklistTxtFile = new File(configDir, "blacklist.txt");
        loadBlacklist();
        syncWithTxtFile();
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

    /**
     * Loads UUIDs from blacklist.txt and adds them to the current blacklist.
     * If a UUID already exists in the blacklist, it retains its original name and reason.
     * If a UUID doesn't exist, it adds it with default "TXT Import" name and "Added from TXT file" reason.
     */
    public void loadFromTxtFile() {
        if (!blacklistTxtFile.exists()) {
            return;
        }

        try (
            BufferedReader reader = new BufferedReader(
                new FileReader(blacklistTxtFile)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Validate UUID format
                if (UUID_PATTERN.matcher(line).matches()) {
                    try {
                        UUID uuid = UUID.fromString(line);
                        // Only add if not already in the blacklist
                        if (!blacklist.containsKey(uuid)) {
                            // Add with default values since TXT file only contains UUID
                            blacklist.put(
                                uuid,
                                new BlacklistedPlayer(
                                    uuid.toString(),
                                    "Added from TXT file"
                                )
                            );
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println(
                            "Invalid UUID format in blacklist.txt: " + line
                        );
                    }
                } else {
                    System.err.println(
                        "Skipping invalid UUID in blacklist.txt: " + line
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the current blacklist to blacklist.txt (only UUIDs).
     */
    public void saveToTxtFile() {
        try (FileWriter writer = new FileWriter(blacklistTxtFile)) {
            for (UUID uuid : blacklist.keySet()) {
                writer.write(uuid.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Syncs the blacklist with the blacklist.txt file.
     * This loads any new UUIDs from the txt file and saves the full list to the txt file.
     */
    public void syncWithTxtFile() {
        // Only load from txt if the file exists to avoid overriding with empty data
        if (blacklistTxtFile.exists()) {
            loadFromTxtFile();
        }
        saveToTxtFile();
    }

    /**
     * Force sync from blacklist.txt to update the main blacklist, preserving existing data
     * when UUIDs exist in both files.
     */
    public void forceSyncFromTxt() {
        if (blacklistTxtFile.exists()) {
            loadFromTxtFile();
            saveBlacklist(); // Update the JSON file with new data from TXT
            saveToTxtFile(); // Ensure TXT file is consistent
        }
    }

    /**
     * Syncs the current blacklist with an external text file (one UUID per line).
     * This loads any new UUIDs from the external file and adds them to the current blacklist.
     * Existing entries in the blacklist retain their original name and reason.
     * New entries from the external file are added with default values.
     *
     * @param externalFile The external file to sync from
     * @return The number of new entries added from the external file
     */
    public int syncWithExternalFile(File externalFile) {
        if (!externalFile.exists()) {
            return 0;
        }

        int newEntriesCount = 0;
        try (
            BufferedReader reader = new BufferedReader(
                new FileReader(externalFile)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Validate UUID format
                if (UUID_PATTERN.matcher(line).matches()) {
                    try {
                        UUID uuid = UUID.fromString(line);
                        // Only add if not already in the blacklist
                        if (!blacklist.containsKey(uuid)) {
                            // Add with default values since external file only contains UUID
                            blacklist.put(
                                uuid,
                                new BlacklistedPlayer(
                                    uuid.toString(),
                                    EXTERNAL_FILE_IMPORT_REASON
                                )
                            );
                            newEntriesCount++;
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println(
                            "Invalid UUID format in external file: " + line
                        );
                    }
                } else {
                    System.err.println(
                        "Skipping invalid UUID in external file: " + line
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0; // Return 0 if there was an error reading the file
        }

        // Save changes to both files if new entries were added
        if (newEntriesCount > 0) {
            saveBlacklist();
            saveToTxtFile();
        }

        return newEntriesCount;
    }

    public boolean addPlayer(UUID uuid, String name, String reason) {
        if (blacklist.containsKey(uuid)) {
            return false; // Player already in blacklist
        }
        blacklist.put(uuid, new BlacklistedPlayer(name, reason));
        saveBlacklist();
        saveToTxtFile(); // Also update the txt file
        return true; // Player added successfully
    }

    /** Finds a saved name without depending on a current online profile. */
    public UUID findPlayerByName(String name) {
        UUID match = null;
        for (Map.Entry<UUID, BlacklistedPlayer> entry : blacklist.entrySet()) {
            if (name.equalsIgnoreCase(entry.getValue().getName())) {
                if (match != null) {
                    throw new IllegalArgumentException(
                        "Multiple blacklist entries match " + name +
                        ". Use the remove action in the blacklist list."
                    );
                }
                match = entry.getKey();
            }
        }
        return match;
    }

    public boolean removePlayer(UUID uuid) {
        if (blacklist.remove(uuid) == null) {
            return false;
        }
        saveBlacklist();
        saveToTxtFile();
        return true;
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

    public static boolean isExternalFileImportReason(String reason) {
        return (
            reason != null &&
            reason.trim().equalsIgnoreCase(EXTERNAL_FILE_IMPORT_REASON)
        );
    }
}
