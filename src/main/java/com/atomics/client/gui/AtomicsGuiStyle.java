package com.atomics.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class AtomicsGuiStyle {
    static final int BACKGROUND = 0xE0101010;
    static final int BAR = 0xD0181818;
    static final int PANEL = 0x8C202020;
    static final int PANEL_BORDER = 0x665F5F5F;
    static final int PANEL_BORDER_HOVER = 0xAA7A7A7A;
    static final int PANEL_SELECTED = 0xAA283142;
    static final int ROW = 0x661F1F1F;
    static final int ROW_HOVER = 0x88323232;
    static final int ACCENT = 0xFF6FA8FF;
    static final int ACCENT_SOFT = 0x556FA8FF;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_MUTED = 0xFFB0B0B0;
    static final int TEXT_DIM = 0xFF808080;

    private AtomicsGuiStyle() {
    }

    static void drawBackground(DrawContext context, int width, int height) {
        context.fill(0, 0, width, height, BACKGROUND);
    }

    static void drawBars(DrawContext context, int width, int height, int headerHeight, int footerHeight) {
        context.fill(0, 0, width, headerHeight, BAR);
        context.fill(0, headerHeight, width, headerHeight + 1, PANEL_BORDER);
        context.fill(0, height - footerHeight, width, height, BAR);
        context.fill(0, height - footerHeight, width, height - footerHeight + 1, PANEL_BORDER);
    }

    static void drawTitle(DrawContext context, TextRenderer textRenderer, Text title, int width) {
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, TEXT);
    }

    static void drawSubtitle(DrawContext context, TextRenderer textRenderer, Text subtitle, int width) {
        context.drawCenteredTextWithShadow(textRenderer, subtitle, width / 2, 25, TEXT_MUTED);
    }

    static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height, PANEL_BORDER);
    }

    static void drawPanel(DrawContext context, int x, int y, int width, int height, int border) {
        if (width <= 0 || height <= 0) {
            return;
        }
        context.fill(x, y, x + width, y + height, PANEL);
        drawBorder(context, x, y, width, height, border);
    }

    static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    static void drawStatus(DrawContext context, TextRenderer textRenderer, Text status, int x, int y, int defaultColor) {
        String text = status == null ? "" : status.getString();
        if (text.isBlank()) {
            return;
        }
        int width = textRenderer.getWidth(status) + 14;
        context.fill(x, y, x + width, y + 17, ROW);
        context.fill(x, y, x + 2, y + 17, defaultColor);
        context.drawTextWithShadow(textRenderer, status, x + 7, y + 5, TEXT);
    }
}
