package com.legions.client.gui;

import com.legions.client.LegionsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class LegionsClientScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int CONTENT_TOP = 52;
    private static final int PANEL_TOP = 24;
    private static final int PANEL_BOTTOM_MARGIN = 18;
    private static final int ROW_SPACING = 24;
    private static final int SCROLL_STEP = ROW_SPACING;
    private final Screen parent;
    private int panelBottom = 330;
    private int scrollOffset;
    private int maxScroll;
    private int contentHeight;

    public LegionsClientScreen(Screen parent) {
        super(Text.literal("Legions Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        int x = (this.width - panelWidth) / 2;
        int y = CONTENT_TOP;

        addToggle(x, screenY(y), panelWidth, "Enabled", () -> LegionsClient.CONFIG.enabled, value -> LegionsClient.CONFIG.enabled = value);
        y += ROW_SPACING;
        addToggle(x, screenY(y), panelWidth, "Rating Nametags", () -> LegionsClient.CONFIG.ratingNametagsEnabled, value -> LegionsClient.CONFIG.ratingNametagsEnabled = value);
        y += ROW_SPACING;
        addToggle(x, screenY(y), panelWidth, "Foe Outlines", () -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled, value -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled = value);
        y += ROW_SPACING;
        addToggle(x, screenY(y), panelWidth, "Spectator Glow", () -> LegionsClient.CONFIG.spectatorGlowEnabled, value -> LegionsClient.CONFIG.spectatorGlowEnabled = value);
        y += ROW_SPACING;
        addToggle(x, screenY(y), panelWidth, "Warning Particles", () -> LegionsClient.CONFIG.warningParticlesEnabled, value -> LegionsClient.CONFIG.warningParticlesEnabled = value);
        y += ROW_SPACING;
        addToggle(x, screenY(y), panelWidth, "Team Ping", () -> LegionsClient.CONFIG.teamPingEnabled, value -> LegionsClient.CONFIG.teamPingEnabled = value);
        y += ROW_SPACING;
        if (LegionsClient.CONFIG.teamPingEnabled) {
            addSlider(x, screenY(y), panelWidth, "Ping Seconds", 3, 10, LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value);
            y += ROW_SPACING;
        }
        addToggle(x, screenY(y), panelWidth, "Team Count Overlay", () -> LegionsClient.CONFIG.teamCountOverlayEnabled, value -> LegionsClient.CONFIG.teamCountOverlayEnabled = value);
        y += ROW_SPACING;
        if (LegionsClient.CONFIG.teamCountOverlayEnabled) {
            addButton(x, screenY(y), panelWidth, Text.literal("Move Team Count Overlay"), button -> MinecraftClient.getInstance().setScreen(new LegionsTeamCountOverlayLayoutScreen(this)));
            y += ROW_SPACING;
        }
        addToggle(x, screenY(y), panelWidth, "Team HUD", () -> LegionsClient.CONFIG.teamHudEnabled, value -> LegionsClient.CONFIG.teamHudEnabled = value);
        y += ROW_SPACING;
        if (LegionsClient.CONFIG.teamHudEnabled) {
            addButton(x, screenY(y), panelWidth, Text.literal("Move Team HUD"), button -> MinecraftClient.getInstance().setScreen(new LegionsTeamHudLayoutScreen(this)));
            y += ROW_SPACING;
        }

        addToggle(x, screenY(y), panelWidth, "Limit Opponents Shown", () -> LegionsClient.CONFIG.opponentLimitEnabled, value -> LegionsClient.CONFIG.opponentLimitEnabled = value);
        y += ROW_SPACING;
        if (LegionsClient.CONFIG.opponentLimitEnabled) {
            addSlider(x, screenY(y), panelWidth, "Opponents Shown", 1, 12, LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value);
            y += ROW_SPACING;
        }
        addToggle(x, screenY(y), panelWidth, "Player Render Optimization", () -> LegionsClient.CONFIG.playerRenderOptimizationEnabled, value -> LegionsClient.CONFIG.playerRenderOptimizationEnabled = value);
        y += ROW_SPACING;
        if (LegionsClient.CONFIG.playerRenderOptimizationEnabled) {
            addSlider(x, screenY(y), panelWidth, "Render Distance", 16, 160, LegionsClient.CONFIG.playerRenderDistance, value -> LegionsClient.CONFIG.playerRenderDistance = value);
            y += ROW_SPACING;
        }
        y += 28;

        int buttonY = screenY(y);
        if (isRowVisible(buttonY)) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
                LegionsClient.saveConfig();
                button.setMessage(Text.literal("Saved"));
            }).dimensions(x, buttonY, panelWidth / 3 - 4, BUTTON_HEIGHT).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> {
                LegionsClient.CONFIG = new com.legions.client.config.LegionsConfig();
                LegionsClient.saveConfig();
                clearAndInit();
            }).dimensions(x + panelWidth / 3 + 2, buttonY, panelWidth / 3 - 4, BUTTON_HEIGHT).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(x + panelWidth * 2 / 3 + 4, buttonY, panelWidth / 3 - 4, BUTTON_HEIGHT).build());
        }
        y += BUTTON_HEIGHT + 12;
        contentHeight = y - CONTENT_TOP;
        maxScroll = Math.max(0, contentHeight - visibleContentHeight());
        int clampedScroll = clamp(scrollOffset, 0, maxScroll);
        if (scrollOffset != clampedScroll) {
            scrollOffset = clampedScroll;
            clearAndInit();
            return;
        }
        panelBottom = visibleBottom();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x99000000);
        int panelWidth = panelWidth();
        int x = (this.width - panelWidth) / 2;
        int bottom = panelBottom;
        if (bottom > PANEL_TOP) {
            context.fill(x, PANEL_TOP, x + panelWidth, bottom, 0xD20F1318);
            context.fill(x, PANEL_TOP, x + panelWidth, PANEL_TOP + 1, 0xFF263241);
            context.fill(x, bottom - 1, x + panelWidth, bottom, 0xFF263241);
            renderScrollbar(context, x, panelWidth, bottom);
        }
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 32, 0xFFE7F0FF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseY >= CONTENT_TOP && mouseY <= visibleBottom()) {
            int oldOffset = scrollOffset;
            int delta = (int) Math.round(verticalAmount * SCROLL_STEP);
            if (delta == 0 && verticalAmount != 0.0) {
                delta = verticalAmount > 0.0 ? SCROLL_STEP : -SCROLL_STEP;
            }
            scrollOffset = clamp(scrollOffset - delta, 0, maxScroll);
            if (scrollOffset != oldOffset) {
                clearAndInit();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        LegionsClient.saveConfig();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void addToggle(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        if (!isRowVisible(y)) {
            return;
        }
        addDrawableChild(ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), button -> {
            boolean value = !getter.getAsBoolean();
            setter.accept(value);
            button.setMessage(toggleText(label, value));
            clearAndInit();
        }).dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    private void addButton(int x, int y, int width, Text label, ButtonWidget.PressAction action) {
        if (isRowVisible(y)) {
            addDrawableChild(ButtonWidget.builder(label, action).dimensions(x, y, width, BUTTON_HEIGHT).build());
        }
    }

    private void addSlider(int x, int y, int width, String label, int min, int max, int initial, IntConsumer setter) {
        if (isRowVisible(y)) {
            addDrawableChild(new IntSlider(x, y, width, label, min, max, initial, setter));
        }
    }

    private void renderScrollbar(DrawContext context, int x, int panelWidth, int bottom) {
        if (maxScroll <= 0 || contentHeight <= 0) {
            return;
        }
        int trackTop = CONTENT_TOP;
        int trackBottom = bottom - 6;
        if (trackBottom <= trackTop) {
            return;
        }
        int trackHeight = trackBottom - trackTop;
        int thumbHeight = clamp(trackHeight * visibleContentHeight() / contentHeight, 18, trackHeight);
        int thumbY = trackTop + (int) Math.round((trackHeight - thumbHeight) * (scrollOffset / (double) maxScroll));
        int trackX = x + panelWidth - 6;
        context.fill(trackX, trackTop, trackX + 2, trackBottom, 0x55354552);
        context.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xAA55E6FF);
    }

    private int screenY(int contentY) {
        return contentY - scrollOffset;
    }

    private boolean isRowVisible(int y) {
        return y + BUTTON_HEIGHT >= CONTENT_TOP && y <= visibleBottom();
    }

    private int visibleBottom() {
        return Math.max(CONTENT_TOP, this.height - PANEL_BOTTOM_MARGIN);
    }

    private int visibleContentHeight() {
        return Math.max(BUTTON_HEIGHT, visibleBottom() - CONTENT_TOP);
    }

    private int panelWidth() {
        return Math.max(240, Math.min(360, this.width - 24));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Text toggleText(String label, boolean enabled) {
        return Text.literal(label + "        " + (enabled ? "ON" : "OFF"));
    }

    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer setter;

        private IntSlider(int x, int y, int width, String label, int min, int max, int initial, IntConsumer setter) {
            super(x, y, width, BUTTON_HEIGHT, Text.empty(), 0.0);
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            setActualValue(initial);
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + "        " + getActualValue()));
        }

        @Override
        protected void applyValue() {
            setter.accept(getActualValue());
        }

        private int getActualValue() {
            return Math.max(min, Math.min(max, (int) Math.round(min + value * (max - min))));
        }

        private void setActualValue(int actualValue) {
            value = max <= min ? 0.0 : (double) (Math.max(min, Math.min(max, actualValue)) - min) / (double) (max - min);
            applyValue();
            updateMessage();
        }
    }
}
