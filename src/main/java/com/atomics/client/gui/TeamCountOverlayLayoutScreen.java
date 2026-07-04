package com.atomics.client.gui;

import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

class TeamCountOverlayLayoutScreen extends Screen {
    private final AtomicsClientScreen parent;
    private int overlayX;
    private int overlayY;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    TeamCountOverlayLayoutScreen(AtomicsClientScreen parent, int overlayX, int overlayY) {
        super(Text.literal("Team Count Overlay Layout"));
        this.parent = parent;
        this.overlayX = overlayX;
        this.overlayY = overlayY;
    }

    @Override
    protected void init() {
        if (overlayX < 0 || overlayY < 0) {
            resetOverlay();
        } else {
            clampOverlay();
        }

        int buttonY = 10;
        int buttonW = 96;
        int gap = 6;
        int x = this.width / 2 - (buttonW * 2 + gap) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> resetOverlay()).dimensions(x, buttonY, buttonW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(x + buttonW + gap, buttonY, buttonW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        AtomicsGuiStyle.drawBackground(context, this.width, this.height);
        super.render(context, mouseX, mouseY, delta);
        renderOverlayPreview(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (isInsideOverlay(click.x(), click.y())) {
            dragging = true;
            dragOffsetX = (int) Math.round(click.x()) - overlayX;
            dragOffsetY = (int) Math.round(click.y()) - overlayY;
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging) {
            overlayX = (int) Math.round(click.x()) - dragOffsetX;
            overlayY = (int) Math.round(click.y()) - dragOffsetY;
            clampOverlay();
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

    private void renderOverlayPreview(DrawContext context) {
        clampOverlay();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientFeatureManager.renderTeamCountOverlayPreview(context, client, overlayX, overlayY);

        int left = overlayX - 4;
        int top = overlayY - 4;
        int right = overlayX + overlayWidth() + 4;
        int bottom = overlayY + overlayHeight() + 4;
        int border = dragging ? AtomicsGuiStyle.ACCENT : AtomicsGuiStyle.ACCENT_SOFT;
        context.fill(left, top, right, top + 1, border);
        context.fill(left, bottom - 1, right, bottom, border);
        context.fill(left, top, left + 1, bottom, border);
        context.fill(right - 1, top, right, bottom, border);
    }

    private boolean isInsideOverlay(double mouseX, double mouseY) {
        return mouseX >= overlayX - 4 && mouseX <= overlayX + overlayWidth() + 4
                && mouseY >= overlayY - 4 && mouseY <= overlayY + overlayHeight() + 4;
    }

    private int overlayWidth() {
        return ClientFeatureManager.teamCountOverlayPreviewWidth(MinecraftClient.getInstance());
    }

    private int overlayHeight() {
        return ClientFeatureManager.teamCountOverlayPreviewHeight(MinecraftClient.getInstance());
    }

    private void resetOverlay() {
        MinecraftClient client = MinecraftClient.getInstance();
        overlayX = ClientFeatureManager.defaultTeamCountOverlayX(client);
        overlayY = ClientFeatureManager.defaultTeamCountOverlayY();
        clampOverlay();
        apply();
    }

    private void clampOverlay() {
        overlayX = Math.max(0, Math.min(this.width - overlayWidth(), overlayX));
        overlayY = Math.max(0, Math.min(this.height - overlayHeight(), overlayY));
    }

    private void apply() {
        parent.applyTeamCountOverlayLayout(overlayX, overlayY);
    }
}
