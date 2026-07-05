package com.legions.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsFeatures {
    private static final Pattern RATING_PATTERN = Pattern.compile("\\[(\\d{1,5}|\\?)\\]");
    private static final boolean ATOMICS_CLIENT_LOADED = FabricLoader.getInstance().isModLoaded("atomics_client");
    private static int particleTick;

    private LegionsFeatures() {
    }

    public static boolean isLegionsServer(MinecraftClient client) {
        if (client == null) {
            return false;
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null || server.address == null) {
            return client.isIntegratedServerRunning();
        }
        String address = server.address.toLowerCase(Locale.ROOT);
        return address.contains("legions");
    }

    public static void tick(MinecraftClient client) {
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.warningParticlesEnabled || client.world == null || client.player == null) {
            return;
        }
        particleTick++;
        if (particleTick % 18 != 0) {
            return;
        }

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !isOpponent(client.player, player)) {
                continue;
            }
            int rating = getRating(client, realUsername(player));
            if (rating < 1800) {
                continue;
            }
            double x = player.getX();
            double y = player.getY() + player.getHeight() + 0.25;
            double z = player.getZ();
            client.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.035, 0.0);
        }
    }

    public static Text customizeNametag(PlayerEntity player, Text original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.ratingNametagsEnabled) {
            return original;
        }
        int rating = getRating(client, realUsername(player));
        if (rating < 0) {
            return original;
        }
        MutableText suffix = Text.literal(" [" + rating + "]");
        suffix = suffix.formatted(rating >= 2000 ? Formatting.RED : rating >= 1600 ? Formatting.GOLD : Formatting.GRAY);
        Text base = ATOMICS_CLIENT_LOADED ? Text.literal(realUsername(player)) : original;
        String originalText = original.getString();
        String ratingSuffix = " [" + rating + "]";
        if (!ATOMICS_CLIENT_LOADED && originalText.endsWith(ratingSuffix)) {
            return original;
        }
        return Text.empty().append(base).append(suffix);
    }

    public static int getOutlineColor(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || client.player == null) {
            return 0;
        }
        if (LegionsPingManager.isMarkedPlayer(player)) {
            return 0xFFFFD84A;
        }
        if (LegionsClient.CONFIG.spectatorGlowEnabled && isSpectatorTeam(player)) {
            return 0xFF6AA8FF;
        }
        if (LegionsClient.CONFIG.automaticFoeOutlinesEnabled && isOpponent(client.player, player)) {
            int rating = getRating(client, realUsername(player));
            return rating >= 2000 ? 0xFFFF3B30 : rating >= 1600 ? 0xFFFF9500 : 0xFFFFD84A;
        }
        return 0;
    }

    public static boolean shouldHidePlayerModel(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || client.world == null || client.player == null || player == null) {
            return false;
        }
        if (player == client.player || LegionsPingManager.isMarkedPlayer(player) || !isOpponent(client.player, player)) {
            return false;
        }

        int visibleOpponents = Math.max(0, LegionsClient.CONFIG.opponentLimit);
        return client.world.getPlayers().stream()
                .filter(candidate -> isOpponent(client.player, candidate))
                .sorted(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(client.player)))
                .limit(visibleOpponents)
                .noneMatch(candidate -> candidate.getUuid().equals(player.getUuid()));
    }

    public static boolean shouldHidePlayerRenderState(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || state == null) {
            return false;
        }
        Entity entity = client.world.getEntityById(state.id);
        return entity instanceof PlayerEntity player && shouldHidePlayerModel(player);
    }

    public static int getRating(MinecraftClient client, String playerName) {
        if (client == null || client.getNetworkHandler() == null || playerName == null) {
            return -1;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (!playerName.equalsIgnoreCase(entry.getProfile().name())) {
                continue;
            }
            Text displayName = entry.getDisplayName();
            String text = displayName == null ? entry.getProfile().name() : displayName.getString();
            Matcher matcher = RATING_PATTERN.matcher(text);
            while (matcher.find()) {
                String value = matcher.group(1);
                if (!"?".equals(value)) {
                    return Integer.parseInt(value);
                }
            }
        }
        return -1;
    }

    public static boolean isTeammate(PlayerEntity local, PlayerEntity other) {
        if (local == null || other == null) {
            return false;
        }
        if (local == other) {
            return true;
        }
        Team localTeam = local.getScoreboardTeam();
        Team otherTeam = other.getScoreboardTeam();
        return localTeam != null && otherTeam != null && localTeam.getName().equals(otherTeam.getName());
    }

    public static boolean isOpponent(PlayerEntity local, PlayerEntity other) {
        if (local == null || other == null || local == other) {
            return false;
        }
        if (isSpectatorTeam(local) || isSpectatorTeam(other)) {
            return false;
        }
        Team localTeam = local.getScoreboardTeam();
        Team otherTeam = other.getScoreboardTeam();
        return localTeam != null && otherTeam != null && !localTeam.getName().equals(otherTeam.getName());
    }

    public static boolean isSpectatorTeam(PlayerEntity player) {
        if (player == null || player.isSpectator()) {
            return true;
        }
        Team team = player.getScoreboardTeam();
        return team != null && team.getName().toLowerCase(Locale.ROOT).contains("spectator");
    }

    public static String realUsername(PlayerEntity player) {
        return player.getGameProfile().name();
    }
}
