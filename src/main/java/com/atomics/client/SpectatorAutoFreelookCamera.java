package com.atomics.client;

import com.atomics.client.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Optional;
import java.util.UUID;

public final class SpectatorAutoFreelookCamera {
    private static final double LOOK_TARGET_RANGE = 42.0;
    private static final double TARGETING_RANGE = 32.0;
    private static final double TARGET_LOST_RANGE = 48.0;
    private static final float DOWNWARD_PITCH_OFFSET = 12.0f;
    private static final float YAW_SMOOTHING = 0.32f;
    private static final float PITCH_SMOOTHING = 0.22f;
    private static final int TARGET_LOST_GRACE_TICKS = 12;

    private static boolean active;
    private static boolean initialized;
    private static Perspective previousPerspective;
    private static UUID focusedPlayerUuid;
    private static UUID interestTargetUuid;
    private static float targetYaw;
    private static float lastYaw;
    private static float currentYaw;
    private static float lastPitch;
    private static float currentPitch;
    private static int missingTargetTicks;

    private SpectatorAutoFreelookCamera() {
    }

    public static void tick(MinecraftClient client) {
        active = false;
        if (!canRun(client)) {
            stop(client, true);
            return;
        }

        PlayerEntity focused = focusedSpectatedPlayer(client);
        if (focused == null) {
            stop(client, true);
            return;
        }

        boolean changedTarget = focusedPlayerUuid == null || !focusedPlayerUuid.equals(focused.getUuid());
        if (!initialized || changedTarget) {
            if (previousPerspective == null) {
                previousPerspective = client.options.getPerspective();
            }
            focusedPlayerUuid = focused.getUuid();
            targetYaw = focused.getYaw();
            lastYaw = targetYaw;
            currentYaw = targetYaw;
            currentPitch = fallbackPitch(focused);
            lastPitch = currentPitch;
            interestTargetUuid = null;
            missingTargetTicks = 0;
            initialized = true;
        }

        lastYaw = currentYaw;
        lastPitch = currentPitch;
        RotationTarget rotationTarget = findRotationTarget(client, focused);
        targetYaw = rotationTarget.yaw();
        currentYaw = lerpAngle(currentYaw, targetYaw, YAW_SMOOTHING);
        currentPitch = MathHelper.clamp(lerp(currentPitch, rotationTarget.pitch(), PITCH_SMOOTHING), -25.0f, 60.0f);

        forceThirdPerson(client);
        active = true;
    }

    public static void applyToCamera(Camera camera, MinecraftClient client, float tickProgress) {
        if (!active || camera == null || client == null) {
            return;
        }

        PlayerEntity focused = focusedSpectatedPlayer(client);
        if (focused == null) {
            return;
        }

        float progress = MathHelper.clamp(tickProgress, 0.0f, 1.0f);
        float yaw = lerpAngle(lastYaw, currentYaw, progress);
        float pitch = lerp(lastPitch, currentPitch, progress);

        CameraAccessor accessor = (CameraAccessor) camera;
        accessor.atomics_client$setRotation(yaw, pitch);
        accessor.atomics_client$setPos(baseCameraPos(focused, camera, tickProgress));
        accessor.atomics_client$moveBy(-accessor.atomics_client$clipToSpace(cameraDistance(focused)), 0.0f, 0.0f);
    }

    public static boolean isActive() {
        return active;
    }

    private static boolean canRun(MinecraftClient client) {
        return client != null
                && client.player != null
                && client.world != null
                && client.options != null
                && isSpectatorMode(client)
                && AtomicsClient.isSpectatorAutoFreelookEnabled()
                && client.currentScreen == null;
    }

