package io.github.expugn.dungeons.scripts;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import io.github.expugn.dungeons.App;
import io.github.expugn.dungeons.AppConstants;
import io.github.expugn.dungeons.AppStatus;
import io.github.expugn.dungeons.AppUtils;
import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.dungeons.PlayerState;
import io.github.expugn.dungeons.itemdrop.ItemDrop;
import io.github.expugn.dungeons.worlds.WorldVariables;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * Manages the execution of scripts and gives functions useful for scripts to call.
 * @author S'pugn
 * @version 0.3
 */
public class ScriptManager implements Script {
    private static ScriptEngine scriptEngine;
    private static final Lock FILE_READ_LOCK = new ReentrantLock();

    /**
     * Initializes the script engine.
     * Temporarily changes the ClassLoader to the plugin's classloader and restores the ClassLoader when done.
     * This is done so classes like WorldEdit's can be accessed in scripts.
     */
    public ScriptManager() {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(AppStatus.getPlugin().getClass().getClassLoader());
        scriptEngine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
        Thread.currentThread().setContextClassLoader(previous);
    }

    private ScriptInfo newScriptInfo(String scriptName, ScriptType scriptType, Player player) {
        ScriptInfo scriptInfo = new ScriptInfo(scriptName, scriptType);
        scriptInfo.setPlayer(player);

        if (player != null) {
            // PLAYER EXISTS
            String dungeonName = AppUtils.getPlayerDungeon(player);
            if (!dungeonName.isEmpty()) {
                // PLAYER IS IN A DUNGEON,
                // USE plugins/<plugin_name>/dungeon/<dungeon_name>/scripts/<script_type>/<script_name>
                // IF ScriptType.None, USE plugins/<plugin_name>/dungeon/<dungeon_name>/scripts/<script_name>
                scriptInfo.setDirectory(AppUtils.getDungeonScriptDirectory(dungeonName, scriptType).toString());
            } else {
                // PLAYER IS NOT IN A DUNGEON,
                // USE plugins/<plugin_name>/worlds/<world_name>/scripts/<script_type>/<script_name>
                // IF ScriptType.None, USE plugins/<plugin_name>/worlds/<world_name>/scripts/<script_name>
                scriptInfo.setDirectory(AppUtils.getWorldScriptDirectory(player.getWorld(), scriptType).toString());
            }
        }
        if (scriptInfo.getDirectory().isEmpty()) {
            // DIRECTORY HAS NOT BEEN SET YET
            // USE plugins/<plugin_name>/scripts
            if (scriptType.equals(ScriptType.None)) {
                // ScriptType.None HAS NO EXTRA DIRECTORY
                scriptInfo.setDirectory(String.format("%s/scripts", AppUtils.getPluginDirectory()));
            } else {
                // ScriptType THAT ISN'T None HAS AN EXTRA DIRECTORY
                scriptInfo.setDirectory(String.format("%s/scripts/%s", AppUtils.getPluginDirectory(), scriptType));
            }
        }

        // ADD FILE TO DIRECTORY
        scriptInfo.setDirectory(String.format("%s/%s%s", scriptInfo.getDirectory(), scriptName,
            AppConstants.SCRIPT_ENGINE_EXTENSION));
        return scriptInfo;
    }

    private ScriptInfo newScriptInfo(String scriptName, ScriptType scriptType, LoadedDungeon dungeon) {
        // NO PLAYER, BUT RUN DUNGEON SCRIPT
        ScriptInfo scriptInfo = new ScriptInfo(scriptName, scriptType);
        String dungeonName = dungeon.getDungeon().getName();
        scriptInfo.setDirectory(String.format("%s/%s%s",
            AppUtils.getDungeonScriptDirectory(dungeonName, scriptType).toString(), scriptName,
                AppConstants.SCRIPT_ENGINE_EXTENSION));

        return scriptInfo;
    }

    public boolean startScript(String scriptName, ScriptType scriptType, Player player) {
        return startScript(scriptName, scriptType, player, "");
    }

    /**
     * Starts a script with the given parameters.
     * @param scriptName Name of script.
     * @param scriptType Type of script.
     * @param player Player to bind to script.
     * @param functionName Name of function to invoke.
     * @return true if the script has started, false otherwise.
     */
    public boolean startScript(String scriptName, ScriptType scriptType, Player player, String functionName) {
        ScriptInfo scriptInfo = newScriptInfo(scriptName, scriptType, player);
        if (!scriptInfo.isScriptExists()) {
            // SCRIPT DOES NOT EXIST, EXIT
            return false;
        }
        scriptInfo.setPlayer(player);

        // CREATE BINDINGS
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("sm", this);
        if (player != null) {
            bindings.put("player", player);
            String dungeonName = AppUtils.getPlayerDungeon(player);
            Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
            if (activeDungeons.containsKey(dungeonName)) {
                bindings.put("dungeon", activeDungeons.get(dungeonName));
            } else {
                // PLAYER IS NOT IN DUNGEON, USE WORLD SCRIPT BINDINGS
                bindings.put("world", player.getWorld());
                bindings.put("variables", new WorldVariables(player.getWorld()));
            }
        }
        scriptInfo.setBindings(bindings);

        // RUN SCRIPT ASYNC
        if (functionName.isEmpty()) {
            AppStatus.getExecutorService().execute(() -> startScript(scriptInfo));
        } else {
            AppStatus.getExecutorService().execute(() -> startScript(scriptInfo, functionName));
        }
        return true;
    }

