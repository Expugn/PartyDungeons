package io.github.expugn.dungeons;

import io.github.expugn.dungeons.dungeons.Dungeon;
import io.github.expugn.dungeons.dungeons.DungeonFile;
import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.dungeons.PlayerState;
import io.github.expugn.dungeons.dungeons.ResetHandler;
import io.github.expugn.dungeons.scripts.ScriptType;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * A utility class that contains helpful functions that may or may not be used frequently.
 * @author S'pugn
 * @version 0.2
 */
public final class AppUtils {
    private AppUtils() {
        // NOT USED, BUT AppUtils IS A UTILITY CLASS AND REQUIRES THIS PRIVATE CONSTRUCTOR.
    }

    /**
     * Check if location is in the bounding box of 2 different string arrays containing ["x", "y", "z"].
     * @param lowerBound Corner 1 of the bounding box.
     * @param upperBound Corner 2 of the bounding box.
     * @param playerLocation Location to check if it is in the bounding box.
     * @return true if location is in bounding box, false otherwise.
     */
    public static boolean isInsideArea(String[] lowerBound, String[] upperBound, Location playerLocation) {
        // LOWER BOUND = [x, y, z]
        // UPPER BOUND = [x, y, z]
        return isInsideArea(Stream.of(lowerBound).mapToInt(Integer::parseInt).toArray(),
            Stream.of(upperBound).mapToInt(Integer::parseInt).toArray(), playerLocation);
    }

    /**
     * Check if a location is in the bounding box of 2 different int arrays containing [x, y, z].
     * @param lowerBound Corner 1 of bounding box.
     * @param upperBound Corner 2 of bounding box.
     * @param playerLocation Location to check if it is in bounding box.
     * @return true if location is in bounding box, false otherwise.
     */
    public static boolean isInsideArea(int[] lowerBound, int[] upperBound, Location playerLocation) {
        // LOWER BOUND = [x, y, z]
        // UPPER BOUND = [x, y, z]
        return new IntRange(lowerBound[0], upperBound[0]).containsInteger(playerLocation.getBlockX())
               && new IntRange(lowerBound[1], upperBound[1]).containsInteger(playerLocation.getBlockY())
               && new IntRange(lowerBound[2], upperBound[2]).containsInteger(playerLocation.getBlockZ());
    }

    /**
     * Check if a location is in the bounding box of 2 different Blocks.
     * @param blockA Corner 1 of bounding box.
     * @param blockB Corner 2 of bounding box.
     * @param playerLocation Location to check if it is in bounding box.
     * @return true if location is in bounding box, false otherwise.
     */
    public static boolean isInsideArea(Block blockA, Block blockB, Location playerLocation) {
        return new IntRange(blockA.getX(), blockB.getX()).containsInteger(playerLocation.getBlockX())
               && new IntRange(blockA.getY(), blockB.getY()).containsInteger(playerLocation.getBlockY())
               && new IntRange(blockA.getZ(), blockB.getZ()).containsInteger(playerLocation.getBlockZ());
    }

    public static boolean isInsideDungeonArea(LoadedDungeon dungeon, Location location) {
        return isInsideDungeonArea(dungeon.getDungeon(), location);
    }

    public static boolean isInsideDungeonArea(Dungeon dungeon, Location location) {
        DungeonFile dungeonFile = dungeon.getDungeonFile();
        return isInsideArea(dungeonFile.getBlockA(), dungeonFile.getBlockB(), location);
    }

    public static boolean isPlayerInDungeon(Player player) {
        return AppStatus.getActivePlayers().containsKey(player.getUniqueId());
    }

    public static boolean isPlayerInDungeon(Player player, String dungeonName) {
        return dungeonName.equals(getPlayerDungeon(player));
    }

    /**
     * Gets the dungeon name of the player.
     * Will return an empty string if player is not in a dungeon.
     * @param player Player to get the dungeon of.
     * @return Dungeon name if exists, otherwise empty string.
     */
    public static String getPlayerDungeon(Player player) {
        UUID uuid = player.getUniqueId();

        // CHECK ACTIVE PLAYERS
        Map<UUID, String> activePlayer = AppStatus.getActivePlayers();
        if (activePlayer.containsKey(uuid)) {
            return activePlayer.get(uuid);
        }

        // PLAYER ISN'T AN ACTIVE PLAYER, ARE THEY AN OFFLINE PLAYER?
        ResetHandler resetHandler = AppStatus.getResetHandler();
        if (resetHandler.containsPlayer(player)) {
            return resetHandler.getPlayer(player);
        }

        return "";
    }

