package io.github.expugn.dungeons;

import io.github.expugn.dungeons.dungeons.Dungeon;
import io.github.expugn.dungeons.dungeons.DungeonScript;
import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.scripts.ScriptType;
import io.github.expugn.dungeons.scripts.ScriptWriter;
import io.github.expugn.dungeons.worlds.WorldVariables;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Manages all the commands for the PartyDungeons App.
 * @author S'pugn
 * @version 0.2
 */
public class AppCommand implements CommandExecutor {
    /**
     * Enum of available commands for PartyDungeons.
     */
    public enum Commands {
        // GENERAL COMMANDS
        /**
         * Display a help menu.
         */
        HELP("help", false),

        /**
         * Join a dungeon.
         * Player must be in a valid dungeon area to do so.
         */
        JOIN("join", false),

        /**
         * Leave a dungeon.
         * Player will be marked as a quitter if dungeon is active.
         * Player also can't rejoin the current dungeon session.
         */
        LEAVE("leave", false),

        /**
         * Display current dungeon status.
         * Player must be in party to get this information.
         */
        STATUS("status", false),

        // ADMIN COMMANDS
        /**
         * Create a new dungeon.
         * An area must be created, run this command while looking at the first block.
         */
        CREATE_DUNGEON("createdungeon", true),

        /**
         * Create a world script directory.
         */
        CREATE_WORLD_DIRECTORY("createworlddirectory", true),

        /**
         * Load an unloaded dungeon.
         * All dungeons are loaded by default..
         */
        LOAD_DUNGEON("loaddungeon", true),

        /**
         * Unload a loaded dungeon.
         * Active players in this dungeon will be kicked and won't be able to rejoin.
         */
        UNLOAD_DUNGEON("unloaddungeon", true),

        /**
         * Set the spawn position of a dungeon.
         * The spawn position is a location where players will teleport to when they leave or disconnect.
         */
        SET_SPAWN_POSITION("setspawnposition", true),

        /**
         * Set the start position of a dungeon.
         * The start positon is a location where all Alive party members will teleport to when the dungeon starts.
         */
        SET_START_POSITION("setstartposition", true),

        /**
         * Creates a new script with the desired type for the player.
         * Also adds additional boilerplate code and comments.
         */
        CREATE_SCRIPT("createscript", true),

        /**
         * Creates a new world script with the desired type for the player.
         * Also adds additional boilerplate code and comments.
         */
        CREATE_WORLD_SCRIPT("createworldscript", true),

        /**
         * Delete a script with the given file path.
         */
        DELETE_SCRIPT("deletescript", true),

        /**
         * Change a dungeon's setting for max_value and daily_clear.
         * Not including the optional [value] argument will display the current value.
         */
        SETTINGS("settings", true),

        /**
         * Runs a script as if the player walked/interacted/etc.
         * For compatibility reasons, the player should be in a party for this to run.
         * Generally, just avoid using this command and have players trigger scripts normally.
         */
        RUN_SCRIPT("runscript", true),

        /**
         * Download all files listed on a file manifest.
         * All files will be overwritten, so avoid using this command if you are afraid of
         * overwriting important files.
         */
        DOWNLOAD("download", true),

        /**
         * Generate a file manifest for a specific dungeon.
         * An optional [root_url] argument can be included to simplify the URL including process.
         * Generated file manifests must be reviewed before they are used.
         */
        MANIFEST("manifest", true);

        private String command;
        private boolean admin;

        Commands(String command, boolean admin) {
            this.command = command;
            this.admin = admin;
        }

        public static Commands get(final String value) {
            return Arrays.stream(values()).filter(v -> v.toString().equals(value)).findFirst().orElse(null);
        }

        public boolean isAdminCommand() {
            return admin;
        }

        @Override
        public String toString() {
            return command;
        }
    }

    private static final int THREE_ARGUMENTS = 3;
    private static final int FOUR_ARGUMENTS = 4;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final String subCommand = (args.length > 0) ? args[0].toLowerCase() : "";
        final Player player = sender instanceof Player ? (Player) sender : null;

        if (player == null) {
            sender.sendMessage("THIS COMMAND CAN ONLY BE RUN BY A PLAYER.");
            return true;
        }

        if (args.length == 0) { // partydungeons
            player.sendMessage(String.format("%sWelcome to PartyDungeons! For a help menu, use %s/partydungeons help",
                ChatColor.YELLOW, ChatColor.GOLD));
            return true;
        }

        Commands sub;
        try {
            sub = Commands.get(subCommand);
        } catch (IllegalArgumentException e) {
            player.sendMessage(String.format("%sUndefined command, use %s/partydungeons help", ChatColor.RED,
                ChatColor.GOLD));
            return true;
        }

