package io.github.expugn.dungeons.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.expugn.dungeons.AppUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Manages a JSON file containing important dungeon information like
 * dungeon settings and permanent/persisting variables.
 * @author S'pugn
 * @version 0.1
 */
public class DungeonFile {
    // MAX AMOUNT OF PLAYERS THAT SHOULD BE IN THE DUNGEON AT A TIME
    private int maxParty;

    // MAX AMOUNT OF CLEARS A PLAYER CAN DO, THEY SHOULD BE PROHIBITED FROM REJOINING
    // THE PARTY WHEN THEY'VE CLEARED THE DUNGEON
    private int dailyClear;

    // WORLD NAME WHERE THE DUNGEON RESIDES, NECESSARY FOR TELEPORTS
    private String worldName;

    // POSITION WHERE THE PLAYER SHOULD BE IF THEY DISCONNECT WHILE THE DUNGEON IS IN PROGRESS
    private List<Integer> spawnPosition;

    // POSITION WHERE THE PLAYER SHOULD BE TELEPORTED TO WHEN THE DUNGEON STARTS
    private List<Integer> startPosition;

    // POSITION OF THE FIRST BLOCK TO DETERMINE DUNGEON BOUNDARIES
    private List<Integer> blockA;

    // POSITION OF THE SECOND BLOCK TO DETERMINE DUNGEON BOUNDARIES
    private List<Integer> blockB;

    // PERMANENT VARIABLES THAT WILL PERSIST EVEN WHEN THE DUNGEON RESETS
    // (money_spent, hall_of_fame, etc)
    private Map<String, Object> variables;

    // PLAYER UUIDS AND THE currentTimeMills WHEN THEY CLEARED THE DUNGEON
    // USED FOR daily_clear
    private Map<UUID, List<Long>> clearedPlayers;

    // CURRENT PARTY, IF THERE IS A PARTY THAT EXISTS WHEN THE DUNGEON HAS LOADED
    // WE KNOW THAT IT PROBABLY WAS RESET DURING AN ACTIVE DUNGEON RUN
    // IF IT ISN'T EMPTY WE SHOULD ADD PLAYERS TO ResetHandler AND RESET DUNGEON
    private Map<UUID, PlayerState> party;

    /**
     * Construct a new dungeon file.
     */
    public DungeonFile() {
        // SET DUNGEON FILE DEFAULTS
        setDefault();
    }

    /**
     * Set default values for a dungeon file.
     */
    public void setDefault() {
        this.maxParty = -1;
        this.dailyClear = -1;
        this.worldName = "";
        this.spawnPosition = List.of(0, 0, 0, 0);  // [x, y, z, yaw]
        this.startPosition = List.of(0, 0, 0, 0);  // [x, y, z, yaw]
        this.blockA = List.of(0, 0, 0);            // [x, y, z]
        this.blockB = List.of(0, 0, 0);            // [x, y, z]
        this.variables = new HashMap<>();
        this.clearedPlayers = new HashMap<>();
        this.party = new HashMap<>();
    }

    /**
     * Get a dungeon's max party setting.
     * @return Dungeon's max party size amount.
     */
    public int getMaxParty() {
        return this.maxParty;
    }

    /**
     * Set a dungeon's max party setting.
     * Values below 0 will be set to -1 (max party setting ignored).
     * @param value Value to set dungeon's max party setting to.
     */
    public void setMaxParty(int value) {
        maxParty = (value > 0) ? value : -1;
    }

    /**
     * Get a dungeon's daily clear amount.
     * Players who have cleared the daily clear amount can
     * no longer enter the dungeon.
     * @return Dungeon's daily clear amount.
     */
    public int getDailyClear() {
        return dailyClear;
    }

    /**
     * Set a dungeon's daily clear amount.
     * Values below 0 will be set to -1 (daily clear setting ignored).
     * @param value Value to set dungeon's daily clear setting to.
     */
    public void setDailyClear(int value) {
        dailyClear = (value > 0) ? value : -1;
    }

    /**
     * Get the world that the dungeon exists in.
     * @return Dungeon's world.
     */
    public World getWorld() {
        if (worldName.isEmpty()) {
            // WORLD NAME ISN'T DEFINED
            return null;
        }
        return Bukkit.getServer().getWorld(worldName);
    }

    /**
     * Set the world that the dungeon exists in.
     * @param world World that the dungeon should exist in.
     */
    public void setWorld(World world) {
        worldName = world.getName();
    }

    /**
     * Get the spawn position of a dungeon.
     * A spawn position is where a player will be teleported to
     * if they quit the dungeon while it is active.
     * @return Dungeon's spawn position.
     */
    public List<Integer> getSpawnPosition() {
        return spawnPosition;
    }

