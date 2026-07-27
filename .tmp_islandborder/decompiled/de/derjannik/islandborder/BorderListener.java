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
import org.bukkit.event.player.PlayerTeleportEvent;

public final class BorderListener
implements Listener {
    private final IslandBorderPlugin plugin;
    private final BorderConfig config;
    private final Map<UUID, Long> lastWarnOuter = new HashMap<UUID, Long>();
    private final Map<UUID, Long> lastWarnDivider = new HashMap<UUID, Long>();

    public BorderListener(IslandBorderPlugin plugin, BorderConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        boolean isPlainsSide;
        Location from;
        boolean wasPlainsSide;
        double z;
        boolean outsideOuter;
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
            return;
        }
        if (!player.getWorld().getName().equals(this.config.getWorldName())) {
            return;
        }
        Location to = event.getTo();
        double x = to.getX();
        boolean bl = outsideOuter = !this.config.isInsideOuter(x, z = to.getZ());
        if (outsideOuter) {
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
        if (this.config.isDividerEnforced() && (wasPlainsSide = this.config.isPlainsSide((from = event.getFrom()).getX(), from.getZ())) != (isPlainsSide = this.config.isPlainsSide(x, z))) {
            if (!this.config.isPushBack()) {
                this.warn(player, this.lastWarnDivider, "hit-divider");
                return;
            }
            Location fixed = to.clone();
            boolean isX = "x".equals(this.config.getDividerAxis());
            if (isX) {
                fixed.setX(this.config.clampToSide(x, z, wasPlainsSide, true));
            } else {
                fixed.setZ(this.config.clampToSide(x, z, wasPlainsSide, false));
            }
            event.setTo(fixed);
            this.warn(player, this.lastWarnDivider, "hit-divider");
        }
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location from;
        if (!this.config.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("islandborder.bypass")) {
            return;
        }
        Location to = event.getTo();
        if (to == null || !to.getWorld().getName().equals(this.config.getWorldName())) {
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
        if (this.config.isDividerEnforced() && (from = event.getFrom()) != null && this.config.isPlainsSide(from.getX(), from.getZ()) != this.config.isPlainsSide(to.getX(), to.getZ())) {
            event.setCancelled(true);
            this.warn(player, this.lastWarnDivider, "hit-divider");
        }
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

