package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

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

    public static void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            clearSession();
            return;
        }

        UUID localUuid = client.player.getUUID();
        if (client.level != activeWorld || !localUuid.equals(activeLocalPlayerUuid)) {
            clearSession();
            activeWorld = client.level;
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
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            tick(client);
        }
        if (isLocalPlayer(entity)) {
            localTotemPops++;
        } else if (entity instanceof Player player) {
            statsFor(player).totemPops++;
        }
    }

    public static Component getWinPercentNameSuffix(Player player) {
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (player == null || pvp == null || !pvp.winOddsEnabled || isLocalPlayer(player)) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return null;
        }

        Player spectatedOpponent = DualSpectateCamera.getOtherSpectatedPlayer(player);
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
        return Component.literal(percent + "%").withColor(winPercentColor(percent));
    }

    public static Component getTotemPopNameSuffix(Player player) {
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (player == null || pvp == null || !pvp.totemPopNametagEnabled || isLocalPlayer(player)) {
            return null;
        }
        int pops = statsFor(player).totemPops;
        if (pops <= 0) {
            return null;
        }
        return Component.literal(formatPopCount(pops)).withStyle(ChatFormatting.GOLD);
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

    private static float readCombatHealth(Minecraft client, Player player) {
        Float scoreboardHealth = readBelowNameHealth(client, player);
        return scoreboardHealth == null ? readHealth(player) : scoreboardHealth + readAbsorption(player);
    }

    private static Float readBelowNameHealth(Minecraft client, Player player) {
        if (client == null || client.level == null || player == null) {
            return null;
        }

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
        if (!isHealthObjective(objective)) {
            return null;
        }

        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(player, objective);
        if (score == null || score.value() < 0) {
            return null;
        }
        return (float) score.value();
    }

    private static boolean isHealthObjective(Objective objective) {
        return objective != null
                && (objective.getCriteria() == ObjectiveCriteria.HEALTH
                || objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS);
    }

    private static float readHealth(Player player) {
        return player == null ? 0.0f : player.getHealth() + readAbsorption(player);
    }

    private static float readAbsorption(Player player) {
        return player == null ? 0.0f : player.getAbsorptionAmount();
    }

    private static boolean isTrackableOpponent(Minecraft client, Player other) {
        if (client == null || client.player == null || other == null || isLocalPlayer(other) || other.isSpectator() || other.isDeadOrDying() || !other.isAlive()) {
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

    private static OpponentStats statsFor(Player player) {
        return OPPONENT_STATS.computeIfAbsent(player.getUUID(), uuid -> new OpponentStats());
    }

    private static boolean isLocalPlayer(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.player != null && entity != null && entity.getUUID().equals(client.player.getUUID());
    }

    private static void pruneMissingPlayers(Minecraft client) {
        Iterator<UUID> iterator = OPPONENT_STATS.keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            boolean present = client.level.players().stream().anyMatch(player -> uuid.equals(player.getUUID()));
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