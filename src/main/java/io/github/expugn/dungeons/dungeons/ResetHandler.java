package io.github.expugn.dungeons.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.expugn.dungeons.AppUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Handles any necessary resets if things were stopped at an inappropriate state.
 * Right now the dungeon side of things here are unused I think (their reset is handled by
 * the dungeon file and if the party in dungeon file has players on load).
 * @author S'pugn
 * @version 0.1
 */
public class ResetHandler {
    private static final File RESET_FILE = new File(String.format("%s/reset.json", AppUtils.getPluginDirectory()));
    private Map<UUID, String> offlinePlayers;
    private List<String> activeDungeons;

    public ResetHandler() {
        this.offlinePlayers = new HashMap<>();
        this.activeDungeons = List.of();
    }

    /**
     * Get a ResetHandler instance.
     * If the reset file does not exist a new one will be created.
     */
    public static ResetHandler getResetHandler() {
        if (!RESET_FILE.exists()) {
            // FILE DOES NOT EXIST
            return new ResetHandler();
        }

        Gson gson = new Gson();
        try (Reader reader = new FileReader(RESET_FILE)) {
            return gson.fromJson(reader, ResetHandler.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResetHandler();
    }

    public void addPlayer(Player offlinePlayer, String dungeonName) {
        addPlayer(offlinePlayer.getUniqueId(), dungeonName);
    }

    public void addPlayer(UUID uuid, String dungeonName) {
        offlinePlayers.put(uuid, dungeonName);
        save();
    }

    /**
     * Remove a player from the reset handler file.
     * This will also save the file and return the dungeon name
     * They were participating in.
     * @param onlinePlayer Player that is now online and needs to have their reset handled.
     * @return String of dungeon name to get reset script from.
     */
    public String removePlayer(Player onlinePlayer) {
        String dungeonName = offlinePlayers.remove(onlinePlayer.getUniqueId());
        save();
        return dungeonName;
    }

    public String getPlayer(Player player) {
        return offlinePlayers.get(player.getUniqueId());
    }

    public boolean containsPlayer(Player player) {
        return offlinePlayers.containsKey(player.getUniqueId());
    }

    public void addDungeon(String dungeonName) {
        activeDungeons.add(dungeonName);
        save();
    }

    public void removeDungeon(String dungeonName) {
        activeDungeons.remove(dungeonName);
        save();
    }

    public boolean containsDungeon(String dungeonName) {
        return activeDungeons.contains(dungeonName);
    }

    /**
     * Save the reset handler file.
     * This will also create a file if it doesn't exist yet.
     */
    public void save() {
        if (!RESET_FILE.exists()) {
            // CREATE FILE IF IT DOESN'T EXIST
            try {
                RESET_FILE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(RESET_FILE)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
