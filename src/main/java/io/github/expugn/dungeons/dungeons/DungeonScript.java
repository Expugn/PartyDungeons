package io.github.expugn.dungeons.dungeons;

import io.github.expugn.dungeons.AppConstants;
import io.github.expugn.dungeons.AppUtils;
import io.github.expugn.dungeons.scripts.ScriptType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages the file names of dungeon scripts and creates them.
 * @author S'pugn
 * @version 0.2
 */
public final class DungeonScript {
    /**
     * Script containing instructions on how to handle `/partydungeons status`.
     */
    public static final String DUNGEON_STATUS = "DungeonStatus";

    /**
     * Script containing instructions for additional things that need to be changed when a dungeon is reset.
     */
    public static final String ON_DUNGEON_RESET = "onDungeonReset";

    /**
     * Script containing instructions on how to deal with an entity (non-player) death in the dungeon area while it is
     * active.
     */
    public static final String ON_ENTITY_DEATH = "onEntityDeath";

    /**
     * Script containing instructions on how to deal with starting a dungeon when the party gains an additional
     * member.
     */
    public static final String ON_PARTY_MEMBER_JOIN = "onPartyMemberJoin";

    /**
     * Script containing instructions on how to deal with Dead players trying to rejoin the dungeon session they were
     * a part of.
     */
    public static final String ON_PLAYER_REBIRTH = "onPlayerRebirth";

    /**
     * Script containing instructions on how to revert a player to their original state if the dungeon has modified
     * their health or given them permanent potion effects.
     */
    public static final String ON_PLAYER_RESET = "onPlayerReset";

    /**
     * Script containing instructions on how to handle a player when they respawn.
     */
    public static final String ON_PLAYER_RESPAWN = "onPlayerRespawn";

    /**
     * Script containing instructions on how to handle a player when they quit the dungeon (quit, leave, disconnect).
     */
    public static final String ON_PARTY_MEMBER_QUIT = "onPartyMemberQuit";

    /**
     * Script containing instructions on how to handle a player when they die in a dungeon.
     */
    public static final String ON_PLAYER_DEATH = "onPlayerDeath";

    private DungeonScript() {
        // NOT CALLED, DungeonScript IS A UTILITY CLASS AND REQUIRES THIS PRIVATE CONSTRUCTOR.
    }

    /**
     * Create and write all dungeon script files for the provided dungeon name.
     * @param dungeonName Name of dungeon to create dungeon type scripts for.
     */
    public static void writeFiles(String dungeonName) {
        File dungeonScripts = AppUtils.getDungeonScriptDirectory(dungeonName, ScriptType.Dungeon);
        if (!dungeonScripts.exists()) {
            dungeonScripts.mkdirs();
        }
        File file;
        String content;

        final String tags = String.format("%s%s%s%s",
            String.format(" * %-12s %s\n", "@author", "CONSOLE"),
            String.format(" * %-12s %s\n", "@version", "0.1"),
            String.format(" * %-12s %s\n", "@version", "DUNGEON"),
            String.format(" * %-12s %s\n", "@dungeon", dungeonName));
        final String binding = String.format("%s%s",
            String.format(" * @param %-17s %-10s %s\n", String.format("{%s}", "ScriptManager"), "sm", "SCRIPT BINDING"),
            String.format(" * @param %-17s %-10s %s\n",
                String.format("{%s}", "LoadedDungeon"), "dungeon", "SCRIPT BINDING"));
        final String bindingPlayer = String.format("%s%s",
            String.format(" * @param %-17s %-10s %s\n",
                String.format("{%s}", "Player"), "player", "SCRIPT BINDING"), binding);
        final String bindingEntity = String.format("%s%s",
            String.format(" * @param %-17s %-10s %s\n",
                String.format("{%s}", "LivingEntity"), "entity", "SCRIPT BINDING"), binding);

        try {
            content = String.format("/**\n%s %s\n *\n%s\n%s\n *\n%s *\n%s */\n%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * DungeonStatus is a script that containing instructions",
                " * on how to handle a user when they run `/partydungeons status`.",
                tags, bindingPlayer, "function main() {\n\tdungeon.showDefaultStatus(player);\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, DUNGEON_STATUS,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onEntityDeath is a script that containing instructions",
                " * on additional things to do when the dungeon resets",
                " * (disposing of custom entities, changing terrain, etc).",
                tags, binding, dungeonName, ": onDungeonReset called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_DUNGEON_RESET,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onEntityDeath is a script that containing instructions",
                " * on how to deal with entities who die in the dungeon",
                " * area while it is active.",
                tags, bindingEntity, dungeonName, ": onEntityDeath called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_ENTITY_DEATH,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onPartyMemberJoin is a script that containing instructions",
                " * on how to start the dungeon once a party member joins.",
                tags, bindingPlayer, dungeonName, ": onPartyMemberJoin called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_PARTY_MEMBER_JOIN,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onPlayerReset is a script that containing instructions",
                " * on how to deal with dead players trying to rejoin",
                " * the dungeon session they were a part of.",
                tags, bindingPlayer, dungeonName, ": onPlayerRebirth called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_PLAYER_REBIRTH,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onPlayerReset is a script that contains instructions on how",
                " * to revert a player back to their original state if the dungeon",
                " * has modified their health or potion effects.",
                tags, bindingPlayer, dungeonName, ": onPlayerReset called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_PLAYER_RESET,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onPlayerRespawn is a script that contains instructions on how",
                " * to handle a player when they respawn in game after they die.",
                " * The player at this state is labeled as \"Dead\" in the dungeon.",
                tags, bindingPlayer, dungeonName, ": onPlayerRespawn called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_PLAYER_RESPAWN,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onPartyMemberQuit is a script that contains instructions on how",
                " * to handle a player when they leave or disconnect from a party while it is active.",
                " * The player at this state is labeled as \"Quitter\" or \"Offline\" in the dungeon.",
                tags, bindingPlayer, dungeonName, ": onPartyMemberQuit called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_PARTY_MEMBER_QUIT,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);

            content = String.format("/**\n%s %s\n *\n%s\n%s\n%s\n *\n%s *\n%s */\nfunction main() {\n\tprint(\"%s%s",
                " * DUNGEON type script for DUNGEON", dungeonName,
                " * onPlayerDeath is a script that contains instructions on how",
                " * to handle a player when they die while a dungeon is active.",
                " * The player at this state is labeled as \"Dead\" in the dungeon.",
                tags, bindingPlayer, dungeonName, ": onPlayerDeath called\");\n}\nmain();");
            file = new File(String.format("%s/%s%s", dungeonScripts, ON_PLAYER_DEATH,
                AppConstants.SCRIPT_ENGINE_EXTENSION));
            write(new FileWriter(file), file, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write(FileWriter writer, File file, String content) throws IOException {
        writer.write(content);
        writer.close();
    }
}
