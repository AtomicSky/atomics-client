/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.BorderCommand;
import de.derjannik.islandborder.BorderConfig;
import de.derjannik.islandborder.BorderEntityTask;
import de.derjannik.islandborder.BorderListener;
import de.derjannik.islandborder.BorderParticleTask;
import de.derjannik.islandborder.BorderProtectionListener;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class IslandBorderPlugin
extends JavaPlugin {
    private BorderConfig borderConfig;
    private BiomeTeamManager teamManager;
    private BorderListener borderListener;
    private BorderParticleTask particleTask;
    private BorderEntityTask entityTask;

    public void onEnable() {
        this.saveDefaultConfig();
        this.borderConfig = new BorderConfig(this);
        this.borderConfig.load();
        this.migrateBiomeRegionsConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.teamManager = new BiomeTeamManager(this);
        this.teamManager.load();
        this.borderListener = new BorderListener(this, this.borderConfig, this.teamManager);
        this.getServer().getPluginManager().registerEvents((Listener)this.borderListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BorderProtectionListener(this, this.borderConfig), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BiomePlayerJoinListener(this, this.teamManager, this.borderListener), (Plugin)this);
        PluginCommand command = this.getCommand("border");
        if (command != null) {
            BorderCommand executor = new BorderCommand(this, this.borderConfig, this.borderListener, this.teamManager);
            command.setExecutor((CommandExecutor)executor);
            command.setTabCompleter((TabCompleter)executor);
        }
        this.registerBiomeCommands();
        this.particleTask = new BorderParticleTask(this, this.borderConfig);
        this.particleTask.runTaskTimer((Plugin)this, 20L, Math.max(1, this.borderConfig.getParticleUpdateIntervalTicks()));
        this.entityTask = new BorderEntityTask(this, this.borderConfig);
        this.entityTask.runTaskTimer((Plugin)this, 20L, Math.max(1, this.borderConfig.getEntityCheckIntervalTicks()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.teamManager.applyScoreboard(player);
            this.borderListener.syncPlayerToAssignedSide(player);
        }
        this.printBanner();
    }

    public void onDisable() {
        if (this.particleTask != null) {
            this.particleTask.cancel();
        }
        if (this.entityTask != null) {
            this.entityTask.cancel();
        }
        this.getLogger().info("IslandBorder disabled.");
    }

    public BorderConfig getBorderConfig() {
        return this.borderConfig;
    }

    public BiomeTeamManager getTeamManager() {
        return this.teamManager;
    }

    public void restartParticleTask() {
        if (this.particleTask != null) {
            this.particleTask.cancel();
        }
        this.particleTask = new BorderParticleTask(this, this.borderConfig);
        this.particleTask.runTaskTimer((Plugin)this, 1L, Math.max(1, this.borderConfig.getParticleUpdateIntervalTicks()));
        if (this.entityTask != null) {
            this.entityTask.cancel();
        }
        this.entityTask = new BorderEntityTask(this, this.borderConfig);
        this.entityTask.runTaskTimer((Plugin)this, 1L, Math.max(1, this.borderConfig.getEntityCheckIntervalTicks()));
    }

    private void registerBiomeCommands() {
        BiomeAssignmentCommand assignmentCommand = new BiomeAssignmentCommand(this.teamManager, this.borderListener);
        for (String commandName : List.of("admin", "desert", "plains")) {
            PluginCommand command = this.getCommand(commandName);
            if (command != null) {
                command.setExecutor(assignmentCommand);
                command.setTabCompleter(assignmentCommand);
            }
        }
        PluginCommand startCommand = this.getCommand("start");
        if (startCommand != null) {
            startCommand.setExecutor(new BiomeStartCommand(this.teamManager));
        }
        PluginCommand locationStartCommand = this.getCommand("locationstart");
        if (locationStartCommand != null) {
            BiomeLocationStartCommand executor = new BiomeLocationStartCommand(this.teamManager);
            locationStartCommand.setExecutor(executor);
            locationStartCommand.setTabCompleter(executor);
        }
    }

    private void migrateBiomeRegionsConfig() {
        FileConfiguration config = this.getConfig();
        if (config.getBoolean("biome-regions.migration-complete", false)) {
            return;
        }
        File pluginsDirectory = this.getDataFolder().getParentFile();
        File legacyFile = new File(new File(pluginsDirectory, "BiomeRegions"), "config.yml");
        if (legacyFile.isFile()) {
            YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
            for (String path : List.of(
                "desert-start.world", "desert-start.x", "desert-start.y", "desert-start.z", "desert-start.yaw", "desert-start.pitch",
                "plains-start.world", "plains-start.x", "plains-start.y", "plains-start.z", "plains-start.yaw", "plains-start.pitch",
                "teams.admin", "teams.desert", "teams.plains"
            )) {
                if (legacy.contains(path)) {
                    config.set(path, legacy.get(path));
                }
            }
            this.getLogger().info("Imported teams and start locations from plugins/BiomeRegions/config.yml.");
        }
        config.set("biome-regions.migration-complete", true);
        this.saveConfig();
    }

    private void printBanner() {
        String[] banner;
        String c = ChatColor.GREEN.toString();
        String w = ChatColor.WHITE.toString();
        String gray = ChatColor.GRAY.toString();
        String r = ChatColor.RESET.toString();
        for (String line : banner = new String[]{"", c + "  ___     _                _   ___             _         ", c + " |_ _|___| |__ _ _ _  __| | | _ ) ___ _ _ __| |___ _ _ ", c + "  | |(_-<| / _` | ' \\/ _` | | _ \\/ _ \\ '_/ _` / -_) '_|", c + " |___/__/|_\\__,_|_||_\\__,_| |___/\\___/_| \\__,_\\___|_|  ", "", gray + "        made by " + w + "derjannik.de" + gray + "  |  " + w + "v" + this.getDescription().getVersion() + r, ""}) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }
}
