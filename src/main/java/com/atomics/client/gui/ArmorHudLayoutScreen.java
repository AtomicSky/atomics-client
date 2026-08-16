package com.atomics.client.gui;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

class ArmorHudLayoutScreen extends Screen {
    private static final int HUD_ITEM_SIZE = 16;
    private static final int HUD_TEXT_HEIGHT = 12;
    private static final int MIN_SPACING = 20;
    private static final int MAX_SPACING = 64;
    private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");
    private static final Identifier HOTBAR_SPRITE_TEXTURE = Identifier.ofVanilla("textures/gui/sprites/hud/hotbar.png");
    private static final Identifier HOTBAR_OFFHAND_LEFT_TEXTURE = Identifier.ofVanilla("hud/hotbar_offhand_left");
    private static final Identifier HEART_CONTAINER_TEXTURE = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL_TEXTURE = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/armor_empty");
    private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");
    private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");

    private final AtomicsClientScreen parent;
    private int hudX;
    private int hudY;
    private boolean vertical;
    private int spacing;
    private final String durabilityMode;
    private final boolean hotbarBorder;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    ArmorHudLayoutScreen(AtomicsClientScreen parent, int hudX, int hudY, boolean vertical, int spacing, String durabilityMode, boolean hotbarBorder) {
        super(Text.literal("Armor HUD Layout"));
        this.parent = parent;
        this.hudX = hudX;
        this.hudY = hudY;
        this.vertical = vertical;
        this.spacing = Math.max(MIN_SPACING, Math.min(MAX_SPACING, spacing));
        this.durabilityMode = durabilityMode;
        this.hotbarBorder = hotbarBorder;
    }

