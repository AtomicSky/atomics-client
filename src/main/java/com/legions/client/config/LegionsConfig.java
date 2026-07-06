package com.legions.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.legions.client.LegionsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LegionsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public List<String> allowedServerAddresses = new ArrayList<>(List.of("legions"));
    public boolean ratingNametagsEnabled = true;
    public boolean automaticFoeOutlinesEnabled = false;
    public boolean spectatorGlowEnabled = true;
    public boolean warningParticlesEnabled = true;
    public boolean teamPingEnabled = true;
    public boolean blockPingDistanceLabelEnabled = true;
    public boolean teamHudEnabled = true;
    public boolean teamCountOverlayEnabled = false;
    public boolean opponentLimitEnabled = true;
    public boolean playerRenderOptimizationEnabled = true;
    public int opponentLimit = 5;
    public int playerRenderDistance = 64;
    public int pingDurationSeconds = 10;
    public int teamHudX = 8;
    public int teamHudY = 8;
    public int teamCountOverlayX = -1;
    public int teamCountOverlayY = -1;

    public static LegionsConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("legions_client.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                LegionsConfig config = GSON.fromJson(reader, LegionsConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException | RuntimeException e) {
                LegionsClient.LOGGER.warn("Failed to load Legions Client config", e);
            }
        }
        LegionsConfig config = new LegionsConfig();
        config.save(path);
        return config;
    }

    public LegionsConfig normalize() {
        allowedServerAddresses = normalizeServerAddresses(allowedServerAddresses);
        opponentLimit = clamp(opponentLimit, 1, 20);
        playerRenderDistance = clamp(playerRenderDistance, 16, 160);
        pingDurationSeconds = clamp(pingDurationSeconds, 1, 25);
        teamHudX = clamp(teamHudX, 0, 10000);
        teamHudY = clamp(teamHudY, 0, 10000);
        teamCountOverlayX = clamp(teamCountOverlayX, -1, 10000);
        teamCountOverlayY = clamp(teamCountOverlayY, -1, 10000);
        return this;
    }

    public LegionsConfig copy() {
        LegionsConfig copy = new LegionsConfig();
        copy.enabled = enabled;
        copy.allowedServerAddresses = new ArrayList<>(allowedServerAddresses);
        copy.ratingNametagsEnabled = ratingNametagsEnabled;
        copy.automaticFoeOutlinesEnabled = automaticFoeOutlinesEnabled;
        copy.spectatorGlowEnabled = spectatorGlowEnabled;
        copy.warningParticlesEnabled = warningParticlesEnabled;
        copy.teamPingEnabled = teamPingEnabled;
        copy.blockPingDistanceLabelEnabled = blockPingDistanceLabelEnabled;
        copy.teamHudEnabled = teamHudEnabled;
        copy.teamCountOverlayEnabled = teamCountOverlayEnabled;
        copy.opponentLimitEnabled = opponentLimitEnabled;
        copy.playerRenderOptimizationEnabled = playerRenderOptimizationEnabled;
        copy.opponentLimit = opponentLimit;
        copy.playerRenderDistance = playerRenderDistance;
        copy.pingDurationSeconds = pingDurationSeconds;
        copy.teamHudX = teamHudX;
        copy.teamHudY = teamHudY;
        copy.teamCountOverlayX = teamCountOverlayX;
        copy.teamCountOverlayY = teamCountOverlayY;
        return copy.normalize();
    }

    public boolean sameSettings(LegionsConfig other) {
        return other != null
                && enabled == other.enabled
                && allowedServerAddresses.equals(other.allowedServerAddresses)
                && ratingNametagsEnabled == other.ratingNametagsEnabled
                && automaticFoeOutlinesEnabled == other.automaticFoeOutlinesEnabled
                && spectatorGlowEnabled == other.spectatorGlowEnabled
                && warningParticlesEnabled == other.warningParticlesEnabled
                && teamPingEnabled == other.teamPingEnabled
                && blockPingDistanceLabelEnabled == other.blockPingDistanceLabelEnabled
                && teamHudEnabled == other.teamHudEnabled
                && teamCountOverlayEnabled == other.teamCountOverlayEnabled
                && opponentLimitEnabled == other.opponentLimitEnabled
                && playerRenderOptimizationEnabled == other.playerRenderOptimizationEnabled
                && opponentLimit == other.opponentLimit
                && playerRenderDistance == other.playerRenderDistance
                && pingDurationSeconds == other.pingDurationSeconds
                && teamHudX == other.teamHudX
                && teamHudY == other.teamHudY
                && teamCountOverlayX == other.teamCountOverlayX
                && teamCountOverlayY == other.teamCountOverlayY;
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException | RuntimeException e) {
            LegionsClient.LOGGER.warn("Failed to save Legions Client config", e);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> normalizeServerAddresses(List<String> addresses) {
        ArrayList<String> normalized = new ArrayList<>();
        if (addresses != null) {
            for (String address : addresses) {
                if (address == null) {
                    continue;
                }
                String cleaned = address.trim().toLowerCase(Locale.ROOT);
                if (!cleaned.isBlank() && !normalized.contains(cleaned)) {
                    normalized.add(cleaned);
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("legions");
        }
        return normalized;
    }
}
