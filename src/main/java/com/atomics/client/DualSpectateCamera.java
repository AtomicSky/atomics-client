package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.atomics.client.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DualSpectateCamera {
    private static final float DEFAULT_DISTANCE = 6.0f;
    private static final float DEFAULT_HEIGHT = 1.5f;
    private static final float MIN_POSITION_SMOOTHING = 0.045f;
    private static final float MAX_POSITION_SMOOTHING = 0.42f;
    private static final float PITCH_SMOOTHING = 0.18f;
    private static final double CLEAR_FIGHT_DISTANCE = 14.0;
    private static final double VERY_CLOSE_FIGHT_DISTANCE = 5.5;
    private static final double MUTUAL_FACING_DOT = 0.42;
    private static final double ONE_SIDED_FACING_DOT = 0.72;
    private static final double ACTIVE_PAIR_SWITCH_MARGIN = 8.0;
    private static final double ACTIVE_FIGHT_SWITCH_MARGIN = 18.0;

    private static boolean active;
    private static boolean initialized;
    private static Vec3d lastCameraPos = Vec3d.ZERO;
    private static Vec3d currentCameraPos = Vec3d.ZERO;
    private static Vec3d lastLookTarget = Vec3d.ZERO;
    private static Vec3d currentLookTarget = Vec3d.ZERO;
    private static float lastYaw;
    private static float currentYaw;
    private static float lastPitch;
    private static float currentPitch;
    private static PlayerEntity spectatedFirst;
    private static PlayerEntity spectatedSecond;

    private DualSpectateCamera() {
    }

    public static void tick(MinecraftClient client) {
        active = false;
        if (client == null || client.player == null || client.world == null || AtomicsClient.CONFIG == null) {
            reset();
            return;
        }

        TpsConfig.PvpSettings pvp = AtomicsClient.CONFIG.pvp;
        if (!pvp.dualSpectateEnabled) {
            reset();
            return;
        }

        PlayerEntity first;
        PlayerEntity second;
        boolean requireOpposingTeams = shouldRequireOpposingScoreboardTeams(client);
        if (pvp.dualSpectateAutoFill) {
            PlayerPair pair = findAutoSpectatePair(client, pvp, requireOpposingTeams);
            if (pair != null) {
                pvp.dualSpectatePlayerOne = pair.first.getNameForScoreboard();
                pvp.dualSpectatePlayerTwo = pair.second.getNameForScoreboard();
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

        if (first == null || second == null || first.getUuid().equals(second.getUuid())) {
            reset();
            return;
        }
        if (!isAllowedSpectatePair(first, second, requireOpposingTeams)) {
            reset();
            return;
        }
        spectatedFirst = first;
        spectatedSecond = second;

        Vec3d firstPos = first.getEyePos();
        Vec3d secondPos = second.getEyePos();
        Vec3d midpoint = firstPos.add(secondPos).multiply(0.5);
        Vec3d difference = firstPos.subtract(secondPos);
        Vec3d side = orthogonal(difference);
        if (side.horizontalLengthSquared() < 0.0001) {
            side = fallbackSide(client);
        } else {
            side = side.normalize();
        }

        float distance = calculateAutoDistance(client, difference, pvp);
        side = chooseStableSide(midpoint, side);
        Vec3d rawCameraPos = midpoint.add(side.multiply(distance)).add(0.0, DEFAULT_HEIGHT, 0.0);
        Vec3d rawLookTarget = midpoint.add(0.0, DEFAULT_HEIGHT * 0.55, 0.0);
        float positionSmoothing = initialized ? positionSmoothingAmount(currentCameraPos.distanceTo(rawCameraPos)) : 1.0f;
        Vec3d targetCameraPos = initialized
                ? smoothStep(currentCameraPos, rawCameraPos, positionSmoothing)
                : rawCameraPos;
        Vec3d targetLookTarget = initialized
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

        client.player.setYaw(currentYaw);
        client.player.setPitch(currentPitch);
        client.player.setHeadYaw(currentYaw);
        client.player.setBodyYaw(currentYaw);

        if (pvp.dualSpectateForceThirdPerson && client.options.getPerspective().isFirstPerson()) {
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        active = true;
    }

    public static void render(MinecraftClient client, float tickProgress) {
        if (!active || !initialized || client == null || client.player == null || client.gameRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        applyToCamera(camera, client, tickProgress);
    }

    public static void applyToCamera(Camera camera, MinecraftClient client, float tickProgress) {
        if (!active || !initialized || camera == null || client == null || client.player == null) {
            return;
        }

        float progress = clamp(tickProgress, 0.0f, 1.0f);
        Vec3d cameraPos = new Vec3d(
                lastCameraPos.x + (currentCameraPos.x - lastCameraPos.x) * progress,
                lastCameraPos.y + (currentCameraPos.y - lastCameraPos.y) * progress,
                lastCameraPos.z + (currentCameraPos.z - lastCameraPos.z) * progress
        );
        float yaw = lerpAngle(lastYaw, currentYaw, progress);
        float pitch = lerp(lastPitch, currentPitch, progress);

        CameraAccessor accessor = (CameraAccessor) camera;
        accessor.atomics_client$setPos(cameraPos);
        accessor.atomics_client$setRotation(yaw, pitch);
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        client.player.setHeadYaw(yaw);
        client.player.setBodyYaw(yaw);
        if (client.player.isSpectator()) {
            client.player.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static PlayerEntity getOtherSpectatedPlayer(PlayerEntity player) {
        if (!active || player == null || spectatedFirst == null || spectatedSecond == null) {
            return null;
        }
        if (player.getUuid().equals(spectatedFirst.getUuid())) {
            return spectatedSecond;
        }
        if (player.getUuid().equals(spectatedSecond.getUuid())) {
            return spectatedFirst;
        }
        return null;
    }

    public static String[] findNearestPair(MinecraftClient client) {
        PlayerPair pair = findBestPlayerPair(client, shouldRequireOpposingScoreboardTeams(client));
        if (pair == null) {
            return null;
        }

        return new String[]{pair.first.getNameForScoreboard(), pair.second.getNameForScoreboard()};
    }

    private static PlayerPair findAutoSpectatePair(MinecraftClient client, TpsConfig.PvpSettings pvp, boolean requireOpposingTeams) {
        PlayerPair configured = findConfiguredPair(client, pvp, requireOpposingTeams);
        PlayerPair best = findBestPlayerPair(client, requireOpposingTeams);
        if (configured == null) {
            return best;
        }
        if (best == null || isSamePair(configured, best)) {
            return configured;
        }

        PairScore configuredScore = scorePair(client, configured);
        PairScore bestScore = scorePair(client, best);
        return shouldSwitchPair(configuredScore, bestScore) ? best : configured;
    }

    private static PlayerPair findBestPlayerPair(MinecraftClient client, boolean requireOpposingTeams) {
        List<PlayerEntity> candidates = autofillCandidates(client);
        PlayerPair bestPair = null;
        PairScore bestScore = null;
        for (int i = 0; i < candidates.size(); i++) {
            PlayerEntity first = candidates.get(i);
            for (int j = i + 1; j < candidates.size(); j++) {
                PlayerEntity second = candidates.get(j);
                if (!isAllowedSpectatePair(first, second, requireOpposingTeams)) {
                    continue;
                }

                PlayerPair pair = new PlayerPair(first, second);
                PairScore score = scorePair(client, pair);
                if (bestScore == null || isBetterPair(score, bestScore)) {
                    bestPair = pair;
                    bestScore = score;
                }
            }
        }
        return bestPair;
    }

    private static PlayerPair findConfiguredPair(MinecraftClient client, TpsConfig.PvpSettings pvp, boolean requireOpposingTeams) {
        PlayerEntity first = findPlayer(client, pvp.dualSpectatePlayerOne);
        PlayerEntity second = findPlayer(client, pvp.dualSpectatePlayerTwo);
        if (isAutofillCandidate(client, first, second)
                && isAutofillCandidate(client, second, first)
                && isAllowedSpectatePair(first, second, requireOpposingTeams)) {
            return new PlayerPair(first, second);
        }
        return null;
    }

    private static List<PlayerEntity> autofillCandidates(MinecraftClient client) {
        ArrayList<PlayerEntity> candidates = new ArrayList<>();
        if (client == null || client.world == null || client.player == null) {
            return candidates;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (isAutofillCandidate(client, player, null)) {
                candidates.add(player);
            }
        }
        return candidates;
    }

    private static boolean isAutofillCandidate(MinecraftClient client, PlayerEntity player, PlayerEntity excluded) {
        return player != null
                && client.player != null
                && !player.getUuid().equals(client.player.getUuid())
                && (excluded == null || !player.getUuid().equals(excluded.getUuid()))
                && !player.isSpectator()
                && !player.isDead()
                && player.isAlive();
    }

    private static boolean shouldRequireOpposingScoreboardTeams(MinecraftClient client) {
        if (client == null || client.world == null) {
            return false;
        }

        Set<String> teams = new HashSet<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == null) {
                continue;
            }
            if (player == client.player) {
                if (player.isSpectator() || player.isDead() || !player.isAlive()) {
                    continue;
                }
            } else if (!isAutofillCandidate(client, player, null)) {
                continue;
            }

            Team team = player.getScoreboardTeam();
            if (team != null && team.getName() != null && !team.getName().isBlank()) {
                teams.add(team.getName());
                if (teams.size() > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAllowedSpectatePair(PlayerEntity first, PlayerEntity second, boolean requireOpposingTeams) {
        if (first == null || second == null || first.getUuid().equals(second.getUuid())) {
            return false;
        }
        if (!requireOpposingTeams) {
            return true;
        }

        Team firstTeam = first.getScoreboardTeam();
        Team secondTeam = second.getScoreboardTeam();
        return firstTeam != null
                && secondTeam != null
                && firstTeam.getName() != null
                && secondTeam.getName() != null
                && !firstTeam.getName().equals(secondTeam.getName());
    }

    private static PairScore scorePair(MinecraftClient client, PlayerPair pair) {
        double distance = pair.first.distanceTo(pair.second);
        double firstFacing = facingDot(pair.first, pair.second);
        double secondFacing = facingDot(pair.second, pair.first);
        boolean clearlyFighting = isClearlyFighting(distance, firstFacing, secondFacing);

        Vec3d midpoint = pair.first.getEyePos().add(pair.second.getEyePos()).multiply(0.5);
        double cameraDistance = client == null || client.player == null ? 0.0 : Math.sqrt(midpoint.squaredDistanceTo(client.player.getEyePos()));
        double facingBonus = Math.max(0.0, firstFacing) + Math.max(0.0, secondFacing);
        double score = distance * 5.0 + cameraDistance * 0.35 - facingBonus * 8.0;
        if (clearlyFighting) {
            score -= 50.0;
        }
        return new PairScore(clearlyFighting, score);
    }

    private static boolean isClearlyFighting(double distance, double firstFacing, double secondFacing) {
        if (distance <= VERY_CLOSE_FIGHT_DISTANCE) {
            return true;
        }
        if (distance > CLEAR_FIGHT_DISTANCE) {
            return false;
        }
        boolean mutualFacing = firstFacing >= MUTUAL_FACING_DOT && secondFacing >= MUTUAL_FACING_DOT;
        boolean oneSideLocked = firstFacing >= ONE_SIDED_FACING_DOT || secondFacing >= ONE_SIDED_FACING_DOT;
        return mutualFacing || oneSideLocked;
    }

    private static double facingDot(PlayerEntity from, PlayerEntity to) {
        if (from == null || to == null) {
            return -1.0;
        }
        Vec3d direction = to.getEyePos().subtract(from.getEyePos());
        if (direction.lengthSquared() < 0.0001) {
            return 1.0;
        }
        return from.getRotationVec(1.0f).normalize().dotProduct(direction.normalize());
    }

    private static boolean isBetterPair(PairScore candidate, PairScore currentBest) {
        if (candidate.clearlyFighting != currentBest.clearlyFighting) {
            return candidate.clearlyFighting;
        }
        return candidate.score < currentBest.score;
    }

    private static boolean shouldSwitchPair(PairScore current, PairScore candidate) {
        if (candidate.clearlyFighting && !current.clearlyFighting) {
            return true;
        }
        if (!candidate.clearlyFighting && current.clearlyFighting) {
            return false;
        }
        double margin = current.clearlyFighting ? ACTIVE_FIGHT_SWITCH_MARGIN : ACTIVE_PAIR_SWITCH_MARGIN;
        return candidate.score + margin < current.score;
    }

    private static boolean isSamePair(PlayerPair a, PlayerPair b) {
        if (a == null || b == null) {
            return false;
        }
        boolean sameOrder = a.first.getUuid().equals(b.first.getUuid()) && a.second.getUuid().equals(b.second.getUuid());
        boolean reverseOrder = a.first.getUuid().equals(b.second.getUuid()) && a.second.getUuid().equals(b.first.getUuid());
        return sameOrder || reverseOrder;
    }

    private static PlayerEntity findPlayer(MinecraftClient client, String username) {
        String normalized = normalizeName(username);
        if (normalized.isEmpty()) return null;
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player != null && normalizeName(player.getNameForScoreboard()).equals(normalized)) {
                return player;
            }
        }
        return null;
    }

    private static float calculateAutoDistance(MinecraftClient client, Vec3d difference, TpsConfig.PvpSettings pvp) {
        double fovDegrees = 70.0;
        try {
            fovDegrees = Math.max(30.0, Math.min(110.0, client.options.getFov().getValue()));
        } catch (RuntimeException ignored) {
            fovDegrees = 70.0;
        }

        double aspectRatio = 16.0 / 9.0;
        if (client.getWindow() != null && client.getWindow().getScaledHeight() > 0) {
            aspectRatio = (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getScaledHeight();
        }

        double horizontalFov = fovDegrees * aspectRatio * 0.8;
        double horizontalDistance = Math.abs((difference.horizontalLength() * 0.5) / Math.tan(Math.toRadians(horizontalFov * 0.5)));
        double verticalDistance = Math.abs((difference.y * 0.5) / Math.tan(Math.toRadians(fovDegrees * 0.5)));
        float needed = (float) (Math.max(horizontalDistance, verticalDistance) * pvp.dualSpectatePadding);
        return clamp(needed, Math.max(DEFAULT_DISTANCE, pvp.dualSpectateMinDistance), pvp.dualSpectateMaxDistance);
    }

    private static Vec3d orthogonal(Vec3d vector) {
        return new Vec3d(vector.z, 0.0, -vector.x);
    }

    private static Vec3d fallbackSide(MinecraftClient client) {
        float yawRadians = (float) Math.toRadians(client.player.getYaw());
        return new Vec3d(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
    }

    private static Vec3d chooseStableSide(Vec3d midpoint, Vec3d side) {
        if (!initialized || side.horizontalLengthSquared() < 0.0001) {
            return side;
        }

        double dot = (currentCameraPos.x - midpoint.x) * side.x + (currentCameraPos.z - midpoint.z) * side.z;
        return dot < 0.0 ? side.multiply(-1.0) : side;
    }

    private static float calculateYaw(Vec3d cameraPos, Vec3d targetPos) {
        double directionX = targetPos.x - cameraPos.x;
        double directionZ = targetPos.z - cameraPos.z;
        return (float) (Math.toDegrees(Math.atan2(directionZ, directionX)) - 90.0);
    }

    private static float calculatePitch(Vec3d cameraPos, Vec3d targetPos) {
        double directionX = targetPos.x - cameraPos.x;
        double directionY = targetPos.y - cameraPos.y;
        double directionZ = targetPos.z - cameraPos.z;
        double horizontal = Math.sqrt(directionX * directionX + directionZ * directionZ);
        return clamp((float) -Math.toDegrees(Math.atan2(directionY, horizontal)), -89.0f, 89.0f);
    }

    private static Vec3d smoothStep(Vec3d current, Vec3d target, float amount) {
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
        lastCameraPos = Vec3d.ZERO;
        currentCameraPos = Vec3d.ZERO;
        lastLookTarget = Vec3d.ZERO;
        currentLookTarget = Vec3d.ZERO;
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

    private record PlayerPair(PlayerEntity first, PlayerEntity second) {
    }

    private record PairScore(boolean clearlyFighting, double score) {
    }
}
