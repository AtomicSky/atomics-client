package de.derjannik.islandborder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class BiomePlayerJoinListener implements Listener {
    private final IslandBorderPlugin plugin;
    private final BiomeTeamManager teamManager;
    private final BorderListener borderListener;

    public BiomePlayerJoinListener(IslandBorderPlugin plugin, BiomeTeamManager teamManager, BorderListener borderListener) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.borderListener = borderListener;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.teamManager.applyScoreboard(player);
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (player.isOnline()) {
                this.borderListener.syncPlayerToAssignedSide(player);
            }
        });
    }
}
