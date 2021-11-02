package io.github.expugn.dungeons.worlds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.expugn.dungeons.AppUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;

/**
 * Manages the variables.json file for world directories.
 * @author S'pugn
 * @version 0.1
 */
public class WorldVariables {
    // having "transient" here will ignore the world variable when json is created.
    private transient World world;
    private Map<String, Object> variables;

    public WorldVariables(World world) {
        this.world = world;
        variables = new HashMap<>();
    }

    /**
     * Get a Map of persisting world variables.
     * @return Map of world variables.
     */
    public Map<String, Object> get() {
        return this.variables;
    }

    /**
     * Get a world variable value.
     * @param variableName Key the object was stored in.
     * @return Value of given Key.
     */
    public Object get(String variableName) {
        return this.variables.get(variableName);
    }

    /**
     * Set a world variable to an object.
     * @param variableName Map Key to save to.
     * @param value Object value to save.
     */
    public void set(String variableName, Object value) {
        this.variables.put(variableName, value);
        saveJSON();
    }

    /**
     * Remove a world variable.
     * @param variableName Map Key to remove.
     */
    public void remove(String variableName) {
        this.variables.remove(variableName);
        saveJSON();
    }

    /**
     * Save the world variable file, given a file location.
     */
    public void saveJSON() {
        File file = AppUtils.getWorldVariableFile(world);
        if (!file.exists()) {
            // CREATE FILE IF IT DOESN'T EXIST
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
