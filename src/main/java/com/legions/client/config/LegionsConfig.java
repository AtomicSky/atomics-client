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
    public int automaticFoeRenderStyle = 1;
    public boolean spectatorGlowEnabled = true;
    public boolean warningParticlesEnabled = true;
    public boolean teamPingEnabled = true;
    public boolean pingLastAttackedPlayerEnabled = false;
    public boolean blockPingDistanceLabelEnabled = true;
    public boolean teamHudEnabled = true;
    public boolean teamCountOverlayEnabled = false;
    public boolean opponentLimitEnabled = true;
    public boolean playerRenderOptimizationEnabled = true;
    public int opponentLimit = 5;
    public int playerRenderDistance = 64;
    public int pingDurationSeconds = 10;
    public int pingRecentTargetTimeoutSeconds = 15;
    public List<PingRow> pingRows = defaultPingRows();
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
        automaticFoeRenderStyle = clamp(automaticFoeRenderStyle, 0, 3);
        opponentLimit = clamp(opponentLimit, 1, 20);
        playerRenderDistance = clamp(playerRenderDistance, 16, 160);
        pingDurationSeconds = clamp(pingDurationSeconds, 1, 25);
        pingRecentTargetTimeoutSeconds = clamp(pingRecentTargetTimeoutSeconds, 1, 60);
        pingRows = normalizePingRows(pingRows);
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
        copy.automaticFoeRenderStyle = automaticFoeRenderStyle;
        copy.spectatorGlowEnabled = spectatorGlowEnabled;
        copy.warningParticlesEnabled = warningParticlesEnabled;
        copy.teamPingEnabled = teamPingEnabled;
        copy.pingLastAttackedPlayerEnabled = pingLastAttackedPlayerEnabled;
        copy.blockPingDistanceLabelEnabled = blockPingDistanceLabelEnabled;
        copy.teamHudEnabled = teamHudEnabled;
        copy.teamCountOverlayEnabled = teamCountOverlayEnabled;
        copy.opponentLimitEnabled = opponentLimitEnabled;
        copy.playerRenderOptimizationEnabled = playerRenderOptimizationEnabled;
        copy.opponentLimit = opponentLimit;
        copy.playerRenderDistance = playerRenderDistance;
        copy.pingDurationSeconds = pingDurationSeconds;
        copy.pingRecentTargetTimeoutSeconds = pingRecentTargetTimeoutSeconds;
        copy.pingRows = copyPingRows(pingRows);
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
                && automaticFoeRenderStyle == other.automaticFoeRenderStyle
                && spectatorGlowEnabled == other.spectatorGlowEnabled
                && warningParticlesEnabled == other.warningParticlesEnabled
                && teamPingEnabled == other.teamPingEnabled
                && pingLastAttackedPlayerEnabled == other.pingLastAttackedPlayerEnabled
                && blockPingDistanceLabelEnabled == other.blockPingDistanceLabelEnabled
                && teamHudEnabled == other.teamHudEnabled
                && teamCountOverlayEnabled == other.teamCountOverlayEnabled
                && opponentLimitEnabled == other.opponentLimitEnabled
                && playerRenderOptimizationEnabled == other.playerRenderOptimizationEnabled
                && opponentLimit == other.opponentLimit
                && playerRenderDistance == other.playerRenderDistance
                && pingDurationSeconds == other.pingDurationSeconds
                && pingRecentTargetTimeoutSeconds == other.pingRecentTargetTimeoutSeconds
                && pingRowsEqual(pingRows, other.pingRows)
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
                String cleaned = cleanServerAddress(address);
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

    private static String cleanServerAddress(String address) {
        String cleaned = address.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("http://")) {
            cleaned = cleaned.substring("http://".length());
        } else if (cleaned.startsWith("https://")) {
            cleaned = cleaned.substring("https://".length());
        }
        int slash = cleaned.indexOf('/');
        return slash >= 0 ? cleaned.substring(0, slash) : cleaned;
    }

    private static List<PingRow> normalizePingRows(List<PingRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return defaultPingRows();
        }

        ArrayList<PingRow> normalized = new ArrayList<>();
        for (PingRow row : rows) {
            normalized.add((row == null ? new PingRow() : row).normalize());
        }
        return normalized.isEmpty() ? defaultPingRows() : normalized;
    }

    private static ArrayList<PingRow> copyPingRows(List<PingRow> rows) {
        ArrayList<PingRow> copy = new ArrayList<>();
        for (PingRow row : normalizePingRows(rows)) {
            copy.add(row.copy());
        }
        return copy;
    }

    private static boolean pingRowsEqual(List<PingRow> first, List<PingRow> second) {
        List<PingRow> normalizedFirst = normalizePingRows(first);
        List<PingRow> normalizedSecond = normalizePingRows(second);
        if (normalizedFirst.size() != normalizedSecond.size()) {
            return false;
        }
        for (int i = 0; i < normalizedFirst.size(); i++) {
            if (!normalizedFirst.get(i).sameSettings(normalizedSecond.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static ArrayList<PingRow> defaultPingRows() {
        ArrayList<PingRow> rows = new ArrayList<>();

        PingRow playerPing = new PingRow();
        playerPing.keyType = PingRow.KEY_TYPE_KEYBOARD;
        playerPing.keyCode = 71;
        playerPing.presses = 1;
        playerPing.targetSource = PingRow.TARGET_SOURCE_CROSSHAIR;
        playerPing.targetType = PingRow.TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE;
        playerPing.message = "Focus {player}";
        playerPing.teammateMessage = "{player} needs help!";
        playerPing.enemyMessage = "Focus {player}";
        playerPing.color = "#ffd84a";
        playerPing.visualAudience = PingRow.VISUAL_AUDIENCE_TEAMMATES;
        rows.add(playerPing.normalize());

        PingRow blockPing = new PingRow();
        blockPing.keyType = PingRow.KEY_TYPE_KEYBOARD;
        blockPing.keyCode = 71;
        blockPing.presses = 2;
        blockPing.targetSource = PingRow.TARGET_SOURCE_CROSSHAIR;
        blockPing.targetType = PingRow.TARGET_TYPE_BLOCKS_ONLY;
        blockPing.message = "Go to {x} {y} {z}";
        blockPing.teammateMessage = "{player} needs help!";
        blockPing.enemyMessage = "Focus {player}";
        blockPing.color = "#ffd84a";
        blockPing.visualAudience = PingRow.VISUAL_AUDIENCE_TEAMMATES;
        rows.add(blockPing.normalize());

        return rows;
    }

    public static class PingRow {
        public static final int KEY_TYPE_KEYBOARD = 0;
        public static final int KEY_TYPE_MOUSE = 1;

        public static final int TARGET_SOURCE_CROSSHAIR = 0;
        public static final int TARGET_SOURCE_LAST_ATTACKER = 1;
        public static final int TARGET_SOURCE_LAST_ATTACKED = 2;
        public static final int TARGET_SOURCE_SELF = 3;

        public static final int TARGET_TYPE_TEAMMATES_ONLY = 0;
        public static final int TARGET_TYPE_ENEMIES_ONLY = 1;
        public static final int TARGET_TYPE_ALL_PLAYERS_SAME_MESSAGE = 2;
        public static final int TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE = 3;
        public static final int TARGET_TYPE_BLOCKS_ONLY = 4;

        public static final int VISUAL_AUDIENCE_TEAMMATES = 0;
        public static final int VISUAL_AUDIENCE_OPPONENTS = 1;
        public static final int VISUAL_AUDIENCE_EVERYONE = 2;

        public int keyType = KEY_TYPE_KEYBOARD;
        public int keyCode = 71;
        public int presses = 1;
        public int targetSource = TARGET_SOURCE_CROSSHAIR;
        public int targetType = TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE;
        public String message = "Focus {player}";
        public String teammateMessage = "{player} needs help!";
        public String enemyMessage = "Focus {player}";
        public String color = "#ffd84a";
        public int visualAudience = VISUAL_AUDIENCE_TEAMMATES;

        public PingRow normalize() {
            keyType = keyType == KEY_TYPE_MOUSE ? KEY_TYPE_MOUSE : KEY_TYPE_KEYBOARD;
            keyCode = keyType == KEY_TYPE_MOUSE ? clamp(keyCode, 0, 7) : clamp(keyCode, 32, 348);
            presses = clamp(presses, 1, 5);
            targetSource = clamp(targetSource, TARGET_SOURCE_CROSSHAIR, TARGET_SOURCE_SELF);
            targetType = clamp(targetType, TARGET_TYPE_TEAMMATES_ONLY, TARGET_TYPE_BLOCKS_ONLY);
            visualAudience = clamp(visualAudience, VISUAL_AUDIENCE_TEAMMATES, VISUAL_AUDIENCE_EVERYONE);
            message = cleanMessage(message, "Focus {player}");
            teammateMessage = cleanMessage(teammateMessage, "{player} needs help!");
            enemyMessage = cleanMessage(enemyMessage, "Focus {player}");
            color = normalizeColor(color);
            return this;
        }

        public PingRow copy() {
            PingRow copy = new PingRow();
            copy.keyType = keyType;
            copy.keyCode = keyCode;
            copy.presses = presses;
            copy.targetSource = targetSource;
            copy.targetType = targetType;
            copy.message = message;
            copy.teammateMessage = teammateMessage;
            copy.enemyMessage = enemyMessage;
            copy.color = color;
            copy.visualAudience = visualAudience;
            return copy.normalize();
        }

        private boolean sameSettings(PingRow other) {
            PingRow normalizedOther = other == null ? new PingRow() : other.copy();
            PingRow normalizedThis = copy();
            return normalizedThis.keyType == normalizedOther.keyType
                    && normalizedThis.keyCode == normalizedOther.keyCode
                    && normalizedThis.presses == normalizedOther.presses
                    && normalizedThis.targetSource == normalizedOther.targetSource
                    && normalizedThis.targetType == normalizedOther.targetType
                    && normalizedThis.message.equals(normalizedOther.message)
                    && normalizedThis.teammateMessage.equals(normalizedOther.teammateMessage)
                    && normalizedThis.enemyMessage.equals(normalizedOther.enemyMessage)
                    && normalizedThis.color.equals(normalizedOther.color)
                    && normalizedThis.visualAudience == normalizedOther.visualAudience;
        }

        private static String cleanMessage(String value, String fallback) {
            String cleaned = value == null ? "" : value.trim();
            return cleaned.isBlank() ? fallback : cleaned;
        }

        private static String normalizeColor(String value) {
            if (value == null) {
                return "#ffd84a";
            }
            String cleaned = value.trim().toLowerCase(Locale.ROOT);
            if (cleaned.startsWith("#")) {
                cleaned = cleaned.substring(1);
            }
            return cleaned.matches("[0-9a-f]{6}") ? "#" + cleaned : "#ffd84a";
        }
    }
}
