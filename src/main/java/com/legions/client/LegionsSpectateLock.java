package com.legions.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class LegionsSpectateLock {
    private static final double LOOK_RANGE = 128.0;
    private static String lockedPlayerName;
    private static boolean savedAtomicsState;
    private static boolean savedDualSpectateEnabled;
    private static boolean savedDualSpectateAutoFill;
    private static String savedDualSpectatePlayerOne = "";
    private static String savedDualSpectatePlayerTwo = "";
    private static String lastSecondPlayerName = "";

    private LegionsSpectateLock() {
    }

    public static void handleKeyPress(MinecraftClient client) {
        if (!LegionsClient.isAtomicsClientLoaded()) {
            sendAction(client, "Atomics Client is required for dual spectate lock");
            return;
        }
        if (!LegionsClient.enabled(client) || client.player == null || client.world == null) {
            sendAction(client, "Dual spectate lock only works on Legions");
            return;
        }
        if (!LegionsFeatures.isSpectatorTeam(client.player)) {
            sendAction(client, "Dual spectate lock only works while spectating");
            return;
        }

        PlayerEntity target = findLookedAtPlayer(client);
        if (target == null) {
            if (lockedPlayerName != null) {
                unlock(client, true);
            } else {
                sendAction(client, "Look at a player to lock dual spectate");
            }
            return;
        }

        String name = LegionsFeatures.realUsername(target);
        if (name.equalsIgnoreCase(lockedPlayerName)) {
            unlock(client, true);
            return;
        }

        lockTo(client, name);
    }

    public static void tick(MinecraftClient client) {
        if (lockedPlayerName == null) {
            return;
        }
        if (!LegionsClient.isAtomicsClientLoaded() || !LegionsClient.enabled(client) || client.player == null
                || client.world == null || !LegionsFeatures.isSpectatorTeam(client.player)) {
            unlock(client, false);
            return;
        }

        PlayerEntity lockedPlayer = findPlayer(client, lockedPlayerName);
        if (!isDualSpectateCandidate(client, lockedPlayer)) {
            sendAction(client, "Dual spectate lock lost: " + lockedPlayerName);
            unlock(client, false);
            return;
        }

        try {
            Object pvp = atomicsPvp();
            setField(pvp, "dualSpectateEnabled", true);
            setField(pvp, "dualSpectateAutoFill", false);
            setField(pvp, "dualSpectatePlayerOne", LegionsFeatures.realUsername(lockedPlayer));

            PlayerEntity second = findBestSecondPlayer(client, lockedPlayer);
            String secondName = second == null ? "" : LegionsFeatures.realUsername(second);
            setField(pvp, "dualSpectatePlayerTwo", secondName);
            lastSecondPlayerName = secondName;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Failed to update Atomics dual spectate lock.", e);
            unlock(client, false);
        }
    }

    private static void lockTo(MinecraftClient client, String playerName) {
        try {
            Object pvp = atomicsPvp();
            if (!savedAtomicsState) {
                savedDualSpectateEnabled = getBooleanField(pvp, "dualSpectateEnabled");
                savedDualSpectateAutoFill = getBooleanField(pvp, "dualSpectateAutoFill");
                savedDualSpectatePlayerOne = getStringField(pvp, "dualSpectatePlayerOne");
                savedDualSpectatePlayerTwo = getStringField(pvp, "dualSpectatePlayerTwo");
                savedAtomicsState = true;
            }

            lockedPlayerName = playerName;
            lastSecondPlayerName = "";
            tick(client);
            String suffix = lastSecondPlayerName == null || lastSecondPlayerName.isBlank() ? "" : " + " + lastSecondPlayerName;
            sendAction(client, "Dual spectate locked: " + playerName + suffix);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Failed to lock Atomics dual spectate.", e);
            sendAction(client, "Could not lock Atomics dual spectate");
        }
    }

    private static void unlock(MinecraftClient client, boolean notify) {
        String previous = lockedPlayerName;
        lockedPlayerName = null;
        lastSecondPlayerName = "";
        try {
            if (savedAtomicsState) {
                Object pvp = atomicsPvp();
                setField(pvp, "dualSpectateEnabled", savedDualSpectateEnabled);
                setField(pvp, "dualSpectateAutoFill", savedDualSpectateAutoFill);
                setField(pvp, "dualSpectatePlayerOne", savedDualSpectatePlayerOne);
                setField(pvp, "dualSpectatePlayerTwo", savedDualSpectatePlayerTwo);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Failed to restore Atomics dual spectate settings.", e);
        } finally {
            savedAtomicsState = false;
            savedDualSpectatePlayerOne = "";
            savedDualSpectatePlayerTwo = "";
        }
        if (notify) {
            sendAction(client, previous == null || previous.isBlank() ? "Dual spectate unlocked" : "Dual spectate unlocked: " + previous);
        }
    }

    private static PlayerEntity findBestSecondPlayer(MinecraftClient client, PlayerEntity lockedPlayer) {
        PlayerEntity opponent = findBestSecondPlayer(client, lockedPlayer, true);
        return opponent == null ? findBestSecondPlayer(client, lockedPlayer, false) : opponent;
    }

    private static PlayerEntity findBestSecondPlayer(MinecraftClient client, PlayerEntity lockedPlayer, boolean requireOpponent) {
        PlayerEntity best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (!isDualSpectateCandidate(client, candidate)
                    || candidate.getUuid().equals(lockedPlayer.getUuid())
                    || requireOpponent && !LegionsFeatures.isOpponent(lockedPlayer, candidate)) {
                continue;
            }

            double distance = lockedPlayer.squaredDistanceTo(candidate);
            double facingBonus = Math.max(0.0, facingDot(lockedPlayer, candidate))
                    + Math.max(0.0, facingDot(candidate, lockedPlayer));
            double score = distance - facingBonus * 10.0;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static double facingDot(PlayerEntity from, PlayerEntity to) {
        Vec3d direction = to.getEyePos().subtract(from.getEyePos());
        if (direction.lengthSquared() < 0.0001) {
            return 1.0;
        }
        return from.getRotationVec(1.0f).normalize().dotProduct(direction.normalize());
    }

    private static boolean isDualSpectateCandidate(MinecraftClient client, PlayerEntity player) {
        return player != null
                && client != null
                && client.player != null
                && !player.getUuid().equals(client.player.getUuid())
                && !LegionsFeatures.isSpectatorTeam(player)
                && !player.isDead()
                && player.isAlive();
    }

    private static PlayerEntity findLookedAtPlayer(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        if (client.crosshairTarget instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof PlayerEntity player
                && isDualSpectateCandidate(client, player)) {
            return player;
        }
        if (client.targetedEntity instanceof PlayerEntity player && isDualSpectateCandidate(client, player)) {
            return player;
        }

        Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d direction = camera.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(LOOK_RANGE));

        PlayerEntity best = null;
        double bestDistanceSq = LOOK_RANGE * LOOK_RANGE;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (!isDualSpectateCandidate(client, candidate)) {
                continue;
            }

            Box box = candidate.getBoundingBox().expand(Math.max(0.35, candidate.getTargetingMargin() + 0.3));
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

    private static PlayerEntity findPlayer(MinecraftClient client, String name) {
        if (client == null || client.world == null || name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (PlayerEntity player : client.world.getPlayers()) {
            if (LegionsFeatures.realUsername(player).toLowerCase(Locale.ROOT).equals(normalized)) {
                return player;
            }
        }
        return null;
    }

    private static Object atomicsPvp() throws ReflectiveOperationException {
        Class<?> atomicsClient = Class.forName("com.atomics.client.AtomicsClient");
        Object config = getStaticField(atomicsClient, "CONFIG");
        if (config == null) {
            throw new NoSuchFieldException("Atomics CONFIG is null");
        }
        Object pvp = getField(config, "pvp");
        if (pvp == null) {
            throw new NoSuchFieldException("Atomics pvp config is null");
        }
        return pvp;
    }

    private static Object getStaticField(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object getField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static boolean getBooleanField(Object owner, String name) throws ReflectiveOperationException {
        Object value = getField(owner, name);
        return value instanceof Boolean bool && bool;
    }

    private static String getStringField(Object owner, String name) throws ReflectiveOperationException {
        Object value = getField(owner, name);
        return value instanceof String text ? text : "";
    }

    private static void setField(Object owner, String name, Object value) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void sendAction(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }
}
