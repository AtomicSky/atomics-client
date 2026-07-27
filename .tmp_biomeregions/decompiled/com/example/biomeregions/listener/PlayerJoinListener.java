/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 */
package com.example.biomeregions.listener;

import com.example.biomeregions.BiomeRegionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener
implements Listener {
    private final BiomeRegionsPlugin plugin;

    public PlayerJoinListener(BiomeRegionsPlugin biomeRegionsPlugin) {
        this.plugin = biomeRegionsPlugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        playerJoinEvent.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}

