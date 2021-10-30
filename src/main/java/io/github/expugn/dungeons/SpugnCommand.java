package io.github.expugn.dungeons;

import io.github.expugn.dungeons.dungeons.LoadedDungeon;
import io.github.expugn.dungeons.dungeons.PlayerState;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Command to give debug/testing options when developing PartyDungeons.
 * @author S'pugn
 * @version 0.1
 */
public class SpugnCommand implements CommandExecutor {
    /**
     * Spugn command handler.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final String subCommand = (args.length > 0) ? args[0].toLowerCase() : "";
        final Player player = sender instanceof Player ? (Player) sender : null;

        if (player == null) {
            sender.sendMessage("THIS COMMAND CAN ONLY BE RUN BY A PLAYER.");
            return true;
        }

        if (args.length == 0) { // spugn
            player.sendMessage("hi. this is a debug print.");
            return true;
        }

        switch (subCommand) {
            case "direction":
                player.sendMessage(AppUtils.straightenYaw(player.getLocation().getYaw()) + "");
                Location l = player.getLocation();
                l.setYaw(AppUtils.straightenYaw(player.getLocation().getYaw()));
                player.teleport(l);
                break;
            case "dungeonarea":
                Map<String, LoadedDungeon> active = AppStatus.getActiveDungeons();
                for (Map.Entry<String, LoadedDungeon> entry : active.entrySet()) {
                    if (entry.getValue().getDungeon().isPlayerInDungeonBoundaries(player)) {
                        player.sendMessage(entry.getValue().getDungeon().getName());
                    }
                }
                break;
            case "modifystate":
                LoadedDungeon d = AppStatus.getActiveDungeons().get(AppUtils.getPlayerDungeon(player));
                if (d != null) {
                    d.modifyPlayerState(player, PlayerState.valueOf(args[1]));
                }
                break;
            case "dummy":
                int amount = Integer.parseInt(args[2]);
                for (int i = 0; i < amount; i++) {
                    AppStatus.getActiveDungeons().get(args[1]).getParty().put(UUID.randomUUID(), PlayerState.Dead);
                }
                break;
            case "buff":
                final int strAmp = 100;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, strAmp));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
                break;
            case "activeplayers":
                Map<UUID, String> ap = AppStatus.getActivePlayers();
                for (Map.Entry<UUID, String> entry : ap.entrySet()) {
                    LoadedDungeon dungeon = AppStatus.getActiveDungeons().get(entry.getValue());
                    String message = String.format("%s (%s) - %s (%s)",
                        AppUtils.uuidToPlayer(entry.getKey()).getName(),
                        entry.getKey(), entry.getValue(),
                        dungeon.getPlayerState(AppUtils.uuidToPlayer(entry.getKey())));
                    player.sendMessage(message);
                }
                break;
            case "loadeddungeons":
                for (Map.Entry<String, LoadedDungeon> entry : AppStatus.getActiveDungeons().entrySet()) {
                    player.sendMessage(entry.getKey());
                }
                break;
            default:
                player.sendMessage("unknown subcommand");
        }
        return true;
    }
}
