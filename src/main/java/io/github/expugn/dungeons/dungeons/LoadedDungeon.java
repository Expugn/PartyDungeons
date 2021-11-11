package io.github.expugn.dungeons.dungeons;

import io.github.expugn.dungeons.AppStatus;
import io.github.expugn.dungeons.AppUtils;
import io.github.expugn.dungeons.scripts.ScriptType;
import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages a dungeon's party, temporary variables, and state.
 * A loaded dungeon is a {@link Dungeon} that is currently "active" on the server. Players can join these loaded
 * dungeons with the `/partymembers join` command when they are in the area.
 * @author S'pugn
 * @version 0.2
 */
public class LoadedDungeon {
    private Dungeon dungeon;
    private boolean active;
    private boolean clear;
    private Map<UUID, PlayerState> party;
    private Map<String, Object> tempVariables;

    /**
     * Construct a new LoadedDungeon instance.
     * @param dungeon Dungeon to turn into a LoadedDungeon.
     */
    public LoadedDungeon(Dungeon dungeon) {
        this.dungeon = dungeon;
        this.active = false;
        this.clear = false;
        this.party = new HashMap<>();
        this.tempVariables = new HashMap<>();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCleared() {
        return clear;
    }

    public Dungeon getDungeon() {
        return dungeon;
    }

    /**
     * Force a dungeon to start. A dungeon with an empty party or one that is already active can not start.
     * - Dungeon active state is set to true.
     * - All (alive) party members will be teleported to the start location.
     * - Party is saved in dungeon file in case of server shutdown.
     * @return true if the dungeon was successfully started, false otherwise.
     */
    public boolean start() {
        return start(true);
    }

    /**
     * Force a dungeon to start. A dungeon with an empty party or one that is already active can not start.
     * - Dungeon active state is set to true.
     * - All (alive) party members will be teleported to the start location.
     * - Party is saved in dungeon file in case of server shutdown.
     * @param teleportPlayers Whether to teleport all party members to the start location.
     * @return Whether the dungeon was successfully started.
     */
    public boolean start(boolean teleportPlayers) {
        if (party.size() <= 0) {
            // PARTY IS EMPTY
            return false;
        }
        if (active) {
            // DUNGEON IS ALREADY ACTIVE
            return false;
        }

        // START DUNGEON AND TELEPORT PARTY MEMBERS TO START
        active = true;
        if (teleportPlayers) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Map.Entry<UUID, PlayerState> entry : party.entrySet()) {
                        // TELEPORT ONLY ALIVE PLAYERS
                        if (entry.getValue().equals(PlayerState.Alive)) {
                            AppUtils.uuidToPlayer(entry.getKey()).teleport(dungeon.getDungeonFile().getStartLocation());
                        }
                    }
                }
            }.runTaskLater(AppStatus.getPlugin(), 0L);
        }

        // SAVE PARTY IN DUNGEON FILE
        dungeon.getDungeonFile().setParty(party);
        dungeon.getDungeonFile().saveJSON(dungeon.getName());

        return true;
    }

    /**
     * Force a dungeon to stop.
     * @return true if the stop was successful, else false.
     */
    public boolean stop() {
        if (!active) {
            // DUNGEON IS NOT CURRENTLY ACTIVE
            return false;
        }

        // STOP DUNGEON AND RESET ALL PLAYERS
        reset();
        return true;
    }

    /**
     * Force a dungeon to reset.
     * - Any players that are Alive will be teleported to the dungeon spawn point.
     * - Players who are teleported get the dungeon player reset script applied to.
     * - All party members get removed from active players so they can join any dungeon after.
     * - The dungeon party will be cleared.
     * - Dungeon temporary variables will be reset.
     * - onDungeonReset will be called.
     * - Dungeon active and clear state will be reset.
     */
    public void reset() {
        // RETURN ACTIVE PLAYERS TO SPAWN POINT
        Location spawn = dungeon.getDungeonFile().getSpawnLocation();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, PlayerState> entry : party.entrySet()) {
                    Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
                    // ONLY TELEPORT ALIVE PLAYERS
                    if (entry.getValue().equals(PlayerState.Alive)) {
                        Player player = AppUtils.uuidToPlayer(entry.getKey());
                        player.sendMessage(String.format("%s%s %sis being reset, %s",
                            ChatColor.GOLD, dungeon.getName(), ChatColor.YELLOW,
                            "you have returned to the spawn location."));

                        // TELEPORT PLAYER BACK TO SPAWN
                        player.teleport(spawn);

                        // RUN RESET SCRIPT ON PLAYER
                        AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon,
                            player);
                    }

                    if (activePlayers.get(entry.getKey()) == dungeon.getName()) {
                        // PLAYER IS IN activePlayers AND ALSO ACTIVE IN THIS DUNGEON (POSSIBLY OFFLINE?)
                        // REMOVE PLAYER FROM ACTIVE PLAYERS, THEIR RESET WILL BE HANDLED LATER
                        activePlayers.remove(entry.getKey());
                    }
                }

                // CLEAR PARTY
                party = new HashMap<>();

                // RESET TEMPORARY VARIABLES
                resetTempVariables();

                // MAKE DUNGEON INACTIVE
                active = false;

                // MARK clear AS FALSE
                clear = false;

                // CLEAR PARTY IN DUNGEON FILE
                dungeon.getDungeonFile().clearParty();
                dungeon.getDungeonFile().saveJSON(dungeon.getName());
            }
        }.runTaskLater(AppStatus.getPlugin(), 0L);

        // RUN onDungeonReset SCRIPT TO DEAL WITH ANY PREVIOUS CHANGES
        AppStatus.getScriptManager().startScript(DungeonScript.ON_DUNGEON_RESET, ScriptType.Dungeon, this);
    }

    /**
     * Mark a dungeon as "cleared".
     * Every alive party member will be marked as a cleared player.
     */
    public boolean clear() {
        // MARK DUNGEON AS CLEARED
        clear = true;

        // MARK CURRENT ALIVE PARTY MEMBERS AS CLEARED
        for (Map.Entry<UUID, PlayerState> entry : party.entrySet()) {
            if (entry.getValue().equals(PlayerState.Alive)) {
                Player partyMember = AppUtils.uuidToPlayer(entry.getKey());
                dungeon.getDungeonFile().addClearedPlayer(partyMember, System.currentTimeMillis());
            }
        }

        // SAVE DUNGEON FILE
        dungeon.saveDungeonFile();

        return clear;
    }

    public Map<UUID, PlayerState> getParty() {
        return party;
    }

    /**
     * Make a player join the dungeon party.
     * A player can not join for the following reasons:
     * - They are already in the party.
     * - They are a quitter.
     * - The dungeon is currently active and they weren't in the party before.
     * - They are in a different dungeon's party for some reason.
     * - The max dungeon party size has been reached.
     * - The player has hit the max daily clears for the dungeon.
     * @param player Player to put into the dungeon party.
     */
    public void join(Player player) {
        UUID uuid = player.getUniqueId();
        if (party.containsKey(uuid)) {
            // PLAYER IS ALREADY IN PARTY
            PlayerState playerState = getPlayerState(player);
            if (playerState.equals(PlayerState.Quitter)) {
                // QUITTERS CAN'T RETURN TO THE PARTY
                player.sendMessage(String.format("%sYou can not join a dungeon that you quit from.", ChatColor.RED));
                return;
            }

            if (playerState.equals(PlayerState.Alive)) {
                // PLAYER IS ALREADY PARTICIPATING IN THIS DUNGEON
                player.sendMessage(String.format("%sYou are already a part of this party.", ChatColor.YELLOW));
                return;
            }

            // PLAYER IS MOST LIKELY PlayerState.Dead ; HANDLE onPlayerRebirth HERE
            AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_REBIRTH, ScriptType.Dungeon, player);

            // IF onPlayerRebirth IS EMPTY OR UNDEFINED, THE PLAYER WILL NOT BE ABLE TO REBIRTH

            return;
        }

        if (active) {
            // DUNGEON IS CURRENTLY ACTIVE, NEW PARTY MEMBERS CAN NOT JOIN.
            player.sendMessage(String.format("%sYou can not join a dungeon that is currently active.", ChatColor.RED));
            return;
        }

        if (AppStatus.getActivePlayers().containsKey(uuid)) {
            // PLAYER IS ALREADY ACTIVE IN A DIFFERENT DUNGEON
            player.sendMessage(String.format("%sYou are already a part of a different dungeon party (%s%s%s).",
                ChatColor.RED, ChatColor.GOLD, AppStatus.getActivePlayers().get(uuid), ChatColor.RED));
            return;
        }

        if (dungeon.getDungeonFile().getMaxParty() != -1 && party.size() >= dungeon.getDungeonFile().getMaxParty()) {
            // MAX PARTY CHECK, IGNORE IF MAX PARTY IS SET TO -1
            player.sendMessage(String.format("%s%s%s's party is currently full.",
                ChatColor.GOLD, dungeon.getName(), ChatColor.RED));
            return;
        }

        if (!dungeon.getDungeonFile().canPlayerJoin(player)) {
            // PLAYER ACHIEVED TOO MANY CLEARS TODAY
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            Duration d = Duration.ofMillis(c.getTimeInMillis() - System.currentTimeMillis());

            player.sendMessage(
                String.format("%s%s%s%s%s%s%s%s %stime(s) a day.\nThis limit will reset in %s%s%s.",
                    ChatColor.RED, "You have achieved ", ChatColor.GOLD, dungeon.getName(), ChatColor.RED,
                    "'s daily clear limit. This dungeon can be cleared only ", ChatColor.GOLD,
                    dungeon.getDungeonFile().getDailyClear(), ChatColor.RED, ChatColor.GOLD,
                    d.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase(), ChatColor.RED));
            return;
        }
        // SAVE DUNGEON FILE IN CASE THE PLAYER'S DAILY CLEARS WERE RESET
        dungeon.saveDungeonFile();

        // DUNGEON IS INACTIVE AND PLAYER ISN'T IN THE PARTY
        // ADD PLAYER TO PARTY AND ALERT EVERYONE THAT THEY JOINED
        addPlayerToParty(player);
        messageParty(String.format("%s%s %shas joined the party!",
            ChatColor.GOLD, player.getName(), ChatColor.GREEN));

        // START SCRIPT TO HANDLE WHEN A DUNGEON SHOULD START
        AppStatus.getScriptManager().startScript(DungeonScript.ON_PARTY_MEMBER_JOIN, ScriptType.Dungeon, player);
    }

    /**
     * Make a player leave the dungeon party.
     * If the dungeon is active, they will be marked as a Quitter.
     * Once a player leaves the party the current party members will be alerted.
     */
    public void leave(Player player) {
        UUID uuid = player.getUniqueId();
        if (!party.containsKey(uuid)) {
            // PLAYER IS NOT A PARTY OF THIS PARTY
            return;
        }

        if (!active || clear) {
            // DUNGEON IS NOT CURRENTLY ACTIVE OR HAS BEEN CLEARED
            // REMOVE PLAYER FROM PARTY WITH NO CONSEQUENCES
            // AND ALERT PARTY
            messageParty(String.format("%s%s %shas left the party.",
                ChatColor.GOLD, player.getName(), ChatColor.YELLOW));

            if (clear) {
                // PLAYER IS PROBABLY STILL IN THE DUNGEON IF THEY'RE TRYING TO LEAVE BY COMMAND,
                // TELEPORT THEM OUT ALSO REMOVE ANY EFFECTS FROM THEM
                AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);
                player.teleport(dungeon.getDungeonFile().getSpawnLocation());
                removePlayerFromParty(player);
                resetCheck();
                return;
            }

            removePlayerFromParty(player);
            return;
        }

        PlayerState playerState = getPlayerState(player);
        if (playerState.equals(PlayerState.Alive)) {
            // PLAYER IS ALIVE AND WANTS TO LEAVE WHILE DUNGEON IS IN PROGRESS
            // MARK THEM AS A QUITTER, DO ANY RESETS NECESSARY, AND TELEPORT THEM TO SPAWN
            // ALSO REMOVE THEM FROM activePlayers SO THEY CAN JOIN OTHER DUNGEONS
            AppStatus.getScriptManager().startScript(DungeonScript.ON_PARTY_MEMBER_QUIT, ScriptType.Dungeon, player);
            AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);
            messageParty(String.format("%s%s %shas abandoned the party.",
                ChatColor.GOLD, player.getName(), ChatColor.RED));
            modifyPlayerState(player, PlayerState.Quitter);
            player.teleport(dungeon.getDungeonFile().getSpawnLocation());
            AppStatus.getActivePlayers().remove(uuid);

            // POSSIBLY RESET DUNGEON?
            resetCheck();
            return;
        }

        if (playerState.equals(PlayerState.Dead)) {
            // PLAYER IS DEAD AND WANTS TO LEAVE WHILE THE DUNGEON IS IN PROGRESS
            // MARK THEM AS A QUITTER, ALSO REMOVE THEM FROM activePlayers
            messageParty(String.format("%s%s %shas left the party.",
                ChatColor.GOLD, player.getName(), ChatColor.GRAY));
            modifyPlayerState(player, PlayerState.Quitter);
            AppStatus.getActivePlayers().remove(uuid);
            return;
        }

        if (playerState.equals(PlayerState.Quitter)) {
            // PLAYER IS ALREADY A QUITTER BUT WANTS TO LEAVE AGAIN WHILE THE DUNGEON IS IN PROGRESS
            player.sendMessage(String.format("%sYou can not leave something you have already left.",
                ChatColor.RED));
            return;
        }
    }

    /**
     * Check if a dungeon can reset (there are no active players in party).
     * If it can reset, it will.
     */
    public void resetCheck() {
        // CHECK IF ALL PLAYERS ARE DEAD/QUIT/OFFLINE
        for (Map.Entry<UUID, PlayerState> entry : party.entrySet()) {
            if (entry.getValue().equals(PlayerState.Alive)) {
                // THERE'S A PLAYER THAT'S ALIVE. WE CAN'T RESET THE DUNGEON.
                return;
            }
        }

        // I GUESS WE CAN RESET?
        reset();
    }

    public void addPlayerToParty(Player player) {
        AppStatus.getActivePlayers().put(player.getUniqueId(), dungeon.getName());
        party.put(player.getUniqueId(), PlayerState.Alive);
    }

    public void removePlayerFromParty(Player player) {
        AppStatus.getActivePlayers().remove(player.getUniqueId());
        party.remove(player.getUniqueId());
    }

    public void setTempVariable(String variableName, Object value) {
        tempVariables.put(variableName, value);
    }

    public Object getTempVariable(String variableName) {
        return tempVariables.get(variableName);
    }

    public Object removeTempVariable(String variableName) {
        return tempVariables.remove(variableName);
    }

    public void resetTempVariables() {
        tempVariables = new HashMap<>();
    }

    /**
     * Send a message to everyone in the dungeon party.
     * @param message Message to send party members.
     */
    public void messageParty(String message) {
        for (Map.Entry<UUID, PlayerState> entry : party.entrySet()) {
            // SEND MESSAGE ONLY TO ALIVE PLAYERS
            if (entry.getValue().equals(PlayerState.Alive)) {
                AppUtils.uuidToPlayer(entry.getKey()).sendMessage(message);
            }
        }
    }

    /**
     * Send a message to everyone in the dungeon bounding box.
     * @param message Message to send everyone in area.
     */
    public void messageArea(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (AppUtils.isInsideDungeonArea(this, player.getLocation())) {
                // PLAYER IS IN DUNGEON AREA LOCATION
                player.sendMessage(message);
            }
        }
    }

    /**
     * Modify a player's PlayerState and save it to the dungeon file.
     * @param player Player to modify the state of.
     * @param playerState PlayerState to modify the player to.
     */
    public void modifyPlayerState(Player player, PlayerState playerState) {
        UUID uuid = player.getUniqueId();
        if (!party.containsKey(uuid)) {
            // PLAYER IS NOT IN THIS DUNGEON PARTY
            return;
        }

        // SET A NEW PLAYER STATE
        party.put(uuid, playerState);

        // UPDATE PARTY IN DUNGEON FILE
        dungeon.getDungeonFile().setParty(party);
        dungeon.getDungeonFile().saveJSON(dungeon.getName());
    }

    /**
     * Get the current player state of a given Player.
     * @param player Player to get the state of.
     * @return PlayerState of a player in this dungeon.
     */
    public PlayerState getPlayerState(Player player) {
        UUID uuid = player.getUniqueId();
        if (!party.containsKey(uuid)) {
            // PLAYER IS NOT IN THIS DUNGEON PARTY
            return PlayerState.None;
        }
        return party.get(uuid);
    }

    /**
     * Show the default version of `/partydungeons status` if a dungeon scripter did not implement a
     * custom version.
     * @param player Player to send dungeon status to
     */
    public void showDefaultStatus(Player player) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%sCurrent status for %s%s%s:\n", ChatColor.YELLOW, ChatColor.GOLD,
            dungeon.getName(), ChatColor.YELLOW));
        stringBuilder.append(String.format("%sActive?: %s%s\n",
            ChatColor.YELLOW, active ? ChatColor.GREEN : ChatColor.RED, active));
        stringBuilder.append(String.format("%sCurrent Party (%d / %d):\n", ChatColor.YELLOW, party.size(),
            dungeon.getDungeonFile().getMaxParty()));

        for (Map.Entry<UUID, PlayerState> entry : party.entrySet()) {
            Player partyMember = AppUtils.uuidToPlayer(entry.getKey());
            stringBuilder.append(String.format("%s- %s%s %s(%s)\n",
                ChatColor.DARK_GRAY, ChatColor.GOLD, partyMember.getName(),
                entry.getValue().equals(PlayerState.Alive) ? ChatColor.GREEN : ChatColor.GRAY,
                entry.getValue().getState()));
        }
        player.sendMessage(stringBuilder.toString());
    }

    @Override
    public String toString() {
        return String.format("%s%s", active ? ChatColor.GREEN : ChatColor.RED, dungeon.getName());
    }
}
