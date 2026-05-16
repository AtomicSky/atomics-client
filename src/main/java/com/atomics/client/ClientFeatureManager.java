package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class ClientFeatureManager {
    private static final long REACH_DISPLAY_MS = 1400L;
    private static final float ZOOM_MIN = 1.5f;
    private static final float ZOOM_MAX = 8.0f;
    private static final float ZOOM_SCROLL_STEP = 0.25f;

    private static double lastReach;
    private static long lastReachMillis;
    private static int trailTick;
    private static boolean fullBrightApplied;
    private static boolean timeChangerApplied;
    private static float currentZoomMultiplier = 1.0f;
    private static float targetZoomMultiplier = 1.0f;
    private static long lastZoomUpdateNanos;
    private static long lastZoomFeedbackMillis;

    private ClientFeatureManager() {
    }

    public static void tick(MinecraftClient client) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || client == null || client.player == null || client.world == null) {
            updateZoom(cfg);
            if (client != null && client.player != null) {
                disableFullBrightIfNeeded(client);
            }
            if (client != null && client.world != null) {
                disableTimeChangerIfNeeded(client);
            }
            return;
        }

        updateZoom(cfg);

        if (cfg.visual.fullBrightEnabled) {
            client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, false, false, false));
            fullBrightApplied = true;
        } else {
            disableFullBrightIfNeeded(client);
        }

        if (cfg.visual.timeChangerEnabled) {
            client.world.setTime(client.world.getTime(), cfg.visual.timeOfDay, false);
            timeChangerApplied = true;
        } else {
            disableTimeChangerIfNeeded(client);
        }

        if (cfg.visual.projectileTrailEnabled) {
            spawnProjectileTrails(client, cfg.visual);
        }
    }

    /**
     * Records reach the same way ReachDisplay does: distance from the attacking
     * player's eye position to the closest point on the target's bounding box.
     *
     * This avoids entity-center / feet-to-feet distances, which can incorrectly
     * show values above normal Minecraft melee reach.
     */
    public static void onReachAttack(PlayerEntity player, Entity target) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || player == null || target == null || !cfg.combat.reachDisplayEnabled) {
            return;
        }

        double reach = calculateClosestPointReach(player, target);
        if (!Double.isFinite(reach)) {
            return;
        }

        lastReach = reach;
        lastReachMillis = System.currentTimeMillis();
    }

    /**
     * Kept for older call sites, but reach should be captured at attack time.
     * Server-confirmed damage can arrive after the target has moved, which is
     * what caused impossible displayed reach values.
     */
    public static void onHitLanded(Entity target) {
        // Intentionally unused. Reach is recorded from ClientPlayerInteractionManagerMixin.
    }

    private static double calculateClosestPointReach(PlayerEntity player, Entity target) {
        Vec3d eyePos = player.getEyePos();
        Box box = target.getBoundingBox();

        double closestX = clampDouble(eyePos.x, box.minX, box.maxX);
        double closestY = clampDouble(eyePos.y, box.minY, box.maxY);
        double closestZ = clampDouble(eyePos.z, box.minZ, box.maxZ);

        return eyePos.distanceTo(new Vec3d(closestX, closestY, closestZ));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static void renderHud(DrawContext context) {
        TpsConfig cfg = liveConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cfg == null || client == null || client.player == null || client.world == null) {
            return;
        }

        if (cfg.combat.reachDisplayEnabled) {
            renderReachDisplay(context, client);
        }
    }

    public static boolean shouldMaskPlayerNames() {
        TpsConfig cfg = liveConfig();
        return cfg != null && cfg.visual.streamerModeEnabled;
    }

    public static String maskedPlayerName(Entity entity) {
        if (entity == null) return "Player";
        return "Player " + (Math.floorMod(entity.getUuid().hashCode(), 900) + 100);
    }

    public static boolean isZoomActive() {
        updateZoom(liveConfig());
        return currentZoomMultiplier > 1.01f || targetZoomMultiplier > 1.01f;
    }

    public static float getZoomFovMultiplier() {
        updateZoom(liveConfig());
        return 1.0f / Math.max(1.0f, currentZoomMultiplier);
    }

    public static boolean onZoomScroll(double verticalAmount) {
        MinecraftClient client = MinecraftClient.getInstance();
        TpsConfig cfg = liveConfig();
        if (client == null || client.currentScreen != null || cfg == null || !cfg.visual.zoomEnabled || !AtomicsClient.isZoomKeyPressed()) {
            return false;
        }
        if (Math.abs(verticalAmount) < 0.0001) {
            return false;
        }

        cfg.visual.zoomMultiplier = clampFloat((float) (cfg.visual.zoomMultiplier + verticalAmount * ZOOM_SCROLL_STEP), ZOOM_MIN, ZOOM_MAX);
        if (client.player != null) {
            long now = System.currentTimeMillis();
            if (now - lastZoomFeedbackMillis > 70L) {
                client.player.sendMessage(Text.literal("Zoom: " + String.format(Locale.US, "%.2fx", cfg.visual.zoomMultiplier)).formatted(Formatting.AQUA), true);
                lastZoomFeedbackMillis = now;
            }
        }
        return true;
    }

    public static void runMacro(MinecraftClient client, int index) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || !cfg.macros.enabled || client == null || client.getNetworkHandler() == null) {
            return;
        }
        if (index < 0 || index >= cfg.macros.messages.length) {
            return;
        }

        String message = cfg.macros.messages[index];
        if (message == null || message.isBlank()) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Macro " + (index + 1) + " is empty").formatted(Formatting.YELLOW), true);
            }
            return;
        }

        message = message.trim();
        if (message.startsWith("/") && message.length() > 1) {
            client.getNetworkHandler().sendChatCommand(message.substring(1));
        } else {
            client.getNetworkHandler().sendChatMessage(message);
        }
    }

    private static void renderReachDisplay(DrawContext context, MinecraftClient client) {
        long age = System.currentTimeMillis() - lastReachMillis;
        if (lastReachMillis <= 0L || age > REACH_DISPLAY_MS) {
            return;
        }

        float alpha = 1.0f - Math.max(0.0f, (age - 900L) / 500.0f);
        int a = Math.max(30, Math.min(150, Math.round(alpha * 150.0f)));
        int color = (a << 24) | 0xB8B8B8;
        String text = String.format(Locale.US, "%.2fm", lastReach);
        float scale = 0.72f;
        int x = client.getWindow().getScaledWidth() / 2 - Math.round(client.textRenderer.getWidth(text) * scale / 2.0f);
        int y = client.getWindow().getScaledHeight() / 2 + 12;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), Math.round(x / scale), Math.round(y / scale), color);
        context.getMatrices().popMatrix();
    }

    public static boolean shouldShowTntTimer(TntEntity tnt) {
        TpsConfig cfg = liveConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cfg == null || client == null || client.player == null || tnt == null || !cfg.visual.tntTimerEnabled) {
            return false;
        }
        double rangeSq = cfg.visual.tntTimerRange * cfg.visual.tntTimerRange;
        return tnt.squaredDistanceTo(client.player) <= rangeSq;
    }

    public static Text tntTimerText(TntEntity tnt) {
        float seconds = Math.max(0.0f, tnt.getFuse() / 20.0f);
        Formatting color = seconds <= 1.0f ? Formatting.RED : seconds <= 2.0f ? Formatting.GOLD : Formatting.GRAY;
        return Text.literal(String.format(Locale.US, "%.1fs", seconds)).formatted(color);
    }

    private static void spawnProjectileTrails(MinecraftClient client, TpsConfig.VisualSettings visual) {
        if (++trailTick % 2 != 0) {
            return;
        }

        if (visual.projectileTrailParticles == null || visual.projectileTrailParticles.isEmpty()) {
            return;
        }
        double rangeSq = 128.0 * 128.0;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity) || entity.squaredDistanceTo(client.player) > rangeSq) {
                continue;
            }
            if (entity.getVelocity().lengthSquared() < 0.0004) {
                continue;
            }

            for (TpsConfig.ParticleBurst burst : visual.projectileTrailParticles) {
                ParticleEffect effect = TotemPopEffects.getParticleEffect(burst.particle);
                if (effect == null) {
                    continue;
                }
                int count = Math.max(0, burst.count);
                for (int i = 0; i < count; i++) {
                    double x = entity.getX() + (client.world.random.nextDouble() - 0.5) * burst.spreadX;
                    double y = entity.getY() + (client.world.random.nextDouble() - 0.5) * burst.spreadY;
                    double z = entity.getZ() + (client.world.random.nextDouble() - 0.5) * burst.spreadZ;
                    double vx = (client.world.random.nextDouble() - 0.5) * burst.speed;
                    double vy = (client.world.random.nextDouble() - 0.5) * burst.speed;
                    double vz = (client.world.random.nextDouble() - 0.5) * burst.speed;
                    client.particleManager.addParticle(effect, x, y, z, vx, vy, vz);
                }
            }
        }
    }

    private static void updateZoom(TpsConfig cfg) {
        targetZoomMultiplier = cfg != null && cfg.visual.zoomEnabled && AtomicsClient.isZoomKeyPressed()
                ? clampFloat(cfg.visual.zoomMultiplier, ZOOM_MIN, ZOOM_MAX)
                : 1.0f;
        long now = System.nanoTime();
        float elapsedSeconds = lastZoomUpdateNanos == 0L ? 0.05f : Math.min(0.2f, (now - lastZoomUpdateNanos) / 1_000_000_000.0f);
        lastZoomUpdateNanos = now;
        float amount = Math.min(1.0f, Math.max(0.0f, elapsedSeconds * 14.0f));
        currentZoomMultiplier += (targetZoomMultiplier - currentZoomMultiplier) * amount;
        if (Math.abs(targetZoomMultiplier - currentZoomMultiplier) < 0.01f) {
            currentZoomMultiplier = targetZoomMultiplier;
        }
    }

    private static void disableFullBrightIfNeeded(MinecraftClient client) {
        if (!fullBrightApplied || client.player == null) {
            return;
        }
        StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (effect != null
                && effect.getAmplifier() == 0
                && effect.getDuration() <= 280
                && !effect.shouldShowParticles()
                && !effect.shouldShowIcon()) {
            client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        fullBrightApplied = false;
    }

    private static void disableTimeChangerIfNeeded(MinecraftClient client) {
        if (!timeChangerApplied || client.world == null) {
            return;
        }
        client.world.setTime(client.world.getTime(), client.world.getTimeOfDay(), true);
        timeChangerApplied = false;
    }

    private static float clampFloat(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static TpsConfig liveConfig() {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled) {
            return null;
        }
        return AtomicsClient.CONFIG;
    }
}
