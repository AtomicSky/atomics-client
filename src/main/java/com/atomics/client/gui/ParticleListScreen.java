package com.atomics.client.gui;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ParticleListScreen extends Screen {
    private static final String[] SHAPES = {"random", "sphere", "ring", "spiral", "beam", "cone"};
    private static final int ROW_HEIGHT = 58;

    private final Screen parent;
    private final List<RowLabel> labels = new ArrayList<>();
    private int scroll;
    private Component status = Component.empty();

    public ParticleListScreen(Screen parent) {
        super(Component.literal("Totem Pop Particles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        labels.clear();
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) {
            cfg = new TpsConfig();
            AtomicsClient.CONFIG = cfg;
        }
        cfg.normalize();
        final TpsConfig finalCfg = cfg;

        int x = 24;
        int y = 48 - scroll;
        int row = 0;
        for (TpsConfig.ParticleBurst burst : new ArrayList<>(cfg.particles.bursts)) {
            int rowY = y + row * ROW_HEIGHT;
            if (rowY > 18 && rowY < this.height - 42) {
                addParticleRow(x, rowY, burst, cfg);
            }
            row++;
        }

        int bottom = this.height - 28;
        addRenderableWidget(Button.builder(Component.literal("+ Add Particle"), b -> {
            finalCfg.particles.bursts.add(TpsConfig.defaultParticleBurst());
            status = Component.literal("Added particle").withStyle(ChatFormatting.GREEN);
            rebuildWidgets();
        }).bounds(24, bottom, 112, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear All"), b -> {
            finalCfg.particles.bursts.clear();
            status = Component.literal("Particle list cleared").withStyle(ChatFormatting.YELLOW);
            rebuildWidgets();
        }).bounds(142, bottom, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(this.width - 96, bottom, 72, 20).build());
    }

    private void addParticleRow(int x, int y, TpsConfig.ParticleBurst burst, TpsConfig cfg) {
        int fixedWidth = 430;
        int fieldW = Math.max(150, this.width - x * 2 - fixedWidth);
        int countX = x + fieldW + 8;
        int patternX = countX + 54;
        int spreadX = patternX + 90;
        int heightX = spreadX + 62;
        int speedX = heightX + 62;
        int removeX = speedX + 62;

        addLabel("Particle ID", x + 2, y + 2);
        addLabel("Count", countX + 2, y + 2);
        addLabel("Pattern", patternX + 2, y + 2);
        addLabel("Spread", spreadX + 2, y + 2);
        addLabel("Height", heightX + 2, y + 2);
        addLabel("Speed", speedX + 2, y + 2);

        EditBox id = new EditBox(this.font, x, y + 16, fieldW, 20, Component.literal("Particle ID"));
        id.setValue(burst.particle == null ? "" : burst.particle);
        id.setResponder(value -> burst.particle = value.trim().isEmpty() ? TpsConfig.DEFAULT_PARTICLE_ID : value.trim());
        addRenderableWidget(id);

        EditBox count = new EditBox(this.font, countX, y + 16, 48, 20, Component.literal("Count"));
        count.setValue(Integer.toString(burst.count));
        count.setResponder(value -> burst.count = parseInt(value, burst.count, 0, 1000));
        addRenderableWidget(count);

        addRenderableWidget(Button.builder(Component.literal("Pattern: " + shapeLabel(burst.shape)), b -> {
            burst.shape = nextShape(burst.shape);
            b.setMessage(Component.literal("Pattern: " + shapeLabel(burst.shape)));
            status = Component.literal("Pattern set to " + shapeLabel(burst.shape)).withStyle(ChatFormatting.AQUA);
        }).bounds(patternX, y + 16, 84, 20).build());

        EditBox spread = new EditBox(this.font, spreadX, y + 16, 56, 20, Component.literal("Spread"));
        spread.setValue(formatNumber(burst.spreadX));
        spread.setResponder(value -> {
            double parsed = parseDouble(value, burst.spreadX, 0.0, 8.0);
            burst.spreadX = parsed;
            burst.spreadZ = parsed;
        });
        addRenderableWidget(spread);

        EditBox height = new EditBox(this.font, heightX, y + 16, 56, 20, Component.literal("Height"));
        height.setValue(formatNumber(burst.spreadY));
        height.setResponder(value -> burst.spreadY = parseDouble(value, burst.spreadY, 0.0, 8.0));
        addRenderableWidget(height);

        EditBox speed = new EditBox(this.font, speedX, y + 16, 56, 20, Component.literal("Speed"));
        speed.setValue(formatNumber(burst.speed));
        speed.setResponder(value -> burst.speed = parseDouble(value, burst.speed, 0.0, 4.0));
        addRenderableWidget(speed);

        addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
            cfg.particles.bursts.remove(burst);
            status = Component.literal("Removed particle").withStyle(ChatFormatting.RED);
            rebuildWidgets();
        }).bounds(removeX, y + 16, 70, 20).build());
    }

    private void addLabel(String text, int x, int y) {
        labels.add(new RowLabel(text, x, y));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int size = AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.particles == null || AtomicsClient.CONFIG.particles.bursts == null
                ? 0
                : AtomicsClient.CONFIG.particles.bursts.size();
        int max = Math.max(0, size * ROW_HEIGHT - (this.height - 92));
        scroll = Math.max(0, Math.min(max, scroll - (int) (verticalAmount * 24)));
        rebuildWidgets();
        return true;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xE0101010);
        context.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        for (RowLabel label : labels) {
            context.drawString(this.font, Component.literal(label.text), label.x, label.y, 0xFFBDBDBD);
        }
        super.render(context, mouseX, mouseY, delta);
        context.drawString(this.font, status, 242, this.height - 22, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private static String nextShape(String current) {
        String normalized = normalizeShape(current);
        for (int i = 0; i < SHAPES.length; i++) {
            if (SHAPES[i].equals(normalized)) {
                return SHAPES[(i + 1) % SHAPES.length];
            }
        }
        return SHAPES[0];
    }

    private static String normalizeShape(String shape) {
        if (shape == null || shape.isBlank()) {
            return TpsConfig.DEFAULT_PARTICLE_SHAPE;
        }
        String normalized = shape.trim().toLowerCase(Locale.ROOT);
        for (String valid : SHAPES) {
            if (valid.equals(normalized)) {
                return normalized;
            }
        }
        return TpsConfig.DEFAULT_PARTICLE_SHAPE;
    }

    private static String shapeLabel(String shape) {
        String normalized = normalizeShape(shape);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private static String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0.0";
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback, double min, double max) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)) {
                return fallback;
            }
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record RowLabel(String text, int x, int y) {
    }
}
