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
import java.util.List;
import java.util.Locale;

public class ParticleListScreen extends Screen {
    private static final String[] SHAPES = {"random", "sphere", "ring", "spiral", "beam", "cone"};
    private static final int ROW_HEIGHT = 58;

    private final Screen parent;
    private final List<RowLabel> labels = new ArrayList<>();
    private int scroll;
    private Text status = Text.empty();

    public ParticleListScreen(Screen parent) {
        super(Text.literal("Totem Pop Particles"));
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
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Particle"), b -> {
            finalCfg.particles.bursts.add(TpsConfig.defaultParticleBurst());
            status = Text.literal("Added particle").formatted(Formatting.GREEN);
            clearAndInit();
        }).dimensions(24, bottom, 112, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), b -> {
            finalCfg.particles.bursts.clear();
            status = Text.literal("Particle list cleared").formatted(Formatting.YELLOW);
            clearAndInit();
        }).dimensions(142, bottom, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(this.width - 96, bottom, 72, 20).build());
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

        TextFieldWidget id = new TextFieldWidget(this.textRenderer, x, y + 16, fieldW, 20, Text.literal("Particle ID"));
        id.setText(burst.particle == null ? "" : burst.particle);
        id.setChangedListener(value -> burst.particle = value.trim().isEmpty() ? TpsConfig.DEFAULT_PARTICLE_ID : value.trim());
        addDrawableChild(id);

        TextFieldWidget count = new TextFieldWidget(this.textRenderer, countX, y + 16, 48, 20, Text.literal("Count"));
        count.setText(Integer.toString(burst.count));
        count.setChangedListener(value -> burst.count = parseInt(value, burst.count, 0, 1000));
        addDrawableChild(count);

        addDrawableChild(ButtonWidget.builder(Text.literal("Pattern: " + shapeLabel(burst.shape)), b -> {
            burst.shape = nextShape(burst.shape);
            b.setMessage(Text.literal("Pattern: " + shapeLabel(burst.shape)));
            status = Text.literal("Pattern set to " + shapeLabel(burst.shape)).formatted(Formatting.AQUA);
        }).dimensions(patternX, y + 16, 84, 20).build());

        TextFieldWidget spread = new TextFieldWidget(this.textRenderer, spreadX, y + 16, 56, 20, Text.literal("Spread"));
        spread.setText(formatNumber(burst.spreadX));
        spread.setChangedListener(value -> {
            double parsed = parseDouble(value, burst.spreadX, 0.0, 8.0);
            burst.spreadX = parsed;
            burst.spreadZ = parsed;
        });
        addDrawableChild(spread);

        TextFieldWidget height = new TextFieldWidget(this.textRenderer, heightX, y + 16, 56, 20, Text.literal("Height"));
        height.setText(formatNumber(burst.spreadY));
        height.setChangedListener(value -> burst.spreadY = parseDouble(value, burst.spreadY, 0.0, 8.0));
        addDrawableChild(height);

        TextFieldWidget speed = new TextFieldWidget(this.textRenderer, speedX, y + 16, 56, 20, Text.literal("Speed"));
        speed.setText(formatNumber(burst.speed));
        speed.setChangedListener(value -> burst.speed = parseDouble(value, burst.speed, 0.0, 4.0));
        addDrawableChild(speed);

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> {
            cfg.particles.bursts.remove(burst);
            status = Text.literal("Removed particle").formatted(Formatting.RED);
            clearAndInit();
        }).dimensions(removeX, y + 16, 70, 20).build());
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
        clearAndInit();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AtomicsGuiStyle.drawBackground(context, this.width, this.height);
        AtomicsGuiStyle.drawBars(context, this.width, this.height, 36, 34);
        AtomicsGuiStyle.drawTitle(context, this.textRenderer, this.title, this.width);
        AtomicsGuiStyle.drawPanel(context, 18, 40, this.width - 36, this.height - 80);
        for (RowLabel label : labels) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(label.text), label.x, label.y, AtomicsGuiStyle.TEXT_MUTED);
        }
        super.render(context, mouseX, mouseY, delta);
        AtomicsGuiStyle.drawStatus(context, this.textRenderer, status, 242, this.height - 24, AtomicsGuiStyle.ACCENT);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
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
