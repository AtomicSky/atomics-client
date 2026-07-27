/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.configuration.file.FileConfiguration
 */
package de.derjannik.islandborder;

import de.derjannik.islandborder.IslandBorderPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;

public final class BorderConfig {
    private static final double PUSH_BACK_DISTANCE = 1.0;
    private final IslandBorderPlugin plugin;
    private String worldName;
    private double minX;
    private double maxX;
    private double minZ;
    private double maxZ;
    private String dividerAxis;
    private double dividerCoordinate;
    private String dividerPositiveSide;
    private boolean dividerEnforced;
    private Color plainsColor;
    private Color desertColor;
    private Color dividerColor;
    private boolean enabled;
    private boolean pushBack;
    private int warnCooldownSeconds;
    private int particleRenderDistance;
    private double particleSpacing;
    private int particleUpdateIntervalTicks;
    private int particleLayers;
    private double particleLayerSpacing;
    private int particleLayerDistance;
    private boolean protectionEnabled;
    private boolean protectBlockPlace;
    private boolean protectBlockBreak;
    private boolean protectInteract;
    private boolean protectCombat;
    private boolean protectProjectiles;
    private boolean protectDroppedItems;
    private boolean protectPistons;
    private boolean protectVehicles;
    private int entityCheckIntervalTicks;

    public BorderConfig(IslandBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.plugin.reloadConfig();
        FileConfiguration cfg = this.plugin.getConfig();
        this.worldName = cfg.getString("world", "world");
        this.minX = Math.min(cfg.getDouble("outer.x1"), cfg.getDouble("outer.x2"));
        this.maxX = Math.max(cfg.getDouble("outer.x1"), cfg.getDouble("outer.x2"));
        this.minZ = Math.min(cfg.getDouble("outer.z1"), cfg.getDouble("outer.z2"));
        this.maxZ = Math.max(cfg.getDouble("outer.z1"), cfg.getDouble("outer.z2"));
        this.dividerAxis = cfg.getString("divider.axis", "x").equalsIgnoreCase("z") ? "z" : "x";
        this.dividerCoordinate = cfg.getDouble("divider.coordinate", 0.0);
        this.dividerPositiveSide = cfg.getString("divider.positive-side", "plains").equalsIgnoreCase("desert") ? "desert" : "plains";
        this.dividerEnforced = cfg.getBoolean("divider.enforce", false);
        this.plainsColor = this.parseColor(cfg.getString("colors.plains", "60,220,60"), Color.fromRGB((int)60, (int)220, (int)60));
        this.desertColor = this.parseColor(cfg.getString("colors.desert", "230,150,40"), Color.fromRGB((int)230, (int)150, (int)40));
        this.dividerColor = this.parseColor(cfg.getString("colors.divider-line", "255,60,60"), Color.fromRGB((int)255, (int)60, (int)60));
        this.enabled = cfg.getBoolean("border.enabled", true);
        this.pushBack = cfg.getBoolean("border.push-back", true);
        this.warnCooldownSeconds = cfg.getInt("border.warn-cooldown-seconds", 3);
        this.particleRenderDistance = cfg.getInt("border.particle-render-distance", 24);
        this.particleSpacing = cfg.getDouble("border.particle-spacing", 0.5);
        this.particleUpdateIntervalTicks = cfg.getInt("border.particle-update-interval-ticks", 5);
        this.particleLayers = Math.max(0, Math.min(10, cfg.getInt("border.particle-layers", 4)));
        this.particleLayerSpacing = Math.max(0.25, cfg.getDouble("border.particle-layer-spacing", 0.5));
        this.particleLayerDistance = cfg.getInt("border.particle-layer-distance", 12);
        if (this.particleLayerDistance <= 0 || this.particleLayerDistance > this.particleRenderDistance) {
            this.particleLayerDistance = this.particleRenderDistance;
        }
        this.protectionEnabled = cfg.getBoolean("protection.enabled", true);
        this.protectBlockPlace = cfg.getBoolean("protection.block-place", true);
        this.protectBlockBreak = cfg.getBoolean("protection.block-break", true);
        this.protectInteract = cfg.getBoolean("protection.interact", true);
        this.protectCombat = cfg.getBoolean("protection.combat", true);
        this.protectProjectiles = cfg.getBoolean("protection.projectiles", true);
        this.protectDroppedItems = cfg.getBoolean("protection.dropped-items", true);
        this.protectPistons = cfg.getBoolean("protection.pistons", true);
        this.protectVehicles = cfg.getBoolean("protection.vehicles", true);
        this.entityCheckIntervalTicks = Math.max(1, cfg.getInt("protection.entity-check-interval-ticks", 2));
    }

    public void save() {
        FileConfiguration cfg = this.plugin.getConfig();
        cfg.set("world", (Object)this.worldName);
        cfg.set("outer.x1", (Object)this.minX);
        cfg.set("outer.z1", (Object)this.minZ);
        cfg.set("outer.x2", (Object)this.maxX);
        cfg.set("outer.z2", (Object)this.maxZ);
        cfg.set("divider.axis", (Object)this.dividerAxis);
        cfg.set("divider.coordinate", (Object)this.dividerCoordinate);
        cfg.set("divider.positive-side", (Object)this.dividerPositiveSide);
        cfg.set("divider.enforce", (Object)this.dividerEnforced);
        cfg.set("border.enabled", (Object)this.enabled);
        cfg.set("border.particle-layers", (Object)this.particleLayers);
        cfg.set("border.particle-layer-spacing", (Object)this.particleLayerSpacing);
        cfg.set("border.particle-layer-distance", (Object)this.particleLayerDistance);
        cfg.set("protection.enabled", (Object)this.protectionEnabled);
        this.plugin.saveConfig();
    }

