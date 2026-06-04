package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    private static final int STATUS_ROW_Y_OFFSET = 39;
    private static final int ICON_SPACING = 8;
    private static final int ICON_WIDTH = 8;

    @Inject(method = "renderPlayerHealth", at = @At("TAIL"))
    private void atomics_client$renderStatusOverlays(GuiGraphics context, CallbackInfo ci) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (cfg == null || !cfg.enabled || cfg.visual == null || player == null) {
            return;
        }

        int top = context.guiHeight() - STATUS_ROW_Y_OFFSET;
        if (cfg.visual.partialStatusIconsEnabled) {
            atomics_client$renderPartialHeartMarker(context, player, top);
        }
    }

    private static void atomics_client$renderPartialHeartMarker(GuiGraphics context, Player player, int top) {
        float displayedHealth = Math.max(0.0f, player.getHealth() + player.getAbsorptionAmount());
        float partialHeart = displayedHealth % 2.0f;
        if (partialHeart <= 0.0f || partialHeart >= 1.999f) {
            return;
        }

        int heart = Math.max(0, (int) Math.floor(displayedHealth / 2.0f));
        int x = context.guiWidth() / 2 - 91 + (heart % 10) * ICON_SPACING;
        int y = top - (heart / 10) * 10 + 8;
        int width = Math.max(1, Math.min(ICON_WIDTH, Math.round(partialHeart * ICON_WIDTH / 2.0f)));
        context.fill(x, y, x + width, y + 1, 0xFFFF5555);
    }

    private static void atomics_client$renderFoodOverlay(GuiGraphics context, Player player, int top) {
        int currentFood = player.getFoodData().getFoodLevel();
        float currentSaturation = player.getFoodData().getSaturationLevel();
        int right = context.guiWidth() / 2 + 91;

        atomics_client$drawFoodBars(context, right, top, 0, currentSaturation, 0xFFE9D262);

        HeldFood heldFood = atomics_client$getHeldFood(player);
        if (heldFood == null || (currentFood >= 20 && !atomics_client$isGoldenApple(heldFood.stack()))) {
            return;
        }

        int nutrition = heldFood.properties().getNutrition();
        float saturation = nutrition * heldFood.properties().getSaturationModifier() * 2.0f;
        atomics_client$drawFoodBars(context, right, top, currentFood, nutrition, 0xAAFF8C42);
        atomics_client$drawFoodBars(context, right, top - 2, Math.round(currentSaturation), saturation, 0xAAE9D262);
    }

    private static void atomics_client$drawFoodBars(GuiGraphics context, int right, int top, int start, float amount, int color) {
        int remaining = Math.max(0, Math.round(amount));
        for (int point = 0; point < remaining; point++) {
            int foodPoint = Math.max(0, start + point);
            int icon = Math.min(9, foodPoint / 2);
            int x = right - icon * ICON_SPACING - 9;
            int width = foodPoint % 2 == 0 ? 4 : ICON_WIDTH;
            context.fill(x, top + 8, x + width, top + 9, color);
        }
    }

    private static HeldFood atomics_client$getHeldFood(Player player) {
        HeldFood mainHand = atomics_client$getFood(player.getMainHandItem());
        return mainHand != null ? mainHand : atomics_client$getFood(player.getOffhandItem());
    }

    private static HeldFood atomics_client$getFood(ItemStack stack) {
        FoodProperties properties = stack == null || stack.isEmpty() ? null : stack.getItem().getFoodProperties();
        return properties == null ? null : new HeldFood(stack, properties);
    }

    private static boolean atomics_client$isGoldenApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private record HeldFood(ItemStack stack, FoodProperties properties) {
    }
}
