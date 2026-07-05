package com.legions.client.gui;

import com.legions.client.LegionsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

public class LegionsClientScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private final Screen parent;

    public LegionsClientScreen(Screen parent) {
        super(Text.literal("Legions Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.max(220, Math.min(320, this.width - 32));
        int x = (this.width - panelWidth) / 2;
        int y = 52;

        addToggle(x, y, panelWidth, "Enabled", () -> LegionsClient.CONFIG.enabled, value -> LegionsClient.CONFIG.enabled = value);
        y += 24;
        addToggle(x, y, panelWidth, "Rating Nametags", () -> LegionsClient.CONFIG.ratingNametagsEnabled, value -> LegionsClient.CONFIG.ratingNametagsEnabled = value);
        y += 24;
        addToggle(x, y, panelWidth, "Foe Outlines", () -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled, value -> LegionsClient.CONFIG.automaticFoeOutlinesEnabled = value);
        y += 24;
        addToggle(x, y, panelWidth, "Spectator Glow", () -> LegionsClient.CONFIG.spectatorGlowEnabled, value -> LegionsClient.CONFIG.spectatorGlowEnabled = value);
        y += 24;
        addToggle(x, y, panelWidth, "Warning Particles", () -> LegionsClient.CONFIG.warningParticlesEnabled, value -> LegionsClient.CONFIG.warningParticlesEnabled = value);
        y += 24;
        addToggle(x, y, panelWidth, "Team Ping", () -> LegionsClient.CONFIG.teamPingEnabled, value -> LegionsClient.CONFIG.teamPingEnabled = value);
        y += 30;

        addDrawableChild(new IntSlider(x, y, panelWidth, "Opponents Shown", 1, 12, LegionsClient.CONFIG.opponentLimit, value -> LegionsClient.CONFIG.opponentLimit = value));
        y += 24;
        addDrawableChild(new IntSlider(x, y, panelWidth, "Ping Seconds", 3, 10, LegionsClient.CONFIG.pingDurationSeconds, value -> LegionsClient.CONFIG.pingDurationSeconds = value));
        y += 34;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            LegionsClient.saveConfig();
            button.setMessage(Text.literal("Saved"));
        }).dimensions(x, y, panelWidth / 3 - 4, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> {
            LegionsClient.CONFIG = new com.legions.client.config.LegionsConfig();
            LegionsClient.saveConfig();
            clearAndInit();
        }).dimensions(x + panelWidth / 3 + 2, y, panelWidth / 3 - 4, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(x + panelWidth * 2 / 3 + 4, y, panelWidth / 3 - 4, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x99000000);
        int panelWidth = Math.max(240, Math.min(360, this.width - 24));
        int x = (this.width - panelWidth) / 2;
        int bottom = Math.min(this.height - 18, 330);
        if (bottom > 24) {
            context.fill(x, 24, x + panelWidth, bottom, 0xD20F1318);
            context.fill(x, 24, x + panelWidth, 25, 0xFF263241);
            context.fill(x, bottom - 1, x + panelWidth, bottom, 0xFF263241);
        }
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 32, 0xFFE7F0FF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        LegionsClient.saveConfig();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void addToggle(int x, int y, int width, String label, BooleanSupplier getter, BooleanSetter setter) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), button -> {
            boolean value = !getter.getAsBoolean();
            setter.set(value);
            button.setMessage(toggleText(label, value));
        }).dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    private static Text toggleText(String label, boolean enabled) {
        return Text.literal(label + "        " + (enabled ? "ON" : "OFF"));
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }

    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final IntSetter setter;

        private IntSlider(int x, int y, int width, String label, int min, int max, int initial, IntSetter setter) {
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
            setter.set(getActualValue());
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