    /**
     * Get a player's PlayerState.
     * If the player is not in a dungeon, returns PlayerState.None.
     * @param player Player to get the PlayerState of.
     * @return PlayerState of player.
     */
    public static PlayerState getPlayerState(Player player) {
        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        UUID uuid = player.getUniqueId();
        if (!activePlayers.containsKey(uuid)) {
            // PLAYER IS NOT ACTIVELY IN A DUNGEON
            return PlayerState.None;
        }

        String dungeonName = activePlayers.get(uuid);
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        if (!activeDungeons.containsKey(dungeonName)) {
            // DUNGEON THAT THE PLAYER IS IN IS NOT ACTIVE, FOR SOME REASON
            return PlayerState.None;
        }

        LoadedDungeon dungeon = activeDungeons.get(dungeonName);
        return dungeon.getPlayerState(player);
    }

    /**
     * Log a message to the console via the plugin's logger.
     * @param level {@link Level} of message.
     * @param msg Message to log to console.
     */
    public static void consoleLog(Level level, String msg) {
        if (AppStatus.getPlugin() != null) {
            AppStatus.getPlugin().getLogger().log(level, msg);
        } else {
            System.out.println(String.format("[%s] %s", level, msg));
        }
    }

    public static UUID playerToUUID(Player player) {
        return player.getUniqueId();
    }

    public static Player uuidToPlayer(UUID id) {
        return Bukkit.getPlayer(id);
    }

    public static Player uuidToPlayer(String id) {
        return Bukkit.getPlayer(UUID.fromString(id));
    }

    /**
     * Force a player to be active in a dungeon (WILL NOT PUT THEM IN THE PARTY).
     * @param player Player to force "join" a dungeon.
     * @param dungeonName Name of dungeon to join.
     * @return true if add was successful, false otherwise.
     */
    public static boolean playerJoinDungeon(Player player, String dungeonName) {
        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        if (activePlayers.containsKey(player.getUniqueId())) {
            // PLAYER IS ALREADY IN A DUNGEON
            return false;
        }
        activePlayers.put(player.getUniqueId(), dungeonName);
        return true;
    }

    /**
     * Remove a player from activePlayers with none of the consequences that LoadedDungeon has.
     * @param player Player to force leave the dungeon
     * @return true if removal successful, false otherwise.
     */
    public static boolean playerLeaveDungeon(Player player) {
        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        if (!activePlayers.containsKey(player.getUniqueId())) {
            // PLAYER IS NOT IN A DUNGEON
            return false;
        }
        activePlayers.remove(player.getUniqueId());
        return true;
    }

    public static String getBlockString(Block block) {
        return String.format("%d_%d_%d", block.getX(), block.getY(), block.getZ());
    }

    public static String getWalkScriptName(Block block) {
        return String.format("%d_%d_%d", block.getX(), block.getY() - 1, block.getZ());
    }

    /**
     * Handle player area target block selection and return an area string.
     * Players will need to run this function twice before they get their area string.
     * @param player Player making the selection.
     * @param targetBlock Block the player is looking at.
     * @return "" if process incomplete, area string if process completed.
     */
    public static String playerSelection(Player player, Block targetBlock) {
        Map<Player, String> activeSelections = AppStatus.getActiveSelections();
        if (!activeSelections.containsKey(player)) {
            // PLAYER IS NOT IN PROGRESS OF MAKING A SELECTION
            activeSelections.put(player, getBlockString(targetBlock));
            return "";
        } else {
            // PLAYER HAS A SELECTION IN PROGRESS, RETURN A COMPLETED STRING
            String completedString = String.format("%s~%s", activeSelections.get(player), getBlockString(targetBlock));

            // SELECTION IS COMPLETE, DELETE FROM activeSelections
            activeSelections.remove(player);

            return completedString;
        }
    }

    public static Block getTargetBlock(Player player) {
        return player.getTargetBlock(null, AppConstants.MAX_TARGET_BLOCK_DISTANCE);
    }

    /**
     * Split an area string (x1_y1_z1~x2_y2_z2) into a 2d array.
     * (x1_y1_z1~x2_y2_z2) => [[x1, y1, z1],[x2, y2, z2]].
     * @param areaString Area string (x1_y1_z1~x2_y2_z2).
     * @return A 2D array containing area string values.
     */
    public static String[][] splitAreaString(String areaString) {
        String[] area = areaString.split("~");
        String[][] blocks = {area[0].split("_"), area[1].split("_")};
        return blocks;
    }

