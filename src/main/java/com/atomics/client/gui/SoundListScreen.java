package com.atomics.client.gui;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.ArrayList;

public class SoundListScreen extends Screen {
    private final Screen parent;
    private int scroll;
    private Text status = Text.empty();

    public SoundListScreen(Screen parent) { super(Text.literal("Totem Pop Sounds")); this.parent = parent; }

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
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Sound"), b -> { finalCfg.sounds.sounds.add(TpsConfig.defaultSoundPlay()); status = Text.literal("Added sound").formatted(Formatting.GREEN); clearAndInit(); }).dimensions(24, bottom, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), b -> { finalCfg.sounds.sounds.clear(); status = Text.literal("Sound list cleared").formatted(Formatting.YELLOW); clearAndInit(); }).dimensions(126, bottom, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(this.width - 96, bottom, 72, 20).build());
    }

    private void addSoundRow(int x, int y, TpsConfig.SoundPlay sound, TpsConfig cfg) {
        int fieldW = Math.max(180, this.width - 376);
        TextFieldWidget id = new TextFieldWidget(this.textRenderer, x, y + 14, fieldW, 20, Text.literal("Sound ID"));
        id.setText(sound.sound == null ? "" : sound.sound);
        id.setChangedListener(v -> sound.sound = v.trim().isEmpty() ? TpsConfig.DEFAULT_SOUND_ID : v.trim());
        addDrawableChild(id);
        TextFieldWidget volume = new TextFieldWidget(this.textRenderer, x + fieldW + 8, y + 14, 56, 20, Text.literal("Volume"));
        volume.setText(Float.toString(sound.volume));
        volume.setChangedListener(v -> sound.volume = (float) parseDouble(v, sound.volume, 0.0, 5.0));
        addDrawableChild(volume);
        TextFieldWidget pitch = new TextFieldWidget(this.textRenderer, x + fieldW + 70, y + 14, 56, 20, Text.literal("Pitch"));
        pitch.setText(Float.toString(sound.pitch));
        pitch.setChangedListener(v -> sound.pitch = (float) parseDouble(v, sound.pitch, 0.1, 4.0));
        addDrawableChild(pitch);
        TextFieldWidget delay = new TextFieldWidget(this.textRenderer, x + fieldW + 132, y + 14, 56, 20, Text.literal("Delay"));
        delay.setText(Integer.toString(sound.delayTicks));
        delay.setChangedListener(v -> sound.delayTicks = parseInt(v, sound.delayTicks, 0, 200));
        addDrawableChild(delay);
        addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> { cfg.sounds.sounds.remove(sound); status = Text.literal("Removed sound").formatted(Formatting.RED); clearAndInit(); }).dimensions(x + fieldW + 194, y + 14, 70, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int size = AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.sounds == null || AtomicsClient.CONFIG.sounds.sounds == null ? 0 : AtomicsClient.CONFIG.sounds.sounds.size();
        int max = Math.max(0, size * 50 - (this.height - 92));
        scroll = Math.max(0, Math.min(max, scroll - (int) (verticalAmount * 24)));
        clearAndInit();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xE0101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, status, 226, this.height - 22, 0xFFFFFF);
    }

    @Override
    public void close() { if (this.client != null) this.client.setScreen(parent); }
    private static int parseInt(String value, int fallback, int min, int max) { try { return Math.max(min, Math.min(max, Integer.parseInt(value.trim()))); } catch (Exception ignored) { return fallback; } }
    private static double parseDouble(String value, double fallback, double min, double max) { try { return Math.max(min, Math.min(max, Double.parseDouble(value.trim()))); } catch (Exception ignored) { return fallback; } }
}