    private static PlayerEntity focusedSpectatedPlayer(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }

        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity instanceof PlayerEntity focused && isValidFocusedPlayer(client, focused)) {
            return focused;
        }

        return null;
    }

    private static RotationTarget findRotationTarget(MinecraftClient client, PlayerEntity focused) {
        PlayerEntity interestTarget = findCurrentInterestTarget(client, focused);
        if (interestTarget != null) {
            interestTargetUuid = interestTarget.getUuid();
            missingTargetTicks = 0;
            return rotationToward(focused, interestTarget);
        }

        if (missingTargetTicks++ < TARGET_LOST_GRACE_TICKS) {
            interestTarget = findLastInterestTarget(client, focused);
            if (interestTarget != null) {
                return rotationToward(focused, interestTarget);
            }
        }

        interestTargetUuid = null;
        return new RotationTarget(focused.getYaw(), fallbackPitch(focused));
    }

    private static PlayerEntity findCurrentInterestTarget(MinecraftClient client, PlayerEntity focused) {
        PlayerEntity target = validPlayerTarget(client, focused, focused.getAttacking());
        if (target != null) {
            return target;
        }

        target = findLookedAtPlayer(client, focused);
        if (target != null) {
            return target;
        }

        target = validPlayerTarget(client, focused, focused.getAttacker());
        if (target != null) {
            return target;
        }
        target = validPlayerTarget(client, focused, focused.getLastAttacker());
        if (target != null) {
            return target;
        }
        target = validPlayerTarget(client, focused, focused.getAttackingPlayer());
        if (target != null) {
            return target;
        }

        return findNearestTargetingPlayer(client, focused);
    }

    private static PlayerEntity findLookedAtPlayer(MinecraftClient client, PlayerEntity focused) {
        if (client == null || client.world == null || focused == null) {
            return null;
        }

        Vec3d start = focused.getCameraPosVec(1.0f);
        Vec3d direction = focused.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(LOOK_TARGET_RANGE));
        PlayerEntity best = null;
        double bestDistanceSq = LOOK_TARGET_RANGE * LOOK_TARGET_RANGE;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (!isValidInterestPlayer(client, focused, candidate)) {
                continue;
            }

            Box box = candidate.getBoundingBox().expand(Math.max(0.3, candidate.getTargetingMargin() + 0.25));
            Optional<Vec3d> hit = box.raycast(start, end);
            if (hit.isEmpty()) {
                continue;
            }

            double distanceSq = start.squaredDistanceTo(hit.get());
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private static PlayerEntity findNearestTargetingPlayer(MinecraftClient client, PlayerEntity focused) {
        if (client == null || client.world == null || focused == null) {
            return null;
        }

        PlayerEntity best = null;
        double bestDistanceSq = TARGETING_RANGE * TARGETING_RANGE;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (!isValidInterestPlayer(client, focused, candidate) || !isLookingAtPlayer(candidate, focused, TARGETING_RANGE)) {
                continue;
            }

            double distanceSq = focused.squaredDistanceTo(candidate);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isLookingAtPlayer(PlayerEntity from, PlayerEntity to, double range) {
        Vec3d start = from.getCameraPosVec(1.0f);
        Vec3d direction = from.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(range));
        Box box = to.getBoundingBox().expand(Math.max(0.35, to.getTargetingMargin() + 0.35));
        return box.raycast(start, end).isPresent();
    }

    private static PlayerEntity findLastInterestTarget(MinecraftClient client, PlayerEntity focused) {
        if (client == null || client.world == null || interestTargetUuid == null) {
            return null;
        }

        double maxDistanceSq = TARGET_LOST_RANGE * TARGET_LOST_RANGE;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate.getUuid().equals(interestTargetUuid)
                    && isValidInterestPlayer(client, focused, candidate)
                    && focused.squaredDistanceTo(candidate) <= maxDistanceSq) {
                return candidate;
            }
        }
        return null;
    }

    private static PlayerEntity validPlayerTarget(MinecraftClient client, PlayerEntity focused, Entity target) {
        return target instanceof PlayerEntity player && isValidInterestPlayer(client, focused, player) ? player : null;
    }

    private static boolean isValidInterestPlayer(MinecraftClient client, PlayerEntity focused, PlayerEntity target) {
        return target != null
                && client != null
                && client.player != null
                && focused != null
                && !target.getUuid().equals(client.player.getUuid())
                && !target.getUuid().equals(focused.getUuid())
                && !target.isSpectator()
                && !target.isDead()
                && target.isAlive();
    }

    private static boolean isValidFocusedPlayer(MinecraftClient client, PlayerEntity focused) {
        return focused != null
                && client != null
                && client.player != null
                && !focused.getUuid().equals(client.player.getUuid())
                && !focused.isSpectator()
                && !focused.isDead()
                && focused.isAlive();
    }

    private static boolean isSpectatorMode(MinecraftClient client) {
        return client != null
                && client.player != null
                && (client.player.isSpectator()
                || client.interactionManager != null
                && client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR);
    }

    private static void forceThirdPerson(MinecraftClient client) {
        if (client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    private static Vec3d baseCameraPos(Entity focusedEntity, Camera camera, float tickProgress) {
        double progress = tickProgress;
        double x = MathHelper.lerp(progress, focusedEntity.lastX, focusedEntity.getX());
        double y = MathHelper.lerp(progress, focusedEntity.lastY, focusedEntity.getY())
                + MathHelper.lerp(tickProgress, ((CameraAccessor) camera).atomics_client$getLastCameraY(), ((CameraAccessor) camera).atomics_client$getCameraY());
        double z = MathHelper.lerp(progress, focusedEntity.lastZ, focusedEntity.getZ());
        return new Vec3d(x, y, z);
    }

    private static float cameraDistance(Entity focusedEntity) {
        float focusedScale = 1.0f;
        float focusedDistance = 4.0f;
        if (focusedEntity instanceof LivingEntity living) {
            focusedScale = living.getScale();
            focusedDistance = (float) living.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
        }

        float cameraScale = focusedScale;
        float cameraDistance = focusedDistance;
        Entity vehicle = focusedEntity.hasVehicle() ? focusedEntity.getVehicle() : null;
        if (vehicle instanceof LivingEntity livingVehicle) {
            cameraScale = livingVehicle.getScale();
            cameraDistance = (float) livingVehicle.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
        }
        return Math.max(focusedScale * focusedDistance, cameraScale * cameraDistance);
    }

    private static void stop(MinecraftClient client, boolean restorePerspective) {
        if (!active && !initialized && previousPerspective == null) {
            return;
        }

        active = false;
        initialized = false;
        focusedPlayerUuid = null;
        interestTargetUuid = null;
        targetYaw = 0.0f;
        lastYaw = 0.0f;
        currentYaw = 0.0f;
        lastPitch = 0.0f;
        currentPitch = 0.0f;
        missingTargetTicks = 0;
        if (restorePerspective && client != null && client.options != null) {
            if (isSpectatorMode(client)) {
                client.options.setPerspective(Perspective.FIRST_PERSON);
            } else if (previousPerspective != null) {
                client.options.setPerspective(previousPerspective);
            }
        }
        previousPerspective = null;
    }

    private static float lerp(float current, float target, float amount) {
        return current + (target - current) * amount;
    }

    private static float lerpAngle(float current, float target, float amount) {
        float delta = wrapDegrees(target - current);
        return current + delta * amount;
    }

    private static RotationTarget rotationToward(PlayerEntity focused, PlayerEntity target) {
        Vec3d from = focused.getEyePos();
        Vec3d to = playerBodyTarget(target);
        return new RotationTarget(calculateYaw(from, to), MathHelper.clamp(calculatePitch(from, to) + DOWNWARD_PITCH_OFFSET, -25.0f, 60.0f));
    }

    private static Vec3d playerBodyTarget(PlayerEntity player) {
        return new Vec3d(
                player.getX(),
                player.getY() + player.getHeight() * 0.58,
                player.getZ()
        );
    }

    private static float fallbackPitch(PlayerEntity focused) {
        return MathHelper.clamp(focused.getPitch() + DOWNWARD_PITCH_OFFSET, -25.0f, 60.0f);
    }

    private static float calculateYaw(Vec3d from, Vec3d to) {
        double directionX = to.x - from.x;
        double directionZ = to.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(directionZ, directionX)) - 90.0);
    }

    private static float calculatePitch(Vec3d from, Vec3d to) {
        double directionX = to.x - from.x;
        double directionY = to.y - from.y;
        double directionZ = to.z - from.z;
        double horizontal = Math.sqrt(directionX * directionX + directionZ * directionZ);
        return MathHelper.clamp((float) -Math.toDegrees(Math.atan2(directionY, horizontal)), -89.0f, 89.0f);
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    private record RotationTarget(float yaw, float pitch) {
    }
}