        switch (sub) {
            case HELP: // partydungeons help
                AppHelpMenu.send(sender);
                break;
            case JOIN: // partydungeons join
                // PLAYER JOINS PARTY IF THEY ARE IN DUNGEON AREA
                joinDungeon(player);
                break;
            case LEAVE: //partydungeons leave
                // PLAYER LEAVES ANY PARTY THEY'RE A PART OF
                leaveDungeon(player);
                break;
            case STATUS: //partydungeons status
                // PRINT STATUS OF CURRENT DUNGEON THE PLAYER IS A PART OF
                showStatus(player);
                break;

            // ADMIN COMMANDS
            case CREATE_DUNGEON: // partydungeons createdungeon <dungeon_name>
                createDungeon(player, args);
                break;
            case CREATE_WORLD_DIRECTORY: // partydungeons createworlddirectory
                createWorldDirectory(player);
                break;
            case LOAD_DUNGEON: // partydungeons loaddungeon <dungeon_name>
                loadDungeon(player, args);
                break;
            case UNLOAD_DUNGEON: // partydungeons unloaddungeon <dungeon_name>
                unloadDungeon(player, args);
                break;
            case SET_SPAWN_POSITION: // partydungeons setspawnposition <dungeon_name>
                setSpawnPosition(player, args);
                break;
            case SET_START_POSITION: // partydungeons setstartposition <dungeon_name>
                setStartPosition(player, args);
                break;
            case CREATE_SCRIPT: // partydungeons createscript <dungeon_name> <script_type>
                createScript(player, args);
                break;
            case CREATE_WORLD_SCRIPT: // partydungeons createworldscript <script_type>
                createWorldScript(player, args);
                break;
            case DELETE_SCRIPT: // partydungeons deletescript <file_path>
                deleteScript(player, args);
                break;
            case SETTINGS: // partydungeons settings <dungeon_name> <max_party/daily_clear> [value]
                changeSettings(player, args);
                break;
            case RUN_SCRIPT: // partydungeons runscript <dungeon_name> <type> <script_name>
                runScript(player, args);
                break;
            case DOWNLOAD: // partydungeons download <dungeon_name> <manifest_url>
                downloadManifest(player, args);
                break;
            case MANIFEST: // partydungeons manifest <dungeon_name> [root_url]
                createManifest(player, args);
                break;

            // UNKNOWN COMMAND
            default:
                player.sendMessage(String.format("%sUnimplemented command, use %s/partydungeons help", ChatColor.RED,
                    ChatColor.GOLD));
        }