    public boolean startScript(String scriptName, ScriptType scriptType, Bindings bindings) {
        return startScript(scriptName, scriptType, bindings, "");
    }

    /**
     * Starts a script with the given parameters.
     * @param scriptName Name of script.
     * @param scriptType Type of script.
     * @param bindings Script bindings.
     * @param functionName Function name to invoke.
     * @param parameters Parameters to pass to function.
     * @return
     */
    public boolean startScript(String scriptName, ScriptType scriptType, Bindings bindings, String functionName,
        Object... parameters) {

        ScriptInfo scriptInfo = newScriptInfo(scriptName, scriptType, (Player) null);
        if (!scriptInfo.isScriptExists()) {
            // SCRIPT DOES NOT EXIST, EXIT
            return false;
        }

        // SET BINDINGS
        scriptInfo.setBindings(bindings);

        // RUN SCRIPT ASYNC
        if (functionName.isEmpty()) {
            AppStatus.getExecutorService().execute(() -> startScript(scriptInfo));
        } else {
            AppStatus.getExecutorService().execute(() -> startScript(scriptInfo, functionName, parameters));
        }
        return true;
    }

    public boolean startScript(String scriptName, ScriptType scriptType, LoadedDungeon dungeon) {
        return startScript(scriptName, scriptType, dungeon, scriptEngine.createBindings());
    }

    /**
     * Starts a script with the given parameters.
     * @param scriptName Name of script.
     * @param scriptType Type of script.
     * @param dungeon Dungeon the script is for.
     * @param bindings Script bindings.
     * @return boolean if script has started.
     */
    public boolean startScript(String scriptName, ScriptType scriptType, LoadedDungeon dungeon, Bindings bindings) {
        ScriptInfo scriptInfo = newScriptInfo(scriptName, scriptType, dungeon);
        if (!scriptInfo.isScriptExists()) {
            // SCRIPT DOES NOT EXIST, EXIT
            return false;
        }

        // CREATE BINDINGS
        bindings.put("sm", this);
        bindings.put("dungeon", dungeon);
        scriptInfo.setBindings(bindings);

        // RUN SCRIPT ASYNC
        AppStatus.getExecutorService().execute(() -> startScript(scriptInfo));
        return true;
    }

    /**
     * Executes a script with the given script info.
     * @param script Script information.
     */
    private void startScript(ScriptInfo script) {
        if (!script.isScriptExists()) {
            // SCRIPT DOES NOT EXIST
            // Bukkit.getLogger().warning("CANNOT FIND " + script.getDirectory());
            return;
        }

        StringBuilder content = new StringBuilder();
        CompiledScript compiledScript;

        // TRY COMPILING SCRIPT
        try {
            FILE_READ_LOCK.lock();
            content.append(script.readScript());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FILE_READ_LOCK.unlock();
        }

        // TRY EVALUATING COMPILED SCRIPT
        try {
            compiledScript = ((Compilable) scriptEngine).compile(content.toString());
            compiledScript.eval(script.getBindings());
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a script with the given script info.
     * This will also invoke a function and pass parameters to it.
     * @param script Script information.
     * @param functionName Function name to invoke.
     * @param parameters Parameters to pass to function.
     */
    private void startScript(ScriptInfo script, String functionName, Object... parameters) {
        if (!script.isScriptExists()) {
            // SCRIPT DOES NOT EXIST
            // Bukkit.getLogger().warning("CANNOT FIND " + script.getDirectory());
            return;
        }

        StringBuilder content = new StringBuilder();
        CompiledScript compiledScript;

        // TRY COMPILING SCRIPT
        try {
            FILE_READ_LOCK.lock();
            content.append(script.readScript());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FILE_READ_LOCK.unlock();
        }

        // TRY EVALUATING COMPILED SCRIPT
        try {
            // CONTEXT IS NEEDED TO INVOKE FUNCTIONS
            ScriptContext scriptContext = new SimpleScriptContext();
            scriptContext.setBindings(script.getBindings(), ScriptContext.ENGINE_SCOPE);

            compiledScript = ((Compilable) scriptEngine).compile(content.toString());
            compiledScript.eval(scriptContext);

            scriptEngine.setContext(scriptContext);
            Invocable invocable = (Invocable) scriptEngine;
            invocable.invokeFunction(functionName, parameters);
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // IGNORED IN CASE THE USER DELETED SPECIAL METHODS
            // e.printStackTrace();
        }
    }

    @Override
    public void log(String level, String log) {
        AppUtils.consoleLog(Level.parse(level.toUpperCase()), String.format("[ScriptLog] - %s", log));
    }

    @Override
    public App getPlugin() {
        return AppStatus.getPlugin();
    }

    @Override
    public PlayerState enumPlayerState(String string) {
        return PlayerState.valueOf(string);
    }

    @Override
    public PlayerState getPlayerState(Player player) {
        return AppUtils.getPlayerState(player);
    }

    @Override
    public LoadedDungeon getDungeon(Player player) {
        return AppStatus.getActiveDungeons().get(AppStatus.getActivePlayers().get(player.getUniqueId()));
    }

    @Override
    public String getScriptDirectory(LoadedDungeon dungeon) {
        return AppUtils.getDungeonScriptDirectory(dungeon.getDungeon().getName(), ScriptType.None).toString();
    }

    @Override
    public String getScriptDirectory(String dungeonName) {
        return AppUtils.getDungeonScriptDirectory(dungeonName, ScriptType.None).toString();
    }

    @Override
    public String getScriptDirectory(String dungeonName, String scriptType) {
        return AppUtils.getDungeonScriptDirectory(dungeonName, ScriptType.valueOf(scriptType)).toString();
    }

    @Override
    public String getDungeonDirectory(LoadedDungeon dungeon) {
        return getDungeonDirectory(dungeon.getDungeon().getName());
    }

    @Override
    public String getDungeonDirectory(String dungeonName) {
        return AppUtils.getDungeonDirectory(dungeonName).toString();
    }

    @Override
    public World getDungeonWorld(LoadedDungeon dungeon) {
        return dungeon.getDungeon().getDungeonFile().getWorld();
    }

    @Override
    public Player getPlayerFromUUID(UUID uuid) {
        return AppUtils.uuidToPlayer(uuid);
    }

    @Override
    public ItemDrop createItemDrop(Location location, Map<ItemStack, Integer> table) {
        return new ItemDrop(location, table);
    }

    @Override
    public boolean isEconomyEnabled() {
        return AppStatus.getEconomy().isEnabled();
    }

    @Override
    public boolean hasMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            // NO ECONOMY SUPPORT
            return false;
        }
        if (amount < 0) {
            // ANYONE CAN HAVE A NEGATIVE AMOUNT I GUESS
            return true;
        }
        return AppStatus.getEconomy().has(player, amount);
    }

