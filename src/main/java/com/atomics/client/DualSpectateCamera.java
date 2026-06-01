package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.atomics.client.mixin.CameraAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class DualSpectateCamera {
    private static final float DEFAULT_DISTANCE = 6.0f;
    private static final float DEFAULT_HEIGHT = 1.5f;
    private static final float MIN_POSITION_SMOOTHING = 0.045f;
    private static final float MAX_POSITION_SMOOTHING = 0.42f;
    private static final float PITCH_SMOOTHING = 0.18f;

    private static boolean active;
    private static boolean initialized;
    private static Vec3 lastCameraPos = Vec3.ZERO;
    private static Vec3 currentCameraPos = Vec3.ZERO;
    private static Vec3 lastLookTarget = Vec3.ZERO;
    private static Vec3 currentLookTarget = Vec3.ZERO;
    private static float lastYaw;
    private static float currentYaw;
    private static float lastPitch;
    private static float currentPitch;
    private static Player spectatedFirst;
    private static Player spectatedSecond;

    private DualSpectateCamera() {
    }

    public static void tick(Minecraft client) {
        active = false;
        if (client == null || client.player == null || client.level == null || AtomicsClient.CONFIG == null) {
            reset();
            return;
        }

        TpsConfig.PvpSettings pvp = AtomicsClient.CONFIG.pvp;
        if (!pvp.dualSpectateEnabled) {
            reset();
            return;
        }

        Player first;
        Player second;
        if (pvp.dualSpectateAutoFill) {
            PlayerPair pair = findConfiguredPair(client, pvp);
            if (pair == null) {
                pair = findNearestPlayerPair(client);
            }
            if (pair != null) {
                pvp.dualSpectatePlayerOne = pair.first.getScoreboardName();
                pvp.dualSpectatePlayerTwo = pair.second.getScoreboardName();
                first = pair.first;
                second = pair.second;
            } else {
                reset();
                return;
            }
        } else {
            first = findPlayer(client, pvp.dualSpectatePlayerOne);
            second = findPlayer(client, pvp.dualSpectatePlayerTwo);
        }

        if (first == null || second == null || first.getUUID().equals(second.getUUID())) {
            reset();
            return;
        }
        spectatedFirst = first;
        spectatedSecond = second;

        Vec3 firstPos = first.getEyePosition();
        Vec3 secondPos = second.getEyePosition();
        Vec3 midpoint = firstPos.add(secondPos).scale(0.5);
        Vec3 difference = firstPos.subtract(secondPos);
        Vec3 side = orthogonal(difference);
        if (side.horizontalDistanceSqr() < 0.0001) {
            side = fallbackSide(client);
        } else {
            side = side.normalize();
        }

        float distance = calculateAutoDistance(client, difference, pvp);
        side = chooseStableSide(midpoint, side);
        Vec3 rawCameraPos = midpoint.add(side.scale(distance)).add(0.0, DEFAULT_HEIGHT, 0.0);
        Vec3 rawLookTarget = midpoint.add(0.0, DEFAULT_HEIGHT * 0.55, 0.0);
        float positionSmoothing = initialized ? positionSmoothingAmount(currentCameraPos.distanceTo(rawCameraPos)) : 1.0f;
        Vec3 targetCameraPos = initialized
                ? smoothStep(currentCameraPos, rawCameraPos, positionSmoothing)
                : rawCameraPos;
        Vec3 targetLookTarget = initialized
                ? smoothStep(currentLookTarget, rawLookTarget, positionSmoothing)
                : rawLookTarget;

        float targetYaw = calculateYaw(targetCameraPos, targetLookTarget);
        float targetPitch = calculatePitch(targetCameraPos, targetLookTarget);

        if (!initialized) {
            lastCameraPos = targetCameraPos;
            currentCameraPos = targetCameraPos;
            lastLookTarget = targetLookTarget;
            currentLookTarget = targetLookTarget;
            lastYaw = targetYaw;
            currentYaw = targetYaw;
            lastPitch = targetPitch;
            currentPitch = targetPitch;
            initialized = true;
        } else {
            lastCameraPos = currentCameraPos;
            currentCameraPos = targetCameraPos;
            lastLookTarget = currentLookTarget;
            currentLookTarget = targetLookTarget;
            lastYaw = currentYaw;
            currentYaw = lerpAngle(currentYaw, targetYaw, Math.max(PITCH_SMOOTHING, positionSmoothing));
            lastPitch = currentPitch;
            currentPitch = lerp(currentPitch, targetPitch, Math.max(PITCH_SMOOTHING, positionSmoothing));
        }

        client.player.setYRot(currentYaw);
        client.player.setXRot(currentPitch);
        client.player.setYHeadRot(currentYaw);
        client.player.setYBodyRot(currentYaw);

        if (pvp.dualSpectateForceThirdPerson && client.options.getCameraType().isFirstPerson()) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        active = true;
    }

    public static void render(Minecraft client, float tickProgress) {
        if (!active || !initialized || client == null || client.player == null || client.gameRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }

        applyToCamera(camera, client, tickProgress);
    }

    public static void applyToCamera(Camera camera, Minecraft client, float tickProgress) {
        if (!active || !initialized || camera == null || client == null || client.player == null) {
            return;
        }

        float progress = clamp(tickProgress, 0.0f, 1.0f);
        Vec3 cameraPos = new Vec3(
                lastCameraPos.x + (currentCameraPos.x - lastCameraPos.x) * progress,
                lastCameraPos.y + (currentCameraPos.y - lastCameraPos.y) * progress,
                lastCameraPos.z + (currentCameraPos.z - lastCameraPos.z) * progress
        );
        float yaw = lerpAngle(lastYaw, currentYaw, progress);
        float pitch = lerp(lastPitch, currentPitch, progress);

        CameraAccessor accessor = (CameraAccessor) camera;
        accessor.atomics_client$setPos(cameraPos);
        accessor.atomics_client$setRotation(yaw, pitch);
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
        client.player.setYHeadRot(yaw);
        client.player.setYBodyRot(yaw);
        if (client.player.isSpectator()) {
            client.player.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static Player getOtherSpectatedPlayer(Player player) {
        if (!active || player == null || spectatedFirst == null || spectatedSecond == null) {
            return null;
        }
        if (player.getUUID().equals(spectatedFirst.getUUID())) {
            return spectatedSecond;
        }
        if (player.getUUID().equals(spectatedSecond.getUUID())) {
            return spectatedFirst;
        }
        return null;
    }

    public static String[] findNearestPair(Minecraft client) {
        PlayerPair pair = findNearestPlayerPair(client);
        if (pair == null) {
            return null;
        }

        return new String[]{pair.first.getScoreboardName(), pair.second.getScoreboardName()};
    }

    private static PlayerPair findNearestPlayerPair(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            return null;
        }

        Player first = null;
        double firstDistance = Double.MAX_VALUE;
        for (Player player : client.level.players()) {
            if (!isAutofillCandidate(client, player, null)) {
                continue;
            }
            double distance = player.distanceToSqr(client.player);
            if (distance < firstDistance) {
                first = player;
                firstDistance = distance;
            }
        }

        if (first == null) {
            return null;
        }

        Player second = null;
        double secondDistance = Double.MAX_VALUE;
        for (Player player : client.level.players()) {
            if (!isAutofillCandidate(client, player, first)) {
                continue;
            }
            double distance = player.distanceToSqr(first);
            if (distance < secondDistance) {
                second = player;
                secondDistance = distance;
            }
        }

        if (second == null) {
            return null;
        }

        return new PlayerPair(first, second);
    }

    private static PlayerPair findConfiguredPair(Minecraft client, TpsConfig.PvpSettings pvp) {
        Player first = findPlayer(client, pvp.dualSpectatePlayerOne);
        Player second = findPlayer(client, pvp.dualSpectatePlayerTwo);
        if (isAutofillCandidate(client, first, second) && isAutofillCandidate(client, second, first)) {
            return new PlayerPair(first, second);
        }
        return null;
    }

    private static boolean isAutofillCandidate(Minecraft client, Player player, Player excluded) {
        return player != null
                && client.player != null
                && !player.getUUID().equals(client.player.getUUID())
                && (excluded == null || !player.getUUID().equals(excluded.getUUID()))
                && !player.isSpectator()
                && !player.isDeadOrDying()
                && player.isAlive();
    }

    private static Player findPlayer(Minecraft client, String username) {
        String normalized = normalizeName(username);
        if (normalized.isEmpty()) return null;
        for (AbstractClientPlayer player : client.level.players()) {
            if (player != null && normalizeName(player.getScoreboardName()).equals(normalized)) {
                return player;
            }
        }
        return null;
    }

    private static float calculateAutoDistance(Minecraft client, Vec3 difference, TpsConfig.PvpSettings pvp) {
        double fovDegrees = 70.0;
        try {
            fovDegrees = Math.max(30.0, Math.min(110.0, client.options.fov().get()));
        } catch (RuntimeException ignored) {
            fovDegrees = 70.0;
        }

        double aspectRatio = 16.0 / 9.0;
        if (client.getWindow() != null && client.getWindow().getGuiScaledHeight() > 0) {
            aspectRatio = (double) client.getWindow().getGuiScaledWidth() / (double) client.getWindow().getGuiScaledHeight();
        }

        double horizontalFov = fovDegrees * aspectRatio * 0.8;
        double horizontalDistance = Math.abs((difference.horizontalDistance() * 0.5) / Math.tan(Math.toRadians(horizontalFov * 0.5)));
        double verticalDistance = Math.abs((difference.y * 0.5) / Math.tan(Math.toRadians(fovDegrees * 0.5)));
        float needed = (float) (Math.max(horizontalDistance, verticalDistance) * pvp.dualSpectatePadding);
        return clamp(needed, Math.max(DEFAULT_DISTANCE, pvp.dualSpectateMinDistance), pvp.dualSpectateMaxDistance);
    }

    private static Vec3 orthogonal(Vec3 vector) {
        return new Vec3(vector.z, 0.0, -vector.x);
    }

    private static Vec3 fallbackSide(Minecraft client) {
        float yawRadians = (float) Math.toRadians(client.player.getYRot());
        return new Vec3(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
    }

    private static Vec3 chooseStableSide(Vec3 midpoint, Vec3 side) {
        if (!initialized || side.horizontalDistanceSqr() < 0.0001) {
            return side;
        }

        double dot = (currentCameraPos.x - midpoint.x) * side.x + (currentCameraPos.z - midpoint.z) * side.z;
        return dot < 0.0 ? side.scale(-1.0) : side;
    }

    private static float calculateYaw(Vec3 cameraPos, Vec3 targetPos) {
        double directionX = targetPos.x - cameraPos.x;
        double directionZ = targetPos.z - cameraPos.z;
        return (float) (Math.toDegrees(Math.atan2(directionZ, directionX)) - 90.0);
    }

    private static float calculatePitch(Vec3 cameraPos, Vec3 targetPos) {
        double directionX = targetPos.x - cameraPos.x;
        double directionY = targetPos.y - cameraPos.y;
        double directionZ = targetPos.z - cameraPos.z;
        double horizontal = Math.sqrt(directionX * directionX + directionZ * directionZ);
        return clamp((float) -Math.toDegrees(Math.atan2(directionY, horizontal)), -89.0f, 89.0f);
    }

    private static Vec3 smoothStep(Vec3 current, Vec3 target, float amount) {
        return current.lerp(target, clamp(amount, 0.0f, 1.0f));
    }

    private static float positionSmoothingAmount(double cameraMoveDistance) {
        if (!Double.isFinite(cameraMoveDistance)) {
            return MIN_POSITION_SMOOTHING;
        }
        float amount = MIN_POSITION_SMOOTHING
                + (MAX_POSITION_SMOOTHING - MIN_POSITION_SMOOTHING) / (1.0f + (float) cameraMoveDistance * 0.28f);
        return clamp(amount, MIN_POSITION_SMOOTHING, MAX_POSITION_SMOOTHING);
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static void reset() {
        initialized = false;
        lastCameraPos = Vec3.ZERO;
        currentCameraPos = Vec3.ZERO;
        lastLookTarget = Vec3.ZERO;
        currentLookTarget = Vec3.ZERO;
        lastYaw = 0.0f;
        currentYaw = 0.0f;
        lastPitch = 0.0f;
        currentPitch = 0.0f;
        spectatedFirst = null;
        spectatedSecond = null;
    }

    private static float lerp(float current, float target, float amount) {
        return current + (target - current) * amount;
    }

    private static float lerpAngle(float current, float target, float amount) {
        float delta = wrapDegrees(target - current);
        return current + delta * amount;
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PlayerPair(Player first, Player second) {
    }
}
