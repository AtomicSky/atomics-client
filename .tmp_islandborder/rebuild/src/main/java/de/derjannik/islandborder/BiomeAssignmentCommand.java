package de.derjannik.islandborder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class BiomeAssignmentCommand implements CommandExecutor, TabCompleter {
    private final BiomeTeamManager teamManager;
    private final BorderListener borderListener;

    public BiomeAssignmentCommand(BiomeTeamManager teamManager, BorderListener borderListener) {
        this.teamManager = teamManager;
        this.borderListener = borderListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player|selector>");
            return true;
        }
        BiomeTeamManager.BiomeTeam team = this.teamForCommand(command.getName());
        if (args[0].startsWith("@")) {
            return this.assignSelector(sender, args[0], team);
        }
        String playerName = args[0];
        this.teamManager.assign(playerName, team);
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            this.applyOnlineAssignment(player, team);
        }
        sender.sendMessage(ChatColor.GREEN + "Assigned " + playerName + " to the " + team.configKey() + " team.");
        return true;
    }

    private boolean assignSelector(CommandSender sender, String selector, BiomeTeamManager.BiomeTeam team) {
        int assigned = 0;
        try {
            for (Entity entity : Bukkit.selectEntities(sender, selector)) {
                if (!(entity instanceof Player player)) {
                    continue;
                }
                this.teamManager.assign(player.getName(), team);
                this.applyOnlineAssignment(player, team);
                ++assigned;
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid selector: " + e.getMessage());
            return true;
        }
        if (assigned == 0) {
            sender.sendMessage(ChatColor.RED + "That selector matched no online players.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Assigned " + assigned + " player" + (assigned == 1 ? "" : "s") + " to the " + team.configKey() + " team.");
        return true;
    }

    private void applyOnlineAssignment(Player player, BiomeTeamManager.BiomeTeam team) {
        this.teamManager.applyScoreboard(player);
        if (team == BiomeTeamManager.BiomeTeam.DESERT || team == BiomeTeamManager.BiomeTeam.PLAINS) {
            this.borderListener.syncPlayerToAssignedSide(player);
        }
        player.sendMessage(ChatColor.GREEN + "You are now on the " + team.configKey() + " team.");
    }

    private BiomeTeamManager.BiomeTeam teamForCommand(String commandName) {
        return switch (commandName.toLowerCase()) {
            case "admin" -> BiomeTeamManager.BiomeTeam.ADMIN;
            case "plains" -> BiomeTeamManager.BiomeTeam.PLAINS;
            default -> BiomeTeamManager.BiomeTeam.DESERT;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length != 1) {
            return completions;
        }
        String input = args[0].toLowerCase();
        for (String selector : Arrays.asList("@a", "@p", "@r", "@s")) {
            if (selector.startsWith(input)) {
                completions.add(selector);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(input)) {
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