        return true;
    }

    private void joinDungeon(Player player) {
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        for (Map.Entry<String, LoadedDungeon> entry : activeDungeons.entrySet()) {
            if (entry.getValue().getDungeon().isPlayerInDungeonBoundaries(player)) {
                // DUNGEON FOUND THAT PLAYER CAN JOIN
                entry.getValue().join(player);
                return;
            }
        }

        // DUNGEON COULD NOT BE FOUND, PLAYER IS NOT IN AN AREA OF AN ACTIVE DUNGEON
        player.sendMessage(String.format("%sDungeon could not be found. Stand in a dungeon area and try again.",
            ChatColor.RED));
    }

    private void leaveDungeon(Player player) {
        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        UUID uuid = player.getUniqueId();
        if (!activePlayers.containsKey(uuid)) {
            // PLAYER IS NOT ACTIVE IN A DUNGEON
            player.sendMessage(String.format("%sYou are not a part of a dungeon.", ChatColor.RED));
            return;
        }

        String dungeonName = activePlayers.get(uuid);
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();

        if (!activeDungeons.containsKey(dungeonName)) {
            // PLAYER IS A PART OF AN UNLOADED DUNGEON, FOR SOME REASON
            player.sendMessage(String.format("%sYou are no longer a part of %s%s%s.",
                ChatColor.YELLOW, ChatColor.GOLD, dungeonName, ChatColor.YELLOW));
            activePlayers.remove(uuid);
            return;
        }

        // PLAYER IS TRYING TO LEAVE AN ACTIVE DUNGEON THEY AREA A PART OF
        LoadedDungeon dungeon = activeDungeons.get(dungeonName);
        dungeon.leave(player);
    }

    private void showStatus(Player player) {
        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        UUID uuid = player.getUniqueId();
        if (!activePlayers.containsKey(uuid)) {
            // PLAYER IS NOT AN ACTIVE PLAYER
            player.sendMessage(String.format("%sYou are not currently in a dungeon.", ChatColor.RED));
            return;
        }

        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        String dungeonName = AppUtils.getPlayerDungeon(player);
        if (!activeDungeons.containsKey(dungeonName)) {
            // PLAYER IS A PART OF A DUNGEON THAT ISN'T ACTIVE FOR SOME REASON
            player.sendMessage(String.format("%sYou are not currently in a dungeon.", ChatColor.RED));
            activePlayers.remove(uuid);
            return;
        }

        // PLAYER IS AN ACTIVE PLAYER AND PART OF A DUNGEON THAT'S ACTIVE
        AppStatus.getScriptManager().startScript(DungeonScript.DUNGEON_STATUS, ScriptType.Dungeon, player);
    }

    private void createDungeon(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons createdungeon <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (AppUtils.getDungeonDirectory(dungeonName).exists()) {
            // DUNGEON ALREADY EXISTS
            player.sendMessage(String.format("%sA dungeon named %s%s %salready exists.",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        Block target = AppUtils.getTargetBlock(player);
        if (target == null) {
            // TARGET COULD NOT BE FOUND
            player.sendMessage(String.format("%sTarget block could not be found. Target must be within %s%d %sblocks.",
                ChatColor.RED, ChatColor.GOLD, AppConstants.MAX_TARGET_BLOCK_DISTANCE, ChatColor.RED));
            return;
        } else if (target.getType() == Material.AIR) {
            // TARGET IS AIR
            player.sendMessage(String.format("%sTarget block can not be %sAIR", ChatColor.RED, ChatColor.GOLD));
            return;
        }

        String result = AppUtils.playerSelection(player, target);
        if (result.isEmpty()) {
            // FIRST SELECTION HAS BEEN MADE, PLAYER NEEDS TO SELECT A SECOND BLOCK
            player.sendMessage(String.format("%sBlock %s%s %shas been selected.\n%s%s",
                ChatColor.GREEN, ChatColor.GOLD, AppUtils.prettyBlockString(target), ChatColor.GREEN, ChatColor.YELLOW,
                "Please run the command again to select the second block."));
            return;
        }

        // SECOND SELECTION HAS BEEN MADE, THE DUNGEON CAN NOW BE CREATED.
        Dungeon dungeon = new Dungeon(args[1]);
        dungeon.create(player.getWorld(), result);

        // ADD DUNGEON TO LOADED DUNGEONS
        AppStatus.loadDungeon(dungeonName);

        // PROCESS COMPLETE
        player.sendMessage(String.format("%s%s %sdungeon has been created in area %s%s%s!",
            ChatColor.GOLD, dungeonName, ChatColor.GREEN, ChatColor.GOLD, AppUtils.prettyAreaString(result),
            ChatColor.GREEN));
    }

    private void createWorldDirectory(Player player) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        // CHECK IF WORLD DIRECTORY EXISTS
        World world = player.getWorld();
        File worldDirectory = AppUtils.getWorldDirectory(world);
        if (worldDirectory.exists()) {
            player.sendMessage(String.format("%sWorld directory for world %s%s %salready exists.",
                ChatColor.RED, ChatColor.GOLD, world.getName(), ChatColor.RED));
            return;
        }

        worldDirectory.mkdirs();

        // CREATE WORLD SCRIPT DIRECTORIES
        File scripts = new File(String.format("%s/%s", worldDirectory.getPath(),
            AppConstants.DUNGEON_SCRIPT_DIRECTORY));
        scripts.mkdir();

        // CREATE SCRIPT DIRECTORIES BESIDES "dungeon" (BECAUSE WORLDS DONT USE DUNGEON SCRIPTS)
        Arrays.stream(AppConstants.DUNGEON_SCRIPT_DIRECTORIES).filter(s -> !s.equals("dungeon"))
            .forEach(sd -> new File(String.format("%s/%s", scripts.getPath(), sd)).mkdir());

        // CREATE WORLD VARIABLE FILE
        File worldVariableFile = AppUtils.getWorldVariableFile(world);
        if (!worldVariableFile.exists()) {
            // CREATE NEW DEFAULT WORLD VARIABLE FILE
            new WorldVariables(world).saveJSON();
        }

        // PROCESS COMPLETE
        player.sendMessage(String.format("%sWorld directory for world %s%s %screated!",
            ChatColor.GREEN, ChatColor.GOLD, world.getName(), ChatColor.GREEN));
    }

    private void loadDungeon(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons loaddungeon <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (AppStatus.getActiveDungeons().containsKey(dungeonName)) {
            // DUNGEON IS ALREADY LOADED
            player.sendMessage(String.format("%sDungeon %s%s %sis already loaded.",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        player.sendMessage(AppStatus.loadDungeon(dungeonName)
            ? String.format("%sLoaded dungeon %s%s %s!",
                ChatColor.GREEN, ChatColor.GOLD, dungeonName, ChatColor.GREEN)
            : String.format("%sSomething went wrong (dungeon doesn't exist?), dungeon could not be loaded.",
                ChatColor.RED));
    }

    private void unloadDungeon(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons unloaddungeon <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (!AppStatus.getActiveDungeons().containsKey(dungeonName)) {
            // DUNGEON IS ALREADY NOT LOADED
            player.sendMessage(String.format("%sDungeon %s%s %sis already unloaded.",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        player.sendMessage(AppStatus.unloadDungeon(dungeonName)
            ? String.format("%sUnloaded dungeon %s%s %s!",
                ChatColor.GREEN, ChatColor.GOLD, dungeonName, ChatColor.GREEN)
            : String.format("%sSomething went wrong, dungeon could not be loaded.",
                ChatColor.RED));
    }

    private void setSpawnPosition(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons setspawnposition <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (!AppStatus.getActiveDungeons().containsKey(dungeonName)) {
            // LOADED DUNGEON DOES NOT EXIST
            player.sendMessage(String.format("%sA loaded dungeon named %s%s %sdoes not exist.",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        // OPEN DUNGEONFILE AND CHANGE SPAWN POSITION TO WHERE THE USER IS STANDING
        Dungeon dungeon = AppStatus.getActiveDungeons().get(dungeonName).getDungeon();
        Location location = player.getLocation();
        dungeon.getDungeonFile().setSpawnPosition(location);
        dungeon.saveDungeonFile();

        // PROCESS COMPLETE
        player.sendMessage(String.format("%sSpawnPosition for dungeon %s%s %shas been set to %s%s%s!",
            ChatColor.GREEN, ChatColor.GOLD, dungeonName, ChatColor.GREEN, ChatColor.GOLD,
            AppUtils.prettyBlockString(location.getBlock()), ChatColor.GREEN));
    }

    private void setStartPosition(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons setstartposition <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (!AppStatus.getActiveDungeons().containsKey(dungeonName)) {
            // DUNGEON DOES NOT EXIST
            player.sendMessage(String.format("%sA loaded dungeon named %s%s %sdoes not exist.",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        // OPEN DUNGEONFILE AND CHANGE START POSITION TO WHERE THE USER IS STANDING
        Dungeon dungeon = AppStatus.getActiveDungeons().get(dungeonName).getDungeon();
        Location location = player.getLocation();
        dungeon.getDungeonFile().setStartPosition(location);
        dungeon.saveDungeonFile();

        // PROCESS COMPLETE
        player.sendMessage(String.format("%sStartPosition for dungeon %s%s %shas been set to %s%s%s!",
            ChatColor.GREEN, ChatColor.GOLD, dungeonName, ChatColor.GREEN, ChatColor.GOLD,
            AppUtils.prettyBlockString(location.getBlock()), ChatColor.GREEN));
    }

    private void createScript(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons createscript <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name> <script_type>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (!AppUtils.getDungeonDirectory(dungeonName).exists()) {
            // DUNGEON DOES NOT EXIST
            player.sendMessage(String.format("%sA dungeon named %s%s %sdoes not exist.",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        // DUNGEON EXISTS, FIGURE OUT SCRIPT TYPE
        if (args.length < THREE_ARGUMENTS) { // partydungeons createscript <dungeon_name> <script_type>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s %s <script_type>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
            return;
        }

        ScriptType scriptType;
        try {
            scriptType = ScriptType.valueOf(args[2]);
        } catch (IllegalArgumentException | NullPointerException e) {
            // INVALID SCRIPT TYPE GIVEN
            player.sendMessage(String.format("%sInvalid ScriptType. %s/partydungeons %s %s <script_type>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
            return;
        }

        // FIGURE OUT TARGET BLOCK (if necessary)
        Block targetBlock = null;
        if (scriptType.equals(ScriptType.Interact)
            || scriptType.equals(ScriptType.Walk)
            || scriptType.equals(ScriptType.AreaWalk)) {
            // SCRIPT IS Interact/Walk/AreaWalk TYPE
            targetBlock = AppUtils.getTargetBlock(player);
            if (targetBlock == null) {
                // TARGET COULD NOT BE FOUND
                player.sendMessage(
                    String.format("%sTarget block could not be found. Target must be within %s%d %sblocks.",
                    ChatColor.RED, ChatColor.GOLD, AppConstants.MAX_TARGET_BLOCK_DISTANCE, ChatColor.RED));
                return;
            } else if (targetBlock.getType() == Material.AIR) {
                // TARGET IS AIR
                player.sendMessage(String.format("%sTarget block can not be %sAIR", ChatColor.RED, ChatColor.GOLD));
                return;
            }
        }

        // FIGURE OUT AUTO-GENERATED SCRIPT NAME
        String scriptName;
        if (scriptType.equals(ScriptType.AreaWalk)) {
            scriptName = AppUtils.playerSelection(player, targetBlock);
            if (scriptName.isEmpty()) {
                // SCRIPT NAME CAN'T BE DECIDED YET, PLAYER NEEDS TO SELECT AGAIN
                player.sendMessage(String.format("%sBlock %s has been selected.\n%s%s",
                    ChatColor.GOLD, AppUtils.prettyBlockString(targetBlock), ChatColor.YELLOW,
                    "Please run the command again to select the second block."));
                return;
            }
            // SCRIPT NAME HAS BEEN DECIDED BEYOND THIS POINT
        } else if (targetBlock != null) {
            // USE x_y_z FOR FILE NAME
            scriptName = AppUtils.getBlockString(targetBlock);
        } else {
            // USE TIME STAMP FOR FILE NAME
            scriptName = String.format("script_%d", System.currentTimeMillis());
        }

        boolean isSuccessful = new ScriptWriter(AppUtils.getDungeonScriptDirectory(dungeonName, scriptType).toString(),
            scriptName, player.getName(), scriptType, null, targetBlock).writeFile();
        player.sendMessage(isSuccessful
            ? String.format("%sScript %s%s %screated!", ChatColor.GREEN, ChatColor.GOLD, scriptName, ChatColor.GREEN)
            : String.format("%sScript %s%s %scould not be created. %s(Does it already exist?)", ChatColor.RED,
                ChatColor.GOLD, scriptName, ChatColor.RED, ChatColor.GRAY));
    }

    private void createWorldScript(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (!AppUtils.getWorldDirectory(player.getWorld()).exists()) {
            // WORLD DIRECTORY DOES NOT EXIST
            player.sendMessage(String.format("%sWorld directory for world %s%s %sdoes not exist.",
                ChatColor.RED, ChatColor.GOLD, player.getWorld(), ChatColor.RED));
            return;
        }

        if (args.length < 2) { // partydungeons createworldscript <script_type>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <script_type>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        ScriptType scriptType;
        try {
            scriptType = ScriptType.valueOf(args[1]);
        } catch (IllegalArgumentException | NullPointerException e) {
            // INVALID SCRIPT TYPE GIVEN
            player.sendMessage(String.format("%sInvalid ScriptType. %s/partydungeons %s <script_type>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        // FIGURE OUT TARGET BLOCK (if necessary)
        Block targetBlock = null;
        if (scriptType.equals(ScriptType.Interact)
            || scriptType.equals(ScriptType.Walk)
            || scriptType.equals(ScriptType.AreaWalk)) {
            // SCRIPT IS Interact/Walk/AreaWalk TYPE
            targetBlock = AppUtils.getTargetBlock(player);
            if (targetBlock == null) {
                // TARGET COULD NOT BE FOUND
                player.sendMessage(
                    String.format("%sTarget block could not be found. Target must be within %s%d %sblocks.",
                    ChatColor.RED, ChatColor.GOLD, AppConstants.MAX_TARGET_BLOCK_DISTANCE, ChatColor.RED));
                return;
            } else if (targetBlock.getType() == Material.AIR) {
                // TARGET IS AIR
                player.sendMessage(String.format("%sTarget block can not be %sAIR", ChatColor.RED, ChatColor.GOLD));
                return;
            }
        }

        // FIGURE OUT AUTO-GENERATED SCRIPT NAME
        String scriptName;
        if (scriptType.equals(ScriptType.AreaWalk)) {
            scriptName = AppUtils.playerSelection(player, targetBlock);
            if (scriptName.isEmpty()) {
                // SCRIPT NAME CAN'T BE DECIDED YET, PLAYER NEEDS TO SELECT AGAIN
                player.sendMessage(String.format("%sBlock %s has been selected.\n%s%s",
                    ChatColor.GOLD, AppUtils.prettyBlockString(targetBlock), ChatColor.YELLOW,
                    "Please run the command again to select the second block."));
                return;
            }
            // SCRIPT NAME HAS BEEN DECIDED BEYOND THIS POINT
        } else if (targetBlock != null) {
            // USE x_y_z FOR FILE NAME
            scriptName = AppUtils.getBlockString(targetBlock);
        } else {
            // USE TIME STAMP FOR FILE NAME
            scriptName = String.format("script_%d", System.currentTimeMillis());
        }

        boolean isSuccessful = new ScriptWriter(
            AppUtils.getWorldScriptDirectory(player.getWorld(), scriptType).toString(), scriptName, player.getName(),
            scriptType, null, targetBlock).writeFile();
        player.sendMessage(isSuccessful
            ? String.format("%sWorld script %s%s %screated!", ChatColor.GREEN, ChatColor.GOLD, scriptName,
                ChatColor.GREEN)
            : String.format("%sWorld script %s%s %scould not be created. %s(Does it already exist?)", ChatColor.RED,
                ChatColor.GOLD, scriptName, ChatColor.RED, ChatColor.GRAY));
    }

    private void deleteScript(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons deletescript <file_path>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <file_path>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        // CHECK IF FILE ENDS WITH .js
        if (!args[1].endsWith(AppConstants.SCRIPT_ENGINE_EXTENSION)) {
            player.sendMessage(String.format("%sOnly script files can be deleted. %s/partydungeons %s <file_path>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        // CHECK IF FILE EXISTS
        File file = new File(String.format("%s/%s", AppUtils.getPluginDirectory(), args[1]));
        if (!file.exists()) {
            player.sendMessage(String.format("%sFile %s%s %sdoes not exist. %s/partydungeons %s <file_path>",
                ChatColor.RED, ChatColor.GOLD, args[1], ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        try {
            file.delete();
            player.sendMessage(String.format("%sScript %s%s %shas been deleted!",
                ChatColor.GREEN, ChatColor.GOLD, file, ChatColor.GREEN));
        } catch (SecurityException e) {
            player.sendMessage(String.format("%sSecurityException: script %s%s %scould not be deleted.",
                ChatColor.RED, ChatColor.GOLD, file, ChatColor.RED));
        }
    }

    private void changeSettings(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons settings <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s %s",
                ChatColor.RED, ChatColor.GOLD, args[0], "<dungeon_name> <max_party | daily_clear> [value]"));
            return;
        }

        String dungeonName = args[1];
        if (!AppStatus.getActiveDungeons().containsKey(dungeonName)) {
            player.sendMessage(String.format("%sDungeon %s%s %snot found. Is it currently loaded?",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        Dungeon dungeon = AppStatus.getActiveDungeons().get(dungeonName).getDungeon();
        if (args.length < THREE_ARGUMENTS || !(args[2].equals("max_party")
            || args[2].equals("daily_clear"))) { // partydungeons settings <dungeon_name> <max_party/daily_clear>

            player.sendMessage(String.format("%sNot enough or incorrect arguments. %s/partydungeons %s %s %s",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1], "<max_party | daily_clear> [value]"));
            return;
        }

        String settingType = args[2];
        if (args.length < FOUR_ARGUMENTS) { // partydungeons settings <dungeon_name> <max_party/daily_clear> [value]
            // TELL USER CURRENT VALUE INSTEAD OF "Not enough arguments" MESSAGE
            switch (settingType) {
                case "max_party":
                    player.sendMessage(String.format("%sCurrent value of %s%s %sis %s%d%s.",
                        ChatColor.YELLOW, ChatColor.GOLD, args[2], ChatColor.YELLOW, ChatColor.GOLD,
                        dungeon.getDungeonFile().getMaxParty(), ChatColor.YELLOW));
                    break;
                case "daily_clear":
                    player.sendMessage(String.format("%sCurrent value of %s%s %sis %s%d%s.",
                        ChatColor.YELLOW, ChatColor.GOLD, args[2], ChatColor.YELLOW, ChatColor.GOLD,
                        dungeon.getDungeonFile().getDailyClear(), ChatColor.YELLOW));
                    break;
                default:
            }
            return;
        }

        int value;
        final int valueIndex = 3;
        try {
            value = Integer.parseInt(args[valueIndex]);
        } catch (NumberFormatException e) {
            player.sendMessage(String.format("%sValue must be a number. %s/partydungeons %s %s %s [value]",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1], args[2]));
            return;
        }

        switch (settingType) {
            case "max_party":
                dungeon.getDungeonFile().setMaxParty(value);
                break;
            case "daily_clear":
                dungeon.getDungeonFile().setDailyClear(value);
                break;
            default:
                return;
        }
        dungeon.saveDungeonFile();
        player.sendMessage(String.format("%sSetting %s%s %sfor dungeon %s%s %schanged to value %s%d%s.",
                ChatColor.GREEN, ChatColor.GOLD, settingType, ChatColor.GREEN, ChatColor.GOLD, dungeonName,
                ChatColor.GREEN, ChatColor.GOLD, value, ChatColor.GREEN));
    }

    private void runScript(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons runscript <dungeon_name>
            player.sendMessage(
                String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name> <type> <script_name>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (args.length < THREE_ARGUMENTS) { // partydungeons settings <dungeon_name> <type>
            player.sendMessage(
                String.format("%sNot enough or incorrect arguments. %s/partydungeons %s %s <type> <script_name>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
            return;
        }

        ScriptType scriptType;
        try {
            scriptType = ScriptType.valueOf(args[2]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(String.format("%sInvalid ScriptType. %s/partydungeons %s %s <type> <script_name>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
            return;
        }

        if (args.length < FOUR_ARGUMENTS) { // partydungeons runscript <dungeon_name> <type> <script_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s %s %s <script_name>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1], args[2]));
            return;
        }

        if (!AppStatus.getActiveDungeons().containsKey(dungeonName)) {
            player.sendMessage(String.format("%sDungeon %s%s %snot found. Is it currently loaded?",
                ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED));
            return;
        }

        final int scriptNameIndex = 3;
        if (AppStatus.getActivePlayers().containsKey(player.getUniqueId())) {
            // USE PLAYER
            AppStatus.getScriptManager().startScript(args[scriptNameIndex], scriptType, player);
        } else {
            // NO PLAYER, USE DUNGEON INSTEAD
            LoadedDungeon dungeon = AppStatus.getActiveDungeons().get(dungeonName);
            AppStatus.getScriptManager().startScript(args[scriptNameIndex], scriptType, dungeon);
        }
    }

    private void downloadManifest(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons download <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name> <url>",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        if (args.length < THREE_ARGUMENTS) { // partydungeons download <dungeon_name> <url>
            // IF 2ND ARGUMENT IS URL, DOWNLOAD THAT INSTEAD
            URL url;
            try {
                url = new URL(args[1]);
                url.toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                player.sendMessage(String.format("%sNot enough or incorrect arguments. %s/partydungeons %s %s <url>",
                    ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
                return;
            }

            // VALID URL HERE
            Scanner scanner;
            try {
                scanner = new Scanner(url.openStream());
            } catch (IOException e) {
                player.sendMessage(String.format("%sCould not open URL %s%s%s.",
                    ChatColor.RED, ChatColor.GOLD, args[1], ChatColor.RED));
                return;
            }
            // DO FILE DOWNLOAD IN A DIFFERENT THREAD
            AppStatus.getExecutorService().execute(() -> {
                while (scanner.hasNext()) {
                    String next = scanner.nextLine();
                    if (next.length() <= 0 || next.startsWith("#")) {
                        // BLANK LINE OR COMMENT, SKIP
                        continue;
                    }
                    String[] line = next.split(",");
                    File location = new File(String.format("%s/%s", AppUtils.getPluginDirectory(), line[0]));
                    player.sendMessage(String.format("%sDownloading %s%s %sto %s%s",
                        ChatColor.YELLOW, ChatColor.GOLD, line[0], ChatColor.YELLOW, ChatColor.GOLD, location));
                    URL fileURL;
                    BufferedInputStream input;
                    try {
                        fileURL = new URL(line[1]);
                        input = new BufferedInputStream(fileURL.openStream());

                        if (!location.exists() && !location.getParentFile().exists()) {
                            // LOCATION DOESN'T EXIST, CREATE PARENT IF NECESSARY
                            File parent = location.getParentFile();
                            parent.mkdirs();
                        }
                        Files.copy(input, Paths.get(location.toString()), StandardCopyOption.REPLACE_EXISTING);
                        input.close();
                    } catch (MalformedURLException e) {
                        player.sendMessage(String.format("%sMalformed URL in manifest. Stopping download. %s%s",
                            ChatColor.RED, ChatColor.GOLD, line[1]));
                        scanner.close();
                        break;
                    } catch (IOException e) {
                        player.sendMessage(String.format("%sIOException occured during manifest download. %s %s%s",
                            ChatColor.RED, "Connection problem or file doesn't exist?", ChatColor.GOLD, line[1]));
                        break;
                    }
                }
                scanner.close();
                player.sendMessage(String.format("%sManifest download process completed.", ChatColor.YELLOW));
            });
            return;
        }

        File dungeonDirectory = AppUtils.getDungeonDirectory(dungeonName);
        if (!dungeonDirectory.exists()) {
            player.sendMessage(String.format("%sDungeon directory does not exist. %s/partydungeons %s %s <url>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
            return;
        }

        URL url;
        Scanner scanner;
        try {
            url = new URL(args[2]);
            scanner = new Scanner(url.openStream());
        } catch (MalformedURLException e) {
            player.sendMessage(String.format("%sMalformed URL provided. %s/partydungeons %s %s <url>",
                ChatColor.RED, ChatColor.GOLD, args[0], args[1]));
            return;
        } catch (IOException e) {
            player.sendMessage(String.format("%sIOException occured. %s %s/partydungeons %s %s <url>",
                ChatColor.RED, "Connection problem or file doesn't exist?", ChatColor.GOLD, args[0], args[1]));
            return;
        }

        // DO FILE DOWNLOAD IN A DIFFERENT THREAD
        AppStatus.getExecutorService().execute(() -> {
            while (scanner.hasNext()) {
                String next = scanner.nextLine();
                if (next.length() <= 0 || next.startsWith("#")) {
                    // BLANK LINE OR COMMENT, SKIP
                    continue;
                }
                String[] line = next.split(",");
                File location = new File(String.format("%s/%s", dungeonDirectory, line[0]));
                player.sendMessage(String.format("%sDownloading %s%s %sto %s%s",
                    ChatColor.YELLOW, ChatColor.GOLD, line[0], ChatColor.YELLOW, ChatColor.GOLD, location));
                URL fileURL;
                BufferedInputStream input;
                try {
                    fileURL = new URL(line[1]);
                    input = new BufferedInputStream(fileURL.openStream());

                    if (!location.exists()) {
                        // LOCATION DOESN'T EXIST, CREATE PARENT IF NECESSARY
                        File parent = location.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                    }
                    Files.copy(input, Paths.get(location.toString()), StandardCopyOption.REPLACE_EXISTING);
                    input.close();
                } catch (MalformedURLException e) {
                    player.sendMessage(String.format("%sMalformed URL in manifest. Stopping download. %s%s",
                        ChatColor.RED, ChatColor.GOLD, line[1]));
                    scanner.close();
                    break;
                } catch (IOException e) {
                    player.sendMessage(String.format("%sIOException occured during manifest download. %s %s%s",
                        ChatColor.RED, "Connection problem or file doesn't exist?", ChatColor.GOLD, line[1]));
                    break;
                }

            }
            scanner.close();
            player.sendMessage(String.format("%sManifest download process completed.", ChatColor.YELLOW));
        });
    }

    private void createManifest(Player player, String[] args) {
        if (!player.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // PLAYER HAS INSUFFICIENT PERMISSIONS
            player.sendMessage("You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) { // partydungeons manifest <dungeon_name>
            player.sendMessage(String.format("%sNot enough arguments. %s/partydungeons %s <dungeon_name> [root_url]",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String dungeonName = args[1];
        File dungeonDirectory = AppUtils.getDungeonDirectory(dungeonName);
        if (!dungeonDirectory.exists()) {
            player.sendMessage(
                String.format("%sDungeon directory does not exist. %s/partydungeons %s <dungeon_name> [root_url]",
                ChatColor.RED, ChatColor.GOLD, args[0]));
            return;
        }

        String rootURL = "";
        if (args.length >= THREE_ARGUMENTS) { // partydungeons manifest <dungeon_name> [root_url]
            rootURL = args[2];
        }

        File manifest = new File(String.format("%s/manifest", dungeonDirectory));
        if (!manifest.exists()) {
            // CREATE FILE
            try {
                manifest.createNewFile();
            } catch (IOException e) {
                player.sendMessage(String.format("%sIOException: Could not create manifest.", ChatColor.RED));
                return;
            }
        }
        String result = manifestString(rootURL, dungeonDirectory, new StringBuilder(), dungeonDirectory.listFiles());
        try {
            FileWriter writer = new FileWriter(manifest, false);
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            player.sendMessage(String.format("%sIOException: Could not create manifest.", ChatColor.RED));
            return;
        }
        player.sendMessage(String.format("%sManifest created in %s%s%s. Please review the file before using.",
            ChatColor.GREEN, ChatColor.GOLD, manifest, ChatColor.GREEN));
    }

    private String manifestString(String rootURL, File dungeonDirectory, StringBuilder stringBuilder, File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                manifestString(rootURL, dungeonDirectory, stringBuilder, file.listFiles());
            } else {
                String s = file.getPath().substring(dungeonDirectory.toString().length() + 1).replace(File.separator,
                    "/");
                stringBuilder.append(String.format("%s,%s/%s,\n", s, rootURL, s));
            }
        }
        return stringBuilder.toString();
    }
}