    /**
     * Set the spawn position of a dungeon.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @param yaw Direction the player is looking.
     */
    public void setSpawnPosition(int x, int y, int z, int yaw) {
        spawnPosition = List.of(x, y, z, yaw);
    }

    /**
     * Set the spawn position of a dungeon, given a Location.
     * @param location Location of the new spawn position.
     */
    public void setSpawnPosition(Location location) {
        Block block = location.getBlock();
        final int yaw = (int) AppUtils.straightenYaw(location.getYaw());
        spawnPosition = List.of(block.getX(), block.getY(), block.getZ(), yaw);
    }

    /**
     * Get the spawn location of a dungeon as a Location object.
     * @return Location of spawn position.
     */
    public Location getSpawnLocation() {
        return getLocation(spawnPosition);
    }

    /**
     * Get the start position of a dungeon.
     * The start position is where players are teleported
     * when the dungeon starts.
     * @return Dungeon's start position.
     */
    public List<Integer> getStartPosition() {
        return startPosition;
    }

    /**
     * Set the start position of a dungeon.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @param yaw Direction the player is looking.
     */
    public void setStartPosition(int x, int y, int z, int yaw) {
        startPosition = List.of(x, y, z, yaw);
    }

    /**
     * Set the start position of a dungeon, given a Location.
     * @param location Location of the start position.
     */
    public void setStartPosition(Location location) {
        Block block = location.getBlock();
        startPosition = List.of(block.getX(), block.getY(), block.getZ(),
            (int) AppUtils.straightenYaw(location.getYaw()));
    }

    /**
     * Get the start position of a dungeon, as a Location object.
     * @return Location object of the start position.
     */
    public Location getStartLocation() {
        return getLocation(startPosition);
    }

    /**
     * Get the location from a list of 3 integers.
     * @param position List of 3 integers.
     * @return Location object of the position.
     */
    private Location getLocation(List<Integer> position) {
        // USED FOR BOTH getSpawnLocation() AND getStartLocation()
        final double blockCenter = 0.5;
        final int yawIndex = 3;
        Location location = new Location(Bukkit.getServer().getWorld(worldName),
            position.get(0) + blockCenter,
            position.get(1) + blockCenter,
            position.get(2) + blockCenter);
        location.setYaw((float) position.get(yawIndex));
        return location;
    }

    /**
     * Get BlockA, a corner block of the dungeon area.
     * @return A Block object of BlockA.
     */
    public Block getBlockA() {
        World w = getWorld();
        if (w == null) {
            return null;
        }
        return w.getBlockAt(blockA.get(0), blockA.get(1), blockA.get(2));
    }

    /**
     * Set BlockA.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     */
    public void setBlockA(int x, int y, int z) {
        blockA = List.of(x, y, z);
    }

