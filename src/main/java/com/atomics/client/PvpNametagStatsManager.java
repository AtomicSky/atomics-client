package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PvpNametagStatsManager {
    private static final double TRACKING_RADIUS = 100.0;
    private static final double TRACKING_RADIUS_SQ = TRACKING_RADIUS * TRACKING_RADIUS;
    private static final float POP_HEALTH_WEIGHT = 20.0f;
    private static final Map<UUID, OpponentStats> OPPONENT_STATS = new HashMap<>();

    private static Object activeWorld;
    private static UUID activeLocalPlayerUuid;
    private static int localTotemPops;

    private PvpNametagStatsManager() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            clearSession();
            return;
        }

        UUID localUuid = client.player.getUuid();
        if (client.world != activeWorld || !localUuid.equals(activeLocalPlayerUuid)) {
            clearSession();
            activeWorld = client.world;
            activeLocalPlayerUuid = localUuid;
            return;
        }

        if (OPPONENT_STATS.size() > 256) {
            pruneMissingPlayers(client);
        }
    }

    public static void recordTotemPop(Entity entity) {
        if (entity == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            tick(client);
        }
        if (isLocalPlayer(entity)) {
            localTotemPops++;
        } else if (entity instanceof PlayerEntity player) {
            statsFor(player).totemPops++;
        }
    }

    public static void resetTotemPopCounts(MinecraftClient client) {
        OPPONENT_STATS.clear();
        localTotemPops = 0;
        if (client == null || client.world == null || client.player == null) {
            activeWorld = null;
            activeLocalPlayerUuid = null;
            return;
        }
        activeWorld = client.world;
        activeLocalPlayerUuid = client.player.getUuid();
    }

    public static Text getWinPercentNameSuffix(PlayerEntity player) {
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (player == null || pvp == null || !pvp.winOddsEnabled || isLocalPlayer(player)) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return null;
        }

        PlayerEntity spectatedOpponent = DualSpectateCamera.getOtherSpectatedPlayer(player);
        int percent;
        if (spectatedOpponent != null) {
            percent = getHealthAndPopWinPercent(
                    readCombatHealth(client, player),
                    readCombatHealth(client, spectatedOpponent),
                    statsFor(player).totemPops,
                    statsFor(spectatedOpponent).totemPops
            );
        } else {
            if (!isTrackableOpponent(client, player)) {
                return null;
            }
            percent = getHealthAndPopWinPercent(readCombatHealth(client, client.player), readCombatHealth(client, player), localTotemPops, statsFor(player).totemPops);
        }
        return Text.literal(percent + "%").withColor(winPercentColor(percent));
    }

    public static Text getTotemPopNameSuffix(PlayerEntity player) {
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (player == null || pvp == null || !pvp.totemPopNametagEnabled || isLocalPlayer(player)) {
            return null;
        }
        int pops = statsFor(player).totemPops;
        if (pops <= 0) {
            return null;
        }
        return Text.literal(formatPopCount(pops)).formatted(Formatting.GOLD);
    }

    private static int getHealthAndPopWinPercent(float localHealth, float opponentHealth, int localPops, int opponentPops) {
        float localScore = Math.max(0.0f, localHealth) + Math.max(0, opponentPops) * POP_HEALTH_WEIGHT;
        float opponentScore = Math.max(0.0f, opponentHealth) + Math.max(0, localPops) * POP_HEALTH_WEIGHT;
        float total = localScore + opponentScore;
        if (total <= 0.0f || !Float.isFinite(total)) {
            return 50;
        }
        return Math.max(0, Math.min(100, Math.round(localScore * 100.0f / total)));
    }

    private static float readCombatHealth(MinecraftClient client, PlayerEntity player) {
        Float scoreboardHealth = readBelowNameHealth(client, player);
        return scoreboardHealth == null ? readHealth(player) : scoreboardHealth + readAbsorption(player);
    }

    private static Float readBelowNameHealth(MinecraftClient client, PlayerEntity player) {
        if (client == null || client.world == null || player == null) {
            return null;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (!isHealthObjective(objective)) {
            return null;
        }

        ReadableScoreboardScore score = scoreboard.getScore(player, objective);
        if (score == null || score.getScore() < 0) {
            return null;
        }
        return (float) score.getScore();
    }

    private static boolean isHealthObjective(ScoreboardObjective objective) {
        return objective != null
                && (objective.getCriterion() == ScoreboardCriterion.HEALTH
                || objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS);
    }

    private static float readHealth(PlayerEntity player) {
        return player == null ? 0.0f : player.getHealth() + readAbsorption(player);
    }
    private static float readAbsorption(PlayerEntity player) {
        return player == null ? 0.0f : player.getAbsorptionAmount();
    }


    private static boolean isTrackableOpponent(MinecraftClient client, PlayerEntity other) {
        if (client == null || client.player == null || other == null || isLocalPlayer(other) || other.isSpectator() || other.isDead() || !other.isAlive()) {
            return false;
        }
        double dx = other.getX() - client.player.getX();
        double dy = other.getY() - client.player.getY();
        double dz = other.getZ() - client.player.getZ();
        return dx * dx + dy * dy + dz * dz <= TRACKING_RADIUS_SQ;
    }

    private static TpsConfig.PvpSettings livePvpSettings() {
        return AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled ? null : AtomicsClient.CONFIG.pvp;
    }

    private static OpponentStats statsFor(PlayerEntity player) {
        return OPPONENT_STATS.computeIfAbsent(player.getUuid(), uuid -> new OpponentStats());
    }

    private static boolean isLocalPlayer(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.player != null && entity != null && entity.getUuid().equals(client.player.getUuid());
    }

    private static void pruneMissingPlayers(MinecraftClient client) {
        Iterator<UUID> iterator = OPPONENT_STATS.keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            boolean present = client.world.getPlayers().stream().anyMatch(player -> uuid.equals(player.getUuid()));
            if (!present) {
                iterator.remove();
            }
        }
    }

    private static void clearSession() {
        OPPONENT_STATS.clear();
        localTotemPops = 0;
        activeWorld = null;
        activeLocalPlayerUuid = null;
    }

    private static String formatPopCount(int pops) {
        return pops == 1 ? "1 pop" : pops + " pops";
    }

    private static int winPercentColor(int percent) {
        float t = Math.max(0.0f, Math.min(1.0f, percent / 100.0f));
        int red;
        int green;
        if (t < 0.5f) {
            red = 255;
            green = Math.round(t * 2.0f * 255.0f);
        } else {
            red = Math.round((1.0f - t) * 2.0f * 255.0f);
            green = 255;
        }
        return (red << 16) | (green << 8);
    }

    private static final class OpponentStats {
        int totemPops;
    }
}
