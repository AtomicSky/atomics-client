/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.scheduler.BukkitRunnable
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.BorderConfig;
import de.derjannik.islandborder.IslandBorderPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class BorderParticleTask
extends BukkitRunnable {
    private final IslandBorderPlugin plugin;
    private final BorderConfig config;

    public BorderParticleTask(IslandBorderPlugin plugin, BorderConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void run() {
        if (!this.config.isEnabled()) {
            return;
        }
        World world = this.plugin.getServer().getWorld(this.config.getWorldName());
        if (world == null) {
            return;
        }
        double spacing = Math.max(0.1, this.config.getParticleSpacing());
        double renderDistance = this.config.getParticleRenderDistance();
        double layerDistance = this.config.getParticleLayerDistance();
        double minX = this.config.getMinX();
        double maxX = this.config.getMaxX();
        double minZ = this.config.getMinZ();
        double maxZ = this.config.getMaxZ();
        double dividerCoord = this.config.getDividerCoordinate();
        boolean dividerIsX = "x".equals(this.config.getDividerAxis());
        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            double px = loc.getX();
            double pz = loc.getZ();
            double py = loc.getY();
            this.renderOuterEdges(player, world, minX, maxX, minZ, maxZ, px, py, pz, renderDistance, layerDistance, spacing);
            this.renderDivider(player, world, minX, maxX, minZ, maxZ, dividerCoord, dividerIsX, px, py, pz, renderDistance, layerDistance, spacing);
        }
    }

    private void renderOuterEdges(Player player, World world, double minX, double maxX, double minZ, double maxZ, double px, double py, double pz, double renderDistance, double layerDistance, double spacing) {
        double distSouth;
        double distNorth;
        double distEast;
        double distWest = Math.abs(px - minX);
        if (distWest <= renderDistance) {
            this.drawEdgeLine(player, world, minX, minZ, maxZ, true, py, spacing, renderDistance, distWest <= layerDistance);
        }
        if ((distEast = Math.abs(px - maxX)) <= renderDistance) {
            this.drawEdgeLine(player, world, maxX, minZ, maxZ, true, py, spacing, renderDistance, distEast <= layerDistance);
        }
        if ((distNorth = Math.abs(pz - minZ)) <= renderDistance) {
            this.drawEdgeLine(player, world, minZ, minX, maxX, false, py, spacing, renderDistance, distNorth <= layerDistance);
        }
        if ((distSouth = Math.abs(pz - maxZ)) <= renderDistance) {
            this.drawEdgeLine(player, world, maxZ, minX, maxX, false, py, spacing, renderDistance, distSouth <= layerDistance);
        }
    }

    private void drawEdgeLine(Player player, World world, double fixedCoord, double rangeStart, double rangeEnd, boolean edgeIsVertical, double y, double spacing, double renderDistance, boolean withLayers) {
        Location loc = player.getLocation();
        double along = edgeIsVertical ? loc.getZ() : loc.getX();
        double start = Math.max(rangeStart, along - renderDistance);
        double end = Math.min(rangeEnd, along + renderDistance);
        for (double t = start; t <= end; t += spacing) {
            double x = edgeIsVertical ? fixedCoord : t;
            double z = edgeIsVertical ? t : fixedCoord;
            Color color = this.config.isPlainsSide(x, z) ? this.config.getPlainsColor() : this.config.getDesertColor();
            this.spawnColumn(player, world, x, y, z, color, withLayers);
        }
    }

    private void renderDivider(Player player, World world, double minX, double maxX, double minZ, double maxZ, double dividerCoord, boolean dividerIsX, double px, double py, double pz, double renderDistance, double layerDistance, double spacing) {
        double distToDivider;
        double d = distToDivider = dividerIsX ? Math.abs(px - dividerCoord) : Math.abs(pz - dividerCoord);
        if (distToDivider > renderDistance) {
            return;
        }
        boolean withLayers = distToDivider <= layerDistance;
        double rangeStart = dividerIsX ? minZ : minX;
        double rangeEnd = dividerIsX ? maxZ : maxX;
        double playerAlongAxis = dividerIsX ? pz : px;
        double start = Math.max(rangeStart, playerAlongAxis - renderDistance);
        double end = Math.min(rangeEnd, playerAlongAxis + renderDistance);
        for (double t = start; t <= end; t += spacing) {
            double x = dividerIsX ? dividerCoord : t;
            double z = dividerIsX ? t : dividerCoord;
            this.spawnColumn(player, world, x, py, z, this.config.getDividerColor(), withLayers);
        }
    }

    private void spawnColumn(Player player, World world, double x, double y, double z, Color color, boolean withLayers) {
        Particle.DustOptions options = new Particle.DustOptions(color, 1.0f);
        double baseY = y + 0.1;
        this.spawnDust(player, world, x, baseY, z, options);
        if (!withLayers) {
            return;
        }
        int layers = this.config.getParticleLayers();
        if (layers <= 0) {
            return;
        }
        double gap = Math.max(0.25, this.config.getParticleLayerSpacing());
        for (int i = 1; i <= layers; ++i) {
            double offset = (double)i * gap;
            this.spawnDust(player, world, x, baseY + offset, z, options);
            this.spawnDust(player, world, x, baseY - offset, z, options);
        }
    }

    private void spawnDust(Player player, World world, double x, double y, double z, Particle.DustOptions options) {
        if (y < (double)world.getMinHeight() || y > (double)world.getMaxHeight()) {
            return;
        }
        player.spawnParticle(Particle.DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, (Object)options);
    }
}

