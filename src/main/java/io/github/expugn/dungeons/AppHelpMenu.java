package io.github.expugn.dungeons;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;

/**
 * Sends the PartyDungeons App's help menu to a command sender.
 * @author S'pugn
 * @version 0.2
 */
public final class AppHelpMenu {
    private AppHelpMenu() {
        // NOT USED, AppHelpMenu IS A UTILITY CLASS THAT REQUIRES THIS PRIVATE CONSTRUCTOR
    }

    /**
     * Create the help menu message and send it to the command sender.
     * @param sender Entity who requested the plugin's help menu.
     */
    public static void send(CommandSender sender) {
        TextComponent title = new TextComponent(String.format("%sPartyDungeons %sHelp Menu\n", ChatColor.GOLD,
            ChatColor.YELLOW));
        TextComponent footer = new TextComponent(String.format("%sHover over the commands for more information and %s",
            ChatColor.YELLOW, "click them to suggest them in your chat box."));
        TextComponent help = new TextComponent(String.format("%s/partydungeons %shelp\n", ChatColor.GRAY,
            ChatColor.GOLD));
        help.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("%s\n%sYou're looking at it now, you know.",
            "Display a help menu with command options and information.", ChatColor.YELLOW))));
        help.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons help"));
        TextComponent join = new TextComponent(String.format("%s/partydungeons %sjoin\n", ChatColor.GRAY,
            ChatColor.GOLD));
        join.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(String.format("Join a dungeon.\n%s%s",
            ChatColor.YELLOW, "You must be standing in a valid dungeon area to do so."))));
        join.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons join"));
        TextComponent leave = new TextComponent(String.format("%s/partydungeons %sleave\n", ChatColor.GRAY,
            ChatColor.GOLD));
        leave.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Leave a dungeon you are a part of.\n%s%s", ChatColor.YELLOW,
            "You can not rejoin a dungeon you left while it was in progress."))));
        leave.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons leave"));
        TextComponent status = new TextComponent(String.format("%s/partydungeons %sstatus\n", ChatColor.GRAY,
            ChatColor.GOLD));
        status.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Get a dungeon's current status.\n%s%s", ChatColor.YELLOW,
            "You must be participating in a dungeon to see this information."))));
        status.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons status"));
        if (!sender.hasPermission(AppConstants.ADMIN_PERMISSION)) {
            // NO ADMIN PERMISSIONS, END HELP HERE
            sender.spigot().sendMessage(title, help, join, leave, status, footer);
            return;
        }

        TextComponent createdungeon = new TextComponent(
            String.format("%s/partydungeons %screatedungeon %s<dungeon_name>\n", ChatColor.GRAY, ChatColor.GOLD,
            ChatColor.AQUA));
        createdungeon.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Create a new dungeon.\n%sYou will need to select a dungeon's area, please look\n%s",
            ChatColor.YELLOW, "at the first block before running this command."))));
        createdungeon.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons createdungeon"));
        TextComponent createworlddirectory = new TextComponent(
            String.format("%s/partydungeons %screateworlddirectory\n", ChatColor.GRAY, ChatColor.GOLD));
        createworlddirectory.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text("Create a script directory for the world you are in.")));
        createworlddirectory.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
            "/partydungeons createworlddirectory"));
        TextComponent loaddungeon = new TextComponent(String.format("%s/partydungeons %sloaddungeon %s<dungeon_name>\n",
            ChatColor.GRAY, ChatColor.GOLD, ChatColor.AQUA));
        loaddungeon.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Load an unloaded dungeon.\n%s%s", ChatColor.YELLOW,
            "All dungeons are loaded when PartyDungeons starts."))));
        loaddungeon.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons loaddungeon"));
        TextComponent unloaddungeon = new TextComponent(
            String.format("%s/partydungeons %sunloaddungeon %s<dungeon_name>\n", ChatColor.GRAY, ChatColor.GOLD,
            ChatColor.AQUA));
        unloaddungeon.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Unload a loaded dungeon.\n%sActive players in this dungeon will be removed.\n%s",
            ChatColor.YELLOW, "The dungeon will be reset and will be unable to be joined."))));
        unloaddungeon.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons unloaddungeon"));
        TextComponent setspawnposition = new TextComponent(
            String.format("%s/partydungeons %ssetspawnposition %s<dungeon_name>\n", ChatColor.GRAY, ChatColor.GOLD,
            ChatColor.AQUA));
        setspawnposition.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Set the SPAWN position of a dungeon.\n%sPlayers who leave or %s", ChatColor.YELLOW,
            "disconnected will be teleported\nto this position."))));
        setspawnposition.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
            "/partydungeons setspawnposition"));
        TextComponent setstartposition = new TextComponent(
            String.format("%s/partydungeons %ssetstartposition %s<dungeon_name>\n", ChatColor.GRAY, ChatColor.GOLD,
            ChatColor.AQUA));
        setstartposition.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Set the START position of a dungeon.\n%s%s", ChatColor.YELLOW,
            "Players who have joined the party will teleport to\nthis position when it starts."))));
        setstartposition.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
            "/partydungeons setstartposition"));
        TextComponent createscript = new TextComponent(
            String.format("%s/partydungeons %screatescript %s<dungeon_name> <script_type>\n", ChatColor.GRAY,
            ChatColor.GOLD, ChatColor.AQUA));
        createscript.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Create a new script for a dungeon.\n%s%s", ChatColor.YELLOW,
            "Generated scripts will need to be edited in\na text editor to be used."))));
        createscript.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons createscript"));
        TextComponent createworldscript = new TextComponent(
            String.format("%s/partydungeons %screateworldscript %s<script_type>\n", ChatColor.GRAY, ChatColor.GOLD,
                ChatColor.AQUA));
        createworldscript.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Create a new script for the world you are in.\n%s%s", ChatColor.YELLOW,
            "Generated scripts will need to be edited in\na text editor to be used."))));
        createworldscript.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
            "/partydungeons createworldscript"));
        TextComponent deletescript = new TextComponent(
            String.format("%s/partydungeons %sdeletescript %s<file_path>\n", ChatColor.GRAY, ChatColor.GOLD,
                ChatColor.AQUA));
        deletescript.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Delete a script in the given file path.\n%s%s%s", ChatColor.YELLOW,
            String.format("Deletes plugins/%s/<file_path>\n", AppStatus.getPlugin().getName()),
            String.format("Only scripts (%s files) can be deleted with this method.",
                AppConstants.SCRIPT_ENGINE_EXTENSION)))));
        deletescript.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons deletescript"));
        TextComponent settings = new TextComponent(
            String.format("%s/partydungeons %ssettings %s<dungeon_name> <setting_type> %s[value]\n", ChatColor.GRAY,
            ChatColor.GOLD, ChatColor.AQUA, ChatColor.DARK_GRAY));
        settings.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Change a setting of a dungeon (max_party and daily_clear).\n%s%s", ChatColor.YELLOW,
            "Value is optional. Not including it\nwill display the current value instead."))));
        settings.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons settings"));
        TextComponent runscript = new TextComponent(
            String.format("%s/partydungeons %srunscript %s<dungeon_name> <script_type> <script_name>\n", ChatColor.GRAY,
            ChatColor.GOLD, ChatColor.AQUA));
        runscript.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Run a dungeon's script.\n%sPlayer must be in a dungeon party %s", ChatColor.YELLOW,
            "for best\ncompatibility. Probably avoid using this command."))));
        runscript.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons runscript"));
        TextComponent download = new TextComponent(
            String.format("%s/partydungeons %sdownload %s<dungeon_name> <manifest_url>\n", ChatColor.GRAY,
            ChatColor.GOLD, ChatColor.AQUA));
        download.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Read a file manifest from a URL and download files.\n%sAll %s%s", ChatColor.YELLOW,
            "files in the manifest will overwrite existing files.",
            "\nDo not use this if you don't want to risk overwritten files."))));
        download.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons download"));
        TextComponent manifest = new TextComponent(
            String.format("%s/partydungeons %smanifest %s<dungeon_name> %s[root_url]\n", ChatColor.GRAY,
            ChatColor.GOLD, ChatColor.AQUA, ChatColor.DARK_GRAY));
        manifest.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new Text(String.format("Read a dungeon directory and generate a file manifest.\n%s%s", ChatColor.YELLOW,
            "File URL will need to be manually added if [root_url]\nis not provided."))));
        manifest.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/partydungeons manifest"));
        sender.spigot().sendMessage(title, help, join, leave, status, createdungeon, createworlddirectory, loaddungeon,
            unloaddungeon, setspawnposition, setstartposition, createscript, createworldscript, deletescript, settings,
            runscript, download, manifest, footer);
    }
}
