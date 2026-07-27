/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.BorderConfig;
import de.derjannik.islandborder.IslandBorderPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class BorderListener
implements Listener {
    private final IslandBorderPlugin plugin;
    private final BorderConfig config;
    private final BiomeTeamManager teamManager;
    private final Map<UUID, Long> lastWarnOuter = new HashMap<UUID, Long>();
    private final Map<UUID, Long> lastWarnDivider = new HashMap<UUID, Long>();
    private final Map<UUID, Boolean> dividerSides = new HashMap<UUID, Boolean>();

    public BorderListener(IslandBorderPlugin plugin, BorderConfig config, BiomeTeamManager teamManager) {
        this.plugin = plugin;
        this.config = config;
        this.teamManager = teamManager;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onMove(PlayerMoveEvent event) {
        if (!this.config.isEnabled()) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getX() == event.getTo().getX() && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("islandborder.bypass")) {
            this.rememberCurrentSide(player, event.getTo());
            return;
        }
        if (!player.getWorld().getName().equals(this.config.getWorldName())) {
            this.dividerSides.remove(player.getUniqueId());
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        double x = to.getX();
        double z = to.getZ();
        UUID playerId = player.getUniqueId();
        boolean destinationPlainsSide = this.config.isPlainsSide(x, z);
        BiomeTeamManager.BiomeTeam team = this.teamManager.getTeam(player.getName());
        Boolean assignedPlainsSide = this.teamManager.getAssignedPlainsSide(player.getName());
        boolean allowedPlainsSide;
        if (assignedPlainsSide != null) {
            allowedPlainsSide = assignedPlainsSide;
            this.dividerSides.put(playerId, allowedPlainsSide);
        } else {
            allowedPlainsSide = this.dividerSides.computeIfAbsent(playerId, ignored -> this.config.isPlainsSide(from.getX(), from.getZ()));
        }
        if (!this.config.isInsideOuter(x, z)) {
            if (!this.config.isPushBack()) {
                this.warn(player, this.lastWarnOuter, "hit-outer");
                return;
            }
            Location fixed = to.clone();
            fixed.setX(this.config.clampX(x));
            fixed.setZ(this.config.clampZ(z));
            event.setTo(fixed);
            this.warn(player, this.lastWarnOuter, "hit-outer");
            return;
        }
        if (!this.config.isDividerEnforced()) {
            this.dividerSides.put(playerId, assignedPlainsSide != null ? assignedPlainsSide : destinationPlainsSide);
            return;
        }
        if (team == BiomeTeamManager.BiomeTeam.ADMIN) {
            this.dividerSides.put(playerId, destinationPlainsSide);
            return;
        }
        if (allowedPlainsSide != destinationPlainsSide) {
            if (!this.config.isPushBack()) {
                if (assignedPlainsSide == null) {
                    this.dividerSides.put(playerId, destinationPlainsSide);
                }
                this.warn(player, this.lastWarnDivider, "hit-divider");
                return;
            }
            Location fixed = to.clone();
            boolean isX = "x".equals(this.config.getDividerAxis());
            if (isX) {
                fixed.setX(this.config.clampToSide(x, z, allowedPlainsSide, true));
            } else {
                fixed.setZ(this.config.clampToSide(x, z, allowedPlainsSide, false));
            }
            event.setTo(fixed);
            this.warn(player, this.lastWarnDivider, "hit-divider");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!this.config.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || !to.getWorld().getName().equals(this.config.getWorldName())) {
            this.dividerSides.remove(player.getUniqueId());
            return;
        }
        if (player.hasPermission("islandborder.bypass")) {
            this.rememberCurrentSide(player, to);
            return;
        }
        if (!this.config.isInsideOuter(to.getX(), to.getZ())) {
            Location fixed = to.clone();
            fixed.setX(this.config.clampX(to.getX()));
            fixed.setZ(this.config.clampZ(to.getZ()));
            event.setTo(fixed);
            this.warn(player, this.lastWarnOuter, "hit-outer");
            return;
        }
        UUID playerId = player.getUniqueId();
        Location from = event.getFrom();
        boolean destinationPlainsSide = this.config.isPlainsSide(to.getX(), to.getZ());
        BiomeTeamManager.BiomeTeam team = this.teamManager.getTeam(player.getName());
        Boolean assignedPlainsSide = this.teamManager.getAssignedPlainsSide(player.getName());
        boolean allowedPlainsSide;
        if (assignedPlainsSide != null) {
            allowedPlainsSide = assignedPlainsSide;
            this.dividerSides.put(playerId, allowedPlainsSide);
        } else {
            allowedPlainsSide = this.dividerSides.computeIfAbsent(playerId, ignored -> {
                if (from != null && from.getWorld() != null && from.getWorld().getName().equals(this.config.getWorldName())) {
                    return this.config.isPlainsSide(from.getX(), from.getZ());
                }
                return destinationPlainsSide;
            });
        }
        if (!this.config.isDividerEnforced()) {
            this.dividerSides.put(playerId, assignedPlainsSide != null ? assignedPlainsSide : destinationPlainsSide);
            return;
        }
        if (team == BiomeTeamManager.BiomeTeam.ADMIN) {
            this.dividerSides.put(playerId, destinationPlainsSide);
            return;
        }
        if (allowedPlainsSide != destinationPlainsSide) {
            event.setCancelled(true);
            this.warn(player, this.lastWarnDivider, "hit-divider");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.dividerSides.remove(playerId);
        this.lastWarnOuter.remove(playerId);
        this.lastWarnDivider.remove(playerId);
    }

    private void rememberCurrentSide(Player player, Location location) {
        if (location.getWorld() != null && location.getWorld().getName().equals(this.config.getWorldName())) {
            this.dividerSides.put(player.getUniqueId(), this.config.isPlainsSide(location.getX(), location.getZ()));
        }
    }

    public boolean switchSide(Player player) {
        if (!player.getWorld().getName().equals(this.config.getWorldName())) {
            return false;
        }
        BiomeTeamManager.BiomeTeam previousTeam = this.teamManager.getTeam(player.getName());
        if (previousTeam == BiomeTeamManager.BiomeTeam.ADMIN) {
            return false;
        }
        boolean currentSide = previousTeam == BiomeTeamManager.BiomeTeam.PLAINS
            || previousTeam == BiomeTeamManager.BiomeTeam.NONE && this.config.isPlainsSide(player.getLocation().getX(), player.getLocation().getZ());
        BiomeTeamManager.BiomeTeam newTeam = currentSide ? BiomeTeamManager.BiomeTeam.DESERT : BiomeTeamManager.BiomeTeam.PLAINS;
        this.teamManager.assign(player.getName(), newTeam);
        if (this.syncPlayerToAssignedSide(player)) {
            return true;
        }
        this.teamManager.assign(player.getName(), previousTeam);
        return false;
    }

    public boolean syncPlayerToAssignedSide(Player player) {
        Boolean assignedPlainsSide = this.teamManager.getAssignedPlainsSide(player.getName());
        if (assignedPlainsSide == null) {
            this.rememberCurrentSide(player, player.getLocation());
            return true;
        }
        if (!player.getWorld().getName().equals(this.config.getWorldName())) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        Location destination = player.getLocation().clone();
        this.dividerSides.put(playerId, assignedPlainsSide);
        if (this.config.isPlainsSide(destination.getX(), destination.getZ()) == assignedPlainsSide) {
            return true;
        }
        boolean dividerIsX = "x".equals(this.config.getDividerAxis());
        if (dividerIsX) {
            destination.setX(this.config.clampToSide(destination.getX(), destination.getZ(), assignedPlainsSide, true));
        } else {
            destination.setZ(this.config.clampToSide(destination.getX(), destination.getZ(), assignedPlainsSide, false));
        }
        return player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public void clearStoredSides() {
        this.dividerSides.clear();
    }

    private void warn(Player player, Map<UUID, Long> cooldowns, String messageKey) {
        long now = System.currentTimeMillis();
        long cooldownMillis = (long)this.config.getWarnCooldownSeconds() * 1000L;
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cooldownMillis) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        player.sendMessage(this.config.message(messageKey));
    }
}
