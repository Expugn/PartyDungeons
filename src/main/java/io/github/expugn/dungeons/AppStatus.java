package io.github.expugn.dungeons;

import io.github.expugn.dungeons.dungeons.Dungeon;
import io.github.expugn.dungeons.dungeons.DungeonFile;
import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.dungeons.PlayerState;
import io.github.expugn.dungeons.dungeons.ResetHandler;
import io.github.expugn.dungeons.scripts.ScriptManager;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Manages the current PartyDungeons App's status.
 * These variables are accessible throughout the entire plugin if needed.
 * @author S'pugn
 * @version 0.2
 */
public final class AppStatus {
    private static boolean isInitialized;
    private static App plugin;
    private static Map<UUID, String> activePlayers;
    private static Map<Player, String> activeSelections;
    private static Map<String, LoadedDungeon> activeDungeons;
    private static ResetHandler resetHandler;
    private static final ScriptManager SCRIPT_MANAGER = new ScriptManager();
    private static final ExecutorService SCRIPT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static Economy economy;

    private AppStatus() {
        // NOT USED, AppStatus IS A UTILITY CLASS THAT REQUIRES THIS PRIVATE CONSTRUCTOR
    }

    /**
     * Initialize AppStatus.
     * @param p Plugin to save into AppStatus.
     */
    public static void init(App p) {
        // THIS SHOULD ONLY BE CALLED ONCE
        if (isInitialized) {
            return;
        }
        isInitialized = true;
        plugin = p;
        activePlayers = new HashMap<>();
        activeSelections = new HashMap<>();
        resetHandler = ResetHandler.getResetHandler();
        economy = null;
        // activeDungeons = new HashMap<>(); DONE IN loadAllDungeons()

        loadAllDungeons();
    }

    public static Map<UUID, String> getActivePlayers() {
        return activePlayers;
    }

    public static Map<Player, String> getActiveSelections() {
        return activeSelections;
    }

    public static Map<String, LoadedDungeon> getActiveDungeons() {
        return activeDungeons;
    }

    public static ResetHandler getResetHandler() {
        return resetHandler;
    }

    public static ScriptManager getScriptManager() {
        return SCRIPT_MANAGER;
    }

    public static ExecutorService getExecutorService() {
        return SCRIPT_EXECUTOR_SERVICE;
    }

    public static App getPlugin() {
        return plugin;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static void setEconomy(Economy eco) {
        economy = eco;
    }

    /**
     * Loads all dungeons in the dungeon directory.
     * Also checks if dungeons were in progress or not (by looking at the dungeon file's saved party).
     * If the dungeon was active, then active players at the time will be handled by {@link ResetHandler}
     * and the dungeon will be reset before it is loaded.
     */
    public static void loadAllDungeons() {
        activeDungeons = new HashMap<>();
        File mainDirectory = AppUtils.getDungeonDirectory();
        File[] dungeonDirectories = mainDirectory.listFiles(File::isDirectory);
        if (dungeonDirectories == null) {
            // NO DUNGEONS FOUND THAT CAN BE LOADED.
            return;
        }

        for (File dd : dungeonDirectories) {
            activeDungeons.put(dd.getName(), new LoadedDungeon(new Dungeon(dd.getName())));
            DungeonFile df = activeDungeons.get(dd.getName()).getDungeon().getDungeonFile();
            if (df.hasParty()) {
                // UH OH, DUNGEON SHUT DOWN WHILE IT WAS IN PROGRESS
                plugin.getLogger().info(String.format("%s was in progress before the plugin was unloaded!",
                    dd.getName()));
                for (Map.Entry<UUID, PlayerState> entry : df.getParty().entrySet()) {
                    Player player = AppUtils.uuidToPlayer(entry.getKey());
                    if (!entry.getValue().equals(PlayerState.Alive)) {
                        // IGNORE PREVIOUSLY Dead, Offline, etc PLAYERS
                        continue;
                    }
                    if (player != null && player.isOnline()) {
                        // IGNORE ONLINE PLAYERS, WE DON'T WANT TO TELEPORT THEM
                        plugin.getLogger().info(String.format("%s was a part of the dungeon but they are online.%s",
                            player.getName(), " Ignoring..."));
                        continue;
                    }

                    // ADD PLAYER TO ResetHandler
                    plugin.getLogger().info(String.format("Marked UUID %s as an OfflinePlayer...", entry.getKey()));
                    resetHandler.addPlayer(entry.getKey(), dd.getName());
                }

                // RESET DUNGEON
                plugin.getLogger().info(String.format("Resetting %s...", dd.getName()));
                activeDungeons.get(dd.getName()).reset();
            }

            plugin.getLogger().info(String.format("Loaded %s...", dd.getName()));
        }
    }

    /**
     * Load a unloaded dungeon.
     * All dungeons are loaded at plugin startup via `loadAllDungeons()`.
     * Loaded dungeons can be used by players.
     * @param dungeonName Name of unloaded dungeon to load.
     * @return true if load successful, false otherwise.
     */
    public static boolean loadDungeon(String dungeonName) {
        if (activeDungeons.containsKey(dungeonName)) {
            // DUNGEON IS ALREADY LOADED
            return false;
        }

        File dungeonDirectory = AppUtils.getDungeonDirectory(dungeonName);
        if (!dungeonDirectory.exists()) {
            // DUNGEON DIRECTORY DOESN'T EXIST
            return false;
        }

        activeDungeons.put(dungeonName, new LoadedDungeon(new Dungeon(dungeonName)));
        plugin.getLogger().info(String.format("Loaded %s...", dungeonName));

        return true;
    }

    /**
     * Unload a loaded dungeon.
     * Unloaded dungeons can not be used by players.
     * @param dungeonName Name of dungeon to unload
     * @return true if unload successful, false otherwise.
     */
    public static boolean unloadDungeon(String dungeonName) {
        if (!activeDungeons.containsKey(dungeonName)) {
            // DUNGEON IS ALREADY UNLOADED
            return false;
        }

        // DO THINGS BEFORE DUNGEON UNLOADS HERE...
        // KICK ACTIVE PLAYERS?
        activeDungeons.get(dungeonName).reset();

        activeDungeons.remove(dungeonName);
        plugin.getLogger().info(String.format("Unloaded %s.", dungeonName));

        return true;
    }
}
