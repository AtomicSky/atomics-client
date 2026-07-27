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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class IslandBorderPlugin
extends JavaPlugin {
    private BorderConfig borderConfig;
    private BorderParticleTask particleTask;
    private BorderEntityTask entityTask;

    public void onEnable() {
        this.saveDefaultConfig();
        this.borderConfig = new BorderConfig(this);
        this.borderConfig.load();
        this.getServer().getPluginManager().registerEvents((Listener)new BorderListener(this, this.borderConfig), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BorderProtectionListener(this, this.borderConfig), (Plugin)this);
        PluginCommand command = this.getCommand("border");
        if (command != null) {
            BorderCommand executor = new BorderCommand(this, this.borderConfig);
            command.setExecutor((CommandExecutor)executor);
            command.setTabCompleter((TabCompleter)executor);
        }
        this.particleTask = new BorderParticleTask(this, this.borderConfig);
        this.particleTask.runTaskTimer((Plugin)this, 20L, Math.max(1, this.borderConfig.getParticleUpdateIntervalTicks()));
        this.entityTask = new BorderEntityTask(this, this.borderConfig);
        this.entityTask.runTaskTimer((Plugin)this, 20L, Math.max(1, this.borderConfig.getEntityCheckIntervalTicks()));
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

