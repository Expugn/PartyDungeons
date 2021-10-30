package io.github.expugn.dungeons.scripts;

import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.bukkit.entity.Player;

/**
 * Enum of script types.
 * Also includes the bindings they're expected to have.
 * @author S'pugn
 * @version 0.1
 */
public enum ScriptType {
    /**
     * None type scripts aren't triggered by anything but a dungeon scripter can use them to hold
     * code that can be reused throughout their scripts.
     */
    None("") {
        @Override
        public Bindings getBindings() {
            return new SimpleBindings(Map.of());
        }
    },

    /**
     * Interact scripts are scripts that are triggered when a player left or right clicks them.
     */
    Interact("interact") {
        @Override
        public Bindings getBindings() {
            return new SimpleBindings(Map.of(
                "sm", ScriptManager.class,
                "player", Player.class
            ));
        }
    },

    /**
     * Walk scripts are scripts that are triggered when a player walks over them.
     * These are only for single blocks, use AreaWalk for a bulk range.
     */
    Walk("walk") {
        @Override
        public Bindings getBindings() {
            return new SimpleBindings(Map.of(
                "sm", ScriptManager.class,
                "player", Player.class
            ));
        }
    },

    /**
     * AreaWalk scripts are scripts that are triggered when a player walks into the area.
     * It can be triggered on enter, exit, or every step the player takes.
     */
    AreaWalk("area_walk") {
        @Override
        public Bindings getBindings() {
            return new SimpleBindings(Map.of(
                "sm", ScriptManager.class,
                "player", Player.class
            ));
        }
    },

    /**
     * Dungeon scripts are scripts that are usually triggered by the dungeon.
     */
    Dungeon("dungeon") {
        @Override
        public Bindings getBindings() {
            return new SimpleBindings(Map.of(
                "sm", ScriptManager.class,
                "dungeon", LoadedDungeon.class
            ));
        }
    };

    private String directory;

    ScriptType(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    /**
     * Get a basic list of what bindings are assumed to be available in a ScriptType.
     * There can be potentially be more bindings that exist.
     * @return {@link javax.script.Bindings} object
     */
    public abstract Bindings getBindings();
}