    @Override
    protected void init() {
        if (hudX < 0 || hudY < 0) {
            centerHud();
        } else {
            clampHud();
        }

        int buttonY = 10;
        int buttonW = 82;
        int gap = 6;
        int x = this.width / 2 - (buttonW * 4 + gap * 3) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal(vertical ? "Vertical" : "Horizontal"), button -> {
            vertical = !vertical;
            clampHud();
            button.setMessage(Text.literal(vertical ? "Vertical" : "Horizontal"));
            apply();
        }).dimensions(x, buttonY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Spacing -"), button -> {
            spacing = Math.max(MIN_SPACING, spacing - 2);
            clampHud();
            apply();
        }).dimensions(x + (buttonW + gap), buttonY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Spacing +"), button -> {
            spacing = Math.min(MAX_SPACING, spacing + 2);
            clampHud();
            apply();
        }).dimensions(x + (buttonW + gap) * 2, buttonY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(x + (buttonW + gap) * 3, buttonY, buttonW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AtomicsGuiStyle.drawBackground(context, this.width, this.height);
        super.render(context, mouseX, mouseY, delta);
        renderVanillaHudReference(context);
        renderHudPreview(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (isInsideHud(click.x(), click.y())) {
            dragging = true;
            dragOffsetX = (int) Math.round(click.x()) - hudX;
            dragOffsetY = (int) Math.round(click.y()) - hudY;
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging) {
            hudX = (int) Math.round(click.x()) - dragOffsetX;
            hudY = (int) Math.round(click.y()) - dragOffsetY;
            clampHud();
            apply();
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging) {
            dragging = false;
            apply();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        apply();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void renderHudPreview(DrawContext context) {
        int left = hudX - 4;
        int top = hudY - 4;
        int right = hudX + hudWidth() + 4;
        int bottom = hudY + hudHeight() + 4;
        int border = dragging ? AtomicsGuiStyle.ACCENT : AtomicsGuiStyle.ACCENT_SOFT;
        context.fill(left, top, right, bottom, dragging ? 0x80222222 : 0x55222222);
        context.fill(left, top, right, top + 1, border);
        context.fill(left, bottom - 1, right, bottom, border);
        context.fill(left, top, left + 1, bottom, border);
        context.fill(right - 1, top, right, bottom, border);

        ItemStack[] stacks = {
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS),
                new ItemStack(Items.DIAMOND_BOOTS)
        };
        int x = hudX;
        int y = hudY;
        for (int i = 0; i < stacks.length; i++) {
            if (hotbarBorder) {
                renderPreviewSlot(context, x, y);
            }
            int itemX = x + (hotbarBorder ? 3 : 0);
            int itemY = y + (hotbarBorder ? 3 : 0);
            context.drawItem(stacks[i], itemX, itemY);
            renderItemDurabilityBar(context, itemX + 2, itemY + 13, 12, i);
            String text = previewDurabilityText(i);
            if (!text.isEmpty()) {
                float scale = 0.68f;
                int textWidth = this.textRenderer.getWidth(text);
                int textX = Math.round((itemX + 8.0f - textWidth * scale / 2.0f) / scale);
                int textY = Math.round((itemY + 18) / scale);
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(scale, scale);
                context.drawTextWithShadow(this.textRenderer, text, textX, textY, 0xFF55FF55);
                context.getMatrices().popMatrix();
            }

            if (vertical) {
                y += spacing;
            } else {
                x += spacing;
            }
        }
    }

    private void renderPreviewSlot(DrawContext context, int x, int y) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE_TEXTURE, x, y, 0.0f, 0.0f, 22, 22, 182, 22);
    }

    private void renderVanillaHudReference(DrawContext context) {
        int hotbarX = this.width / 2 - 91;
        int hotbarY = this.height - 23;
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, hotbarX, hotbarY, 182, 22);

        int offhandX = hotbarX - 28;
        int offhandY = hotbarY;
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_TEXTURE, offhandX - 1, offhandY - 1, 29, 24);
        context.drawItem(new ItemStack(Items.SHIELD), offhandX + 3, offhandY + 3);

        int statusY = hotbarY - 20;
        int armorY = hotbarY - 30;
        renderGuiIconRow(context, hotbarX, statusY, 10, HEART_CONTAINER_TEXTURE, HEART_FULL_TEXTURE);
        renderGuiIconRow(context, hotbarX + 101, statusY, 10, FOOD_EMPTY_TEXTURE, FOOD_FULL_TEXTURE);
        renderGuiIconRow(context, hotbarX, armorY, 10, ARMOR_EMPTY_TEXTURE, ARMOR_FULL_TEXTURE);
    }

    private void renderGuiIconRow(DrawContext context, int x, int y, int count, Identifier background, Identifier foreground) {
        for (int i = 0; i < count; i++) {
            int iconX = x + i * 8;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, background, iconX, y, 9, 9);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, foreground, iconX, y, 9, 9);
        }
    }

    private void renderItemDurabilityBar(DrawContext context, int x, int y, int width, int index) {
        int fill = Math.max(1, width - index * 2);
        context.fill(x, y, x + width, y + 2, 0xFF000000);
        context.fill(x, y, x + fill, y + 1, index >= 3 ? 0xFFFF5555 : 0xFF55FF55);
    }

    private String previewDurabilityText(int index) {
        if (TpsConfig.ARMOR_HUD_DURABILITY_BAR.equals(durabilityMode)) {
            return "";
        }
        if (TpsConfig.ARMOR_HUD_DURABILITY_PERCENT.equals(durabilityMode)) {
            return (92 - index * 13) + "%";
        }
        return String.valueOf(420 - index * 37);
    }

    private boolean isInsideHud(double mouseX, double mouseY) {
        return mouseX >= hudX - 4 && mouseX <= hudX + hudWidth() + 4
                && mouseY >= hudY - 4 && mouseY <= hudY + hudHeight() + 4;
    }

    private int hudWidth() {
        int slotSize = hotbarBorder ? 22 : HUD_ITEM_SIZE;
        return vertical ? slotSize : slotSize + spacing * 3;
    }

    private int hudHeight() {
        int slotSize = hotbarBorder ? 22 : HUD_ITEM_SIZE;
        int itemHeight = TpsConfig.ARMOR_HUD_DURABILITY_BAR.equals(durabilityMode)
                ? slotSize
                : HUD_ITEM_SIZE + HUD_TEXT_HEIGHT;
        itemHeight = Math.max(slotSize, itemHeight);
        return vertical ? itemHeight + spacing * 3 : itemHeight;
    }

    private void centerHud() {
        hudX = Math.max(0, this.width / 2 - hudWidth() / 2);
        hudY = Math.max(0, this.height / 2 - hudHeight() / 2);
        apply();
    }

    private void clampHud() {
        hudX = Math.max(0, Math.min(this.width - hudWidth(), hudX));
        hudY = Math.max(0, Math.min(this.height - hudHeight(), hudY));
    }

    private void apply() {
        parent.applyArmorHudLayout(hudX, hudY, vertical, spacing);
    }
}
