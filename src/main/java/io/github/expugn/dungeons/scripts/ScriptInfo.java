package io.github.expugn.dungeons.scripts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.script.Bindings;
import org.bukkit.entity.Player;

/**
 * Manages script information and reads files.
 * @author S'pugn
 * @version 0.1
 */
public class ScriptInfo {
    private ScriptType scriptType;
    private Bindings bindings;
    private String scriptName;
    private String directory;
    private Player player;

    public ScriptInfo(String scriptName, ScriptType scriptType) {
        this(scriptName, scriptType, null);
    }

    /**
     * Construct a script info object.
     * @param scriptName Name of script.
     * @param scriptType Type of script.
     * @param bindings Bindings script should use.
     */
    public ScriptInfo(String scriptName, ScriptType scriptType, Bindings bindings) {
        this.scriptType = scriptType;
        this.bindings = bindings;
        this.scriptName = scriptName;
        this.directory = "";
    }

    public ScriptType getScriptType() {
        return scriptType;
    }

    public void setScriptType(ScriptType scriptType) {
        this.scriptType = scriptType;
    }

    public Bindings getBindings() {
        return bindings;
    }

    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isScriptExists() {
        return new File(directory).exists();
    }

    /**
     * Read the script from a saved file.
     * Throws IOException if fails.
     * @return String of script contents.
     * @throws IOException
     */
    public String readScript() throws IOException {
        if (isScriptExists()) {
            return readFile(directory, Charset.defaultCharset());
        }
        return "";
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
