package com.legions.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class LegionsHud {
    private static final String TEAM_COUNT_TITLE = "Teams Left";
    private static final int TEAM_COUNT_MIN_WIDTH = 78;
    private static final int TEAM_COUNT_HEADER_HEIGHT = 13;
    private static final int TEAM_COUNT_ROW_HEIGHT = 10;
    private static final int TEAM_COUNT_PADDING_X = 5;
    private static final int TEAM_COUNT_MARGIN = 8;
    private static final int TEAM_COUNT_DEFAULT_Y = 36;
    private static final List<TeamCount> SAMPLE_TEAM_COUNTS = List.of(
            new TeamCount("BLUE", 0xFF5555FF, 8),
            new TeamCount("RED", 0xFFFF5555, 7)
    );
    private static final ArrayList<TeamCount> teamCountCache = new ArrayList<>();
    private static final LinkedHashMap<String, TeamCount> teamCountScratch = new LinkedHashMap<>();
    private static Object teamCountCacheHandler;
    private static long teamCountCacheTick = Long.MIN_VALUE;
    private static int teamCountCacheSize = -1;

    private LegionsHud() {
    }

    public static void renderHud(DrawContext context) {
        renderTeamHud(context);
        renderTeamCountOverlay(context);
    }

    public static void renderTeamHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamHudEnabled || client.player == null) {
            return;
        }

        TextRenderer renderer = client.textRenderer;
        String text = teamHudText(client);
        int x = clamp(LegionsClient.CONFIG.teamHudX, 0, Math.max(0, context.getScaledWindowWidth() - renderer.getWidth(text)));
        int y = clamp(LegionsClient.CONFIG.teamHudY, 0, Math.max(0, context.getScaledWindowHeight() - renderer.fontHeight));
        context.drawTextWithShadow(renderer, Text.literal(text), x, y, teamHudColor(client));
    }

    public static String teamHudText(MinecraftClient client) {
        if (client == null || client.player == null) {
            return "Team: ?";
        }
        Team team = client.player.getScoreboardTeam();
        if (team == null) {
            return "Team: None";
        }
        String name = team.getDisplayName().getString();
        if (name == null || name.isBlank()) {
            name = team.getName();
        }
        return "Team: " + readableTeamName(name);
    }

    public static int teamHudColor(MinecraftClient client) {
        if (client == null || client.player == null) {
            return 0xFFE7F0FF;
        }
        Team team = client.player.getScoreboardTeam();
        if (team == null) {
            return 0xFFE7F0FF;
        }
        Formatting color = team.getColor();
        Integer rgb = color == null ? null : color.getColorValue();
        if (rgb != null) {
            return 0xFF000000 | rgb;
        }
        return fallbackTeamColor(team.getName());
    }

    public static void renderTeamCountOverlay(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamCountOverlayEnabled || client.player == null) {
            return;
        }

        renderTeamCountOverlay(context, client, LegionsClient.CONFIG.teamCountOverlayX, LegionsClient.CONFIG.teamCountOverlayY, false);
    }

    public static int teamCountOverlayPreviewWidth(MinecraftClient client) {
        MinecraftClient renderClient = client == null ? MinecraftClient.getInstance() : client;
        return teamCountOverlayWidth(renderClient, displayTeamCounts(client, true));
    }

    public static int teamCountOverlayPreviewHeight(MinecraftClient client) {
        return teamCountOverlayHeight(displayTeamCounts(client, true));
    }

    public static int defaultTeamCountOverlayX(MinecraftClient client) {
        MinecraftClient renderClient = client == null ? MinecraftClient.getInstance() : client;
        int screenWidth = renderClient.getWindow().getScaledWidth();
        return Math.max(0, screenWidth - teamCountOverlayPreviewWidth(renderClient) - TEAM_COUNT_MARGIN);
    }

    public static int defaultTeamCountOverlayY() {
        return TEAM_COUNT_DEFAULT_Y;
    }

    public static void renderTeamCountOverlayPreview(DrawContext context, MinecraftClient client, int x, int y) {
        MinecraftClient renderClient = client == null ? MinecraftClient.getInstance() : client;
        renderTeamCountOverlay(context, renderClient, x, y, true);
    }

    private static void renderTeamCountOverlay(DrawContext context, MinecraftClient client, int configuredX, int configuredY, boolean preview) {
        List<TeamCount> counts = displayTeamCounts(client, preview);
        if (counts.isEmpty()) {
            return;
        }

        int width = teamCountOverlayWidth(client, counts);
        int height = teamCountOverlayHeight(counts);
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int x = configuredX;
        int y = configuredY;
        if (x < 0 || y < 0) {
            x = Math.max(0, screenWidth - width - TEAM_COUNT_MARGIN);
            y = Math.min(Math.max(0, TEAM_COUNT_DEFAULT_Y), Math.max(0, screenHeight - height));
        }
        x = clamp(x, 0, Math.max(0, screenWidth - width));
        y = clamp(y, 0, Math.max(0, screenHeight - height));

        context.fill(x, y, x + width, y + height, 0x66000000);
        context.fill(x, y, x + width, y + TEAM_COUNT_HEADER_HEIGHT, 0x99000000);
        int titleX = x + width / 2 - client.textRenderer.getWidth(TEAM_COUNT_TITLE) / 2;
        context.drawTextWithShadow(client.textRenderer, TEAM_COUNT_TITLE, titleX, y + 2, 0xFFFFFF55);

        int rowY = y + TEAM_COUNT_HEADER_HEIGHT + 2;
        for (int i = 0; i < counts.size(); i++) {
            TeamCount count = counts.get(i);
            int rowTop = rowY + i * TEAM_COUNT_ROW_HEIGHT;
            context.fill(x, rowTop, x + width, rowTop + TEAM_COUNT_ROW_HEIGHT, (i & 1) == 0 ? 0x52000000 : 0x3F000000);
            context.drawTextWithShadow(client.textRenderer, count.name(), x + TEAM_COUNT_PADDING_X, rowTop + 1, count.color());
            String value = count.valueText();
            int valueX = x + width - TEAM_COUNT_PADDING_X - client.textRenderer.getWidth(value);
            context.drawTextWithShadow(client.textRenderer, value, valueX, rowTop + 1, 0xFFFFFFFF);
        }
    }

    private static int teamCountOverlayWidth(MinecraftClient client, List<TeamCount> counts) {
        int width = client.textRenderer.getWidth(TEAM_COUNT_TITLE) + TEAM_COUNT_PADDING_X * 2;
        for (TeamCount count : counts) {
            String value = count.valueText();
            int rowWidth = client.textRenderer.getWidth(count.name()) + client.textRenderer.getWidth(value) + TEAM_COUNT_PADDING_X * 3 + 8;
            width = Math.max(width, rowWidth);
        }
        return Math.max(TEAM_COUNT_MIN_WIDTH, width);
    }

    private static int teamCountOverlayHeight(List<TeamCount> counts) {
        return TEAM_COUNT_HEADER_HEIGHT + 4 + counts.size() * TEAM_COUNT_ROW_HEIGHT;
    }

    private static List<TeamCount> displayTeamCounts(MinecraftClient client, boolean preview) {
        List<TeamCount> counts = collectTeamCounts(client);
        if (preview && counts.isEmpty()) {
            return SAMPLE_TEAM_COUNTS;
        }
        return counts;
    }

    private static List<TeamCount> collectTeamCounts(MinecraftClient client) {
        if (client == null || client.getNetworkHandler() == null) {
            clearTeamCountCache();
            return teamCountCache;
        }

        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        int size = networkHandler.getPlayerList().size();
        long tick = client.world == null ? Long.MIN_VALUE : client.world.getTime();
        if (teamCountCacheHandler == networkHandler && teamCountCacheTick == tick && teamCountCacheSize == size) {
            return teamCountCache;
        }

        teamCountScratch.clear();
        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            if (entry == null || entry.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            Team team = entry.getScoreboardTeam();
            if (team == null || team.getName() == null || team.getName().isBlank() || isSpectatorTeamName(team.getName())) {
                continue;
            }

            String key = team.getName();
            TeamCount current = teamCountScratch.get(key);
            if (current == null) {
                teamCountScratch.put(key, new TeamCount(teamDisplayName(team), teamTextColor(team), 1));
            } else {
                teamCountScratch.put(key, new TeamCount(current.name(), current.color(), current.count() + 1));
            }
        }

        teamCountCacheHandler = networkHandler;
        teamCountCacheTick = tick;
        teamCountCacheSize = size;
        teamCountCache.clear();
        teamCountCache.addAll(teamCountScratch.values());
        teamCountScratch.clear();
        return teamCountCache;
    }

    private static void clearTeamCountCache() {
        teamCountCacheHandler = null;
        teamCountCacheTick = Long.MIN_VALUE;
        teamCountCacheSize = -1;
        teamCountCache.clear();
        teamCountScratch.clear();
    }

    private static String teamDisplayName(Team team) {
        String name = team.getDisplayName() == null ? "" : team.getDisplayName().getString();
        if (name.isBlank()) {
            name = team.getName();
        }
        return formatTeamName(name);
    }

    private static String formatTeamName(String name) {
        if (name == null || name.isBlank()) {
            return "Team";
        }
        String trimmed = name.trim();
        return trimmed.length() <= 18 ? trimmed : trimmed.substring(0, 18);
    }

    private static int teamTextColor(Team team) {
        Formatting formatting = team.getColor();
        if (formatting == null) {
            return fallbackTeamColor(team.getName());
        }

        Integer rgb = formatting.getColorValue();
        if (rgb == null) {
            return fallbackTeamColor(team.getName());
        }

        int color = rgb & 0xFFFFFF;
        return color <= 0x202020 ? 0xFFAAAAAA : 0xFF000000 | color;
    }

    private static boolean isSpectatorTeamName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).contains("spectator");
    }

    private static String readableTeamName(String value) {
        if (value == null || value.isBlank()) {
            return "None";
        }
        String normalized = value.trim().replace('_', ' ').replace('-', ' ');
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("spectator")) {
            return "Spectator";
        }
        if (lower.contains("red")) {
            return "Red";
        }
        if (lower.contains("blue")) {
            return "Blue";
        }
        if (lower.contains("green")) {
            return "Green";
        }
        if (lower.contains("yellow")) {
            return "Yellow";
        }
        return normalized;
    }

    private static int fallbackTeamColor(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (normalized.contains("red")) {
            return 0xFFFF5555;
        }
        if (normalized.contains("blue")) {
            return 0xFF55A6FF;
        }
        if (normalized.contains("green")) {
            return 0xFF55FF55;
        }
        if (normalized.contains("yellow")) {
            return 0xFFFFFF55;
        }
        if (normalized.contains("spectator")) {
            return 0xFF9AA8B8;
        }
        return 0xFFE7F0FF;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record TeamCount(String name, int color, int count) {
        private String valueText() {
            return String.valueOf(count);
        }
    }
}
