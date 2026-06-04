package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

public final class FoodOverlayRenderer {
    private static final int STATUS_ROW_Y_OFFSET = 39;
    private static final int ICON_SPACING = 8;
    private static final int ICON_SIZE = 9;
    private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
    private static final int FOOD_TEXTURE_V = 27;
    private static final int FOOD_EMPTY_U = 16;
    private static final int FOOD_FULL_U = 52;
    private static final int FOOD_HALF_U = 61;
    private static final int FOOD_EMPTY_HUNGER_U = 25;
    private static final int FOOD_FULL_HUNGER_U = 88;
    private static final int FOOD_HALF_HUNGER_U = 97;
    private static final float APPLESKIN_MAX_FLASH_ALPHA = 0.65f;
    private static final Random RANDOM = new Random();
    private static float unclampedFlashAlpha;
    private static byte flashAlphaDirection = 1;
    private static int lastFlashTick = Integer.MIN_VALUE;

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
        boolean hunger = player.hasEffect(MobEffects.HUNGER);
        int emptyU = hunger ? FOOD_EMPTY_HUNGER_U : FOOD_EMPTY_U;
        int halfU = hunger ? FOOD_HALF_HUNGER_U : FOOD_HALF_U;
        int fullU = hunger ? FOOD_FULL_HUNGER_U : FOOD_FULL_U;
        int guiTicks = client.gui == null ? 0 : client.gui.getGuiTicks();

        drawSaturationOverlay(context, currentSaturation, 0.0f, player, right, top, guiTicks, 1.0f, emptyU);

        HeldFood heldFood = getHeldFood(player);
        if (heldFood == null || (currentFood >= 20 && !isGoldenApple(heldFood.stack()))) {
            return;
        }

        int nutrition = heldFood.properties().getNutrition();
        float saturation = nutrition * heldFood.properties().getSaturationModifier() * 2.0f;
        float flashAlpha = automaticAlpha(guiTicks, APPLESKIN_MAX_FLASH_ALPHA);
        if (currentFood < 20 && nutrition > 0) {
            drawHungerOverlay(context, nutrition, currentFood, player, right, top, guiTicks, flashAlpha, emptyU, halfU, fullU);
        }

        int previewFood = Math.min(20, currentFood + Math.max(0, nutrition));
        float previewSaturation = currentSaturation + saturation;
        float saturationIncrement = previewSaturation > previewFood ? previewFood - currentSaturation : saturation;
        if (saturationIncrement > 0.0f) {
            drawSaturationOverlay(context, currentSaturation, saturationIncrement, player, right, top, guiTicks, flashAlpha, emptyU);
        }
    }

    private static void drawHungerOverlay(GuiGraphics context, int foodRestored, int currentFood, Player player, int right, int top, int guiTicks, float alpha, int emptyU, int halfU, int fullU) {
        if (foodRestored <= 0) {
            return;
        }

        int modifiedFood = Math.max(0, Math.min(20, currentFood + foodRestored));
        int startIcon = Math.max(0, currentFood / 2);
        int endIcon = (int) Math.ceil(modifiedFood / 2.0);

        withAlpha(alpha * 0.25f, () -> {
            for (int icon = startIcon; icon < endIcon; icon++) {
                drawFoodIcon(context, right, top, player, guiTicks, icon, emptyU, ICON_SIZE);
            }
        });
        withAlpha(alpha, () -> {
            for (int icon = startIcon; icon < endIcon; icon++) {
                boolean half = icon * 2 + 1 == modifiedFood;
                drawFoodIcon(context, right, top, player, guiTicks, icon, half ? halfU : fullU, ICON_SIZE);
            }
        });
    }

    private static void drawSaturationOverlay(GuiGraphics context, float currentSaturation, float saturationRestored, Player player, int right, int top, int guiTicks, float alpha, int emptyU) {
        if (currentSaturation + saturationRestored < 0.0f) {
            return;
        }

        float modifiedSaturation = Math.max(0.0f, Math.min(20.0f, currentSaturation + saturationRestored));
        int startIcon = saturationRestored == 0.0f ? 0 : Math.max(0, (int) (currentSaturation / 2.0f));
        int endIcon = (int) Math.ceil(modifiedSaturation / 2.0f);

        withAlpha(alpha, () -> {
            for (int icon = startIcon; icon < endIcon; icon++) {
                float iconFill = modifiedSaturation / 2.0f - icon;
                int width = saturationWidth(iconFill);
                if (width > 0) {
                    drawFoodIcon(context, right, top, player, guiTicks, icon, emptyU, width);
                }
            }
        });
    }

    private static void drawFoodIcon(GuiGraphics context, int right, int top, Player player, int guiTicks, int icon, int textureU, int width) {
        int visibleWidth = Math.max(0, Math.min(ICON_SIZE, width));
        if (visibleWidth <= 0) {
            return;
        }

        int xOffset = ICON_SIZE - visibleWidth;
        int x = foodIconX(right, icon) + xOffset;
        int y = top + foodIconYOffset(player, guiTicks, icon);
        context.blit(GUI_ICONS_LOCATION, x, y, textureU + xOffset, FOOD_TEXTURE_V, visibleWidth, ICON_SIZE);
    }

    private static int foodIconX(int right, int icon) {
        return right - icon * ICON_SPACING - ICON_SIZE;
    }

    private static int foodIconYOffset(Player player, int guiTicks, int icon) {
        int currentFood = player.getFoodData().getFoodLevel();
        if (player.getFoodData().getSaturationLevel() > 0.0f) {
            return 0;
        }

        int divisor = currentFood * 3 + 1;
        if (divisor <= 0 || guiTicks % divisor != 0) {
            return 0;
        }

        RANDOM.setSeed(guiTicks * 312871L);
        int offset = 0;
        for (int i = 0; i <= icon; i++) {
            offset = RANDOM.nextInt(3) - 1;
        }
        return offset;
    }

    private static int saturationWidth(float iconFill) {
        if (iconFill >= 1.0f) return ICON_SIZE;
        if (iconFill > 0.5f) return 7;
        if (iconFill > 0.25f) return 5;
        if (iconFill > 0.0f) return 3;
        return 0;
    }

    private static void withAlpha(float alpha, Runnable draw) {
        float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, clampedAlpha);
        draw.run();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static float automaticAlpha(int guiTicks, float maxAlpha) {
        if (guiTicks != lastFlashTick) {
            lastFlashTick = guiTicks;
            if (unclampedFlashAlpha >= 1.5f) {
                flashAlphaDirection = -1;
            } else if (unclampedFlashAlpha <= -0.5f) {
                flashAlphaDirection = 1;
            }
            unclampedFlashAlpha += flashAlphaDirection * 0.125f;
        }

        float alpha = Math.max(0.0f, Math.min(1.0f, unclampedFlashAlpha));
        return alpha * Math.max(0.0f, Math.min(1.0f, maxAlpha));
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
