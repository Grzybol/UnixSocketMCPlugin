package org.betterbox.unixSocketTest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PluginLogger pluginLogger;
    private final UnixSocketTest unixSocketTest;
    public CommandManager(JavaPlugin plugin, ConfigManager configManager, PluginLogger pluginLogger, UnixSocketTest unixSocketTest) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.unixSocketTest=unixSocketTest;
        this.pluginLogger=pluginLogger;
        plugin.getCommand("ust").setExecutor(this);
        plugin.getCommand("ust").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String transactionID = UUID.randomUUID().toString();
        if (!command.getName().equalsIgnoreCase("ust")) return false;
        if(!sender.isOp()){
            return false;
        }
        boolean isPlayer = false;
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
            isPlayer = true;
        }else{
            isPlayer=false;
        }


        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ust <reload|cm>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                sender.sendMessage(ChatColor.GREEN + "Plugin created by: "+plugin.getDescription().getAuthors());
                sender.sendMessage(ChatColor.GREEN + "version: "+plugin.getDescription().getVersion());
                break;
            case "reload":
                if (sender.isOp()) {
                    configManager.reloadConfig();
                }else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                }
                    break;
            case "cm":
                if (sender.isOp()) {
                    String commandText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    unixSocketTest.handleCommand(commandText);
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                }
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use: /sg <reloadconfig|spawnnpc|info|upgradeItem>");
                break;
        }

        return true;
    }




    private void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("setsGenerator.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!command.getName().equalsIgnoreCase("sg")) return suggestions;

        if (args.length == 1) {
            if ("reloadconfig".startsWith(args[0].toLowerCase())) suggestions.add("reloadconfig");
            if ("spawnnpc".startsWith(args[0].toLowerCase())) suggestions.add("spawnnpc");
            if ("saveitem".startsWith(args[0].toLowerCase())) suggestions.add("saveitem");
            if ("upgradeItem".startsWith(args[0].toLowerCase())) suggestions.add("upgradeItem");
        }

        return suggestions;
    }
}
