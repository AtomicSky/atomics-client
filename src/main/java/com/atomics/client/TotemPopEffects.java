package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public class TotemPopEffects {
    private static final List<PendingSound> PENDING_SOUNDS = new ArrayList<>();
    private static final Map<String, ParticleOptions> PARTICLE_CACHE = new HashMap<>();
    private static final Map<String, SoundEvent> SOUND_CACHE = new HashMap<>();
    private static final int SHAPE_RANDOM = 0;
    private static final int SHAPE_SPHERE = 1;
    private static final int SHAPE_RING = 2;
    private static final int SHAPE_SPIRAL = 3;
    private static final int SHAPE_BEAM = 4;
    private static final int SHAPE_CONE = 5;

    public static void play(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (client.level == null || client.player == null || cfg == null) return;
        if (cfg.utility.onlyForSelf && entity != client.player) return;

        spawnParticles(client, entity, cfg);
        playSounds(client, entity, cfg);
    }

    public static void playParticles(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || AtomicsClient.CONFIG == null) return;
        if (AtomicsClient.CONFIG.utility.onlyForSelf && entity != client.player) return;
        spawnParticles(client, entity, AtomicsClient.CONFIG);
    }

    public static void playSounds(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || AtomicsClient.CONFIG == null) return;
        if (AtomicsClient.CONFIG.utility.onlyForSelf && entity != client.player) return;
        playSounds(client, entity, AtomicsClient.CONFIG);
    }

    private static void spawnParticles(Minecraft client, Entity entity, TpsConfig cfg) {
        if (!cfg.particles.enabled) return;

        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.55;
        double z = entity.getZ();
        List<String> disabledParticles = cfg.particles.disabledParticleIds;
        boolean hasDisabledParticles = disabledParticles != null && !disabledParticles.isEmpty();

        for (TpsConfig.ParticleBurst burst : cfg.particles.bursts) {
            if (hasDisabledParticles && disabledParticles.contains(burst.particle)) continue;
            ParticleOptions effect = getParticleEffect(burst.particle);
            if (effect == null) continue;

            int count = Math.max(0, burst.count);
            int shape = shapeId(burst.shape);
            double speed = Math.max(0.0, burst.speed);
            ParticleMotion motion = new ParticleMotion();
            for (int i = 0; i < count; i++) {
                createParticleMotion(client.level.random, burst, shape, speed, i, count, motion);
                client.particleEngine.createParticle(effect, x + motion.x, y + motion.y, z + motion.z, motion.vx, motion.vy, motion.vz);
            }
        }
    }

    public static ParticleOptions getParticleEffect(String particleId) {
        if (particleId == null || particleId.isBlank()) return null;
        if (PARTICLE_CACHE.containsKey(particleId)) {
            return PARTICLE_CACHE.get(particleId);
        }

        ParticleOptions effect = null;
        Identifier id = Identifier.tryParse(particleId);
        if (id != null) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
            if (type instanceof ParticleOptions particleEffect) {
                effect = particleEffect;
            }
        }
        PARTICLE_CACHE.put(particleId, effect);
        return effect;
    }

    private static int shapeId(String shape) {
        if (shape == null) return SHAPE_RANDOM;
        return switch (shape.toLowerCase(Locale.ROOT)) {
            case "sphere" -> SHAPE_SPHERE;
            case "ring" -> SHAPE_RING;
            case "spiral" -> SHAPE_SPIRAL;
            case "beam" -> SHAPE_BEAM;
            case "cone" -> SHAPE_CONE;
            default -> SHAPE_RANDOM;
        };
    }

    private static void createParticleMotion(RandomSource random, TpsConfig.ParticleBurst burst, int shape, double speed, int index, int count, ParticleMotion motion) {
        switch (shape) {
            case SHAPE_SPHERE -> sphereMotion(random, burst, speed, motion);
            case SHAPE_RING -> ringMotion(random, burst, index, count, speed, motion);
            case SHAPE_SPIRAL -> spiralMotion(random, burst, index, count, speed, motion);
            case SHAPE_BEAM -> beamMotion(random, burst, index, count, speed, motion);
            case SHAPE_CONE -> coneMotion(random, burst, speed, motion);
            default -> randomMotion(random, burst, speed, motion);
        }
    }

    private static void randomMotion(RandomSource random, TpsConfig.ParticleBurst burst, double speed, ParticleMotion motion) {
        double x = (random.nextDouble() - 0.5) * burst.spreadX;
        double y = (random.nextDouble() - 0.5) * burst.spreadY;
        double z = (random.nextDouble() - 0.5) * burst.spreadZ;
        double vx = (random.nextDouble() - 0.5) * speed;
        double vy = random.nextDouble() * speed;
        double vz = (random.nextDouble() - 0.5) * speed;
        motion.set(x, y, z, vx, vy, vz);
    }

    private static void sphereMotion(RandomSource random, TpsConfig.ParticleBurst burst, double speed, ParticleMotion motion) {
        double yaw = random.nextDouble() * Math.PI * 2.0;
        double cosPitch = random.nextDouble() * 2.0 - 1.0;
        double sinPitch = Math.sqrt(Math.max(0.0, 1.0 - cosPitch * cosPitch));
        double radius = 0.35 + random.nextDouble() * 0.65;
        double dx = Math.cos(yaw) * sinPitch;
        double dy = cosPitch;
        double dz = Math.sin(yaw) * sinPitch;
        motion.set(
                dx * burst.spreadX * radius,
                dy * burst.spreadY * radius,
                dz * burst.spreadZ * radius,
                dx * speed,
                dy * speed,
                dz * speed
        );
    }

    private static void ringMotion(RandomSource random, TpsConfig.ParticleBurst burst, int index, int count, double speed, ParticleMotion motion) {
        double angle = indexedAngle(random, index, count);
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        double wobble = (random.nextDouble() - 0.5) * burst.spreadY * 0.15;
        motion.set(
                dx * burst.spreadX * 0.5,
                wobble,
                dz * burst.spreadZ * 0.5,
                dx * speed,
                random.nextDouble() * speed * 0.25,
                dz * speed
        );
    }

    private static void spiralMotion(RandomSource random, TpsConfig.ParticleBurst burst, int index, int count, double speed, ParticleMotion motion) {
        double progress = count <= 1 ? 0.0 : index / (double) (count - 1);
        double angle = progress * Math.PI * 6.0;
        double radius = 0.15 + progress * 0.45;
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        motion.set(
                dx * burst.spreadX * radius,
                (progress - 0.5) * burst.spreadY,
                dz * burst.spreadZ * radius,
                dx * speed * 0.8,
                speed * (0.35 + random.nextDouble() * 0.35),
                dz * speed * 0.8
        );
    }

    private static void beamMotion(RandomSource random, TpsConfig.ParticleBurst burst, int index, int count, double speed, ParticleMotion motion) {
        double progress = count <= 1 ? 0.0 : index / (double) (count - 1);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * 0.12;
        motion.set(
                Math.cos(angle) * burst.spreadX * radius,
                (progress - 0.5) * burst.spreadY,
                Math.sin(angle) * burst.spreadZ * radius,
                (random.nextDouble() - 0.5) * speed * 0.2,
                speed,
                (random.nextDouble() - 0.5) * speed * 0.2
        );
    }

    private static void coneMotion(RandomSource random, TpsConfig.ParticleBurst burst, double speed, ParticleMotion motion) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * 0.5;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        double lift = 0.25 + random.nextDouble() * 0.75;
        motion.set(
                dx * burst.spreadX,
                (lift - 0.5) * burst.spreadY,
                dz * burst.spreadZ,
                dx * speed,
                lift * speed,
                dz * speed
        );
    }

    private static double indexedAngle(RandomSource random, int index, int count) {
        if (count <= 0) return random.nextDouble() * Math.PI * 2.0;
        return (index / (double) count) * Math.PI * 2.0 + random.nextDouble() * 0.08;
    }


    private static void playSounds(Minecraft client, Entity entity, TpsConfig cfg) {
        if (!cfg.sounds.enabled) return;

        BlockPos pos = entity.blockPosition();
        for (TpsConfig.SoundPlay s : cfg.sounds.sounds) {
            int delayTicks = Math.max(0, s.delayTicks);
            if (delayTicks == 0) {
                playSound(client, pos, s);
            } else {
                PENDING_SOUNDS.add(new PendingSound(pos, s.sound, s.volume, s.pitch, delayTicks));
            }
        }
    }

    public static void tick(Minecraft client) {
        if (PENDING_SOUNDS.isEmpty()) return;
        if (client.level == null || client.player == null) {
            PENDING_SOUNDS.clear();
            return;
        }

        Iterator<PendingSound> iterator = PENDING_SOUNDS.iterator();
        while (iterator.hasNext()) {
            PendingSound pending = iterator.next();
            pending.delayTicks--;
            if (pending.delayTicks <= 0) {
                playSound(client, pending.pos, pending.sound, pending.volume, pending.pitch);
                iterator.remove();
            }
        }
    }

    private static void playSound(Minecraft client, BlockPos pos, TpsConfig.SoundPlay sound) {
        playSound(client, pos, sound.sound, sound.volume, sound.pitch);
    }

    private static void playSound(Minecraft client, BlockPos pos, String soundId, float volume, float pitch) {
        SoundEvent event = getSoundEvent(soundId);
        if (event == null) return;
        client.level.playSound(client.player, pos, event, SoundSource.PLAYERS, volume, pitch);
    }

    private static SoundEvent getSoundEvent(String soundId) {
        if (soundId == null || soundId.isBlank()) return null;
        if (SOUND_CACHE.containsKey(soundId)) {
            return SOUND_CACHE.get(soundId);
        }

        SoundEvent event = null;
        Identifier id = Identifier.tryParse(soundId);
        if (id != null) {
            event = BuiltInRegistries.SOUND_EVENT.getValue(id);
        }
        SOUND_CACHE.put(soundId, event);
        return event;
    }

    private static class ParticleMotion {
        private double x;
        private double y;
        private double z;
        private double vx;
        private double vy;
        private double vz;

        private void set(double x, double y, double z, double vx, double vy, double vz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }
    }

    private static class PendingSound {
        private final BlockPos pos;
        private final String sound;
        private final float volume;
        private final float pitch;
        private int delayTicks;

        private PendingSound(BlockPos pos, String sound, float volume, float pitch, int delayTicks) {
            this.pos = pos;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.delayTicks = delayTicks;
        }
    }
}
