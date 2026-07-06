package com.legions.client.mixin;

import com.legions.client.LegionsSpectateLock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "com.atomics.client.DualSpectateCamera")
public abstract class AtomicsDualSpectateCameraMixin {
    private static final float LOCKED_FRAME_PADDING = 2.5F;
    private static final float LOCKED_MIN_DISTANCE = 2.0F;
    private static final float LOCKED_MAX_DISTANCE = 160.0F;
    private static final float LOCKED_MAX_Y_DIFFERENCE = 10.0F;
    private static final double MIN_USEFUL_CAMERA_DISTANCE_SQUARED = 4.0D;
    private static final double WALL_BACKOFF = 0.35D;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void legions_client$forceDualSpectateDefaults(MinecraftClient client, CallbackInfo ci) {
        if (!LegionsSpectateLock.hasLock()) {
            return;
        }
        forceDualSpectateDefaults();
    }

    @Inject(method = "resolveCameraPosition", at = @At("RETURN"), cancellable = true, remap = false)
    private static void legions_client$preferClearCameraPosition(MinecraftClient client, PlayerEntity first,
                                                                 PlayerEntity second, Vec3d center, Vec3d side,
                                                                 float distance, Vec3d lookTarget,
                                                                 Vec3d preferredCameraPos, @Coerce Object pvp,
                                                                 CallbackInfoReturnable<Vec3d> cir) {
        if (!LegionsSpectateLock.isLockedPair(first, second)) {
            return;
        }

        Vec3d preferred = cir.getReturnValue();
        Vec3d adjusted = findClearCameraPosition(client, lookTarget, preferred);
        if (adjusted != null) {
            cir.setReturnValue(adjusted);
        }
    }

    @Inject(method = "isWithinYDifference", at = @At("HEAD"), cancellable = true, remap = false)
    private static void legions_client$onlyLimitYDifferenceInOverhead(PlayerEntity first, PlayerEntity second,
                                                                      @Coerce Object pvp,
                                                                      CallbackInfoReturnable<Boolean> cir) {
        if (!LegionsSpectateLock.isLockedPair(first, second)) {
            return;
        }

        if (!isDualSpectateOverheadEnabled(pvp)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isAllowedSpectatePair", at = @At("HEAD"), cancellable = true, remap = false)
    private static void legions_client$allowExplicitLockedPair(MinecraftClient client, PlayerEntity first,
                                                               PlayerEntity second, @Coerce Object pvp,
                                                               @Coerce Object teamRules,
                                                               CallbackInfoReturnable<Boolean> cir) {
        if (LegionsSpectateLock.isLockedPair(first, second)) {
            cir.setReturnValue(true);
        }
    }

    private static Vec3d findClearCameraPosition(MinecraftClient client, Vec3d lookTarget, Vec3d preferred) {
        if (client == null || client.world == null || lookTarget == null || preferred == null) {
            return preferred;
        }

        if (hasClearPath(client, lookTarget, preferred) && isClearAt(client, preferred)) {
            return preferred;
        }

        Vec3d clipped = clipBeforeWall(client, lookTarget, preferred);
        if (isUsableCameraPosition(client, lookTarget, clipped)) {
            return clipped;
        }

        Vec3d direction = preferred.subtract(lookTarget);
        if (direction.lengthSquared() < 1.0E-4D) {
            return preferred;
        }

        Vec3d[] candidates = new Vec3d[] {
                preferred.add(0.0D, 0.75D, 0.0D),
                preferred.add(0.0D, 1.5D, 0.0D),
                preferred.add(0.0D, -0.75D, 0.0D),
                preferred.add(0.0D, -1.5D, 0.0D),
                lookTarget.add(direction.multiply(0.75D)),
                lookTarget.add(direction.multiply(0.5D))
        };

        for (Vec3d candidate : candidates) {
            Vec3d candidateClip = hasClearPath(client, lookTarget, candidate) ? candidate : clipBeforeWall(client, lookTarget, candidate);
            if (isUsableCameraPosition(client, lookTarget, candidateClip)) {
                return candidateClip;
            }
        }

        return preferred;
    }

    private static boolean isUsableCameraPosition(MinecraftClient client, Vec3d lookTarget, Vec3d position) {
        return position != null
                && position.squaredDistanceTo(lookTarget) >= MIN_USEFUL_CAMERA_DISTANCE_SQUARED
                && isClearAt(client, position);
    }

    private static Vec3d clipBeforeWall(MinecraftClient client, Vec3d start, Vec3d end) {
        HitResult hit = raycast(client, start, end);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return end;
        }

        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-4D) {
            return end;
        }

        Vec3d clipped = hit.getPos().subtract(direction.normalize().multiply(WALL_BACKOFF));
        return clipped.squaredDistanceTo(start) < end.squaredDistanceTo(start) ? clipped : end;
    }

    private static boolean hasClearPath(MinecraftClient client, Vec3d start, Vec3d end) {
        HitResult hit = raycast(client, start, end);
        return hit == null || hit.getType() != HitResult.Type.BLOCK;
    }

    private static HitResult raycast(MinecraftClient client, Vec3d start, Vec3d end) {
        return client.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
    }

    private static boolean isClearAt(MinecraftClient client, Vec3d position) {
        BlockPos blockPos = BlockPos.ofFloored(position);
        return client.world.getBlockState(blockPos).getCollisionShape(client.world, blockPos).isEmpty();
    }

    private static void forceDualSpectateDefaults() {
        try {
            Class<?> atomicsClientClass = Class.forName("com.atomics.client.AtomicsClient");
            Field configField = atomicsClientClass.getDeclaredField("CONFIG");
            configField.setAccessible(true);
            Object config = configField.get(null);
            if (config == null) {
                return;
            }

            Field pvpField = config.getClass().getDeclaredField("pvp");
            pvpField.setAccessible(true);
            Object pvp = pvpField.get(config);
            if (pvp == null) {
                return;
            }

            setFloatField(pvp, "dualSpectatePadding", LOCKED_FRAME_PADDING);
            setFloatField(pvp, "dualSpectateMinDistance", LOCKED_MIN_DISTANCE);
            setFloatField(pvp, "dualSpectateMaxDistance", LOCKED_MAX_DISTANCE);
            setFloatField(pvp, "dualSpectateMaxYDifference", LOCKED_MAX_Y_DIFFERENCE);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isDualSpectateOverheadEnabled(Object pvp) {
        if (pvp == null) {
            return false;
        }
        try {
            Field field = pvp.getClass().getDeclaredField("dualSpectateOverheadEnabled");
            field.setAccessible(true);
            Object value = field.get(pvp);
            return value instanceof Boolean enabled && enabled;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void setFloatField(Object owner, String fieldName, float value) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        if (field.getType() == float.class) {
            field.setFloat(owner, value);
        } else if (field.getType() == double.class) {
            field.setDouble(owner, value);
        }
    }
}
