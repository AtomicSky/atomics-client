package de.derjannik.islandborder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BiomeStartCommand implements CommandExecutor {
    private final BiomeTeamManager teamManager;

    public BiomeStartCommand(BiomeTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        int teleported = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            BiomeTeamManager.BiomeTeam team = this.teamManager.getTeam(player.getName());
            if (team == BiomeTeamManager.BiomeTeam.ADMIN) {
                continue;
            }
            if (team == BiomeTeamManager.BiomeTeam.NONE) {
                team = BiomeTeamManager.BiomeTeam.DESERT;
                this.teamManager.assign(player.getName(), team);
            }
            Location destination = this.teamManager.getStartLocation(team);
            if (!player.teleport(destination)) {
                continue;
            }
            player.sendMessage(ChatColor.GREEN + "Teleported to your biome start location!");
            ++teleported;
        }
        sender.sendMessage(ChatColor.GREEN + "Teleported " + teleported + " player(s) to their start locations. Admins were skipped.");
        return true;
    }
}
