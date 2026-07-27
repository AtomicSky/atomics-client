/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand
implements CommandExecutor {
    private final BiomeRegionsPlugin plugin;

    public AdminCommand(BiomeRegionsPlugin biomeRegionsPlugin) {
        this.plugin = biomeRegionsPlugin;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (stringArray.length < 1) {
            commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Usage: /admin <player>"}, (String)String.valueOf(ChatColor.RED))));
            return true;
        }
        String string2 = stringArray[0];
        this.plugin.addToTeam(string2, "admin");
        commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Added \u0001 to the admin team."}, (String)String.valueOf(ChatColor.GREEN), string2)));
        Player player = Bukkit.getPlayerExact((String)string2);
        if (player != null) {
            player.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001You have been added to the admin team!"}, (String)String.valueOf(ChatColor.GREEN))));
        }
        return true;
    }
}

