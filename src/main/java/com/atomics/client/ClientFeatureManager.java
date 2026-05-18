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
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ClientFeatureManager {
    private static final long REACH_DISPLAY_MS = 1400L;
    private static final float ZOOM_MIN = 1.5f;
    private static final float ZOOM_MAX = 8.0f;
    private static final float ZOOM_SCROLL_STEP = 0.25f;

    private static long lastReachMillis;
    private static String lastReachTextString = "";
    private static Text lastReachText = Text.empty();
    private static int lastReachTextWidth = -1;
    private static int trailTick;
    private static boolean fullBrightApplied;
    private static boolean timeChangerApplied;
    private static float currentZoomMultiplier = 1.0f;
    private static float targetZoomMultiplier = 1.0f;
    private static long lastZoomUpdateNanos;
    private static long lastZoomFeedbackMillis;
    private static final Map<UUID, String> MASKED_PLAYER_NAMES = new HashMap<>();
    private static final Text[] TNT_TIMER_TEXT_CACHE = new Text[2001];
    private static final List<PreparedParticleBurst> PREPARED_PROJECTILE_TRAIL_PARTICLES = new ArrayList<>();
    private static boolean projectileTrailParticleCacheInitialized;
    private static int projectileTrailParticleFingerprint;

    private ClientFeatureManager() {
    }

    public static void tick(MinecraftClient client) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || client == null || client.player == null || client.world == null) {
            updateZoom(cfg);
            if (client == null || client.world == null) {
                MASKED_PLAYER_NAMES.clear();
            }
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
            applyFullBrightIfNeeded(client);
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

        lastReachMillis = System.currentTimeMillis();
        lastReachTextString = String.format(Locale.US, "%.2fm", reach);
        lastReachText = Text.literal(lastReachTextString);
        lastReachTextWidth = -1;
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

        double dx = eyePos.x - closestX;
        double dy = eyePos.y - closestY;
        double dz = eyePos.z - closestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
        UUID uuid = entity.getUuid();
        String cached = MASKED_PLAYER_NAMES.get(uuid);
        if (cached != null) {
            return cached;
        }

        if (MASKED_PLAYER_NAMES.size() > 512) {
            MASKED_PLAYER_NAMES.clear();
        }
        String maskedName = "Player " + (Math.floorMod(uuid.hashCode(), 900) + 100);
        MASKED_PLAYER_NAMES.put(uuid, maskedName);
        return maskedName;
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
        float scale = 0.72f;
        if (lastReachTextWidth < 0) {
            lastReachTextWidth = client.textRenderer.getWidth(lastReachTextString);
        }
        int x = client.getWindow().getScaledWidth() / 2 - Math.round(lastReachTextWidth * scale / 2.0f);
        int y = client.getWindow().getScaledHeight() / 2 + 12;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(client.textRenderer, lastReachText, Math.round(x / scale), Math.round(y / scale), color);
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
        int fuse = Math.max(0, tnt.getFuse());
        if (fuse < TNT_TIMER_TEXT_CACHE.length) {
            Text cached = TNT_TIMER_TEXT_CACHE[fuse];
            if (cached != null) {
                return cached;
            }
            Text created = createTntTimerText(fuse);
            TNT_TIMER_TEXT_CACHE[fuse] = created;
            return created;
        }
        return createTntTimerText(fuse);
    }

    private static Text createTntTimerText(int fuse) {
        float seconds = fuse / 20.0f;
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
        List<PreparedParticleBurst> preparedParticles = prepareProjectileTrailParticles(visual.projectileTrailParticles);
        if (preparedParticles.isEmpty()) {
            return;
        }

        double rangeSq = 128.0 * 128.0;
        Random random = client.world.random;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity) || entity.squaredDistanceTo(client.player) > rangeSq) {
                continue;
            }
            if (entity.getVelocity().lengthSquared() < 0.0004) {
                continue;
            }

            for (PreparedParticleBurst prepared : preparedParticles) {
                TpsConfig.ParticleBurst burst = prepared.burst;
                ParticleEffect effect = prepared.effect;
                int count = prepared.count;
                for (int i = 0; i < count; i++) {
                    double x = entity.getX() + (random.nextDouble() - 0.5) * burst.spreadX;
                    double y = entity.getY() + (random.nextDouble() - 0.5) * burst.spreadY;
                    double z = entity.getZ() + (random.nextDouble() - 0.5) * burst.spreadZ;
                    double vx = (random.nextDouble() - 0.5) * burst.speed;
                    double vy = (random.nextDouble() - 0.5) * burst.speed;
                    double vz = (random.nextDouble() - 0.5) * burst.speed;
                    client.particleManager.addParticle(effect, x, y, z, vx, vy, vz);
                }
            }
        }
    }

    private static List<PreparedParticleBurst> prepareProjectileTrailParticles(List<TpsConfig.ParticleBurst> bursts) {
        int fingerprint = particleBurstFingerprint(bursts);
        if (projectileTrailParticleCacheInitialized && fingerprint == projectileTrailParticleFingerprint) {
            return PREPARED_PROJECTILE_TRAIL_PARTICLES;
        }

        projectileTrailParticleCacheInitialized = true;
        projectileTrailParticleFingerprint = fingerprint;
        PREPARED_PROJECTILE_TRAIL_PARTICLES.clear();
        for (TpsConfig.ParticleBurst burst : bursts) {
            if (burst == null) {
                continue;
            }
            int count = Math.max(0, burst.count);
            if (count <= 0) {
                continue;
            }
            ParticleEffect effect = TotemPopEffects.getParticleEffect(burst.particle);
            if (effect != null) {
                PREPARED_PROJECTILE_TRAIL_PARTICLES.add(new PreparedParticleBurst(burst, effect, count));
            }
        }
        return PREPARED_PROJECTILE_TRAIL_PARTICLES;
    }

    private static int particleBurstFingerprint(List<TpsConfig.ParticleBurst> bursts) {
        int result = bursts.size();
        for (TpsConfig.ParticleBurst burst : bursts) {
            if (burst == null) {
                result = 31 * result;
                continue;
            }
            result = 31 * result + (burst.particle == null ? 0 : burst.particle.hashCode());
            result = 31 * result + burst.count;
            result = 31 * result + Double.hashCode(burst.spreadX);
            result = 31 * result + Double.hashCode(burst.spreadY);
            result = 31 * result + Double.hashCode(burst.spreadZ);
            result = 31 * result + Double.hashCode(burst.speed);
        }
        return result;
    }

    private static void updateZoom(TpsConfig cfg) {
        targetZoomMultiplier = cfg != null && cfg.visual.zoomEnabled && AtomicsClient.isZoomKeyPressed()
                ? clampFloat(cfg.visual.zoomMultiplier, ZOOM_MIN, ZOOM_MAX)
                : 1.0f;
        if (targetZoomMultiplier == 1.0f && currentZoomMultiplier == 1.0f) {
            lastZoomUpdateNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        float elapsedSeconds = lastZoomUpdateNanos == 0L ? 0.05f : Math.min(0.2f, (now - lastZoomUpdateNanos) / 1_000_000_000.0f);
        lastZoomUpdateNanos = now;
        float amount = Math.min(1.0f, Math.max(0.0f, elapsedSeconds * 14.0f));
        currentZoomMultiplier += (targetZoomMultiplier - currentZoomMultiplier) * amount;
        if (Math.abs(targetZoomMultiplier - currentZoomMultiplier) < 0.01f) {
            currentZoomMultiplier = targetZoomMultiplier;
        }
    }

    private static void applyFullBrightIfNeeded(MinecraftClient client) {
        StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (effect == null
                || effect.getAmplifier() != 0
                || effect.getDuration() < 220
                || effect.shouldShowParticles()
                || effect.shouldShowIcon()) {
            client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, false, false, false));
        }
        fullBrightApplied = true;
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

    private record PreparedParticleBurst(TpsConfig.ParticleBurst burst, ParticleEffect effect, int count) {
    }
}