    /**
     * Set BlockA, given a Block.
     * @param block Block object to replace BlockA with.
     */
    public void setBlockA(Block block) {
        blockA = List.of(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Set BlockA, given an Array of 3 Strings.
     * @param block Array of 3 strings to be converted to Integers.
     */
    public void setBlockA(String[] block) {
        blockA = List.of(Integer.parseInt(block[0]), Integer.parseInt(block[1]), Integer.parseInt(block[2]));
    }

    /**
     * Get BlockB, a corner block of the dungeon area.
     * @return Block object of BlockB.
     */
    public Block getBlockB() {
        World w = getWorld();
        if (w == null) {
            return null;
        }
        return w.getBlockAt(blockB.get(0), blockB.get(1), blockB.get(2));
    }

    /**
     * Set BlockB.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     */
    public void setBlockB(int x, int y, int z) {
        blockB = List.of(x, y, z);
    }

    /**
     * Set BlockB, given a Block object.
     * @param block Block object to replace BlockB with.
     */
    public void setBlockB(Block block) {
        blockB = List.of(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Set BlockB, given an array of 3 strings.
     * @param block Array of 3 strings to be translated to Integers.
     */
    public void setBlockB(String[] block) {
        blockB = List.of(Integer.parseInt(block[0]), Integer.parseInt(block[1]), Integer.parseInt(block[2]));
    }

    /**
     * Set the dungeon party.
     * @param party Map containing player UUIDs and PlayerStates.
     */
    public void setParty(Map<UUID, PlayerState> party) {
        this.party = party;
    }

    /**
     * Get the dungeon party.
     * @return Map of the dungeon party.
     */
    public Map<UUID, PlayerState> getParty() {
        return party;
    }

    /**
     * Check if the dungeon has a party (size > 0).
     * @return true if the dungeon has a party, false otherwise.
     */
    public boolean hasParty() {
        return party.size() > 0;
    }

    /**
     * Clear a dungeon party.
     */
    public void clearParty() {
        this.party = new HashMap<>();
    }

    /**
     * Get a Map of persisting dungeon variables.
     * @return Map of dungeon variables.
     */
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    /**
     * Set a dungeon variable to an object.
     * @param variableName Map Key to save to.
     * @param value Object value to save.
     */
    public void setVariable(String variableName, Object value) {
        this.variables.put(variableName, value);
    }

    /**
     * Get a dungeon variable value.
     * @param variableName Key the object was stored in.
     * @return Value of given Key.
     */
    public Object getVariable(String variableName) {
        return this.variables.get(variableName);
    }

    /**
     * Get a Map of players who have cleared the dungeon
     * and the timestamps that they cleared in.
     * @return Map of cleared players.
     */
    public Map<UUID, List<Long>> getClearedPlayers() {
        return this.clearedPlayers;
    }

    /**
     * Add a player to the list of cleared players.
     * A player's daily clear count will be reset if their last
     * clear was not on the same date.
     * @param player Player to add.
     * @param currentTimeMillis Time in Millis of when they cleared the dungeon.
     */
    public void addClearedPlayer(Player player, Long currentTimeMillis) {
        UUID uuid = player.getUniqueId();

        // MAKE A COPY OF THE PLAYERS CLEAR TIMES BECAUSE THE VARIABLE
        // WOULD BE IMMUTABLE ON FIRST CREATION.
        final List<Long> current = this.clearedPlayers.get(uuid) != null
            ? new ArrayList<>(this.clearedPlayers.get(uuid))
            : new ArrayList<>();
        if (this.clearedPlayers.containsKey(uuid) && current.size() > 0) {
            // THERE ARE ENTRIES IN HERE, CHECK LATEST IS FROM SAME DAY
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Date currentDate = new Date(currentTimeMillis);
            Date lastDate = new Date(current.get(current.size() - 1));
            if (sdf.format(currentDate).equals(sdf.format(lastDate))) {
                // DATE IS EQUAL, ADD TO LIST
                current.add(currentTimeMillis);
                this.clearedPlayers.put(uuid, current);
                return;
            }
            // IF DATE IS NOT EQUAL, CLEAR LIST BEFORE ADDING
        }
        // NO EXISTING ENTRIES FOR PLAYER OR DATE WAS NOT EQUAL, ADD
        this.clearedPlayers.put(uuid, List.of(currentTimeMillis));
    }

    /**
     * Remove a player from the cleared players list.
     * @param player Player to delete their entry for.
     */
    public void removeClearedPlayer(Player player) {
        this.clearedPlayers.remove(player.getUniqueId());
    }

    /**
     * Check if a player has exceeded the dungeon's daily clear count.
     * Their clear counts may be reset if the last clear was not on the
     * same date, so a file save may be necessary.
     * @param player Player to check.
     * @return true if they can join, false otherwise.
     */
    public boolean canPlayerJoin(Player player) {
        if (dailyClear <= 0) {
            // LET PLAYER IN
            return true;
        }
        UUID uuid = player.getUniqueId();
        List<Long> current = this.clearedPlayers.get(uuid);
        if (this.clearedPlayers.containsKey(uuid) && current.size() > 0) {
            // THERE ARE ENTRIES IN HERE, CHECK LATEST IS FROM SAME DAY
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Date currentDate = new Date(System.currentTimeMillis());
            Date lastDate = new Date(current.get(current.size() - 1));
            if (sdf.format(currentDate).equals(sdf.format(lastDate))) {
                // DATE IS EQUAL
                return current.size() < dailyClear;
            }
            // DATE IS NOT EQUAL, WE CAN CLEAR PLAYER'S EXISTING ENTRIES
            this.clearedPlayers.remove(uuid);
        }
        return true;
    }

    /**
     * Save the dungeon file, given a dungeon name.
     * @param dungeonName Name of the dungeon to save this file to.
     */
    public void saveJSON(String dungeonName) {
        saveJSON(AppUtils.getDungeonConfigFile(dungeonName));
    }

    /**
     * Save the dungeon file, given a file location.
     * @param file File of location to save dungeon file to.
     */
    public void saveJSON(File file) {
        if (!file.exists()) {
            // CREATE FILE IF IT DOESN'T EXIST
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the dungeon file from a File.
     * @param file File pointing to the dungeon file.
     * @return A DungeonFile object.
     */
    public static DungeonFile getDungeonFile(File file) {
        if (!file.exists()) {
            // FILE DOES NOT EXIST
            return null;
        }

        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, DungeonFile.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
