/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Projectile
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.BorderConfig;
import de.derjannik.islandborder.IslandBorderPlugin;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class BorderEntityTask
extends BukkitRunnable {
    private final IslandBorderPlugin plugin;
    private final BorderConfig config;
    private Map<UUID, double[]> lastSeen = new HashMap<UUID, double[]>();

    public BorderEntityTask(IslandBorderPlugin plugin, BorderConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void run() {
        if (!this.config.isEnabled() || !this.config.isProtectionEnabled()) {
            if (!this.lastSeen.isEmpty()) {
                this.lastSeen = new HashMap<UUID, double[]>();
            }
            return;
        }
        boolean watchProjectiles = this.config.isProtectProjectiles();
        boolean watchItems = this.config.isProtectDroppedItems();
        if (!watchProjectiles && !watchItems) {
            if (!this.lastSeen.isEmpty()) {
                this.lastSeen = new HashMap<UUID, double[]>();
            }
            return;
        }
        World world = this.plugin.getServer().getWorld(this.config.getWorldName());
        if (world == null) {
            return;
        }
        Collection<Entity> entities = world.getEntitiesByClasses(Projectile.class, Item.class);
        HashMap<UUID, double[]> stillAlive = new HashMap<UUID, double[]>();
        for (Entity entity : entities) {
            boolean isItem = entity instanceof Item;
            if (isItem && !watchItems || !isItem && !watchProjectiles) continue;
            UUID id = entity.getUniqueId();
            Location current = entity.getLocation();
            double[] previous = this.lastSeen.get(id);
            if (previous == null) {
                stillAlive.put(id, new double[]{current.getX(), current.getY(), current.getZ()});
                continue;
            }
            if (this.config.crossesBoundary(previous[0], previous[2], current.getX(), current.getZ())) {
                if (isItem) {
                    entity.setVelocity(new Vector(0, 0, 0));
                    entity.teleport(new Location(world, previous[0], previous[1], previous[2]));
                    stillAlive.put(id, previous);
                    continue;
                }
                entity.remove();
                continue;
            }
            stillAlive.put(id, new double[]{current.getX(), current.getY(), current.getZ()});
        }
        this.lastSeen = stillAlive;
    }
}
