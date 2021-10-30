package io.github.expugn.dungeons.dungeons;

import io.github.expugn.dungeons.AppConstants;
import io.github.expugn.dungeons.AppUtils;
import java.io.File;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * An object containing a dungeon's name and configuration file.
 * @author S'pugn
 * @version 0.1
 */
public class Dungeon {
    private String name;
    private DungeonFile config;

    /**
     * Dungeon constructor.
     * DungeonFile will be loaded if it exists.
     * @param name Dungeon's name.
     */
    public Dungeon(String name) {
        this.name = name;

        // ONLY TRY TO GET CONFIG IF DUNGEON FILES EXIST
        if (AppUtils.getDungeonDirectory(name).exists()) {
            File df = AppUtils.getDungeonConfigFile(name);
            config = DungeonFile.getDungeonFile(df);
        }
    }

    /**
     * Create a new dungeon and all necessary files needed to run it.
     * @param world World that the dungeon will be in.
     * @param areaString A string containing the bounding box of the dungeon.
     */
    public void create(World world, String areaString) {
        // CREATE ALL FILES RELATED TO A DUNGEON
        if (name.isEmpty()) {
            // name ISN'T SET, DUNGEON CAN NOT BE CREATED
            return;
        }

        File dungeonDirectory = AppUtils.getDungeonDirectory(name);
        if (dungeonDirectory.exists()) {
            // DUNGEON DIRECTORY ALREADY EXISTS
            return;
        }

        dungeonDirectory.mkdirs();

        // CREATE DUNGEON SCRIPT DIRECTORIES
        File scripts = new File(String.format("%s/%s", dungeonDirectory.getPath(),
            AppConstants.DUNGEON_SCRIPT_DIRECTORY));
        scripts.mkdir();
        for (String sd : AppConstants.DUNGEON_SCRIPT_DIRECTORIES) {
            new File(String.format("%s/%s", scripts.getPath(), sd)).mkdir();
        }

        // CREATE DUNGEON SCRIPTS
        DungeonScript.writeFiles(name);

        // CREATE DUNGEON SETTING FILE
        File dungeonConfig = AppUtils.getDungeonConfigFile(name);
        if (!dungeonConfig.exists()) {
            // CREATE NEW DEFAULT DUNGEON FILE
            DungeonFile df = new DungeonFile();

            // SET DUNGEON WORLD
            df.setWorld(world);

            // SET DUNGEON BOUNDARIES
            String[][] split = AppUtils.splitAreaString(areaString);
            df.setBlockA(split[0]);
            df.setBlockB(split[1]);

            // SAVE FILE
            df.saveJSON(dungeonConfig);

            this.config = df;
        }
    }

    /**
     * Get a dungeon's dungeon file.
     * @return Dungeon's DungeonFile.
     */
    public DungeonFile getDungeonFile() {
        return config;
    }

    /**
     * Get a dungeon's name.
     * @return Dungeon's name
     */
    public String getName() {
        return name;
    }

    /**
     * Save the dungeon's DungeonFile.
     */
    public void saveDungeonFile() {
        config.saveJSON(AppUtils.getDungeonConfigFile(name));
    }

    /**
     * Check if the player is in the dungeon's bounding box.
     * @param player Player to check.
     * @return true if the player is in the area, false otherwise.
     */
    public boolean isPlayerInDungeonBoundaries(Player player) {
        return AppUtils.isInsideArea(config.getBlockA(), config.getBlockB(), player.getLocation());
    }
}
