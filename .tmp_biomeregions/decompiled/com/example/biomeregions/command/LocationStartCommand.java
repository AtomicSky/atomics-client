/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.example.biomeregions.command;

import com.example.biomeregions.BiomeRegionsPlugin;
import java.lang.invoke.StringConcatFactory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LocationStartCommand
implements CommandExecutor {
    private final BiomeRegionsPlugin plugin;

    public LocationStartCommand(BiomeRegionsPlugin biomeRegionsPlugin) {
        this.plugin = biomeRegionsPlugin;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        Location location;
        if (stringArray.length < 1) {
            commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Usage: /locationstart <desert|plains> [x y z yaw pitch]"}, (String)String.valueOf(ChatColor.RED))));
            return true;
        }
        String string2 = stringArray[0].toLowerCase();
        if (!string2.equals("desert") && !string2.equals("plains")) {
            commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Invalid biome. Use 'desert' or 'plains'."}, (String)String.valueOf(ChatColor.RED))));
            return true;
        }
        if (stringArray.length >= 5) {
            try {
                double d = Double.parseDouble(stringArray[1]);
                double d2 = Double.parseDouble(stringArray[2]);
                double d3 = Double.parseDouble(stringArray[3]);
                float f = stringArray.length >= 6 ? Float.parseFloat(stringArray[4]) : 0.0f;
                float f2 = stringArray.length >= 7 ? Float.parseFloat(stringArray[5]) : 0.0f;
                String string3 = this.plugin.getConfig().getString((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001-start.world"}, (String)string2)), "world");
                World world = this.plugin.getServer().getWorld(string3);
                if (world == null) {
                    world = (World)this.plugin.getServer().getWorlds().getFirst();
                }
                location = new Location(world, d, d2, d3, f, f2);
            }
            catch (NumberFormatException numberFormatException) {
                commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001Invalid coordinates. Use numbers for x y z yaw pitch."}, (String)String.valueOf(ChatColor.RED))));
                return true;
            }
        } else {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001You must specify coordinates or be a player to use your current location."}, (String)String.valueOf(ChatColor.RED))));
                return true;
            }
            Player player = (Player)commandSender;
            location = player.getLocation();
        }
        if (string2.equals("desert")) {
            this.plugin.setDesertStartLocation(location);
        } else {
            this.plugin.setPlainsStartLocation(location);
        }
        String string4 = String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
        commandSender.sendMessage((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001\u0001\u0001 start location set to \u0001 in world '\u0001'."}, (String)String.valueOf(ChatColor.GREEN), string2.substring(0, 1).toUpperCase(), string2.substring(1), string4, location.getWorld().getName())));
        return true;
    }
}

