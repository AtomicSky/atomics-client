/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 */
package com.example.biomeregions;

import com.example.biomeregions.command.AdminCommand;
import com.example.biomeregions.command.DesertCommand;
import com.example.biomeregions.command.LocationStartCommand;
import com.example.biomeregions.command.PlainsCommand;
import com.example.biomeregions.command.StartCommand;
import com.example.biomeregions.listener.PlayerJoinListener;
import java.lang.invoke.StringConcatFactory;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class BiomeRegionsPlugin
extends JavaPlugin {
    private Scoreboard scoreboard;
    private Team adminTeam;
    private Team desertTeam;
    private Team plainsTeam;
    private Location desertStartLocation;
    private Location plainsStartLocation;

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfigValues();
        this.setupTeams();
        this.registerCommands();
        this.registerListeners();
        this.getLogger().info("BiomeRegions enabled!");
    }

    public void onDisable() {
        this.getLogger().info("BiomeRegions disabled!");
    }

    private void loadConfigValues() {
        this.reloadConfig();
        FileConfiguration fileConfiguration = this.getConfig();
        this.desertStartLocation = this.loadLocation(fileConfiguration, "desert-start");
        this.plainsStartLocation = this.loadLocation(fileConfiguration, "plains-start");
    }

    private Location loadLocation(FileConfiguration fileConfiguration, String string) {
        String string2 = fileConfiguration.getString((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.world"}, (String)string)), "world");
        World world = Bukkit.getWorld((String)string2);
        if (world == null) {
            world = (World)Bukkit.getWorlds().getFirst();
        }
        double d = fileConfiguration.getDouble((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.x"}, (String)string)), 0.0);
        double d2 = fileConfiguration.getDouble((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.y"}, (String)string)), 64.0);
        double d3 = fileConfiguration.getDouble((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.z"}, (String)string)), 0.0);
        float f = (float)fileConfiguration.getDouble((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.yaw"}, (String)string)), 0.0);
        float f2 = (float)fileConfiguration.getDouble((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.pitch"}, (String)string)), 0.0);
        return new Location(world, d, d2, d3, f, f2);
    }

    private void setupTeams() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String string : new FileConfiguration[]{"admin", "desert", "plains"}) {
            Team team = this.scoreboard.getTeam(string);
            if (team == null) continue;
            team.unregister();
        }
        this.adminTeam = this.scoreboard.registerNewTeam("admin");
        this.adminTeam.color(NamedTextColor.DARK_RED);
        this.adminTeam.prefix((Component)Component.text((String)"[Admin] "));
        this.desertTeam = this.scoreboard.registerNewTeam("desert");
        this.desertTeam.color(NamedTextColor.GOLD);
        this.plainsTeam = this.scoreboard.registerNewTeam("plains");
        this.plainsTeam.color(NamedTextColor.GREEN);
        FileConfiguration fileConfiguration = this.getConfig();
        for (String string : fileConfiguration.getStringList("teams.admin")) {
            this.adminTeam.addEntry(string);
        }
        for (String string : fileConfiguration.getStringList("teams.desert")) {
            this.desertTeam.addEntry(string);
        }
        for (String string : fileConfiguration.getStringList("teams.plains")) {
            this.plainsTeam.addEntry(string);
        }
    }

    private void registerCommands() {
        this.getCommand("admin").setExecutor((CommandExecutor)new AdminCommand(this));
        this.getCommand("desert").setExecutor((CommandExecutor)new DesertCommand(this));
        this.getCommand("plains").setExecutor((CommandExecutor)new PlainsCommand(this));
        this.getCommand("start").setExecutor((CommandExecutor)new StartCommand(this));
        this.getCommand("locationstart").setExecutor((CommandExecutor)new LocationStartCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents((Listener)new PlayerJoinListener(this), (Plugin)this);
    }

    public void addToTeam(String string, String string2) {
        this.adminTeam.removeEntry(string);
        this.desertTeam.removeEntry(string);
        this.plainsTeam.removeEntry(string);
        switch (string2.toLowerCase()) {
            case "admin": {
                this.adminTeam.addEntry(string);
                break;
            }
            case "desert": {
                this.desertTeam.addEntry(string);
                break;
            }
            case "plains": {
                this.plainsTeam.addEntry(string);
            }
        }
        this.saveTeamToConfig(string, string2);
    }

    private void saveTeamToConfig(String string, String string2) {
        FileConfiguration fileConfiguration = this.getConfig();
        for (String string3 : new String[]{"admin", "desert", "plains"}) {
            List list = fileConfiguration.getStringList((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"teams.\u0001"}, (String)string3)));
            list.remove(string);
            fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"teams.\u0001"}, (String)string3)), (Object)list);
        }
        List list = fileConfiguration.getStringList((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"teams.\u0001"}, (String)string2)));
        if (!list.contains(string)) {
            list.add(string);
        }
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"teams.\u0001"}, (String)string2)), (Object)list);
        this.saveConfig();
    }

    public Location getDesertStartLocation() {
        return this.desertStartLocation.clone();
    }

    public Location getPlainsStartLocation() {
        return this.plainsStartLocation.clone();
    }

    public void setDesertStartLocation(Location location) {
        this.desertStartLocation = location.clone();
        this.saveLocationToConfig("desert-start", location);
    }

    public void setPlainsStartLocation(Location location) {
        this.plainsStartLocation = location.clone();
        this.saveLocationToConfig("plains-start", location);
    }

    private void saveLocationToConfig(String string, Location location) {
        FileConfiguration fileConfiguration = this.getConfig();
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.world"}, (String)string)), (Object)location.getWorld().getName());
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.x"}, (String)string)), (Object)location.getX());
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.y"}, (String)string)), (Object)location.getY());
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.z"}, (String)string)), (Object)location.getZ());
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.yaw"}, (String)string)), (Object)location.getYaw());
        fileConfiguration.set((String)((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"\u0001.pitch"}, (String)string)), (Object)location.getPitch());
        this.saveConfig();
    }

    public boolean isPlayerInTeam(String string, String string2) {
        boolean bl;
        switch (string2.toLowerCase()) {
            case "admin": {
                bl = this.adminTeam.hasEntry(string);
                break;
            }
            case "desert": {
                bl = this.desertTeam.hasEntry(string);
                break;
            }
            case "plains": {
                bl = this.plainsTeam.hasEntry(string);
                break;
            }
            default: {
                bl = false;
            }
        }
        return bl;
    }
}

