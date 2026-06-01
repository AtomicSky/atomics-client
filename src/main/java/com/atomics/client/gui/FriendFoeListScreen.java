package com.atomics.client.gui;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class FriendFoeListScreen extends Screen {
    private static final int ROW_HEIGHT = 46;

    private final Screen parent;
    private final List<RowLabel> labels = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private boolean loaded;
    private int scroll;
    private Component status = Component.empty();

    public FriendFoeListScreen(Screen parent) {
        super(Component.literal("Friend/Foe Players"));
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
        addRenderableWidget(Button.builder(Component.literal("+ Add Player"), b -> {
            entries.add(new Entry("", true));
            writeToConfig();
            status = Component.literal("Added player row").withStyle(ChatFormatting.GREEN);
            rebuildWidgets();
        }).bounds(24, bottom, 104, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear All"), b -> {
            entries.clear();
            writeToConfig();
            status = Component.literal("Player list cleared").withStyle(ChatFormatting.YELLOW);
            rebuildWidgets();
        }).bounds(134, bottom, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save()).bounds(this.width - 174, bottom, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(this.width - 96, bottom, 72, 20).build());
    }

    private void addPlayerRow(int x, int y, Entry entry) {
        int fieldW = Math.max(180, this.width - x * 2 - 194);
        int typeX = x + fieldW + 8;
        int removeX = typeX + 86;

        addLabel("Username", x + 2, y + 2);
        addLabel("Type", typeX + 2, y + 2);

        EditBox name = new EditBox(this.font, x, y + 16, fieldW, 20, Component.literal("Username"));
        name.setValue(entry.name);
        name.setResponder(value -> {
            entry.name = sanitizeName(value);
            writeToConfig();
        });
        addRenderableWidget(name);

        addRenderableWidget(Button.builder(Component.literal(entry.friend ? "Friend" : "Foe"), b -> {
            entry.friend = !entry.friend;
            b.setMessage(Component.literal(entry.friend ? "Friend" : "Foe"));
            writeToConfig();
            status = Component.literal("Set " + (entry.friend ? "Friend" : "Foe")).withStyle(ChatFormatting.AQUA);
        }).bounds(typeX, y + 16, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
            entries.remove(entry);
            writeToConfig();
            status = Component.literal("Removed player").withStyle(ChatFormatting.RED);
            rebuildWidgets();
        }).bounds(removeX, y + 16, 70, 20).build());
    }

    private void addLabel(String text, int x, int y) {
        labels.add(new RowLabel(text, x, y));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        int max = Math.max(0, entries.size() * ROW_HEIGHT - (this.height - 92));
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
        context.drawString(this.font, status, 234, this.height - 22, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        writeToConfig();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private void save() {
        try {
            writeToConfig();
            AtomicsClient.CONFIG.save(FMLPaths.CONFIGDIR.get().resolve("atomics_client.json"));
            status = Component.literal("Saved player list").withStyle(ChatFormatting.GREEN);
        } catch (Exception e) {
            status = Component.literal("Save failed: " + e.getMessage()).withStyle(ChatFormatting.RED);
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
