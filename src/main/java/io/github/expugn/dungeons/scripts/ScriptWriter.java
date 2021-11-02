package io.github.expugn.dungeons.scripts;

import io.github.expugn.dungeons.AppConstants;
import io.github.expugn.dungeons.AppUtils;
import io.github.expugn.dungeons.worlds.WorldVariables;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Handles creating and writing to new script files.
 * @author S'pugn
 * @version 0.2
 */
public class ScriptWriter {
    private String directory;
    private String scriptName;
    private String author;
    private ScriptType scriptType;
    private Bindings bindings;
    private Block block;

    /**
     * Construct a new script writer.
     * @param directory File path.
     * @param scriptName Name of script.
     * @param author Author name.
     * @param scriptType Type of script.
     * @param bindings Script bindings.
     */
    public ScriptWriter(String directory, String scriptName, String author, ScriptType scriptType, Bindings bindings) {
        this(directory, scriptName, author, scriptType, bindings, null);
    }

    /**
     * Construct a new script writer.
     * @param directory File path.
     * @param scriptName Name of script.
     * @param author Author name.
     * @param scriptType Type of script.
     */
    public ScriptWriter(String directory, String scriptName, String author, ScriptType scriptType) {
        this(directory, scriptName, author, scriptType, null, null);
    }

    /**
     * Construct a new script writer.
     */
    public ScriptWriter(String directory, String scriptName, String author, Bindings bindings) {
        this(directory, scriptName, author, ScriptType.None, bindings, null);
    }

    /**
     * Construct a new script writer.
     */
    public ScriptWriter(String directory, String scriptName, String author) {
        this(directory, scriptName, author, ScriptType.None, null, null);
    }

    /**
     * Construct a new script writer.
     */
    public ScriptWriter(String directory, String scriptName) {
        this(directory, scriptName, "CONSOLE", ScriptType.None, null, null);
    }

    /**
     * Construct a new script writer.
     */
    public ScriptWriter() {
        this("", "", "CONSOLE", ScriptType.None, null);
    }

    /**
     * Construct a new script writer.
     * @param directory File path.
     * @param scriptName Name of script.
     * @param author Author name.
     * @param scriptType Type of script.
     * @param bindings Script bindings.
     * @param block Block to interact with.
     */
    public ScriptWriter(String directory, String scriptName, String author, ScriptType scriptType, Bindings bindings,
        Block block) {

        this.directory = directory;
        this.scriptName = scriptName;
        this.author = author;
        this.scriptType = scriptType;
        this.bindings = bindings;
        this.block = block;
    }

