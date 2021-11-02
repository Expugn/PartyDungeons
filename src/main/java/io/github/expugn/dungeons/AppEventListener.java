package io.github.expugn.dungeons;

import io.github.expugn.dungeons.dungeons.DungeonFile;
import io.github.expugn.dungeons.dungeons.DungeonScript;
import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.dungeons.PlayerState;
import io.github.expugn.dungeons.dungeons.ResetHandler;
import io.github.expugn.dungeons.scripts.ScriptType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * This class handles all EventListening for the PartyDungeons App.
 * @author S'pugn
 * @version 0.2
 */
public class AppEventListener implements Listener {
    /**
     * Triggers whenever a player left or right clicks a block.
     * Used to trigger Interact type scripts.
     * @param e PlayerInteractEvent
     */
    @EventHandler
    public void onClickBlock(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            // IGNORE OFF HAND, EVENT WILL FIRE TWICE OTHERWISE
            return;
        }

        if (e.getAction() != Action.LEFT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            // NOT A LEFT CLICK OR RIGHT CLICK INTERACT EVENT
            return;
        }

        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();
        String scriptName = AppUtils.getBlockString(clickedBlock);

        if (!AppUtils.isPlayerInDungeon(player)) {
            // PLAYER IS NOT ACTIVELY IN A DUNGEON
            // CHECK FOR WORLD INTERACT SCRIPTS
            if (!AppUtils.getWorldScriptDirectory(player.getWorld(), ScriptType.None).exists()) {
                // WORLD SCRIPT DIRECTORY PROBABLY DOESN'T EXIST (INTERACT SCRIPTS WON'T EXIST), SO STOP HERE
                return;
            }

            if (!AppUtils.getWorldScript(player.getWorld(), ScriptType.Interact, scriptName).exists()) {
                // WORLD SCRIPT DOESN'T EXIST
                return;
            }

            // SCRIPT EXISTS, RUN
            e.setCancelled(AppStatus.getScriptManager().startScript(scriptName, ScriptType.Interact, player));
            return;
        }

        if (!AppUtils.getPlayerState(player).equals(PlayerState.Alive)) {
            // PLAYER IS NOT ALIVE IN THEIR DUNGEON PARTY
            return;
        }

