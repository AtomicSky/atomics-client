package com.atomics.client.gui;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class FriendFoeListScreen extends Screen {
    private static final int ROW_HEIGHT = 46;

    private final Screen parent;
    private final List<RowLabel> labels = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private boolean loaded;
    private int scroll;
    private Text status = Text.empty();

    public FriendFoeListScreen(Screen parent) {
        super(Text.literal("Friend/Foe Players"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        labels.clear();
        TpsConfig cfg = liveConfig();
        if (!loaded) {
            loadFromConfig(cfg);
        }

        int x = 24;
        int y = 48 - scroll;
        for (int i = 0; i < entries.size(); i++) {
            int rowY = y + i * ROW_HEIGHT;
            if (rowY > 18 && rowY < this.height - 42) {
                addPlayerRow(x, rowY, entries.get(i));
            }
        }

        int bottom = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Player"), b -> {
            entries.add(new Entry("", true));
            writeToConfig();
            status = Text.literal("Added player row").formatted(Formatting.GREEN);
            clearAndInit();
        }).dimensions(24, bottom, 104, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), b -> {
            entries.clear();
            writeToConfig();
            status = Text.literal("Player list cleared").formatted(Formatting.YELLOW);
            clearAndInit();
        }).dimensions(134, bottom, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save()).dimensions(this.width - 174, bottom, 72, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(this.width - 96, bottom, 72, 20).build());
    }

    private void addPlayerRow(int x, int y, Entry entry) {
        int fieldW = Math.max(180, this.width - x * 2 - 194);
        int typeX = x + fieldW + 8;
        int removeX = typeX + 86;

        addLabel("Username", x + 2, y + 2);
        addLabel("Type", typeX + 2, y + 2);

        TextFieldWidget name = new TextFieldWidget(this.textRenderer, x, y + 16, fieldW, 20, Text.literal("Username"));
        name.setText(entry.name);
        name.setChangedListener(value -> {
            entry.name = sanitizeName(value);
            writeToConfig();
        });
        addDrawableChild(name);

        addDrawableChild(ButtonWidget.builder(Text.literal(entry.friend ? "Friend" : "Foe"), b -> {
            entry.friend = !entry.friend;
            b.setMessage(Text.literal(entry.friend ? "Friend" : "Foe"));
            writeToConfig();
            status = Text.literal("Set " + (entry.friend ? "Friend" : "Foe")).formatted(Formatting.AQUA);
        }).dimensions(typeX, y + 16, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> {
            entries.remove(entry);
            writeToConfig();
            status = Text.literal("Removed player").formatted(Formatting.RED);
            clearAndInit();
        }).dimensions(removeX, y + 16, 70, 20).build());
    }

    private void addLabel(String text, int x, int y) {
        labels.add(new RowLabel(text, x, y));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, entries.size() * ROW_HEIGHT - (this.height - 92));
        scroll = Math.max(0, Math.min(max, scroll - (int) (verticalAmount * 24)));
        clearAndInit();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xE0101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        for (RowLabel label : labels) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(label.text), label.x, label.y, 0xFFBDBDBD);
        }
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, status, 234, this.height - 22, 0xFFFFFF);
    }

    @Override
    public void close() {
        writeToConfig();
        if (this.client != null) this.client.setScreen(parent);
    }

    private void save() {
        try {
            writeToConfig();
            AtomicsClient.CONFIG.save(FabricLoader.getInstance().getConfigDir().resolve("atomics_client.json"));
            status = Text.literal("Saved player list").formatted(Formatting.GREEN);
        } catch (Exception e) {
            status = Text.literal("Save failed: " + e.getMessage()).formatted(Formatting.RED);
        }
    }

    private void loadFromConfig(TpsConfig cfg) {
        entries.clear();
        if (cfg.pvp.friendNames != null) {
            for (String name : cfg.pvp.friendNames) {
                if (name != null && !name.isBlank()) entries.add(new Entry(sanitizeName(name), true));
            }
        }
        if (cfg.pvp.foeNames != null) {
            for (String name : cfg.pvp.foeNames) {
                if (name != null && !name.isBlank()) entries.add(new Entry(sanitizeName(name), false));
            }
        }
        loaded = true;
    }

    private void writeToConfig() {
        TpsConfig cfg = liveConfig();
        cfg.pvp.friendFoeOverlayEnabled = true;
        cfg.pvp.friendNames = new ArrayList<>();
        cfg.pvp.foeNames = new ArrayList<>();
        for (Entry entry : entries) {
            String name = sanitizeName(entry.name);
            if (name.isEmpty()) {
                continue;
            }
            AtomicsClient.removeName(cfg.pvp.friendNames, name.toLowerCase(java.util.Locale.ROOT));
            AtomicsClient.removeName(cfg.pvp.foeNames, name.toLowerCase(java.util.Locale.ROOT));
            if (entry.friend) {
                cfg.pvp.friendNames.add(name);
            } else {
                cfg.pvp.foeNames.add(name);
            }
        }
        cfg.normalize();
    }

    private static TpsConfig liveConfig() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) {
            cfg = new TpsConfig();
            AtomicsClient.CONFIG = cfg;
        }
        return cfg.normalize();
    }

    private static String sanitizeName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed;
    }

    private static class Entry {
        String name;
        boolean friend;

        Entry(String name, boolean friend) {
            this.name = name;
            this.friend = friend;
        }
    }

    private record RowLabel(String text, int x, int y) {
    }
}
