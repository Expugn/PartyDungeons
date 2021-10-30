package io.github.expugn.dungeons.scripts;

import io.github.expugn.dungeons.App;
import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.dungeons.PlayerState;
import io.github.expugn.dungeons.itemdrop.ItemDrop;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * An interface containing functions a script can use.
 * @author S'pugn
 * @version 0.1
 */
public interface Script {
    /**
     * Log something to the Bukkit logger.
     * Example:
     *   sm.log("INFO", "Hello World!");
     * @param level     Logging level from {@link java.util.logging.Level} (SEVERE, WARNING, INFO, CONFIG)
     * @param log       String of whatever should be logged
     */
    void log(String level, String log);

    /**
     * Return the plugin.
     */
    App getPlugin();

    /**
     * Get a PlayerState enum variable from string.
     * @param string PlayerState as string.
     * @return PlayerState enum object.
     */
    PlayerState enumPlayerState(String string);

    /**
     * Get player's current state in a dungeon.
     * @param player Player to get state of.
     * @return PlayerState enum object.
     */
    PlayerState getPlayerState(Player player);

    /**
     * Get the dungeon a player is participating in.
     * @param player Player to get dungeon of.
     * @return LoadedDungeon object.
     */
    LoadedDungeon getDungeon(Player player);

    /**
     * Get the script directory for a LoadedDungeon.
     * @param dungeon Dungeon to get the script directory of.
     * @return String of file path.
     */
    String getScriptDirectory(LoadedDungeon dungeon);

    /**
     * Get the script directory from a dungeon name.
     */
    String getScriptDirectory(String dungeonName);

    /**
     * Get the script directory of the given ScriptType for the given dungeon name.
     * @param dungeonName Name of dungeon.
     * @param scriptType ScriptType to get directory of.
     * @return String of file path.
     */
    String getScriptDirectory(String dungeonName, String scriptType);

    /**
     * Get world that dungeon exists in.
     * @param dungeon LoadedDungeon object.
     * @return World object that dungeon exists in.
     */
    World getDungeonWorld(LoadedDungeon dungeon);

    /**
     * Gets a Player object from a UUID.
     * @param uuid UUID of a player.
     * @return Player object from UUID.
     */
    Player getPlayerFromUUID(UUID uuid);

    /**
     * Creates a new ItemDrop instance, see {@link ItemDrop}.
     * @param location Location where items should be dropped.
     * @param table Table of items to be selected from.
     * @return ItemDrop object.
     */
    ItemDrop createItemDrop(Location location, Map<ItemStack, Integer> table);

    /**
     * Check if the server has an economy system enabled.
     * @return true if there is an economy system.
     */
    boolean isEconomyEnabled();

    /**
     * Check if the user has enough money.
     */
    boolean hasMoney(Player player, double amount);

    /**
     * Deposit money to a player.
     */
    boolean depositMoney(Player player, double amount);

    /**
     * Withdraw money from a player.
     */
    boolean withdrawMoney(Player player, double amount);

    /**
     * Clear specific PotionEffectTypes from the player if they have it.
     * Implementation REQUIRES using the Scheduler to runTaskLater
     * Example:
     *   const PotionEffectType = Java.type("org.bukkit.potion.PotionEffectType");
     *   sm.clearPotionEffect(player, PotionEffectType.SPEED);
     *   sm.clearPotionEffect(player, PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.ABSORPTION);
     * @param player             {@link org.bukkit.entity.Player} script binding
     * @param potionEffectTypes  {@link org.bukkit.potion.PotionEffectType}
     */
    void clearPotionEffect(Player player, PotionEffectType... potionEffectTypes);

    /**
     * Clear ALL PotionEffectTypes from the player.
     * Implementation REQUIRES using the Scheduler to runTaskLater
     * Example:
     *   sm.clearPotionEffect(player);
     * @param player             {@link org.bukkit.entity.Player} script binding
     */
    void clearPotionEffect(Player player);

    /**
     * Clear ALL PotionEffectTypes from the player.
     * Implementation REQUIRES using the Scheduler to runTaskLater
     * Example:
     *   const Integer = Java.type("java.lang.Integer");
     *   const PotionEffectType = Java.type("org.bukkit.potion.PotionEffectType");
     *   sm.addPotionEffect(player, Integer.MAX_VALUE, 1, PotionEffectType.SPEED, PotionEffectType.ABSORPTION);
     * @param player             {@link org.bukkit.entity.Player} script binding
     * @param duration           Integer of potion effect duration
     * @param amplifier          Integer of potion effect strength
     * @param potionEffectTypes  {@link org.bukkit.potion.PotionEffectType}
     */
    void addPotionEffect(Player player, int duration, int amplifier, PotionEffectType... potionEffectTypes);
}
