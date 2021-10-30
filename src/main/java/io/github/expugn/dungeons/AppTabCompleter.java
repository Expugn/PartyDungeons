package io.github.expugn.dungeons;

import io.github.expugn.dungeons.AppCommand.Commands;
import io.github.expugn.dungeons.scripts.ScriptType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

/**
 * Handles command tab completion.
 * @author S'pugn
 * @version 0.1
 */
public class AppTabCompleter implements TabCompleter {
    private static final int THREE_ARGUMENTS = 3;
    private static final int FOUR_ARGUMENTS = 4;
    private static final int FIVE_ARGUMENTS = 5;

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        final List<String> results = new ArrayList<>();

        if (args.length < 2) { // partydungeons <typing...>
            // GIVE LIST OF COMMANDS
            List<String> cmds = Stream.of(Commands.values())
                .filter(c -> c.isAdminCommand() ? sender.hasPermission(AppConstants.ADMIN_PERMISSION) : true)
                .map(Enum::toString)
                .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[0], cmds, results);
            Collections.sort(results, String.CASE_INSENSITIVE_ORDER);
            return results;
        }

        // FIGURE OUT COMMAND ARGUMENTS
        Commands sub = Commands.get(args[0]);
        if (sub == null) {
            return null;
        }

        if (args.length < THREE_ARGUMENTS) { // partydungeons COMMAND <typing...>
            switch (sub) {
                case CREATE_DUNGEON:
                case LOAD_DUNGEON:
                    // UNKNOWN DUNGEON NAME
                    results.add("<dungeon_name>");
                    break;
                case UNLOAD_DUNGEON:
                case SET_SPAWN_POSITION:
                case SET_START_POSITION:
                case CREATE_SCRIPT:
                case SETTINGS:
                case RUN_SCRIPT:
                case DOWNLOAD:
                case MANIFEST:
                    // LOADED DUNGEON NAMES
                    results.addAll(AppStatus.getActiveDungeons().keySet());
                    break;
                default:
                    break;
            }
        } else if (args.length < FOUR_ARGUMENTS) { // partydungeons COMMAND ARGUMENT <typing...>
            switch (sub) {
                case CREATE_SCRIPT:
                case RUN_SCRIPT:
                    // SCRIPT TYPES
                    results.addAll(Stream.of(ScriptType.values()).map(Enum::name).collect(Collectors.toList()));
                    break;
                case SETTINGS:
                    results.add("max_party");
                    results.add("daily_clear");
                    break;
                case DOWNLOAD:
                    results.add("<manifest_url>");
                    break;
                case MANIFEST:
                    results.add("[root_url]");
                    break;
                default:
                    break;
            }
        } else if (args.length < FIVE_ARGUMENTS) { // partydungeons COMMAND ARGUMENT ARGUMENT <typing...>
            switch (sub) {
                case RUN_SCRIPT:
                    results.add("<script_name>");
                    break;
                default:
                    break;
            }
        }
        return results;
    }
}