        // RUN DUNGEON SCRIPT
        // CANCEL EVENT IF A SCRIPT HAS SUCCESSFULLY TRIGGERED
        // THIS WILL STOP BLOCKS FROM BEING PLACED
        e.setCancelled(AppStatus.getScriptManager().startScript(scriptName, ScriptType.Interact, player));
    }

    /**
     * Triggers whenever a player dies.
     * Used to mark an active player in a dungeon as Dead if necessary.
     * @param e PlayerDeathEvent
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (!AppUtils.isPlayerInDungeon(player)) {
            // PLAYER IS NOT ACTIVELY IN A DUNGEON
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        String dungeonName = AppStatus.getActivePlayers().get(uuid);

        if (!activeDungeons.containsKey(dungeonName)) {
            // PLAYER IS A PART OF A DUNGEON THAT ISN'T LOADED FOR SOME REASON
            activePlayers.remove(uuid);
            return;
        }

        // PLAYER DIED AND IS ACTIVELY PART OF A LOADED DUNGEON
        // THIS MEANS THE PLAYER IS EITHER Alive OR Dead (Quitters AREN'T ACTIVE)

        LoadedDungeon dungeon = activeDungeons.get(dungeonName);
        if (!dungeon.isActive()) {
            // DUNGEON ISN'T ACTIVE AND PLAYER DIED FOR SOME REASON
            // DO NOTHING
            return;
        }

        PlayerState playerState = AppUtils.getPlayerState(player);
        if (playerState.equals(PlayerState.Dead)) {
            // PLAYER IS MARKED AS DEAD AND DIED AGAIN, DO NOTHING
            return;
        }

        // PLAYER IS ALIVE WHILE THE DUNGEON IS ACTIVE AND DIED
        // MARK THEM AS DEAD
        AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);
        dungeon.modifyPlayerState(player, PlayerState.Dead);

        // CHECK IF WE SHOULD RESET THE DUNGEON
        dungeon.resetCheck();
    }

    /**
     * Triggers whenever a player moves from one block to another.
     * Used to fire Walk or AreaWalk type scripts and to check if a player walks out of dungeon bounds.
     * @param e PlayerMoveEvent
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (e.getTo().getBlockX() == e.getFrom().getBlockX()
            && e.getTo().getBlockY() == e.getFrom().getBlockY()
            && e.getTo().getBlockZ() == e.getFrom().getBlockZ()) {
            // NO PLAYER MOVEMENT
            return;
        }
        if (!AppUtils.isPlayerInDungeon(player)) {
            // PLAYER IS NOT ACTIVELY IN A DUNGEON ; POTENTIAL NON-DUNGEON SCRIPT EXISTS?
            // LOOK FOR SCRIPT IN PartyDungeons/world/<world_name>/<script_type>/<script_name>.js
            if (!AppUtils.getWorldScriptDirectory(player.getWorld(), ScriptType.None).exists()) {
                // WORLD SCRIPT DIRECTORY PROBABLY DOESN'T EXIST (WALK/AREAWALK SCRIPTS WON'T EXIST), SO STOP HERE
                return;
            }
            AppStatus.getExecutorService().execute(() -> {
                // CHECK WORLD WALK SCRIPT
                String worldWalkScriptName = AppUtils.getWalkScriptName(e.getTo().getBlock());
                File worldWalkScript = AppUtils.getWorldScript(player.getWorld(), ScriptType.Walk, worldWalkScriptName);
                if (worldWalkScript.exists()) {
                    // WORLD WALK SCRIPT EXISTS, RUN IT
                    AppStatus.getScriptManager().startScript(worldWalkScriptName, ScriptType.Walk, e.getPlayer());
                }

                // CHECK WORLD AREA SCRIPT
                File worldAreaWalkScripts = AppUtils.getWorldScriptDirectory(player.getWorld(), ScriptType.AreaWalk);
                if (!worldAreaWalkScripts.exists()) {
                    return;
                }
                try (Stream<Path> paths = Files.walk(worldAreaWalkScripts.toPath())) {
                    paths.filter(Files::isRegularFile)
                        .forEach(filePath -> {
                            String fileName = filePath.getFileName().toString();
                            String areaString = fileName.substring(0, fileName.lastIndexOf("."));
                            String[][] splitAreaString = AppUtils.splitAreaString(areaString);
                            boolean playerPreviouslyInArea = AppUtils.isInsideArea(splitAreaString[0],
                                splitAreaString[1], e.getFrom());
                            boolean playerCurrentlyInArea = AppUtils.isInsideArea(splitAreaString[0],
                                splitAreaString[1], e.getTo());
                            if ((!playerPreviouslyInArea && playerCurrentlyInArea) || (playerPreviouslyInArea
                                && !playerCurrentlyInArea)) {

                                // PLAYER ENTERED OR EXITED AREA
                                AppStatus.getScriptManager().startScript(areaString, ScriptType.AreaWalk, e.getPlayer(),
                                    (!playerPreviouslyInArea && playerCurrentlyInArea) ? "_enter" : "_exit");
                            }
                        });
                } catch (IOException io) {
                    io.printStackTrace();
                }
            });
            return;
        }
        if (!AppUtils.getPlayerState(player).equals(PlayerState.Alive)) {
            // PLAYER IS NOT ALIVE IN THEIR DUNGEON PARTY
            return;
        }
        AppStatus.getExecutorService().execute(() -> {
            Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
            Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
            String dungeonName = AppUtils.getPlayerDungeon(player);
            UUID uuid = player.getUniqueId();
            if (!activeDungeons.containsKey(dungeonName)) {
                // PLAYER IS A PART OF AN UNLOADED DUNGEON FOR SOME REASON
                activePlayers.remove(uuid);
                return;
            }
            LoadedDungeon dungeon = activeDungeons.get(dungeonName);
            if (!AppUtils.isInsideDungeonArea(dungeon, e.getTo())) {
                // PLAYER MOVED OUTSIDE THE DUNGEON THEY'RE A PART OF
                if (dungeon.isCleared()) {
                    // DUNGEON IS CLEARED AND PLAYER MOVED OUT OF BOUNDS, JUST REMOVE THEM FROM ACTIVE PLAYERS,
                    // NO PENALTY NEEDED. INFORM ALL CURRENT PARTY MEMBERS TOO.
                    dungeon.messageParty(
                        String.format("%s%s %shas walked out of dungeon boundaries and left the party.",
                        ChatColor.GOLD, player.getName(), ChatColor.YELLOW));
                    AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);
                    activeDungeons.get(dungeonName).removePlayerFromParty(player);
                    dungeon.resetCheck();
                    return;
                }
                if (!dungeon.isActive()) {
                    // DUNGEON ISN'T ACTIVE AND PLAYER MOVED OUT OF BOUNDS, JUST REMOVE THEM FROM ACTIVE PLAYERS
                    // NO PENALTY NEEDED. INFORM ALL CURRENT PARTY MEMBERS TOO.
                    dungeon.messageParty(
                        String.format("%s%s %shas walked out of dungeon boundaries and left the party.",
                        ChatColor.GOLD, player.getName(), ChatColor.YELLOW));
                    activeDungeons.get(dungeonName).removePlayerFromParty(player);
                    return;
                }
                // PLAYER IS ALIVE IN A ACTIVE DUNGEON PARTY THAT HASN'T BEEN CLEARED YET
                // EFFECTIVELY THE SAME AS LEAVING
                dungeon.messageParty(
                    String.format("%s%s %shas walked out of dungeon boundaries and abandoned the party.",
                    ChatColor.GOLD, player.getName(), ChatColor.RED));

                // RUN DUNGEON SPECIFIC RESET SCRIPTS HERE (RESET HEALTH CHANGES, POTION EFFECTS, ETC)
                AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);

                // MODIFY PLAYER TYPE IN DUNGEON PARTY TO BE "QUITTER"
                dungeon.modifyPlayerState(player, PlayerState.Quitter);

                // REMOVE THEM FROM activePlayers
                AppStatus.getActivePlayers().remove(player.getUniqueId());

                // CHECK IF WE SHOULD RESET THE DUNGEON
                dungeon.resetCheck();
                return;
            }

            // PLAYER SHOULD BE ABLE TO RUN SCRIPTS ; TRIGGER WALK SCRIPT
            // DOING AN ADDITIONAL FILE CHECK BECAUSE IT HAD FALSE POSITIVE TRIGGERS FOR SOME REASON
            String walkScriptName = AppUtils.getWalkScriptName(e.getTo().getBlock());
            if (new File(String.format("%s/%s%s", AppUtils.getDungeonScriptDirectory(dungeonName, ScriptType.Walk),
                walkScriptName, AppConstants.SCRIPT_ENGINE_EXTENSION)).exists()) {
                AppStatus.getScriptManager().startScript(walkScriptName, ScriptType.Walk, e.getPlayer());
            }

            // TRIGGER AREAWALK SCRIPT
            File areaWalkScripts = AppUtils.getDungeonScriptDirectory(dungeonName, ScriptType.AreaWalk);
            if (!areaWalkScripts.exists()) {
                return;
            }
            try (Stream<Path> paths = Files.walk(areaWalkScripts.toPath())) {
                paths.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        String areaString = fileName.substring(0, fileName.lastIndexOf("."));
                        String[][] splitAreaString = AppUtils.splitAreaString(areaString);
                        boolean playerPreviouslyInArea = AppUtils.isInsideArea(splitAreaString[0], splitAreaString[1],
                            e.getFrom());
                        boolean playerCurrentlyInArea = AppUtils.isInsideArea(splitAreaString[0], splitAreaString[1],
                            e.getTo());
                        if ((!playerPreviouslyInArea && playerCurrentlyInArea) || (playerPreviouslyInArea
                            && !playerCurrentlyInArea)) {

                            // PLAYER ENTERED OR EXITED AREA
                            AppStatus.getScriptManager().startScript(areaString, ScriptType.AreaWalk, e.getPlayer(),
                                (!playerPreviouslyInArea && playerCurrentlyInArea) ? "_enter" : "_exit");
                        }
                    });
            } catch (IOException io) {
                io.printStackTrace();
            }
        });
    }

    /**
     * Triggers whenever a player joins the server.
     * Used to reset previously active offline players if necessary.
     * @param e PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        ResetHandler resetHandler = AppStatus.getResetHandler();
        Player player = e.getPlayer();
        if (!resetHandler.containsPlayer(player)) {
            // PLAYER WAS NOT AN OFFLINE PLAYER IN A DUNGEON
            return;
        }

        // RUN DUNGEON SPECIFIC RESET SCRIPTS HERE (RESET HEALTH CHANGES, POTION EFFECTS, ETC)
        AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, e.getPlayer());

        // PREVIOUSLY ACTIVE PLAYER HAS LOGGED IN AGAIN
        // TELEPORT PLAYER TO DUNGEON SPAWN LOCATION
        String dungeonName = resetHandler.removePlayer(player);
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        if (!activeDungeons.containsKey(dungeonName)) {
            // THE DUNGEON THE PLAYER WAS ASSIGNED TO IS NOT CURRENTLY ACTIVE
            // DON'T BOTHER TELEPORTING THEM AND JUST REMOVE THEM FROM THE OFFLINE PLAYER LIST
            return;
        }

        // TELEPORT PLAYER AND GIVE THEM A REASON WHY
        AppStatus.getPlugin().getLogger().info(
            String.format("Teleporting player %s to %s's spawn location because %s",
            player.getName(), dungeonName, "they have disconnected while participating."));
        player.teleport(activeDungeons.get(dungeonName).getDungeon().getDungeonFile().getSpawnLocation());
        player.sendMessage(
            String.format("%sYou have disconnected from the server while participating in %s%s%s. %s",
            ChatColor.RED, ChatColor.GOLD, dungeonName, ChatColor.RED,
            "You have returned to the spawn location as a result."));

        // CHANGE PLAYER STATE IF THEY ARE IN THE CURRENT PARTY (IF THEY AREN'T IN THE PARTY THEN NOTHING HAPPENS)
        activeDungeons.get(dungeonName).modifyPlayerState(player, PlayerState.Dead);
    }

    /**
     * Triggers whenever a player quits or leaves the server.
     * Used to check if the player was a part of a dungeon to add them to {@link ResetHandler} if needed.
     * @param e PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        ResetHandler resetHandler = AppStatus.getResetHandler();
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        Map<UUID, String> activePlayers = AppStatus.getActivePlayers();
        if (!activePlayers.containsKey(uuid)) {
            // PLAYER WAS NOT AN ACTIVE PLAYER
            return;
        }

        String dungeonName = activePlayers.get(uuid);
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        if (!activeDungeons.containsKey(dungeonName)) {
            // DUNGEON IS UNLOADED FOR SOME REASON, FORGET ABOUT IT
            // REMOVE PLAYER FROM ACTIVE PLAYERS TOO
            activeDungeons.get(dungeonName).removePlayerFromParty(player);
            return;
        }

        LoadedDungeon dungeon = activeDungeons.get(dungeonName);
        if (!dungeon.isActive() || dungeon.isCleared()) {
            // DUNGEON ISN'T ACTIVE OR CLEARED AND PLAYER DISCONNECTED, JUST REMOVE THEM FROM ACTIVE PLAYERS
            // NO PENALTY NEEDED. INFORM ALL CURRENT PARTY MEMBERS TOO
            activeDungeons.get(dungeonName).removePlayerFromParty(player);
            dungeon.messageParty(String.format("%s%s %shas disconnected and left the party.",
                ChatColor.GOLD, player.getName(), ChatColor.YELLOW));

            // CHECK IF WE SHOULD RESET THE DUNGEON IF THE DUNGEON HAS BEEN USED
            // DUNGEON IS CLEARED AND PLAYER CAN STILL QUIT?
            // ADD THEM TO RESET HANDLER
            if (dungeon.isCleared()) {
                resetHandler.addPlayer(player, dungeonName);
                dungeon.resetCheck();
            }
            return;
        }

        // DUNGEON IS ACTIVE AND LOADED
        // ADD PLAYER TO OFFLINE PLAYER HANDLER
        // DO NOT REMOVE THEM FROM activePlayers YET, IN CASE THEY RELOG SO THEY WILL BE ABLE TO REJOIN THE DUNGEON.
        resetHandler.addPlayer(player, dungeonName);
        AppStatus.getPlugin().getLogger().info(
            String.format("Player %s disconnected while %s was in progress. They have been marked as an OfflinePlayer.",
            player.getName(), dungeonName));
        dungeon.messageParty(
            String.format("%s%s %shas disconnected. They will be marked as %s%s %swhen they come back online.",
            ChatColor.GOLD, player.getName(), ChatColor.YELLOW, ChatColor.RED, PlayerState.Dead, ChatColor.YELLOW));

        // MODIFY PLAYER TYPE IN DUNGEON PARTY TO BE "OFFLINE"
        dungeon.modifyPlayerState(player, PlayerState.Offline);

        // CHECK IF WE SHOULD RESET THE DUNGEON
        dungeon.resetCheck();
    }

    /**
     * Triggers whenever an entity dies.
     * Used to check if an entity died in an active dungeon area to fire the onEntityDeath dungeon script.
     * @param e EntityDeathEvent.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player) {
            // ENTITY THAT DIED IS A PLAYER (ALREADY HANDLED AT onPlayerDeath())
            return;
        }

        // FIND WHICH DUNGEON THE ENTITY DIED AT
        LivingEntity livingEntity = e.getEntity();
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        for (Map.Entry<String, LoadedDungeon> entry : activeDungeons.entrySet()) {
            if (!entry.getValue().isActive()) {
                // DUNGEON ISN'T ACTIVE, IGNORE
                continue;
            }

            LoadedDungeon dungeon = entry.getValue();
            DungeonFile dungeonFile = dungeon.getDungeon().getDungeonFile();
            if (AppUtils.isInsideArea(dungeonFile.getBlockA(), dungeonFile.getBlockB(), livingEntity.getLocation())) {
                // AREA FOUND
                Bindings bindings = new SimpleBindings();
                bindings.put("entity", livingEntity);
                AppStatus.getScriptManager().startScript(DungeonScript.ON_ENTITY_DEATH, ScriptType.Dungeon, dungeon,
                    bindings);
                return;
            }
        }
    }

    /**
     * Triggers whenever the player teleports (with command, ender pearl, etc).
     * Used to check if a player teleports and leaves a dungeon area.
     * @param e PlayerTeleportEvent.
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        if (!AppUtils.isPlayerInDungeon(player)) {
            // PLAYER IS NOT ACTIVELY IN A DUNGEON
            return;
        }
        if (!AppUtils.getPlayerState(player).equals(PlayerState.Alive)) {
            // PLAYER IS NOT ALIVE IN THEIR DUNGEON PARTY
            return;
        }

        // PLAYER IS ALIVE, IN A DUNGEON PARTY
        UUID uuid = player.getUniqueId();
        Map<String, LoadedDungeon> activeDungeons = AppStatus.getActiveDungeons();
        String dungeonName = AppStatus.getActivePlayers().get(uuid);
        LoadedDungeon dungeon = activeDungeons.get(dungeonName);

        if (AppUtils.isInsideDungeonArea(dungeon, e.getTo())) {
            // CHECKING IF USER IS INSIDE DUNGEON AREA STILL
            return;
        }
        if (e.getTo().getWorld().equals(e.getFrom().getWorld())) {
            // PLAYER IS STILL IN THE SAME WORLD
            return;
        }

        if (!dungeon.isActive() || dungeon.isCleared()) {
            // DUNGEON ISN'T ACTIVE OR CLEARED AND PLAYER WORLD CHANGED, JUST REMOVE THEM FROM ACTIVE PLAYERS
            // NO PENALTY NEEDED. INFORM ALL CURRENT PARTY MEMBERS TOO
            dungeon.messageParty(String.format("%s%s %shas teleported away and left the party.",
                ChatColor.GOLD, player.getName(), ChatColor.YELLOW));

            if (dungeon.isCleared()) {
                // RESET PLAYER IF THEY TELEPORT AWAY
                AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);
                activeDungeons.get(dungeonName).removePlayerFromParty(player);
                dungeon.resetCheck();
                return;
            }

            activeDungeons.get(dungeonName).removePlayerFromParty(player);
            return;
        }

        // PLAYER IS ALIVE IN A ACTIVE DUNGEON PARTY THAT HASN'T BEEN CLEARED YET
        // EFFECTIVELY THE SAME AS LEAVING
        dungeon.messageParty(String.format("%s%s %shas teleported away and abandoned the party.",
            ChatColor.GOLD, player.getName(), ChatColor.RED));

        // MODIFY PLAYER TYPE IN DUNGEON PARTY TO BE "QUITTER"
        dungeon.modifyPlayerState(player, PlayerState.Quitter);

        // REMOVE THEM FROM activePlayers
        AppStatus.getActivePlayers().remove(uuid);

        // RUN DUNGEON SPECIFIC RESET SCRIPTS HERE (RESET HEALTH CHANGES, POTION EFFECTS, ETC)
        AppStatus.getScriptManager().startScript(DungeonScript.ON_PLAYER_RESET, ScriptType.Dungeon, player);

        // CHECK IF WE SHOULD RESET THE DUNGEON
        dungeon.resetCheck();
    }
}
