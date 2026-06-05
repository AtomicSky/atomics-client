package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.config.TpsConfig;
import com.atomics.client.render.FoodOverlayTextureCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private static final Identifier HEART_CONTAINER_TEXTURE = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_CONTAINER_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/container_blinking");
    private static final Identifier HEART_HARDCORE_CONTAINER_TEXTURE = Identifier.ofVanilla("hud/heart/container_hardcore");
    private static final Identifier HEART_HARDCORE_CONTAINER_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/container_hardcore_blinking");
    private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
    private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");
    private static final float APPLESKIN_MAX_FLASH_ALPHA = 0.65f;
    private static final int HEART_NORMAL = 0;
    private static final int HEART_POISONED = 1;
    private static final int HEART_WITHERED = 2;
    private static final int HEART_ABSORBING = 3;
    private static final int HEART_FROZEN = 4;
    private static final Random ATOMICS_CLIENT_RANDOM = new Random();
    private static float atomics_client$unclampedFlashAlpha;
    private static byte atomics_client$flashAlphaDirection = 1;
    private static int atomics_client$lastFlashTick = Integer.MIN_VALUE;

    @Shadow
    public abstract int getTicks();

    @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
    private void atomics_client$renderPreciseHealthBar(DrawContext context, PlayerEntity player, int x, int y, int rowHeight, int regeneratingHeartIndex, float maxHealth, int currentHealth, int renderHealth, int absorption, boolean blinking, CallbackInfo ci) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (cfg == null || !cfg.enabled || cfg.visual == null || !cfg.visual.partialStatusIconsEnabled || player == null) {
            return;
        }

        ci.cancel();
        boolean hardcore = player.getEntityWorld().getLevelProperties().isHardcore();
        int heartVariant = atomics_client$heartVariant(player);
        int healthHearts = (int) Math.ceil(maxHealth / 2.0f);
        int absorptionHearts = (int) Math.ceil(absorption / 2.0f);
        int totalHearts = healthHearts + absorptionHearts;
        int healthAndAbsorption = (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount());

        ATOMICS_CLIENT_RANDOM.setSeed(getTicks() * 312871L);
        for (int heart = totalHearts - 1; heart >= 0; heart--) {
            int row = heart / 10;
            int column = heart % 10;
            int heartX = x + column * 8;
            int heartY = y - row * rowHeight;
            if (healthAndAbsorption <= 4) {
                heartY += ATOMICS_CLIENT_RANDOM.nextInt(2);
            }
            if (heart < healthHearts && heart == regeneratingHeartIndex) {
                heartY -= 2;
            }

            atomics_client$drawHeartWhole(context, atomics_client$heartContainerTexture(hardcore, blinking), heartX, heartY);

            if (heart >= healthHearts) {
                float absorptionFill = (player.getAbsorptionAmount() - (heart - healthHearts) * 2.0f) / 2.0f;
                int absorptionVariant = heartVariant == HEART_WITHERED ? HEART_WITHERED : HEART_ABSORBING;
                atomics_client$drawHeartPartial(context, atomics_client$heartTexture(absorptionVariant, hardcore, false), heartX, heartY, absorptionFill);
                continue;
            }

            if (blinking) {
                float blinkingFill = (renderHealth - heart * 2.0f) / 2.0f;
                atomics_client$drawHeartPartial(context, atomics_client$heartTexture(heartVariant, hardcore, true), heartX, heartY, blinkingFill);
            }

            float healthFill = (player.getHealth() - heart * 2.0f) / 2.0f;
            atomics_client$drawHeartPartial(context, atomics_client$heartTexture(heartVariant, hardcore, false), heartX, heartY, healthFill);
        }
    }

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
        atomics_client$drawSaturationOverlay(context, currentSaturation, 0.0f, player, right, top, 1.0f, emptyTexture);

        HeldFood heldFood = atomics_client$getHeldFood(player);
        FoodComponent foodComponent = heldFood == null ? null : heldFood.foodComponent();
        if (foodComponent == null || (foodComponent.nutrition() <= 0 && foodComponent.saturation() <= 0.0f)) {
            return;
        }
        if (currentFood >= 20 && !atomics_client$isGoldenApple(heldFood.stack())) {
            return;
        }

        float maxFlashAlpha = APPLESKIN_MAX_FLASH_ALPHA;
        float flashAlpha = atomics_client$automaticAlpha(maxFlashAlpha);
        if (currentFood < 20 && foodComponent.nutrition() > 0) {
            atomics_client$drawHungerOverlay(context, foodComponent.nutrition(), currentFood, player, right, top, flashAlpha, emptyTexture, halfTexture, fullTexture);
        }

        int previewFood = Math.min(20, currentFood + Math.max(0, foodComponent.nutrition()));
        float previewSaturation = currentSaturation + foodComponent.saturation();
        float saturationIncrement = previewSaturation > previewFood ? previewFood - currentSaturation : foodComponent.saturation();
        if (saturationIncrement > 0.0f && atomics_client$hasNaturalRegeneration(player)) {
            atomics_client$drawSaturationOverlay(context, currentSaturation, saturationIncrement, player, right, top, flashAlpha, emptyTexture);
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
            atomics_client$drawFoodBarIcon(context, emptyTexture, x, y, backgroundColor);
            boolean half = icon * 2 + 1 == modifiedFood;
            atomics_client$drawFoodBarIcon(context, half ? halfTexture : fullTexture, x, y, iconColor);
        }
    }

    private void atomics_client$drawSaturationOverlay(DrawContext context, float currentSaturation, float saturationRestored, PlayerEntity player, int right, int top, float alpha, Identifier outlineTexture) {
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
            atomics_client$drawFoodOverlayIcon(context, outlineTexture, x, y, width, color);
        }
    }

    private static int atomics_client$heartVariant(PlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON)) {
            return HEART_POISONED;
        }
        if (player.hasStatusEffect(StatusEffects.WITHER)) {
            return HEART_WITHERED;
        }
        if (player.isFrozen()) {
            return HEART_FROZEN;
        }
        return HEART_NORMAL;
    }

    private static Identifier atomics_client$heartContainerTexture(boolean hardcore, boolean blinking) {
        if (hardcore) {
            return blinking ? HEART_HARDCORE_CONTAINER_BLINKING_TEXTURE : HEART_HARDCORE_CONTAINER_TEXTURE;
        }
        return blinking ? HEART_CONTAINER_BLINKING_TEXTURE : HEART_CONTAINER_TEXTURE;
    }

    private static Identifier atomics_client$heartTexture(int variant, boolean hardcore, boolean blinking) {
        String variantPrefix = switch (variant) {
            case HEART_POISONED -> "poisoned_";
            case HEART_WITHERED -> "withered_";
            case HEART_ABSORBING -> "absorbing_";
            case HEART_FROZEN -> "frozen_";
            default -> "";
        };
        String hardcorePart = hardcore ? "hardcore_" : "";
        String blinkingPart = blinking ? "_blinking" : "";
        return Identifier.ofVanilla("hud/heart/" + variantPrefix + hardcorePart + "full" + blinkingPart);
    }

    private static void atomics_client$drawHeartWhole(DrawContext context, Identifier texture, int x, int y) {
        context.drawGuiTexture(RenderLayer::getGuiTextured, texture, x, y, 9, 9);
    }

    private static void atomics_client$drawHeartPartial(DrawContext context, Identifier texture, int x, int y, float fill) {
        if (fill <= 0.0f) {
            return;
        }

        int width = Math.max(1, Math.min(9, (int) Math.ceil(Math.min(1.0f, fill) * 9.0f)));
        if (width >= 9) {
            atomics_client$drawHeartWhole(context, texture, x, y);
            return;
        }

        context.drawGuiTexture(RenderLayer::getGuiTextured, texture, 9, 9, 0, 0, x, y, width, 9);
    }

    private static void atomics_client$drawFoodOverlayIcon(DrawContext context, Identifier vanillaSpriteId, int x, int y, int color) {
        atomics_client$drawFoodOverlayIcon(context, vanillaSpriteId, x, y, 9, color);
    }

    private static void atomics_client$drawFoodBarIcon(DrawContext context, Identifier vanillaSpriteId, int x, int y, int color) {
        context.drawGuiTexture(RenderLayer::getGuiTextured, vanillaSpriteId, x, y, 9, 9, color);
    }

    private static void atomics_client$drawFoodOverlayIcon(DrawContext context, Identifier vanillaSpriteId, int x, int y, int width, int color) {
        Identifier shiftedTexture = FoodOverlayTextureCache.get(vanillaSpriteId);
        if (shiftedTexture == null) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, vanillaSpriteId, x, y, 9, 9, color);
            return;
        }
        int visibleWidth = Math.max(0, Math.min(9, width));
        int xOffset = 9 - visibleWidth;
        context.drawTexture(RenderLayer::getGuiTextured, shiftedTexture, x + xOffset, y, xOffset, 0.0f, visibleWidth, 9, 9, 9, color);
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

    private static HeldFood atomics_client$getHeldFood(PlayerEntity player) {
        HeldFood mainHandFood = atomics_client$getFood(player.getMainHandStack());
        if (mainHandFood != null) {
            return mainHandFood;
        }
        return atomics_client$getFood(player.getOffHandStack());
    }

    private static HeldFood atomics_client$getFood(ItemStack stack) {
        FoodComponent foodComponent = stack == null || stack.isEmpty() ? null : stack.get(DataComponentTypes.FOOD);
        return foodComponent == null ? null : new HeldFood(stack, foodComponent);
    }

    private static boolean atomics_client$isGoldenApple(ItemStack stack) {
        return stack != null && (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE));
    }

    private static boolean atomics_client$hasNaturalRegeneration(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getServer() != null) {
            ServerWorld serverWorld = client.getServer().getWorld(player.getEntityWorld().getRegistryKey());
            if (serverWorld != null) {
                return serverWorld.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
            }
        }
        return true;
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

    private record HeldFood(ItemStack stack, FoodComponent foodComponent) {
    }
}