    private Color parseColor(String raw, Color fallback) {
        if (raw == null) {
            return fallback;
        }
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return fallback;
        }
        try {
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return Color.fromRGB((int)this.clamp(r), (int)this.clamp(g), (int)this.clamp(b));
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public String message(String key) {
        String prefix = this.plugin.getConfig().getString("messages.prefix", "");
        String msg = this.plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(prefix + msg));
    }

    public boolean isInsideOuter(double x, double z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    public double clampX(double x) {
        return Math.max(this.minX + PUSH_BACK_DISTANCE, Math.min(this.maxX - PUSH_BACK_DISTANCE, x));
    }

    public double clampZ(double z) {
        return Math.max(this.minZ + PUSH_BACK_DISTANCE, Math.min(this.maxZ - PUSH_BACK_DISTANCE, z));
    }

    public boolean isPlainsSide(double x, double z) {
        double coordinateOnAxis = "x".equals(this.dividerAxis) ? x : z;
        boolean isPositiveSide = coordinateOnAxis >= this.dividerCoordinate;
        boolean positiveSideIsPlains = "plains".equals(this.dividerPositiveSide);
        return isPositiveSide == positiveSideIsPlains;
    }

    public double clampToSide(double x, double z, boolean wantPlainsSide, boolean isX) {
        boolean wantPositiveSide;
        double value = isX ? x : z;
        boolean positiveSideIsPlains = "plains".equals(this.dividerPositiveSide);
        boolean bl = wantPositiveSide = wantPlainsSide == positiveSideIsPlains;
        if (wantPositiveSide) {
            return Math.max(value, this.dividerCoordinate + PUSH_BACK_DISTANCE);
        }
        return Math.min(value, this.dividerCoordinate - PUSH_BACK_DISTANCE);
    }

    public String getWorldName() {
        return this.worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getMinX() {
        return this.minX;
    }

    public double getMaxX() {
        return this.maxX;
    }

    public double getMinZ() {
        return this.minZ;
    }

    public double getMaxZ() {
        return this.maxZ;
    }

    public void setOuter(double x1, double z1, double x2, double z2) {
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);
    }

    public String getDividerAxis() {
        return this.dividerAxis;
    }

    public double getDividerCoordinate() {
        return this.dividerCoordinate;
    }

    public String getDividerPositiveSide() {
        return this.dividerPositiveSide;
    }

    public void setDivider(String axis, double coordinate) {
        this.dividerAxis = "z".equalsIgnoreCase(axis) ? "z" : "x";
        this.dividerCoordinate = coordinate;
    }

    public boolean isDividerEnforced() {
        return this.dividerEnforced;
    }

    public void setDividerEnforced(boolean enforced) {
        this.dividerEnforced = enforced;
    }

    public Color getPlainsColor() {
        return this.plainsColor;
    }

    public Color getDesertColor() {
        return this.desertColor;
    }

    public Color getDividerColor() {
        return this.dividerColor;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPushBack() {
        return this.pushBack;
    }

    public int getWarnCooldownSeconds() {
        return this.warnCooldownSeconds;
    }

    public int getParticleRenderDistance() {
        return this.particleRenderDistance;
    }

    public double getParticleSpacing() {
        return this.particleSpacing;
    }

    public int getParticleUpdateIntervalTicks() {
        return this.particleUpdateIntervalTicks;
    }

    public int getParticleLayers() {
        return this.particleLayers;
    }

    public double getParticleLayerSpacing() {
        return this.particleLayerSpacing;
    }

    public int getParticleLayerDistance() {
        return this.particleLayerDistance;
    }

    public void setParticleLayers(int layers) {
        this.particleLayers = Math.max(0, Math.min(10, layers));
    }

    public void setParticleLayerSpacing(double spacing) {
        this.particleLayerSpacing = Math.max(0.25, spacing);
    }

    public boolean isProtectionEnabled() {
        return this.protectionEnabled;
    }

    public void setProtectionEnabled(boolean enabled) {
        this.protectionEnabled = enabled;
    }

    public boolean isProtectBlockPlace() {
        return this.protectBlockPlace;
    }

    public boolean isProtectBlockBreak() {
        return this.protectBlockBreak;
    }

    public boolean isProtectInteract() {
        return this.protectInteract;
    }

    public boolean isProtectCombat() {
        return this.protectCombat;
    }

    public boolean isProtectProjectiles() {
        return this.protectProjectiles;
    }

    public boolean isProtectDroppedItems() {
        return this.protectDroppedItems;
    }

    public boolean isProtectPistons() {
        return this.protectPistons;
    }

    public boolean isProtectVehicles() {
        return this.protectVehicles;
    }

    public int getEntityCheckIntervalTicks() {
        return this.entityCheckIntervalTicks;
    }

    public boolean crossesBoundary(double fromX, double fromZ, double toX, double toZ) {
        if (!this.isInsideOuter(toX, toZ)) {
            return true;
        }
        return this.dividerEnforced && this.isPlainsSide(fromX, fromZ) != this.isPlainsSide(toX, toZ);
    }

    public void setParticleLayerDistance(int distance) {
        this.particleLayerDistance = distance <= 0 || distance > this.particleRenderDistance ? this.particleRenderDistance : distance;
    }
}
