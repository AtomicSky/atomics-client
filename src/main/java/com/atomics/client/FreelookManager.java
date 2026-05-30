package com.atomics.client;

import com.atomics.client.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FreelookManager {
    private static boolean active;
    private static boolean toggled;
    private static boolean keyWasPressed;
    private static Perspective previousPerspective;
    private static float cameraYaw;
    private static float cameraPitch;

    private FreelookManager() {
    }

    public static void tick(MinecraftClient client) {
        boolean canFreelook = client != null
                && client.player != null
                && client.world != null
                && client.currentScreen == null
                && AtomicsClient.isFreelookEnabled()
                && !DualSpectateCamera.isActive();
        boolean keyPressed = AtomicsClient.isFreelookKeyPressed();
        boolean toggleMode = AtomicsClient.isFreelookToggleMode();
        if (!toggleMode) {
            toggled = false;
        } else if (canFreelook && keyPressed && !keyWasPressed) {
            toggled = !toggled;
        }
        keyWasPressed = keyPressed;
        if (!canFreelook) {
            toggled = false;
        }

        boolean shouldBeActive = canFreelook && (toggleMode ? toggled : keyPressed);

        if (!shouldBeActive) {
            stop(client, !DualSpectateCamera.isActive());
            return;
        }

        if (!active) {
            previousPerspective = client.options.getPerspective();
            cameraYaw = client.player.getYaw();
            cameraPitch = client.player.getPitch();
            active = true;
        }

        if (client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public static void onMouseLook(double cursorDeltaX, double cursorDeltaY) {
        if (!active) {
            return;
        }

        cameraYaw += (float) cursorDeltaX * 0.15f;
        cameraPitch = MathHelper.clamp(cameraPitch + (float) cursorDeltaY * 0.15f, -90.0f, 90.0f);
    }

    public static void applyToCamera(Camera camera, Entity focusedEntity, float tickProgress) {
        if (!active || camera == null || focusedEntity == null) {
            return;
        }

        CameraAccessor accessor = (CameraAccessor) camera;
        accessor.atomics_client$setRotation(cameraYaw, cameraPitch);
        accessor.atomics_client$setPos(baseCameraPos(focusedEntity, camera, tickProgress));
        accessor.atomics_client$moveBy(-accessor.atomics_client$clipToSpace(cameraDistance(focusedEntity)), 0.0f, 0.0f);
    }

    public static boolean isActive() {
        return active;
    }

    private static void stop(MinecraftClient client, boolean restorePerspective) {
        if (!active) {
            return;
        }

        active = false;
        if (restorePerspective && client != null && client.options != null && previousPerspective != null) {
            client.options.setPerspective(previousPerspective);
        }
        previousPerspective = null;
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
}
