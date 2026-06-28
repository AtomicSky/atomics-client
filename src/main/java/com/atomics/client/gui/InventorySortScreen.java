package com.atomics.client.gui;

import com.atomics.client.AtomicsClient;
import com.atomics.client.InventorySorter;
import com.atomics.client.config.TpsConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InventorySortScreen extends Screen {
    private static final int BG = 0xE0100D0A;
    private static final int PANEL = 0xAA1A130F;
    private static final int PANEL_BORDER = 0x70684A32;
    private static final int ACCENT = 0xFFFFA13D;
    private static final int ACCENT_SOFT = 0x55FF7A21;
    private static final int TEXT_MAIN = 0xFFFFF2E4;
    private static final int TEXT_MUTED = 0xFFCDB59C;
    private static final int SLOT = 18;
    private static final int ITEM_OFFSET = 1;
    private static final int BUTTON_H = 20;
    private static final int INVENTORY_TEXTURE_SIZE = 256;
    private static final int INVENTORY_SLOT_U = 7;
    private static final int INVENTORY_ARMOR_SLOT_V = 7;
    private static final int INVENTORY_MAIN_SLOT_V = 83;
    private static final int INVENTORY_HOTBAR_SLOT_V = 141;
    private static final int INVENTORY_OFFHAND_SLOT_U = 76;
    private static final int INVENTORY_OFFHAND_SLOT_V = 61;
    private static final Identifier INVENTORY_TEXTURE = Identifier.ofVanilla("textures/gui/container/inventory.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.ofVanilla("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.ofVanilla("container/slot_highlight_front");
    private static final Identifier EMPTY_HELMET_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/helmet");
    private static final Identifier EMPTY_CHESTPLATE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/chestplate");
    private static final Identifier EMPTY_LEGGINGS_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/leggings");
    private static final Identifier EMPTY_BOOTS_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/boots");
    private static final Identifier EMPTY_OFFHAND_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/shield");

    private final Screen parent;
    private final List<TpsConfig.InventorySortKit> kits = new ArrayList<>();

    private boolean loaded;
    private boolean sorterEnabled;
    private int selectedIndex;
    private int kitScroll;
    private int sidebarX;
    private int sidebarY;
    private int sidebarW;
    private int sidebarH;
    private int beforeX;
    private int afterX;
    private int layoutY;
    private String heldStack = "";
    private int heldLayout = -1;
    private int heldSlot = -1;
    private Text status = Text.empty();
    private TextFieldWidget nameField;
    private TextFieldWidget serverField;

    public InventorySortScreen(Screen parent) {
        super(Text.literal("Inventory Sorter"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!loaded) {
            loadFromConfig();
        }

        sidebarX = 18;
        sidebarY = 48;
        sidebarW = Math.max(150, Math.min(210, this.width / 4));
        sidebarH = this.height - 88;
        int detailX = sidebarX + sidebarW + 18;
        int detailW = Math.max(320, this.width - detailX - 18);
        int gap = Math.max(12, Math.min(26, (detailW - 162 * 2) / 3));
        beforeX = detailX + gap;
        afterX = beforeX + 162 + gap;
        if (afterX + 162 > this.width - 18) {
            afterX = Math.max(beforeX + 174, this.width - 180);
        }
        layoutY = 132;

        addDrawableChild(ButtonWidget.builder(Text.literal("Sorter: " + (sorterEnabled ? "ON" : "OFF")), b -> {
            restoreHeldStack();
            sorterEnabled = !sorterEnabled;
            b.setMessage(Text.literal("Sorter: " + (sorterEnabled ? "ON" : "OFF")));
            writeToConfig();
            status = Text.literal("Inventory sorter " + (sorterEnabled ? "enabled" : "disabled")).formatted(Formatting.AQUA);
        }).dimensions(sidebarX, 22, sidebarW, BUTTON_H).build());

        addKitListButtons();
        addKitEditorWidgets(detailX, detailW);

        int bottom = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save()).dimensions(this.width - 174, bottom, 72, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(this.width - 96, bottom, 72, BUTTON_H).build());
    }

    private void addKitListButtons() {
        int rowH = 24;
        int listTop = sidebarY + 24;
        int listBottom = sidebarY + sidebarH - 58;
        for (int i = 0; i < kits.size(); i++) {
            int rowY = listTop + i * rowH - kitScroll;
            if (rowY < listTop || rowY + BUTTON_H > listBottom) {
                continue;
            }
            TpsConfig.InventorySortKit kit = kits.get(i);
            String label = (i == selectedIndex ? "> " : "") + kit.name;
            int index = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                restoreHeldStack();
                selectedIndex = index;
                clearAndInit();
            }).dimensions(sidebarX, rowY, sidebarW, BUTTON_H).build());
        }

        int y = sidebarY + sidebarH - 48;
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Kit"), b -> {
            restoreHeldStack();
            TpsConfig.InventorySortKit kit = new TpsConfig.InventorySortKit();
            kit.id = "kit-" + System.currentTimeMillis() + "-" + kits.size();
            kit.name = "Kit " + (kits.size() + 1);
            kit.serverAddress = currentServerAddress();
            kits.add(kit);
            selectedIndex = kits.size() - 1;
            writeToConfig();
            status = Text.literal("Added kit").formatted(Formatting.GREEN);
            clearAndInit();
        }).dimensions(sidebarX, y, sidebarW, BUTTON_H).build());

        ButtonWidget remove = ButtonWidget.builder(Text.literal("Remove Kit"), b -> {
            restoreHeldStack();
            if (selectedKit() != null) {
                kits.remove(selectedIndex);
                selectedIndex = Math.max(0, Math.min(selectedIndex, kits.size() - 1));
                writeToConfig();
                status = Text.literal("Removed kit").formatted(Formatting.YELLOW);
                clearAndInit();
            }
        }).dimensions(sidebarX, y + 24, sidebarW, BUTTON_H).build();
        remove.active = selectedKit() != null;
        addDrawableChild(remove);
    }

    private void addKitEditorWidgets(int detailX, int detailW) {
        TpsConfig.InventorySortKit kit = selectedKit();
        if (kit == null) {
            return;
        }

        int fieldW = Math.max(170, Math.min(250, (detailW - 12) / 2));
        nameField = new TextFieldWidget(this.textRenderer, detailX, 58, fieldW, BUTTON_H, Text.literal("Kit Name"));
        nameField.setText(kit.name);
        nameField.setChangedListener(value -> {
            kit.name = sanitizeName(value);
            writeToConfig();
        });
        addDrawableChild(nameField);

        serverField = new TextFieldWidget(this.textRenderer, detailX + fieldW + 12, 58, fieldW, BUTTON_H, Text.literal("Server IP"));
        serverField.setText(kit.serverAddress);
        serverField.setPlaceholder(Text.literal("server.example.net").formatted(Formatting.DARK_GRAY));
        serverField.setChangedListener(value -> {
            kit.serverAddress = sanitizeServer(value);
            writeToConfig();
        });
        addDrawableChild(serverField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Kit: " + (kit.enabled ? "ON" : "OFF")), b -> {
            restoreHeldStack();
            kit.enabled = !kit.enabled;
            b.setMessage(Text.literal("Kit: " + (kit.enabled ? "ON" : "OFF")));
            writeToConfig();
        }).dimensions(detailX, 86, 100, BUTTON_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Paste Before"), b -> {
            restoreHeldStack();
            List<String> snapshot = InventorySorter.captureCurrentInventory(MinecraftClient.getInstance());
            kit.beforeSlots = copySlots(snapshot);
            if (!hasConfiguredSlots(kit.afterSlots)) {
                kit.afterSlots = copySlots(snapshot);
            }
            writeToConfig();
            status = Text.literal("Before kit pasted").formatted(Formatting.GREEN);
        }).dimensions(beforeX, layoutY - 28, 102, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            restoreHeldStack();
            kit.beforeSlots = InventorySorter.emptySlots();
            writeToConfig();
            status = Text.literal("Before kit cleared").formatted(Formatting.YELLOW);
        }).dimensions(beforeX + 108, layoutY - 28, 54, BUTTON_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Paste After"), b -> {
            restoreHeldStack();
            kit.afterSlots = InventorySorter.captureCurrentInventory(MinecraftClient.getInstance());
            writeToConfig();
            status = Text.literal("After kit pasted").formatted(Formatting.GREEN);
        }).dimensions(afterX, layoutY - 28, 102, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Mirror"), b -> {
            restoreHeldStack();
            kit.afterSlots = copySlots(kit.beforeSlots);
            writeToConfig();
            status = Text.literal("After kit mirrored").formatted(Formatting.AQUA);
        }).dimensions(afterX + 108, layoutY - 28, 54, BUTTON_H).build());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (handleLayoutClick(click.x(), click.y())) {
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE && !heldStack.isEmpty()) {
            restoreHeldStack();
            writeToConfig();
            status = Text.literal("Move cancelled").formatted(Formatting.YELLOW);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        return super.charTyped(charInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= sidebarX && mouseX <= sidebarX + sidebarW && mouseY >= sidebarY && mouseY <= sidebarY + sidebarH) {
            int max = Math.max(0, kits.size() * 24 - (sidebarH - 82));
            kitScroll = Math.max(0, Math.min(max, kitScroll - (int) (verticalAmount * 24)));
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, BG);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, TEXT_MAIN);
        drawPanel(context, sidebarX - 8, sidebarY - 8, sidebarW + 16, sidebarH + 16);
        int detailX = sidebarX + sidebarW + 18;
        drawPanel(context, detailX - 8, 48, this.width - detailX - 10, this.height - 88);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Kits"), sidebarX + 4, sidebarY + 2, TEXT_MAIN);
        TpsConfig.InventorySortKit kit = selectedKit();
        if (kit != null) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Name"), detailX + 2, 46, TEXT_MUTED);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Server IP"), detailX + Math.max(170, Math.min(250, (this.width - detailX - 30) / 2)) + 14, 46, TEXT_MUTED);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Before"), beforeX, layoutY - 44, TEXT_MAIN);
            context.drawTextWithShadow(this.textRenderer, Text.literal("After"), afterX, layoutY - 44, TEXT_MAIN);
            drawLayout(context, kit.beforeSlots, beforeX, layoutY, 0);
            drawLayout(context, kit.afterSlots, afterX, layoutY, 1);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No kits yet"), detailX + (this.width - detailX) / 2, this.height / 2, TEXT_MUTED);
        }

        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, status, 24, this.height - 22, TEXT_MAIN);
        renderHeldStack(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        restoreHeldStack();
        writeToConfig();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private boolean handleLayoutClick(double mouseX, double mouseY) {
        TpsConfig.InventorySortKit kit = selectedKit();
        if (kit == null) {
            return false;
        }

        SlotHit beforeHit = findSlot(mouseX, mouseY, beforeX, layoutY);
        if (beforeHit != null) {
            clickVirtualSlot(kit.beforeSlots, beforeHit.index, 0);
            return true;
        }
        SlotHit afterHit = findSlot(mouseX, mouseY, afterX, layoutY);
        if (afterHit != null) {
            clickVirtualSlot(kit.afterSlots, afterHit.index, 1);
            return true;
        }
        return false;
    }

    private void clickVirtualSlot(List<String> slots, int index, int layout) {
        slots = ensureSlots(slots);
        TpsConfig.InventorySortKit kit = selectedKit();
        if (kit == null) {
            return;
        }
        if (layout == 0) {
            kit.beforeSlots = slots;
        } else {
            kit.afterSlots = slots;
        }

        String slotStack = slots.get(index);
        if (heldStack.isEmpty()) {
            if (!slotStack.isEmpty()) {
                heldStack = slotStack;
                heldLayout = layout;
                heldSlot = index;
                slots.set(index, "");
                status = Text.literal("Picked up slot " + InventorySorter.slotLabel(index)).formatted(Formatting.AQUA);
            }
            return;
        }

        slots.set(index, heldStack);
        heldStack = slotStack;
        heldLayout = heldStack.isEmpty() ? -1 : layout;
        heldSlot = heldStack.isEmpty() ? -1 : index;
        writeToConfig();
        status = Text.literal("Moved slot " + InventorySorter.slotLabel(index)).formatted(Formatting.GREEN);
    }

    private void restoreHeldStack() {
        if (heldStack.isEmpty()) {
            return;
        }
        TpsConfig.InventorySortKit kit = selectedKit();
        if (kit == null) {
            clearHeldStack();
            return;
        }
        List<String> slots = heldLayout == 0 ? ensureSlots(kit.beforeSlots) : ensureSlots(kit.afterSlots);
        if (heldLayout == 0) {
            kit.beforeSlots = slots;
        } else {
            kit.afterSlots = slots;
        }
        int target = heldSlot >= 0 && heldSlot < InventorySorter.SLOT_COUNT && slots.get(heldSlot).isEmpty()
                ? heldSlot
                : firstEmptySlot(slots);
        if (target >= 0) {
            slots.set(target, heldStack);
        }
        clearHeldStack();
    }

    private void clearHeldStack() {
        heldStack = "";
        heldLayout = -1;
        heldSlot = -1;
    }

    private void drawLayout(DrawContext context, List<String> encodedSlots, int x, int y, int layout) {
        List<String> slots = ensureSlots(encodedSlots);
        for (int i = 36; i <= 39; i++) {
            drawVirtualSlot(context, slots, i, x + (i - 36) * SLOT, y, layout);
        }
        drawVirtualSlot(context, slots, 40, x + SLOT * 8, y, layout);

        int mainY = y + 28;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                drawVirtualSlot(context, slots, index, x + col * SLOT, mainY + row * SLOT, layout);
            }
        }

        int hotbarY = mainY + SLOT * 3 + 8;
        for (int col = 0; col < 9; col++) {
            drawVirtualSlot(context, slots, col, x + col * SLOT, hotbarY, layout);
        }
    }

    private void drawVirtualSlot(DrawContext context, List<String> slots, int index, int x, int y, int layout) {
        boolean highlighted = heldLayout == layout && heldSlot == index;
        if (highlighted) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_TEXTURE, x - 3, y - 3, 24, 24);
        }
        drawInventorySlot(context, index, x, y);

        String encoded = slots.get(index);
        if (encoded == null || encoded.isEmpty()) {
            drawEmptySlotIcon(context, index, x, y);
            if (highlighted) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_TEXTURE, x - 3, y - 3, 24, 24);
            }
            return;
        }

        ItemStack stack = InventorySorter.deserializeStack(encoded, MinecraftClient.getInstance());
        if (!stack.isEmpty()) {
            context.drawItem(stack, x + ITEM_OFFSET, y + ITEM_OFFSET);
            context.drawStackOverlay(this.textRenderer, stack, x + ITEM_OFFSET, y + ITEM_OFFSET);
        }
        if (highlighted) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_TEXTURE, x - 3, y - 3, 24, 24);
        }
    }

    private void drawInventorySlot(DrawContext context, int index, int x, int y) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                INVENTORY_TEXTURE,
                x,
                y,
                inventorySlotU(index),
                inventorySlotV(index),
                SLOT,
                SLOT,
                INVENTORY_TEXTURE_SIZE,
                INVENTORY_TEXTURE_SIZE
        );
    }

    private void drawEmptySlotIcon(DrawContext context, int index, int x, int y) {
        Identifier texture = emptySlotIcon(index);
        if (texture != null) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x + ITEM_OFFSET, y + ITEM_OFFSET, 16, 16);
        }
    }

    private static int inventorySlotU(int index) {
        if (index == 40) {
            return INVENTORY_OFFHAND_SLOT_U;
        }
        if (index >= 36 && index <= 39) {
            return INVENTORY_SLOT_U;
        }
        if (index >= 0 && index <= 8) {
            return INVENTORY_SLOT_U + index * SLOT;
        }
        if (index >= 9 && index <= 35) {
            return INVENTORY_SLOT_U + ((index - 9) % 9) * SLOT;
        }
        return INVENTORY_SLOT_U;
    }

    private static int inventorySlotV(int index) {
        if (index == 40) {
            return INVENTORY_OFFHAND_SLOT_V;
        }
        if (index >= 36 && index <= 39) {
            return INVENTORY_ARMOR_SLOT_V + (index - 36) * SLOT;
        }
        if (index >= 0 && index <= 8) {
            return INVENTORY_HOTBAR_SLOT_V;
        }
        if (index >= 9 && index <= 35) {
            return INVENTORY_MAIN_SLOT_V + ((index - 9) / 9) * SLOT;
        }
        return INVENTORY_MAIN_SLOT_V;
    }

    private static Identifier emptySlotIcon(int index) {
        return switch (index) {
            case 36 -> EMPTY_HELMET_SLOT_TEXTURE;
            case 37 -> EMPTY_CHESTPLATE_SLOT_TEXTURE;
            case 38 -> EMPTY_LEGGINGS_SLOT_TEXTURE;
            case 39 -> EMPTY_BOOTS_SLOT_TEXTURE;
            case 40 -> EMPTY_OFFHAND_SLOT_TEXTURE;
            default -> null;
        };
    }

    private void renderHeldStack(DrawContext context, int mouseX, int mouseY) {
        if (heldStack.isEmpty()) {
            return;
        }
        ItemStack stack = InventorySorter.deserializeStack(heldStack, MinecraftClient.getInstance());
        if (!stack.isEmpty()) {
            context.drawItem(stack, mouseX + 8, mouseY + 8);
            context.drawStackOverlay(this.textRenderer, stack, mouseX + 8, mouseY + 8);
        }
    }

    private SlotHit findSlot(double mouseX, double mouseY, int x, int y) {
        for (int i = 36; i <= 39; i++) {
            int slotX = x + (i - 36) * SLOT;
            if (isInside(mouseX, mouseY, slotX, y)) {
                return new SlotHit(i);
            }
        }
        if (isInside(mouseX, mouseY, x + SLOT * 8, y)) {
            return new SlotHit(40);
        }

        int mainY = y + 28;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + col * SLOT;
                int slotY = mainY + row * SLOT;
                if (isInside(mouseX, mouseY, slotX, slotY)) {
                    return new SlotHit(9 + row * 9 + col);
                }
            }
        }

        int hotbarY = mainY + SLOT * 3 + 8;
        for (int col = 0; col < 9; col++) {
            int slotX = x + col * SLOT;
            if (isInside(mouseX, mouseY, slotX, hotbarY)) {
                return new SlotHit(col);
            }
        }
        return null;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }

    private void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL);
        context.fill(x, y, x + width, y + 1, PANEL_BORDER);
        context.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        context.fill(x, y, x + 1, y + height, PANEL_BORDER);
        context.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
        context.fill(x, y, x + 3, y + height, ACCENT_SOFT);
    }

    private void save() {
        try {
            restoreHeldStack();
            writeToConfig();
            AtomicsClient.CONFIG.save(FabricLoader.getInstance().getConfigDir().resolve("atomics_client.json"));
            status = Text.literal("Saved inventory sorter").formatted(Formatting.GREEN);
        } catch (Exception e) {
            status = Text.literal("Save failed: " + e.getMessage()).formatted(Formatting.RED);
        }
    }

    private void loadFromConfig() {
        TpsConfig cfg = liveConfig();
        sorterEnabled = cfg.inventorySorter.enabled;
        kits.clear();
        for (TpsConfig.InventorySortKit kit : cfg.inventorySorter.kits) {
            kits.add(copyKit(kit));
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, kits.size() - 1));
        loaded = true;
    }

    private void writeToConfig() {
        TpsConfig cfg = liveConfig();
        cfg.inventorySorter.enabled = sorterEnabled;
        cfg.inventorySorter.kits = new ArrayList<>();
        for (TpsConfig.InventorySortKit kit : kits) {
            cfg.inventorySorter.kits.add(copyKit(kit));
        }
        cfg.normalize();
    }

    private TpsConfig.InventorySortKit selectedKit() {
        if (kits.isEmpty()) {
            return null;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, kits.size() - 1));
        return kits.get(selectedIndex);
    }

    private static TpsConfig liveConfig() {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null) {
            cfg = new TpsConfig();
            AtomicsClient.CONFIG = cfg;
        }
        return cfg.normalize();
    }

    private static TpsConfig.InventorySortKit copyKit(TpsConfig.InventorySortKit source) {
        TpsConfig.InventorySortKit copy = new TpsConfig.InventorySortKit();
        if (source != null) {
            copy.id = source.id;
            copy.name = sanitizeName(source.name);
            copy.serverAddress = sanitizeServer(source.serverAddress);
            copy.enabled = source.enabled;
            copy.beforeSlots = copySlots(source.beforeSlots);
            copy.afterSlots = copySlots(source.afterSlots);
        }
        return copy;
    }

    private static List<String> copySlots(List<String> slots) {
        return new ArrayList<>(ensureSlots(slots));
    }

    private static List<String> ensureSlots(List<String> slots) {
        ArrayList<String> normalized = new ArrayList<>(InventorySorter.SLOT_COUNT);
        if (slots != null) {
            for (String slot : slots) {
                normalized.add(slot == null ? "" : slot);
                if (normalized.size() >= InventorySorter.SLOT_COUNT) {
                    break;
                }
            }
        }
        while (normalized.size() < InventorySorter.SLOT_COUNT) {
            normalized.add("");
        }
        return normalized;
    }

    private static boolean hasConfiguredSlots(List<String> slots) {
        if (slots == null) {
            return false;
        }
        for (String slot : slots) {
            if (slot != null && !slot.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int firstEmptySlot(List<String> slots) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) == null || slots.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static String sanitizeName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "Kit";
        }
        return trimmed.length() > 48 ? trimmed.substring(0, 48) : trimmed;
    }

    private static String sanitizeServer(String value) {
        return value == null ? "" : value.trim();
    }

    private static String currentServerAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.isInSingleplayer()) {
            return "";
        }
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            return client.getCurrentServerEntry().address.trim();
        }
        if (client.getNetworkHandler() != null
                && client.getNetworkHandler().getServerInfo() != null
                && client.getNetworkHandler().getServerInfo().address != null) {
            return client.getNetworkHandler().getServerInfo().address.trim();
        }
        return "";
    }

    private record SlotHit(int index) {
    }
}
