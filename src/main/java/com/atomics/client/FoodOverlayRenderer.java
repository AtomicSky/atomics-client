package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FoodOverlayRenderer {
    private static final int STATUS_ROW_Y_OFFSET = 39;
    private static final int ICON_SPACING = 8;
    private static final int ICON_WIDTH = 8;

    private FoodOverlayRenderer() {
    }

    public static void render(GuiGraphics context) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (context == null || cfg == null || !cfg.enabled || cfg.visual == null || !cfg.visual.foodOverlayEnabled || player == null) {
            return;
        }
        if (client.options.hideGui || player.isSpectator()) {
            return;
        }

        int top = context.guiHeight() - STATUS_ROW_Y_OFFSET;
        int right = context.guiWidth() / 2 + 91;
        int currentFood = player.getFoodData().getFoodLevel();
        float currentSaturation = player.getFoodData().getSaturationLevel();

        drawFoodBars(context, right, top, 0, currentSaturation, 0xFFE9D262);

        HeldFood heldFood = getHeldFood(player);
        if (heldFood == null || (currentFood >= 20 && !isGoldenApple(heldFood.stack()))) {
            return;
        }

        int nutrition = heldFood.properties().getNutrition();
        float saturation = nutrition * heldFood.properties().getSaturationModifier() * 2.0f;
        drawFoodBars(context, right, top, currentFood, nutrition, 0xAAFF8C42);
        drawFoodBars(context, right, top - 2, Math.round(currentSaturation), saturation, 0xAAE9D262);
    }

    private static void drawFoodBars(GuiGraphics context, int right, int top, int start, float amount, int color) {
        int remaining = Math.max(0, Math.round(amount));
        for (int point = 0; point < remaining; point++) {
            int foodPoint = Math.max(0, start + point);
            int icon = Math.min(9, foodPoint / 2);
            int x = right - icon * ICON_SPACING - 9;
            int width = foodPoint % 2 == 0 ? 4 : ICON_WIDTH;
            context.fill(x, top + 8, x + width, top + 9, color);
        }
    }

    private static HeldFood getHeldFood(Player player) {
        HeldFood mainHand = getFood(player.getMainHandItem());
        return mainHand != null ? mainHand : getFood(player.getOffhandItem());
    }

    private static HeldFood getFood(ItemStack stack) {
        FoodProperties properties = stack == null || stack.isEmpty() ? null : stack.getItem().getFoodProperties();
        return properties == null ? null : new HeldFood(stack, properties);
    }

    private static boolean isGoldenApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private record HeldFood(ItemStack stack, FoodProperties properties) {
    }
}