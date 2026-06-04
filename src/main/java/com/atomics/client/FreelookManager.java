package com.atomics.client;

import com.atomics.client.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class FreelookManager {
    private static boolean active;
    private static boolean toggled;
    private static boolean keyWasPressed;
    private static CameraType previousPerspective;
    private static float cameraYaw;
    private static float cameraPitch;

    private FreelookManager() {
    }

    public static void tick(Minecraft client) {
        boolean canFreelook = client != null
                && client.player != null
                && client.level != null
                && client.screen == null
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
            previousPerspective = client.options.getCameraType();
            cameraYaw = client.player.getYRot();
            cameraPitch = client.player.getXRot();
            active = true;
        }

        if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    public static void onMouseLook(double cursorDeltaX, double cursorDeltaY) {
        if (!active) {
            return;
        }

        cameraYaw += (float) cursorDeltaX * 0.15f;
        cameraPitch = Mth.clamp(cameraPitch + (float) cursorDeltaY * 0.15f, -90.0f, 90.0f);
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

    private static void stop(Minecraft client, boolean restorePerspective) {
        if (!active) {
            return;
        }

        active = false;
        if (restorePerspective && client != null && client.options != null && previousPerspective != null) {
            client.options.setCameraType(previousPerspective);
        }
        previousPerspective = null;
    }

    private static Vec3 baseCameraPos(Entity focusedEntity, Camera camera, float tickProgress) {
        double progress = tickProgress;
        double x = Mth.lerp(progress, focusedEntity.xo, focusedEntity.getX());
        double y = Mth.lerp(progress, focusedEntity.yo, focusedEntity.getY())
                + Mth.lerp(tickProgress, ((CameraAccessor) camera).atomics_client$getLastCameraY(), ((CameraAccessor) camera).atomics_client$getCameraY());
        double z = Mth.lerp(progress, focusedEntity.zo, focusedEntity.getZ());
        return new Vec3(x, y, z);
    }

    private static float cameraDistance(Entity focusedEntity) {
        float focusedScale = 1.0f;
        float focusedDistance = 4.0f;
        if (focusedEntity instanceof LivingEntity living) {
            focusedScale = living.getScale();
            focusedDistance = (float) living.getAttributeValue(Attributes.CAMERA_DISTANCE);
        }

        float cameraScale = focusedScale;
        float cameraDistance = focusedDistance;
        Entity vehicle = focusedEntity.isPassenger() ? focusedEntity.getVehicle() : null;
        if (vehicle instanceof LivingEntity livingVehicle) {
            cameraScale = livingVehicle.getScale();
            cameraDistance = (float) livingVehicle.getAttributeValue(Attributes.CAMERA_DISTANCE);
        }
        return Math.max(focusedScale * focusedDistance, cameraScale * cameraDistance);
    }
}
