/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.example.biomeregions.command;

import com.example.biomeregions.BiomeRegionsPlugin;
import java.lang.invoke.StringConcatFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand
implements CommandExecutor {
    private final BiomeRegionsPlugin plugin;

    public StartCommand(BiomeRegionsPlugin biomeRegionsPlugin) {
        this.plugin = biomeRegionsPlugin;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001This command can only be used by players."}, (String)String.valueOf(ChatColor.RED))));
            return true;
        }
        int n = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.isPlayerInTeam(player.getName(), "admin")) continue;
            Location location = this.plugin.isPlayerInTeam(player.getName(), "desert") ? this.plugin.getDesertStartLocation() : (this.plugin.isPlayerInTeam(player.getName(), "plains") ? this.plugin.getPlainsStartLocation() : this.plugin.getDesertStartLocation());
            player.teleport(location);
            player.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Teleported to your biome start location!"}, (String)String.valueOf(ChatColor.GREEN))));
            ++n;
        }
        commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Teleported \u0001 player(s) to their start locations. Admins were skipped."}, (String)String.valueOf(ChatColor.GREEN), (int)n)));
        return true;
    }
}