    /**
     * Creates a new script file and writes initial file code as well as
     * provide a comment block with script details.
     */
    public boolean writeFile() {
        if (directory.isEmpty() || scriptName.isEmpty()) {
            // NO DIRECTORY OR NAME SET, FILE CAN'T BE WRITTEN
            return false;
        }

        StringBuilder content = new StringBuilder();
        String temp = "";
        String prettyAreaString = null;

        // COMMENT HEADER
        content.append("/**\n");
        if (scriptType.equals(ScriptType.AreaWalk)) {
            prettyAreaString = AppUtils.prettyAreaString(scriptName);
            temp = String.format(" for AREA %s", prettyAreaString);
        } else {
            temp = (block != null)
                ? String.format(" for BLOCK (%d, %d, %d)", block.getX(), block.getY(), block.getZ())
                : "";
        }

        content.append(String.format(" * %s type script", scriptType.toString().toUpperCase()) + temp + "\n");
        content.append(" * @todo ADD A DESCRIPTION OF WHAT THE SCRIPT DOES HERE!\n *\n");

        // SCRIPT DETAILS
        content.append(String.format(" * %-12s %s\n", "@author", author));
        content.append(String.format(" * %-12s %s\n", "@version", "0.1"));
        content.append(String.format(" * %-12s %s\n", "@type", scriptType.toString().toUpperCase()));
        if (scriptType.equals(ScriptType.AreaWalk)) {
            content.append(String.format(" * %-12s %s\n", "@block", prettyAreaString));
        } else if (block != null) {
            content.append(String.format(" * %-12s %d, %d, %d\n", "@block", block.getX(), block.getY(), block.getZ()));
        }

        // SCRIPT BINDINGS
        if (bindings == null) {
            if (directory.indexOf(AppUtils.getWorldDirectory().toString()) >= 0) {
                // USE WORLD SCRIPT BINDINGS
                bindings = new SimpleBindings(Map.of(
                    "player", Player.class,
                    "sm", ScriptManager.class,
                    "variables", WorldVariables.class,
                    "world", World.class
                ));
            } else {
                // USE DEFAULT BINDINGS
                bindings = scriptType.getBindings();
            }
        }
        if (bindings.size() > 0) {
            content.append(" *\n");
        }
        bindings.forEach((key, obj) -> {
            // CLASS NAME LOGIC IN CASE OF DEFAULT BINDINGS
            String className;
            if (obj.getClass().getSimpleName().equals("Class")) {
                String typeName = obj.toString();
                className = typeName.substring(typeName.lastIndexOf(".") + 1);
            } else {
                className = obj.getClass().getSimpleName();
            }

            content.append(String.format(" * @param %-17s %-10s %s\n",
                String.format("{%s}", className),
                key,
                "SCRIPT BINDING"));
        });

        // FINISH UP WRITING FILE (CLOSE COMMENT AND SET UP INITIAL MAIN FUNCTION FOR USER)
        if (scriptType.equals(ScriptType.AreaWalk)) {
            // HELLO WORLD FROM AREA
            temp = String.format("Hello World%s!", String.format(", from area %s", prettyAreaString));
        } else {
            temp = String.format("Hello World%s!", block != null
                ? String.format(", from block %s", AppUtils.prettyBlockString(block))
                : "");
        }

        // APPEND main()
        if (scriptType.equals(ScriptType.AreaWalk)) {
            // AreaWalk NEEDS TO WORRY ABOUT main() CONTENTS SINCE THEY'RE INVOKED EVERY STEP A USER TAKES IN THE AREA
            content.append(String.format("%s%s%s\n%s}\nmain();",
                " */\nfunction main() {\n    // WRITE YOUR CODE HERE ; THIS WILL BE CALLED ",
                "ONLY WHEN A PLAYER ENTERS OR LEAVES THE AREA.\n    ",
                String.format("// sm.log(\"INFO\", \"%s\");", temp),
                bindings.containsKey("player") ? String.format("    // player.sendMessage(\"%s\");\n", temp) : ""));
        } else {
            // NORMAL main()
            content.append(String.format("%ssm.log(\"INFO\", \"%s\");\n%s}\nmain();",
                " */\nfunction main() {\n    // WRITE YOUR CODE HERE\n    ",
                temp,
                bindings.containsKey("player") ? String.format("    player.sendMessage(\"%s\");\n", temp) : ""));
        }

        // APPEND SPECIAL ScriptType FUNCTIONS
        if (scriptType.equals(ScriptType.AreaWalk)) {
            // APPEND SPECIAL AreaWalk FUNCTIONS: _enter AND _exit
            content.append(String.format("%s%s%s%s\");\n}\n\n",
                "\n\n/**\n * AreaWalk SPECIAL FUNCTION: CALLED WHEN A PLAYER ENTERS THE AREA.\n",
                " * DO NOT REMOVE THIS FUNCTION.\n",
                " */\nfunction _enter() {\n\tplayer.sendMessage(\"Entered Area ", prettyAreaString));
            content.append(String.format("%s%s%s%s\");\n}",
                "/**\n * AreaWalk SPECIAL FUNCTION: CALLED WHEN A PLAYER LEAVES THE AREA.\n",
                " * DO NOT REMOVE THIS FUNCTION.\n",
                " */\nfunction _exit() {\n\tplayer.sendMessage(\"Exited Area ", prettyAreaString));
        }

        // WRITE FILE
        try {
            String location = String.format("%s/%s%s", directory, scriptName, AppConstants.SCRIPT_ENGINE_EXTENSION);

            File newScriptFile = new File(location);
            new File(directory).mkdirs();

            if (!newScriptFile.exists() && newScriptFile.createNewFile()) {
                // file created
                FileWriter writer = new FileWriter(location);
                writer.write(content.toString());
                writer.close();
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
