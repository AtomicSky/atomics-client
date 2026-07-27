package de.derjannik.islandborder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class BiomeLocationStartCommand implements CommandExecutor, TabCompleter {
    private final BiomeTeamManager teamManager;

    public BiomeLocationStartCommand(BiomeTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            this.sendUsage(sender);
            return true;
        }
        BiomeTeamManager.BiomeTeam team;
        if (args[0].equalsIgnoreCase("desert")) {
            team = BiomeTeamManager.BiomeTeam.DESERT;
        } else if (args[0].equalsIgnoreCase("plains")) {
            team = BiomeTeamManager.BiomeTeam.PLAINS;
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid biome. Use 'desert' or 'plains'.");
            return true;
        }
        Location location;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Specify coordinates or run this command as a player.");
                return true;
            }
            location = player.getLocation();
        } else if (args.length >= 4 && args.length <= 6) {
            try {
                Location currentStart = this.teamManager.getStartLocation(team);
                World world = Bukkit.getWorld(currentStart.getWorld().getName());
                if (world == null) {
                    world = Bukkit.getWorlds().getFirst();
                }
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                float yaw = args.length >= 5 ? Float.parseFloat(args[4]) : 0.0f;
                float pitch = args.length >= 6 ? Float.parseFloat(args[5]) : 0.0f;
                location = new Location(world, x, y, z, yaw, pitch);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Coordinates, yaw, and pitch must be numbers.");
                return true;
            }
        } else {
            this.sendUsage(sender);
            return true;
        }
        this.teamManager.setStartLocation(team, location);
        sender.sendMessage(
            ChatColor.GREEN + this.prettyName(team) + " start location set to "
                + String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ())
                + " in world '" + location.getWorld().getName() + "'."
        );
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /locationstart <desert|plains> [x y z [yaw pitch]]");
    }

    private String prettyName(BiomeTeamManager.BiomeTeam team) {
        return team == BiomeTeamManager.BiomeTeam.PLAINS ? "Plains" : "Desert";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            for (String biome : Arrays.asList("desert", "plains")) {
                if (biome.startsWith(args[0].toLowerCase())) {
                    completions.add(biome);
                }
            }
        }
        return completions;
    }
}