    @Override
    public boolean depositMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            // NO ECONOMY SUPPORT
            return false;
        }
        if (amount < 0) {
            // VAULT DOESN'T SUPPORT NEGATIVE AMOUNTS HERE
            return false;
        }
        return AppStatus.getEconomy().depositPlayer(player, amount).type == ResponseType.SUCCESS;
    }

    @Override
    public boolean withdrawMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            // NO ECONOMY SUPPORT
            return false;
        }
        if (amount < 0) {
            // VAULT DOESN'T SUPPORT NEGATIVE AMOUNTS HERE
            return false;
        }
        Economy economy = AppStatus.getEconomy();
        if (!economy.has(player, amount)) {
            // PLAYER DOESN'T HAVE ENOUGH MONEY
            return false;
        }
        return economy.withdrawPlayer(player, amount).type == ResponseType.SUCCESS;
    }

    @Override
    public void pasteSchematic(String filePath, Location location) {
        ClipboardFormat format = ClipboardFormats.findByFile(new File(filePath));
        try {
            format.load(new File(filePath))
                .paste(new BukkitWorld(location.getWorld()), BlockVector3.at(location.getX(), location.getY(),
                    location.getZ()), true, false, (Transform) null)
                .close();
        } catch (IOException | MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompoundBinaryTag getNBT(Location location) {
        BukkitWorld world = new BukkitWorld(location.getWorld());
        EditSession edit = WorldEdit.getInstance().newEditSession(world);
        BlockVector3 position = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        CompoundBinaryTag cbt = edit.getBlock(position).getNbt();
        edit.close();
        return cbt;
    }

    @Override
    public void setNBT(Location location, CompoundBinaryTag cbt) {
        BukkitWorld world = new BukkitWorld(location.getWorld());
        EditSession edit = WorldEdit.getInstance().newEditSession(world);
        BlockVector3 position = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        BlockState blockState = edit.getBlock(position);
        BaseBlock newBlock = blockState.toBaseBlock(LazyReference.computed(cbt));
        edit.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), newBlock);
        edit.flushQueue();
        edit.close();
    }

    @Override
    public void clearPotionEffect(Player player, PotionEffectType... potionEffectTypes) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PotionEffectType pet : potionEffectTypes) {
                    if (player.hasPotionEffect(pet)) {
                        player.removePotionEffect(pet);
                    }
                }
            }
        }.runTaskLater(AppStatus.getPlugin(), 0L);
    }

    @Override
    public void clearPotionEffect(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                    player.removePotionEffect(potionEffect.getType());
                }
            }
        }.runTaskLater(AppStatus.getPlugin(), 0L);
    }

    @Override
    public void addPotionEffect(Player player, int duration, int amplifier, PotionEffectType... potionEffectTypes) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PotionEffectType pet : potionEffectTypes) {
                    player.addPotionEffect(new PotionEffect(pet, duration, amplifier));
                }
            }
        }.runTaskLater(AppStatus.getPlugin(), 0L);
    }
}