    /**
     * Takes an area string (x1_y1_z1~x2_y2_z2) and makes it pretty.
     * (x1_y1_z1~x2_y2_z2) => "(x1, y1, z1) ~ (x2, y2, z2)".
     * @param areaString Area string (x1_y1_z1~x2_y2_z2).
     * @return A pretty area string.
     */
    public static String prettyAreaString(String areaString) {
        String[][] blocks = splitAreaString(areaString);
        return String.format("(%s, %s, %s) ~ (%s, %s, %s)",
            blocks[0][0], blocks[0][1], blocks[0][2], blocks[1][0], blocks[1][1], blocks[1][2]);
    }

    public static String prettyBlockString(Block block) {
        return String.format("(%s, %s, %s)", block.getX(), block.getY(), block.getZ());
    }

    public static File getPluginDirectory() {
        return new File(String.format("plugins/%s", AppStatus.getPlugin().getName()));
    }

    public static File getDungeonDirectory() {
        return new File(String.format("plugins/%s/dungeon", AppStatus.getPlugin().getName()));
    }

    public static File getDungeonDirectory(String dungeonName) {
        return new File(String.format("plugins/%s/dungeon/%s", AppStatus.getPlugin().getName(), dungeonName));
    }

    /**
     * Gets a dungeon's specific script directory.
     * @param dungeonName Name of dungeon to get the directory of.
     * @param scriptType ScriptType to get the directory of,
     * @return File of the dungeon's script directory.
     */
    public static File getDungeonScriptDirectory(String dungeonName, ScriptType scriptType) {
        if (scriptType.equals(ScriptType.None)) {
            // scriptType IS NONE. USE plugins/<plugin_name>/dungeon/<dungeon_name>/scripts
            return new File(String.format("plugins/%s/dungeon/%s/scripts",
                AppStatus.getPlugin().getName(), dungeonName));
        }
        // scriptType IS NOT NONE. USE plugins/<plugin_name>/dungeon/<dungeon_name>/scripts/<script_type>
        return new File(String.format("plugins/%s/dungeon/%s/scripts/%s",
            AppStatus.getPlugin().getName(), dungeonName, scriptType.getDirectory()));
    }

    public static File getDungeonConfigFile(String dungeonName) {
        return new File(String.format("%s/config.json", getDungeonDirectory(dungeonName)));
    }

    public static File getWorldDirectory() {
        return new File(String.format("plugins/%s/worlds", AppStatus.getPlugin().getName()));
    }

    public static File getWorldDirectory(World world) {
        return getWorldDirectory(world.getName());
    }

    public static File getWorldDirectory(String worldName) {
        return new File(String.format("plugins/%s/worlds/%s", AppStatus.getPlugin().getName(), worldName));
    }

    public static File getWorldScriptDirectory(World world, ScriptType scriptType) {
        return getWorldScriptDirectory(world.getName(), scriptType);
    }

    /**
     * Get the world directory's script directory.
     */
    public static File getWorldScriptDirectory(String worldName, ScriptType scriptType) {
        if (scriptType.equals(ScriptType.None)) {
            // scriptType IS NONE. USE plugins/<plugin_name>/worlds/<world_name>/scripts/
            return new File(String.format("%s/scripts", getWorldDirectory(worldName)));
        }
        // scriptType IS NOT NONE. USE plugins/<plugin_name>/worlds/<world_name>/scripts/<script_type>
        return new File(String.format("%s/scripts/%s", getWorldDirectory(worldName), scriptType.getDirectory()));
    }

    public static File getWorldScript(World world, ScriptType scriptType, String scriptName) {
        return new File(String.format("%s/%s%s", getWorldScriptDirectory(world, scriptType), scriptName,
            AppConstants.SCRIPT_ENGINE_EXTENSION));
    }

    public static File getWorldVariableFile(World world) {
        return new File(String.format("%s/variables.json", getWorldDirectory(world)));
    }

    /**
     * "Straighten" a given yaw.
     * Straighten a yaw meaning this will make sure a player is looking exactly straight in a cardinal direction.
     * @param yaw Yaw value.
     * @return A "straightened" yaw value.
     */
    public static float straightenYaw(float yaw) {
        final int[] northRange = {135, -135};
        final int[] eastRange = {-135, -45};
        final int[] southRange = {-45, 45};
        final float north = 180f;
        final float east = -90f;
        final float south = 0f;
        final float west = 90f;
        if (yaw >= northRange[0] || yaw < northRange[1]) {
            return north;
        }
        if (yaw >= eastRange[0] && yaw < eastRange[1]) {
            return east;
        }
        if (yaw >= southRange[0] && yaw < southRange[1]) {
            return south;
        }
        // if (yaw >= 45 && yaw < 135)
        return west;
    }
}
