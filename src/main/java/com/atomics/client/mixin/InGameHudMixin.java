package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import com.atomics.client.render.FoodOverlayTextureCache;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
    private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");
    private static final Identifier APPLESKIN_ICONS_TEXTURE = Identifier.of(AtomicsClient.MOD_ID, "textures/gui/appleskin_icons.png");
    private static final float APPLESKIN_MAX_FLASH_ALPHA = 0.65f;
    private static final Random ATOMICS_CLIENT_RANDOM = new Random();
    private static float atomics_client$unclampedFlashAlpha;
    private static byte atomics_client$flashAlphaDirection = 1;
    private static int atomics_client$lastFlashTick = Integer.MIN_VALUE;

    @Shadow
    public abstract int getTicks();

    @Inject(method = "renderFood", at = @At("TAIL"))
    private void atomics_client$renderFoodPreviewOverlay(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null || !cfg.enabled || cfg.visual == null || !cfg.visual.foodOverlayEnabled || player == null) {
            return;
        }

        boolean hunger = player.hasStatusEffect(StatusEffects.HUNGER);
        Identifier emptyTexture = hunger ? FOOD_EMPTY_HUNGER_TEXTURE : FOOD_EMPTY_TEXTURE;
        Identifier halfTexture = hunger ? FOOD_HALF_HUNGER_TEXTURE : FOOD_HALF_TEXTURE;
        Identifier fullTexture = hunger ? FOOD_FULL_HUNGER_TEXTURE : FOOD_FULL_TEXTURE;

        int currentFood = player.getHungerManager().getFoodLevel();
        float currentSaturation = player.getHungerManager().getSaturationLevel();
        boolean vanillaShape = FoodOverlayTextureCache.hasVanillaShape(emptyTexture) && FoodOverlayTextureCache.hasVanillaShape(fullTexture);
        atomics_client$drawSaturationOverlay(context, currentSaturation, 0.0f, player, right, top, vanillaShape ? 1.0f : cfg.visual.foodOverlayAlpha, fullTexture);

        FoodComponent foodComponent = atomics_client$getHeldFood(player);
        if (foodComponent == null || (foodComponent.nutrition() <= 0 && foodComponent.saturation() <= 0.0f)) {
            return;
        }

        float maxFlashAlpha = vanillaShape ? APPLESKIN_MAX_FLASH_ALPHA : cfg.visual.foodOverlayAlpha * APPLESKIN_MAX_FLASH_ALPHA;
        float flashAlpha = atomics_client$automaticAlpha(maxFlashAlpha);
        if (currentFood < 20 && foodComponent.nutrition() > 0) {
            atomics_client$drawHungerOverlay(context, foodComponent.nutrition(), currentFood, player, right, top, flashAlpha, emptyTexture, halfTexture, fullTexture);
        }

        int previewFood = Math.min(20, currentFood + Math.max(0, foodComponent.nutrition()));
        float previewSaturation = currentSaturation + foodComponent.saturation();
        float saturationIncrement = previewSaturation > previewFood ? previewFood - currentSaturation : foodComponent.saturation();
        if (saturationIncrement > 0.0f) {
            atomics_client$drawSaturationOverlay(context, currentSaturation, saturationIncrement, player, right, top, flashAlpha, fullTexture);
        }
    }

    private void atomics_client$drawHungerOverlay(DrawContext context, int foodRestored, int currentFood, PlayerEntity player, int right, int top, float alpha, Identifier emptyTexture, Identifier halfTexture, Identifier fullTexture) {
        if (foodRestored <= 0) {
            return;
        }

        int modifiedFood = Math.max(0, Math.min(20, currentFood + foodRestored));
        int startIcon = Math.max(0, currentFood / 2);
        int endIcon = (int) Math.ceil(modifiedFood / 2.0);
        int iconColor = atomics_client$whiteWithAlpha(alpha);
        int backgroundColor = atomics_client$whiteWithAlpha(alpha * 0.25f);

        for (int icon = startIcon; icon < endIcon; icon++) {
            int x = atomics_client$foodIconX(right, icon);
            int y = top + atomics_client$foodIconYOffset(player, icon);
            atomics_client$drawFoodOverlayIcon(context, emptyTexture, x, y, backgroundColor);
            boolean half = icon * 2 + 1 == modifiedFood;
            atomics_client$drawFoodOverlayIcon(context, half ? halfTexture : fullTexture, x, y, iconColor);
        }
    }

    private void atomics_client$drawSaturationOverlay(DrawContext context, float currentSaturation, float saturationRestored, PlayerEntity player, int right, int top, float alpha, Identifier fullTexture) {
        if (currentSaturation + saturationRestored < 0.0f) {
            return;
        }

        float modifiedSaturation = Math.max(0.0f, Math.min(20.0f, currentSaturation + saturationRestored));
        int startIcon = saturationRestored == 0.0f ? 0 : Math.max(0, (int) (currentSaturation / 2.0f));
        int endIcon = (int) Math.ceil(modifiedSaturation / 2.0f);
        int color = atomics_client$whiteWithAlpha(alpha);

        for (int icon = startIcon; icon < endIcon; icon++) {
            float iconFill = modifiedSaturation / 2.0f - icon;
            int width = atomics_client$saturationWidth(iconFill);
            if (width <= 0) {
                continue;
            }

            int x = atomics_client$foodIconX(right, icon);
            int y = top + atomics_client$foodIconYOffset(player, icon);
            if (FoodOverlayTextureCache.hasVanillaShape(fullTexture)) {
                atomics_client$drawAppleSkinSaturationIcon(context, x, y, width, color);
            } else {
                atomics_client$drawFoodOverlayIcon(context, fullTexture, x, y, width, color);
            }
        }
    }

    private static void atomics_client$drawAppleSkinSaturationIcon(DrawContext context, int x, int y, int width, int color) {
        int visibleWidth = Math.max(0, Math.min(9, width));
        if (visibleWidth <= 0) {
            return;
        }

        int slice = switch (visibleWidth) {
            case 9 -> 3;
            case 7 -> 2;
            case 5 -> 1;
            default -> 0;
        };
        context.drawTexture(RenderPipelines.GUI_TEXTURED, APPLESKIN_ICONS_TEXTURE, x, y, slice * 9.0f, 0.0f, 9, 9, 256, 256, color);
    }

    private static void atomics_client$drawFoodOverlayIcon(DrawContext context, Identifier vanillaSpriteId, int x, int y, int color) {
        atomics_client$drawFoodOverlayIcon(context, vanillaSpriteId, x, y, 9, color);
    }

    private static void atomics_client$drawFoodOverlayIcon(DrawContext context, Identifier vanillaSpriteId, int x, int y, int width, int color) {
        Identifier shiftedTexture = FoodOverlayTextureCache.get(vanillaSpriteId);
        if (shiftedTexture == null) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, vanillaSpriteId, x, y, 9, 9, color);
            return;
        }
        int visibleWidth = Math.max(0, Math.min(9, width));
        int xOffset = 9 - visibleWidth;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, shiftedTexture, x + xOffset, y, xOffset, 0.0f, visibleWidth, 9, 9, 9, color);
    }

    private int atomics_client$foodIconYOffset(PlayerEntity player, int icon) {
        int currentFood = player.getHungerManager().getFoodLevel();
        if (player.getHungerManager().getSaturationLevel() > 0.0f) {
            return 0;
        }

        int divisor = currentFood * 3 + 1;
        if (divisor <= 0 || getTicks() % divisor != 0) {
            return 0;
        }

        ATOMICS_CLIENT_RANDOM.setSeed(getTicks() * 312871L);
        for (int i = 0; i <= icon; i++) {
            int yOffset = ATOMICS_CLIENT_RANDOM.nextInt(3) - 1;
            if (i == icon) {
                return yOffset;
            }
        }
        return 0;
    }

    private static int atomics_client$foodIconX(int right, int icon) {
        return right - icon * 8 - 9;
    }

    private static int atomics_client$saturationWidth(float iconFill) {
        if (iconFill >= 1.0f) {
            return 9;
        }
        if (iconFill > 0.5f) {
            return 7;
        }
        if (iconFill > 0.25f) {
            return 5;
        }
        if (iconFill > 0.0f) {
            return 3;
        }
        return 0;
    }

    private static FoodComponent atomics_client$getHeldFood(PlayerEntity player) {
        FoodComponent mainHandFood = atomics_client$getFood(player.getMainHandStack());
        if (mainHandFood != null) {
            return mainHandFood;
        }
        return atomics_client$getFood(player.getOffHandStack());
    }

    private static FoodComponent atomics_client$getFood(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(DataComponentTypes.FOOD);
    }

    private float atomics_client$automaticAlpha(float maxAlpha) {
        int ticks = getTicks();
        if (ticks != atomics_client$lastFlashTick) {
            atomics_client$lastFlashTick = ticks;
            if (atomics_client$unclampedFlashAlpha >= 1.5f) {
                atomics_client$flashAlphaDirection = -1;
            } else if (atomics_client$unclampedFlashAlpha <= -0.5f) {
                atomics_client$flashAlphaDirection = 1;
            }
            atomics_client$unclampedFlashAlpha += atomics_client$flashAlphaDirection * 0.125f;
        }

        float alpha = Math.max(0.0f, Math.min(1.0f, atomics_client$unclampedFlashAlpha));
        return alpha * Math.max(0.0f, Math.min(1.0f, maxAlpha));
    }

    private static int atomics_client$whiteWithAlpha(float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (a << 24) | 0xFFFFFF;
    }
}
