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

public class LegionsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean ratingNametagsEnabled = true;
    public boolean automaticFoeOutlinesEnabled = false;
    public boolean spectatorGlowEnabled = true;
    public boolean warningParticlesEnabled = true;
    public boolean teamPingEnabled = true;
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
        opponentLimit = clamp(opponentLimit, 1, 20);
        playerRenderDistance = clamp(playerRenderDistance, 16, 160);
        pingDurationSeconds = clamp(pingDurationSeconds, 1, 25);
        teamHudX = clamp(teamHudX, 0, 10000);
        teamHudY = clamp(teamHudY, 0, 10000);
        teamCountOverlayX = clamp(teamCountOverlayX, -1, 10000);
        teamCountOverlayY = clamp(teamCountOverlayY, -1, 10000);
        return this;
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
}
