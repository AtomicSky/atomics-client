/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Vehicle
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPistonExtendEvent
 *  org.bukkit.event.block.BlockPistonRetractEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.vehicle.VehicleMoveEvent
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.util.Vector
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.BorderConfig;
import de.derjannik.islandborder.IslandBorderPlugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class BorderProtectionListener
implements Listener {
    private final BorderConfig config;
    private final Map<UUID, Long> lastWarn = new HashMap<UUID, Long>();

    public BorderProtectionListener(IslandBorderPlugin plugin, BorderConfig config) {
        this.config = config;
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!this.config.isProtectBlockPlace()) {
            return;
        }
        this.cancelIfAcross(event.getPlayer(), event.getBlock().getLocation(), (Cancellable)event, "blocked-build");
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!this.config.isProtectBlockBreak()) {
            return;
        }
        this.cancelIfAcross(event.getPlayer(), event.getBlock().getLocation(), (Cancellable)event, "blocked-build");
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event) {
        if (!this.config.isProtectInteract()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        this.cancelIfAcross(event.getPlayer(), block.getLocation(), (Cancellable)event, "blocked-interact");
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!this.config.isProtectInteract()) {
            return;
        }
        this.cancelIfAcross(event.getPlayer(), event.getRightClicked().getLocation(), (Cancellable)event, "blocked-interact");
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity victim;
        ProjectileSource shooter;
        Entity damager;
        if (!this.config.isProtectCombat()) {
            return;
        }
        if (!this.isProtectionActive()) {
            return;
        }
        Entity source = damager = event.getDamager();
        if (damager instanceof Projectile && (shooter = ((Projectile)damager).getShooter()) instanceof Entity) {
            source = (Entity)shooter;
        }
        if (!this.inBorderWorld(victim = event.getEntity()) || !this.inBorderWorld(source)) {
            return;
        }
        if (this.hasBypass(source)) {
            return;
        }
        Location from = source.getLocation();
        Location to = victim.getLocation();
        if (this.config.crossesBoundary(from.getX(), from.getZ(), to.getX(), to.getZ())) {
            event.setCancelled(true);
            if (source instanceof Player) {
                this.warn((Player)source, "blocked-attack");
            }
        }
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!this.config.isProtectPistons()) {
            return;
        }
        if (this.pistonWouldCross(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!this.config.isProtectPistons()) {
            return;
        }
        if (this.pistonWouldCross(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    private boolean pistonWouldCross(Block piston, List<Block> moved, BlockFace direction) {
        if (!this.isProtectionActive()) {
            return false;
        }
        if (!this.inBorderWorld(piston.getLocation())) {
            return false;
        }
        int dx = direction.getModX();
        int dz = direction.getModZ();
        for (Block block : moved) {
            double z;
            double x = (double)block.getX() + 0.5;
            if (this.config.crossesBoundary(x, z = (double)block.getZ() + 0.5, x + (double)dx, z + (double)dz)) {
                return true;
            }
            if (!this.config.crossesBoundary(x, z, x - (double)dx, z - (double)dz)) continue;
            return true;
        }
        return false;
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!this.config.isProtectVehicles()) {
            return;
        }
        if (!this.isProtectionActive()) {
            return;
        }
        Vehicle vehicle = event.getVehicle();
        if (!this.inBorderWorld((Entity)vehicle)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
            return;
        }
        if (!this.config.crossesBoundary(from.getX(), from.getZ(), to.getX(), to.getZ())) {
            return;
        }
        for (Entity passenger : vehicle.getPassengers()) {
            if (!this.hasBypass(passenger)) continue;
            return;
        }
        vehicle.setVelocity(new Vector(0, 0, 0));
        vehicle.teleport(from);
        for (Entity passenger : vehicle.getPassengers()) {
            if (!(passenger instanceof Player)) continue;
            this.warn((Player)passenger, "hit-outer");
        }
    }

    private void cancelIfAcross(Player player, Location target, Cancellable event, String messageKey) {
        if (!this.isProtectionActive()) {
            return;
        }
        if (player.hasPermission("islandborder.bypass")) {
            return;
        }
        if (!player.getWorld().getName().equals(this.config.getWorldName())) {
            return;
        }
        Location from = player.getLocation();
        if (this.config.crossesBoundary(from.getX(), from.getZ(), target.getX() + 0.5, target.getZ() + 0.5)) {
            event.setCancelled(true);
            this.warn(player, messageKey);
        }
    }

    private boolean isProtectionActive() {
        return this.config.isEnabled() && this.config.isProtectionEnabled();
    }

    private boolean inBorderWorld(Entity entity) {
        return entity != null && entity.getWorld().getName().equals(this.config.getWorldName());
    }

    private boolean inBorderWorld(Location location) {
        World world = location.getWorld();
        return world != null && world.getName().equals(this.config.getWorldName());
    }

    private boolean hasBypass(Entity entity) {
        return entity instanceof Player && ((Player)entity).hasPermission("islandborder.bypass");
    }

    private void warn(Player player, String messageKey) {
        long now = System.currentTimeMillis();
        long cooldownMillis = (long)this.config.getWarnCooldownSeconds() * 1000L;
        Long last = this.lastWarn.get(player.getUniqueId());
        if (last != null && now - last < cooldownMillis) {
            return;
        }
        this.lastWarn.put(player.getUniqueId(), now);
        player.sendMessage(this.config.message(messageKey));
    }
}

