package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.atomics.client.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DualSpectateCamera {
    private static final float DEFAULT_DISTANCE = 6.0f;
    private static final float DEFAULT_HEIGHT = 1.5f;
    private static final double OVERHEAD_PLAYER_MARGIN = 1.5;
    private static final double MIN_OVERHEAD_HALF_EXTENT = 1.25;
    private static final float MIN_POSITION_SMOOTHING = 0.045f;
    private static final float MAX_POSITION_SMOOTHING = 0.42f;
    private static final float PITCH_SMOOTHING = 0.18f;
    private static final double CLEAR_FIGHT_DISTANCE = 14.0;
    private static final double VERY_CLOSE_FIGHT_DISTANCE = 5.5;
    private static final double MUTUAL_FACING_DOT = 0.42;
    private static final double ONE_SIDED_FACING_DOT = 0.72;
    private static final double ACTIVE_PAIR_SWITCH_MARGIN = 8.0;
    private static final double ACTIVE_FIGHT_SWITCH_MARGIN = 18.0;
    private static final double CAMERA_COLLISION_PADDING = 0.35;
    private static final double MIN_CLIPPED_CAMERA_DISTANCE = 1.5;
    private static final double[] CAMERA_SIDE_ANGLES = new double[]{0.0, -35.0, 35.0, -70.0, 70.0, -110.0, 110.0, 180.0};
    private static final double[] CAMERA_HEIGHT_OFFSETS = new double[]{0.0, 0.9, -0.45, 1.7};
    private static final double[] CAMERA_DISTANCE_FACTORS = new double[]{1.0, 0.78, 0.58};

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
        TeamRules teamRules = scoreboardTeamRules(client);
        if (pvp.dualSpectateAutoFill) {
            PlayerPair pair = findAutoSpectatePair(client, pvp, teamRules);
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
        if (!isAllowedSpectatePair(client, first, second, pvp, teamRules)) {
            reset();
            return;
        }
        spectatedFirst = first;
        spectatedSecond = second;

        Vec3d rawLookTarget;
        Vec3d resolvedCameraPos;
        if (pvp.dualSpectateOverheadEnabled) {
            OverheadFrame frame = calculateOverheadFrame(client, pvp, teamRules, first, second);
            rawLookTarget = frame.lookTarget();
            resolvedCameraPos = frame.cameraPos();
        } else {
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
            rawLookTarget = midpoint.add(0.0, DEFAULT_HEIGHT * 0.55, 0.0);
            resolvedCameraPos = resolveCameraPosition(client, first, second, midpoint, side, distance, rawLookTarget, rawCameraPos, pvp);
        }

        float positionSmoothing = initialized ? positionSmoothingAmount(currentCameraPos.distanceTo(resolvedCameraPos)) : 1.0f;
        Vec3d targetCameraPos = initialized
                ? smoothStep(currentCameraPos, resolvedCameraPos, positionSmoothing)
                : resolvedCameraPos;
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
        TpsConfig.PvpSettings pvp = AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.pvp == null
                ? new TpsConfig.PvpSettings()
                : AtomicsClient.CONFIG.pvp;
        PlayerPair pair = findBestPlayerPair(client, pvp, scoreboardTeamRules(client));
        if (pair == null) {
            return null;
        }

        return new String[]{pair.first.getNameForScoreboard(), pair.second.getNameForScoreboard()};
    }

    private static PlayerPair findAutoSpectatePair(MinecraftClient client, TpsConfig.PvpSettings pvp, TeamRules teamRules) {
        PlayerPair configured = findConfiguredPair(client, pvp, teamRules);
        PlayerPair best = findBestPlayerPair(client, pvp, teamRules);
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

    private static PlayerPair findBestPlayerPair(MinecraftClient client, TpsConfig.PvpSettings pvp, TeamRules teamRules) {
        List<PlayerEntity> candidates = autofillCandidates(client);
        PlayerPair bestPair = null;
        PairScore bestScore = null;
        for (int i = 0; i < candidates.size(); i++) {
            PlayerEntity first = candidates.get(i);
            for (int j = i + 1; j < candidates.size(); j++) {
                PlayerEntity second = candidates.get(j);
                if (!isAllowedSpectatePair(client, first, second, pvp, teamRules)) {
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

    private static PlayerPair findConfiguredPair(MinecraftClient client, TpsConfig.PvpSettings pvp, TeamRules teamRules) {
        PlayerEntity first = findPlayer(client, pvp.dualSpectatePlayerOne);
        PlayerEntity second = findPlayer(client, pvp.dualSpectatePlayerTwo);
        if (isAllowedSpectatePair(client, first, second, pvp, teamRules)) {
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
        return isSpectateCandidate(client, player)
                && (excluded == null || !player.getUuid().equals(excluded.getUuid()))
                && player.isAlive();
    }

    private static boolean isSpectateCandidate(MinecraftClient client, PlayerEntity player) {
        return player != null
                && client != null
                && client.player != null
                && !player.getUuid().equals(client.player.getUuid())
                && !player.isSpectator()
                && !player.isDead()
                && player.isAlive();
    }

    private static TeamRules scoreboardTeamRules(MinecraftClient client) {
        if (client == null || client.world == null) {
            return new TeamRules(false, false);
        }

        Set<String> teams = new HashSet<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (!isTeamRuleCandidate(client, player)) {
                continue;
            }

            Team team = player.getScoreboardTeam();
            if (team != null && team.getName() != null && !team.getName().isBlank()) {
                teams.add(team.getName());
            }
        }
        return new TeamRules(!teams.isEmpty(), teams.size() > 1);
    }

    private static boolean isTeamRuleCandidate(MinecraftClient client, PlayerEntity player) {
        if (player == null || player.isSpectator() || player.isDead() || !player.isAlive()) {
            return false;
        }
        return client == null || client.player == null || !player.getUuid().equals(client.player.getUuid()) || !client.player.isSpectator();
    }

    private static boolean isAllowedSpectatePair(MinecraftClient client, PlayerEntity first, PlayerEntity second, TpsConfig.PvpSettings pvp, TeamRules teamRules) {
        if (first == null || second == null || first.getUuid().equals(second.getUuid())) {
            return false;
        }
        if (!isSpectateCandidate(client, first) || !isSpectateCandidate(client, second)) {
            return false;
        }
        if (!isWithinYDifference(first, second, pvp)) {
            return false;
        }
        if (!teamRules.hasScoreboardTeams()) {
            return true;
        }

        Team firstTeam = first.getScoreboardTeam();
        Team secondTeam = second.getScoreboardTeam();
        String firstTeamName = validTeamName(firstTeam);
        String secondTeamName = validTeamName(secondTeam);
        if (firstTeamName.isEmpty() || secondTeamName.isEmpty()) {
            return false;
        }
        return !teamRules.requireOpposingTeams() || !firstTeamName.equals(secondTeamName);
    }

    private static String validTeamName(Team team) {
        return team == null || team.getName() == null ? "" : team.getName().trim();
    }

    private static boolean isWithinYDifference(PlayerEntity first, PlayerEntity second, TpsConfig.PvpSettings pvp) {
        return Math.abs(first.getY() - second.getY()) <= maxYDifference(pvp);
    }

    private static double maxYDifference(TpsConfig.PvpSettings pvp) {
        return pvp == null ? TpsConfig.DEFAULT_DUAL_SPECTATE_MAX_Y_DIFFERENCE : Math.max(2.0, Math.min(48.0, pvp.dualSpectateMaxYDifference));
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
        double fovDegrees = clientFovDegrees(client);
        double aspectRatio = clientAspectRatio(client);

        double horizontalFov = fovDegrees * aspectRatio * 0.8;
        double horizontalDistance = Math.abs((difference.horizontalLength() * 0.5) / Math.tan(Math.toRadians(horizontalFov * 0.5)));
        double verticalDistance = Math.abs((difference.y * 0.5) / Math.tan(Math.toRadians(fovDegrees * 0.5)));
        float needed = (float) (Math.max(horizontalDistance, verticalDistance) * pvp.dualSpectatePadding);
        return clamp(needed, Math.max(DEFAULT_DISTANCE, pvp.dualSpectateMinDistance), pvp.dualSpectateMaxDistance);
    }

    private static OverheadFrame calculateOverheadFrame(MinecraftClient client, TpsConfig.PvpSettings pvp, TeamRules teamRules, PlayerEntity first, PlayerEntity second) {
        List<PlayerEntity> players = findOverheadPlayers(client, pvp, teamRules, first, second);
        OverheadBounds bounds = overheadBounds(players);
        Vec3d lookTarget = new Vec3d(bounds.centerX(), bounds.centerY(), bounds.centerZ());
        float distance = calculateOverheadDistance(client, bounds, pvp);
        Vec3d cameraPos = new Vec3d(lookTarget.x, bounds.maxY() + distance, lookTarget.z);
        return new OverheadFrame(cameraPos, lookTarget);
    }

    private static List<PlayerEntity> findOverheadPlayers(MinecraftClient client, TpsConfig.PvpSettings pvp, TeamRules teamRules, PlayerEntity first, PlayerEntity second) {
        ArrayList<PlayerEntity> players = new ArrayList<>();
        addUniquePlayer(players, first);
        addUniquePlayer(players, second);

        List<PlayerEntity> candidates = autofillCandidates(client);
        double groupDistance = Math.max(4.0, Math.min(80.0, pvp.dualSpectateOverheadGroupDistance));
        double groupDistanceSq = groupDistance * groupDistance;
        double maxYDifference = maxYDifference(pvp);
        boolean changed;
        do {
            changed = false;
            for (PlayerEntity candidate : candidates) {
                if (containsPlayer(players, candidate)
                        || !isAllowedOverheadPlayer(candidate, teamRules)
                        || !isNearAnyPlayer(candidate, players, groupDistanceSq, maxYDifference)) {
                    continue;
                }
                players.add(candidate);
                changed = true;
            }
        } while (changed);

        return players;
    }

    private static boolean isAllowedOverheadPlayer(PlayerEntity player, TeamRules teamRules) {
        if (!teamRules.hasScoreboardTeams()) {
            return true;
        }
        return !validTeamName(player.getScoreboardTeam()).isEmpty();
    }

    private static void addUniquePlayer(List<PlayerEntity> players, PlayerEntity player) {
        if (player != null && !containsPlayer(players, player)) {
            players.add(player);
        }
    }

    private static boolean containsPlayer(List<PlayerEntity> players, PlayerEntity player) {
        if (player == null) {
            return false;
        }
        for (PlayerEntity existing : players) {
            if (existing != null && existing.getUuid().equals(player.getUuid())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearAnyPlayer(PlayerEntity candidate, List<PlayerEntity> players, double maxDistanceSq, double maxYDifference) {
        if (candidate == null) {
            return false;
        }
        for (PlayerEntity player : players) {
            if (player != null
                    && horizontalDistanceSquared(candidate, player) <= maxDistanceSq
                    && Math.abs(candidate.getY() - player.getY()) <= maxYDifference) {
                return true;
            }
        }
        return false;
    }

    private static double horizontalDistanceSquared(PlayerEntity first, PlayerEntity second) {
        double x = first.getX() - second.getX();
        double z = first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private static OverheadBounds overheadBounds(List<PlayerEntity> players) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (PlayerEntity player : players) {
            if (player == null) {
                continue;
            }
            Box box = player.getBoundingBox();
            minX = Math.min(minX, box.minX);
            minY = Math.min(minY, box.minY);
            minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX);
            maxY = Math.max(maxY, box.maxY);
            maxZ = Math.max(maxZ, box.maxZ);
        }

        if (!Double.isFinite(minX)) {
            return new OverheadBounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        return new OverheadBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static float calculateOverheadDistance(MinecraftClient client, OverheadBounds bounds, TpsConfig.PvpSettings pvp) {
        double verticalFovRadians = Math.toRadians(clientFovDegrees(client));
        double horizontalFovRadians = 2.0 * Math.atan(Math.tan(verticalFovRadians * 0.5) * clientAspectRatio(client));
        double halfWidth = Math.max(MIN_OVERHEAD_HALF_EXTENT, bounds.width() * 0.5 + OVERHEAD_PLAYER_MARGIN);
        double halfDepth = Math.max(MIN_OVERHEAD_HALF_EXTENT, bounds.depth() * 0.5 + OVERHEAD_PLAYER_MARGIN);
        double horizontalDistance = halfWidth / Math.tan(horizontalFovRadians * 0.5);
        double verticalDistance = halfDepth / Math.tan(verticalFovRadians * 0.5);
        float needed = (float) (Math.max(horizontalDistance, verticalDistance) * pvp.dualSpectatePadding);
        return clamp(needed, Math.max(DEFAULT_DISTANCE, pvp.dualSpectateMinDistance), pvp.dualSpectateMaxDistance);
    }

    private static double clientFovDegrees(MinecraftClient client) {
        try {
            if (client != null && client.options != null) {
                return Math.max(30.0, Math.min(110.0, client.options.getFov().getValue()));
            }
        } catch (RuntimeException ignored) {
        }
        return 70.0;
    }

    private static double clientAspectRatio(MinecraftClient client) {
        if (client != null && client.getWindow() != null && client.getWindow().getScaledHeight() > 0) {
            return (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getScaledHeight();
        }
        return 16.0 / 9.0;
    }

    private static Vec3d resolveCameraPosition(MinecraftClient client, PlayerEntity first, PlayerEntity second, Vec3d midpoint, Vec3d side, float distance, Vec3d lookTarget, Vec3d preferredCameraPos, TpsConfig.PvpSettings pvp) {
        if (client == null || client.world == null || first == null || second == null || side.horizontalLengthSquared() < 0.0001) {
            return preferredCameraPos;
        }

        CameraCandidate best = null;
        for (double angle : CAMERA_SIDE_ANGLES) {
            Vec3d candidateSide = rotateSide(side, angle);
            if (candidateSide.horizontalLengthSquared() < 0.0001) {
                continue;
            }
            for (double heightOffset : CAMERA_HEIGHT_OFFSETS) {
                for (double distanceFactor : CAMERA_DISTANCE_FACTORS) {
                    double candidateDistance = Math.max(MIN_CLIPPED_CAMERA_DISTANCE, distance * distanceFactor);
                    Vec3d candidate = midpoint
                            .add(candidateSide.multiply(candidateDistance))
                            .add(0.0, DEFAULT_HEIGHT + heightOffset, 0.0);
                    Vec3d clipped = clipCameraToBlocks(client, lookTarget, candidate);
                    double score = scoreCameraCandidate(client, clipped, preferredCameraPos, lookTarget, first, second, distance, angle, heightOffset);
                    if (best == null || score < best.score()) {
                        best = new CameraCandidate(clipped, score);
                    }
                }
            }
        }

        return best == null ? clipCameraToBlocks(client, lookTarget, preferredCameraPos) : best.position();
    }

    private static double scoreCameraCandidate(MinecraftClient client, Vec3d cameraPos, Vec3d preferredCameraPos, Vec3d lookTarget, PlayerEntity first, PlayerEntity second, double preferredDistance, double angle, double heightOffset) {
        double score = cameraPos.squaredDistanceTo(preferredCameraPos) * 0.16;
        score += visibilityPenalty(client, cameraPos, first);
        score += visibilityPenalty(client, cameraPos, second);

        double cameraDistance = cameraPos.distanceTo(lookTarget);
        double minimumUsefulDistance = Math.max(MIN_CLIPPED_CAMERA_DISTANCE, Math.min(preferredDistance * 0.65, 7.0));
        if (cameraDistance < minimumUsefulDistance) {
            score += (minimumUsefulDistance - cameraDistance) * 90.0;
        }

        score += anglePenalty(angle);
        score += Math.abs(heightOffset) * 7.0;
        if (initialized) {
            score += cameraPos.squaredDistanceTo(currentCameraPos) * 0.018;
        }
        return score;
    }

    private static double visibilityPenalty(MinecraftClient client, Vec3d cameraPos, PlayerEntity player) {
        if (player == null) {
            return 900.0;
        }

        int visibleSamples = 0;
        if (hasLineOfSight(client, cameraPos, player.getEyePos())) {
            visibleSamples++;
        }
        if (hasLineOfSight(client, cameraPos, playerBodyTarget(player))) {
            visibleSamples++;
        }
        return switch (visibleSamples) {
            case 2 -> 0.0;
            case 1 -> 85.0;
            default -> 900.0;
        };
    }

    private static Vec3d playerBodyTarget(PlayerEntity player) {
        Box box = player.getBoundingBox();
        return new Vec3d(
                (box.minX + box.maxX) * 0.5,
                box.minY + (box.maxY - box.minY) * 0.58,
                (box.minZ + box.maxZ) * 0.5
        );
    }

    private static Vec3d clipCameraToBlocks(MinecraftClient client, Vec3d from, Vec3d desired) {
        HitResult hit = raycastBlocks(client, from, desired);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return desired;
        }

        Vec3d delta = desired.subtract(from);
        double length = delta.length();
        if (length < 0.0001) {
            return desired;
        }

        double hitDistance = from.distanceTo(hit.getPos());
        double clippedDistance = Math.max(0.0, hitDistance - CAMERA_COLLISION_PADDING);
        return from.add(delta.multiply(clippedDistance / length));
    }

    private static boolean hasLineOfSight(MinecraftClient client, Vec3d from, Vec3d to) {
        HitResult hit = raycastBlocks(client, from, to);
        return hit == null
                || hit.getType() == HitResult.Type.MISS
                || hit.getPos().squaredDistanceTo(to) <= CAMERA_COLLISION_PADDING * CAMERA_COLLISION_PADDING;
    }

    private static HitResult raycastBlocks(MinecraftClient client, Vec3d from, Vec3d to) {
        if (client == null || client.world == null || client.player == null || from == null || to == null) {
            return null;
        }
        return client.world.raycast(new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
    }

    private static Vec3d rotateSide(Vec3d side, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vec3d rotated = new Vec3d(side.x * cos - side.z * sin, 0.0, side.x * sin + side.z * cos);
        return rotated.horizontalLengthSquared() < 0.0001 ? side : rotated.normalize();
    }

    private static double anglePenalty(double angle) {
        double normalized = Math.abs(angle) % 360.0;
        normalized = Math.min(normalized, 360.0 - normalized);
        if (normalized <= 1.0) {
            return 0.0;
        }
        if (normalized <= 40.0) {
            return 4.0;
        }
        if (normalized <= 80.0) {
            return 11.0;
        }
        if (normalized <= 120.0) {
            return 18.0;
        }
        return 26.0;
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

    private record TeamRules(boolean hasScoreboardTeams, boolean requireOpposingTeams) {
    }

    private record CameraCandidate(Vec3d position, double score) {
    }

    private record OverheadFrame(Vec3d cameraPos, Vec3d lookTarget) {
    }

    private record OverheadBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private double centerX() {
            return (minX + maxX) * 0.5;
        }

        private double centerY() {
            return (minY + maxY) * 0.5;
        }

        private double centerZ() {
            return (minZ + maxZ) * 0.5;
        }

        private double width() {
            return maxX - minX;
        }

        private double depth() {
            return maxZ - minZ;
        }
    }
}
