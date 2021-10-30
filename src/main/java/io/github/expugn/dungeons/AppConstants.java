package io.github.expugn.dungeons;

/**
 * Manages some constants for the PartyDungeons App.
 * @author S'pugn
 * @version 0.1
 */
public final class AppConstants {
    /**
     * PartyDungeon's admin permission.
     * This enables dungeon creation and other features, be careful on who you give this to.
     */
    public static final String ADMIN_PERMISSION = "partydungeons.admin";

    /**
     * PartyDungeon's developer permission.
     * This enables usage of `/spugn` which is a command that provides debug tools.
     * The value needs to be updated too in `plugin.yml` if changed.
     */
    public static final String DEVELOPER_PERMISSION = "partydungeons.dev";

    /**
     * Max distance where a target block will be searched for.
     * Used in instances like area selecting or Interact script creation.
     */
    public static final int MAX_TARGET_BLOCK_DISTANCE = 5;

    /**
     * Dungeon scripts will be saved in the following directory.
     * plugins/<plugin_name>/dungeons/<dungeon_name>/<DUNGEON_SCRIPT_DIRECTORY>.
     */
    public static final String DUNGEON_SCRIPT_DIRECTORY = "scripts";

    /**
     * Dungeon script directories will be created in the following directory.
     * plugins/<plugin_name>/dungeons/<dungeon_name>/<DUNGEON_SCRIPT_DIRECTORY>/<directory>
     */
    public static final String[] DUNGEON_SCRIPT_DIRECTORIES = {"area_walk", "dungeon", "interact", "walk"};

    /**
     * Script engine name, see {@link ScriptManager}.
     * "Nashorn" is a JavaScript engine.
     */
    public static final String SCRIPT_ENGINE_NAME = "Nashorn";

    /**
     * All script files must have their file extensions be <SCRIPT_ENGINE_EXTENSION>.
     */
    public static final String SCRIPT_ENGINE_EXTENSION = ".js";

    private AppConstants() {
        // NOT USED, BUT AppConstants IS A UTILITY CLASS THAT REQUIRES THIS PRIVATE CONSTRUCTOR
    }
}
