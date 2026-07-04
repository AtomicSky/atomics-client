package com.legions.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LegionsHud {
    private static final int PANEL_WIDTH = 156;
    private static final int LINE_HEIGHT = 10;

    private LegionsHud() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.rosterHudEnabled || client.world == null || client.player == null) {
            return;
        }

        List<PlayerEntity> teammates = new ArrayList<>();
        List<PlayerEntity> opponents = new ArrayList<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (LegionsFeatures.isTeammate(client.player, player)) {
                teammates.add(player);
            } else if (LegionsFeatures.isOpponent(client.player, player)) {
                opponents.add(player);
            }
        }
        opponents.sort(Comparator.comparingDouble(player -> player.squaredDistanceTo(client.player)));

        int visibleOpponents = Math.min(LegionsClient.CONFIG.opponentLimit, opponents.size());
        int lines = 2 + teammates.size() + 1 + visibleOpponents;
        int x = context.getScaledWindowWidth() - PANEL_WIDTH - 8;
        int y = 8;
        int height = 8 + lines * LINE_HEIGHT;
        context.fill(x - 5, y - 5, x + PANEL_WIDTH + 5, y + height, 0xAA101317);

        TextRenderer renderer = client.textRenderer;
        int lineY = y;
        draw(context, renderer, "Legions", x, lineY, 0xFFE7F0FF);
        lineY += LINE_HEIGHT + 2;
        draw(context, renderer, "Team " + teammates.size() + " | Foes " + opponents.size(), x, lineY, 0xFFB8C7D9);
        lineY += LINE_HEIGHT;

        for (PlayerEntity player : teammates) {
            draw(context, renderer, shortPlayerLine(client.player, player), x, lineY, player == client.player ? 0xFF9AE66E : 0xFF8FD8FF);
            lineY += LINE_HEIGHT;
        }

        lineY += 2;
        for (int i = 0; i < visibleOpponents; i++) {
            draw(context, renderer, shortPlayerLine(client.player, opponents.get(i)), x, lineY, 0xFFFFA657);
            lineY += LINE_HEIGHT;
        }
    }

    private static String shortPlayerLine(PlayerEntity local, PlayerEntity player) {
        String name = player.getName().getString();
        if (name.length() > 12) {
            name = name.substring(0, 12);
        }
        int distance = (int) Math.round(local.distanceTo(player));
        int health = (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
        String self = player == local ? " you" : "";
        return name + self + " " + distance + "m " + health + "hp";
    }

    private static void draw(DrawContext context, TextRenderer renderer, String text, int x, int y, int color) {
        context.drawTextWithShadow(renderer, Text.literal(text), x, y, color);
    }
}
