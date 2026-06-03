package com.atomics.client.gui;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class ArmorHudLayoutScreen extends Screen {
    private static final int HUD_ITEM_SIZE = 16;
    private static final int HUD_TEXT_HEIGHT = 12;
    private static final int MIN_SPACING = 20;
    private static final int MAX_SPACING = 64;
    private static final Identifier HOTBAR_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SPRITE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");
    private static final Identifier HOTBAR_OFFHAND_LEFT_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final Identifier HEART_CONTAINER_TEXTURE = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL_TEXTURE = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.withDefaultNamespace("hud/armor_empty");
    private static final Identifier ARMOR_FULL_TEXTURE = Identifier.withDefaultNamespace("hud/armor_full");
    private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_FULL_TEXTURE = Identifier.withDefaultNamespace("hud/food_full");

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
        super(Component.literal("Armor HUD Layout"));
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
        addRenderableWidget(Button.builder(Component.literal(vertical ? "Vertical" : "Horizontal"), button -> {
            vertical = !vertical;
            clampHud();
            button.setMessage(Component.literal(vertical ? "Vertical" : "Horizontal"));
            apply();
        }).bounds(x, buttonY, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Spacing -"), button -> {
            spacing = Math.max(MIN_SPACING, spacing - 2);
            clampHud();
            apply();
        }).bounds(x + (buttonW + gap), buttonY, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Spacing +"), button -> {
            spacing = Math.min(MAX_SPACING, spacing + 2);
            clampHud();
            apply();
        }).bounds(x + (buttonW + gap) * 2, buttonY, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose()).bounds(x + (buttonW + gap) * 3, buttonY, buttonW, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xDD080706);
        super.render(context, mouseX, mouseY, delta);
        renderVanillaHudReference(context);
        renderHudPreview(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (isInsideHud(click.x(), click.y())) {
            dragging = true;
            dragOffsetX = (int) Math.round(click.x()) - hudX;
            dragOffsetY = (int) Math.round(click.y()) - hudY;
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
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
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging) {
            dragging = false;
            apply();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        apply();
        Minecraft.getInstance().setScreen(parent);
    }

    private void renderHudPreview(GuiGraphics context) {
        int left = hudX - 4;
        int top = hudY - 4;
        int right = hudX + hudWidth() + 4;
        int bottom = hudY + hudHeight() + 4;
        int border = dragging ? 0xFFFFA13D : 0x99FFA13D;
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
            context.renderItem(stacks[i], itemX, itemY);
            renderItemDurabilityBar(context, itemX + 2, itemY + 13, 12, i);
            String text = previewDurabilityText(i);
            if (!text.isEmpty()) {
                float scale = 0.68f;
                int textWidth = this.font.width(text);
                int textX = Math.round((itemX + 8.0f - textWidth * scale / 2.0f) / scale);
                int textY = Math.round((itemY + 18) / scale);
                context.pose().pushMatrix();
                context.pose().scale(scale, scale);
                context.drawString(this.font, text, textX, textY, 0xFF55FF55);
                context.pose().popMatrix();
            }

            if (vertical) {
                y += spacing;
            } else {
                x += spacing;
            }
        }
    }

    private void renderPreviewSlot(GuiGraphics context, int x, int y) {
        context.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE_TEXTURE, x, y, 0.0f, 0.0f, 22, 22, 182, 22);
    }

    private void renderVanillaHudReference(GuiGraphics context) {
        int hotbarX = this.width / 2 - 91;
        int hotbarY = this.height - 23;
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, hotbarX, hotbarY, 182, 22);

        int offhandX = hotbarX - 28;
        int offhandY = hotbarY;
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_TEXTURE, offhandX - 1, offhandY - 1, 29, 24);
        context.renderItem(new ItemStack(Items.SHIELD), offhandX + 3, offhandY + 3);

        int statusY = hotbarY - 20;
        int armorY = hotbarY - 30;
        renderGuiIconRow(context, hotbarX, statusY, 10, HEART_CONTAINER_TEXTURE, HEART_FULL_TEXTURE);
        renderGuiIconRow(context, hotbarX + 101, statusY, 10, FOOD_EMPTY_TEXTURE, FOOD_FULL_TEXTURE);
        renderGuiIconRow(context, hotbarX, armorY, 10, ARMOR_EMPTY_TEXTURE, ARMOR_FULL_TEXTURE);
    }

    private void renderGuiIconRow(GuiGraphics context, int x, int y, int count, Identifier background, Identifier foreground) {
        for (int i = 0; i < count; i++) {
            int iconX = x + i * 8;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, background, iconX, y, 9, 9);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, foreground, iconX, y, 9, 9);
        }
    }

    private void renderItemDurabilityBar(GuiGraphics context, int x, int y, int width, int index) {
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
