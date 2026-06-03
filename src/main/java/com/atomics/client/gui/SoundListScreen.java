package com.atomics.client.gui;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SoundListScreen extends Screen {
    private final Screen parent;
    private int scroll;
    private Component status = Component.empty();

    public SoundListScreen(Screen parent) { super(Component.literal("Totem Pop Sounds")); this.parent = parent; }

    @Override
    protected void init() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) { cfg = new TpsConfig(); AtomicsClient.CONFIG = cfg; }
        cfg.normalize();
        final TpsConfig finalCfg = cfg;
        int x = 24;
        int y = 48 - scroll;
        int row = 0;
        for (TpsConfig.SoundPlay sound : new ArrayList<>(cfg.sounds.sounds)) {
            int rowY = y + row * 50;
            if (rowY > 18 && rowY < this.height - 42) addSoundRow(x, rowY, sound, cfg);
            row++;
        }
        int bottom = this.height - 28;
        addRenderableWidget(Button.builder(Component.literal("+ Add Sound"), b -> { finalCfg.sounds.sounds.add(TpsConfig.defaultSoundPlay()); status = Component.literal("Added sound").withStyle(ChatFormatting.GREEN); rebuildWidgets(); }).bounds(24, bottom, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear All"), b -> { finalCfg.sounds.sounds.clear(); status = Component.literal("Sound list cleared").withStyle(ChatFormatting.YELLOW); rebuildWidgets(); }).bounds(126, bottom, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(this.width - 96, bottom, 72, 20).build());
    }

    private void addSoundRow(int x, int y, TpsConfig.SoundPlay sound, TpsConfig cfg) {
        int fieldW = Math.max(180, this.width - 376);
        EditBox id = new EditBox(this.font, x, y + 14, fieldW, 20, Component.literal("Sound ID"));
        id.setValue(sound.sound == null ? "" : sound.sound);
        id.setResponder(v -> sound.sound = v.trim().isEmpty() ? TpsConfig.DEFAULT_SOUND_ID : v.trim());
        addRenderableWidget(id);
        EditBox volume = new EditBox(this.font, x + fieldW + 8, y + 14, 56, 20, Component.literal("Volume"));
        volume.setValue(Float.toString(sound.volume));
        volume.setResponder(v -> sound.volume = (float) parseDouble(v, sound.volume, 0.0, 5.0));
        addRenderableWidget(volume);
        EditBox pitch = new EditBox(this.font, x + fieldW + 70, y + 14, 56, 20, Component.literal("Pitch"));
        pitch.setValue(Float.toString(sound.pitch));
        pitch.setResponder(v -> sound.pitch = (float) parseDouble(v, sound.pitch, 0.1, 4.0));
        addRenderableWidget(pitch);
        EditBox delay = new EditBox(this.font, x + fieldW + 132, y + 14, 56, 20, Component.literal("Delay"));
        delay.setValue(Integer.toString(sound.delayTicks));
        delay.setResponder(v -> sound.delayTicks = parseInt(v, sound.delayTicks, 0, 200));
        addRenderableWidget(delay);
        addRenderableWidget(Button.builder(Component.literal("Remove"), b -> { cfg.sounds.sounds.remove(sound); status = Component.literal("Removed sound").withStyle(ChatFormatting.RED); rebuildWidgets(); }).bounds(x + fieldW + 194, y + 14, 70, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int size = AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.sounds == null || AtomicsClient.CONFIG.sounds.sounds == null ? 0 : AtomicsClient.CONFIG.sounds.sounds.size();
        int max = Math.max(0, size * 50 - (this.height - 92));
        scroll = Math.max(0, Math.min(max, scroll - (int) (verticalAmount * 24)));
        rebuildWidgets();
        return true;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xE0101010);
        context.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        context.drawString(this.font, status, 226, this.height - 22, 0xFFFFFF);
    }

    @Override
    public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(parent); }
    private static int parseInt(String value, int fallback, int min, int max) { try { return Math.max(min, Math.min(max, Integer.parseInt(value.trim()))); } catch (Exception ignored) { return fallback; } }
    private static double parseDouble(String value, double fallback, double min, double max) { try { return Math.max(min, Math.min(max, Double.parseDouble(value.trim()))); } catch (Exception ignored) { return fallback; } }
}
