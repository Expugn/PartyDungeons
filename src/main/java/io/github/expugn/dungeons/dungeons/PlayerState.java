package io.github.expugn.dungeons.dungeons;

/**
 * Enum of different states a player can be in during a dungeon run.
 * @author S'pugn
 * @version 0.1
 */
public enum PlayerState {
    /**
     * Player is alive and active in the dungeon.
     */
    Alive("ALIVE"),

    /**
     * Player has died naturally or is back online after a disconnect.
     * They should be able to "Rebirth" if desired or implemented.
     */
    Dead("DEAD"),

    /**
     * Player has disconnected and has not logged back in.
     * When the player returns back online, their state should be "Dead".
     */
    Offline("OFFLINE"),

    /**
     * Player has left the dungeon through warp commands or `/partydungeons leave`.
     * Player should not be able to "rebirth" or rejoin the dungeon session they quit.
     */
    Quitter("QUITTER"),

    /**
     * Player is not a part of a dungeon.
     * This is more of a fallback PlayerState.
     */
    None("NONE");

    private String state;

    PlayerState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
