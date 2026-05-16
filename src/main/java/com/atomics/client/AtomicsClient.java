package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.atomics.client.gui.AtomicsClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AtomicsClient implements ClientModInitializer {
    public static final String MOD_ID = "atomics_client";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TpsConfig CONFIG;
    private static KeyBinding.Category keyCategory;
    private static KeyBinding openStudioKey;
    private static KeyBinding resetTotemCounterKey;
    private static KeyBinding zoomKey;
    private static KeyBinding toggleAutoGgKey;
    private static KeyBinding toggleDualSpectateKey;
    private static KeyBinding toggleFullBrightKey;
    private static KeyBinding toggleTimeChangerKey;
    private static KeyBinding toggleProjectileTrailKey;
    private static KeyBinding toggleStreamerModeKey;
    private static final List<KeyBinding> macroKeys = new ArrayList<>();
    private static final int EMPTY_BUCKET_OVERLAY_COLOR = 0xFFFF2828;
    private static final int SHIELD_WARNING_OVERLAY_COLOR = 0xFFFF2323;
    private static String cachedReplacementItemId;
    private static Item cachedReplacementItem;
    private static long lastLocalShieldDisabledMillis;
    private static final ThreadLocal<Boolean> renderingLocalPlayerHeldItem = ThreadLocal.withInitial(() -> false);

    @Override
    public void onInitializeClient() {
        CONFIG = TpsConfig.load().normalize();

        try {
            keyCategory = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
            openStudioKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.atomics_client.open_gui",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    keyCategory
            ));

            resetTotemCounterKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.atomics_client.reset_totem_counter",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_F9,
                    keyCategory
            ));

            zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.atomics_client.zoom",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    keyCategory
            ));

            toggleAutoGgKey = registerUnboundKey("key.atomics_client.toggle_auto_gg");
            toggleDualSpectateKey = registerUnboundKey("key.atomics_client.toggle_dual_spectate");
            toggleFullBrightKey = registerUnboundKey("key.atomics_client.toggle_full_bright");
            toggleTimeChangerKey = registerUnboundKey("key.atomics_client.toggle_time_changer");
            toggleProjectileTrailKey = registerUnboundKey("key.atomics_client.toggle_projectile_trail");
            toggleStreamerModeKey = registerUnboundKey("key.atomics_client.toggle_streamer_mode");

            registerMacroKeys(TpsConfig.MAX_MACRO_SLOTS);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to register keybindings", e);
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openStudioKey != null && openStudioKey.wasPressed()) {
                client.setScreen(new AtomicsClientScreen(client.currentScreen));
            }
            while (resetTotemCounterKey != null && resetTotemCounterKey.wasPressed()) {
                PvpStatsManager.resetTotemCounters();
            }
            while (toggleAutoGgKey != null && toggleAutoGgKey.wasPressed()) {
                toggleAutoGg(client);
            }
            while (toggleDualSpectateKey != null && toggleDualSpectateKey.wasPressed()) {
                toggleDualSpectate(client);
            }
            while (toggleFullBrightKey != null && toggleFullBrightKey.wasPressed()) {
                toggleFullBright(client);
            }
            while (toggleTimeChangerKey != null && toggleTimeChangerKey.wasPressed()) {
                toggleTimeChanger(client);
            }
            while (toggleProjectileTrailKey != null && toggleProjectileTrailKey.wasPressed()) {
                toggleProjectileTrail(client);
            }
            while (toggleStreamerModeKey != null && toggleStreamerModeKey.wasPressed()) {
                toggleStreamerMode(client);
            }
            for (int i = 0; i < macroKeys.size(); i++) {
                KeyBinding macroKey = macroKeys.get(i);
                while (macroKey != null && macroKey.wasPressed()) {
                    ClientFeatureManager.runMacro(client, i);
                }
            }
            TotemPopEffects.tick(client);
            PvpStatsManager.tick(client);
            DualSpectateCamera.tick(client);
            ClientFeatureManager.tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> ClientFeatureManager.renderHud(context));
    }

    private static KeyBinding registerUnboundKey(String translationKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                keyCategory
        ));
    }

    private static void registerMacroKeys(int count) {
        if (keyCategory == null) return;
        while (macroKeys.size() < count) {
            int index = macroKeys.size();
            macroKeys.add(KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.atomics_client.macro_" + (index + 1),
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    keyCategory
            )));
        }
    }

    public static boolean isZoomKeyPressed() {
        return zoomKey != null && zoomKey.isPressed();
    }

    public static KeyBinding getOpenStudioKeyBinding() {
        return openStudioKey;
    }

    public static KeyBinding getResetTotemCounterKeyBinding() {
        return resetTotemCounterKey;
    }

    public static KeyBinding getZoomKeyBinding() {
        return zoomKey;
    }

    public static KeyBinding getToggleAutoGgKeyBinding() {
        return toggleAutoGgKey;
    }

    public static KeyBinding getToggleDualSpectateKeyBinding() {
        return toggleDualSpectateKey;
    }

    public static KeyBinding getToggleFullBrightKeyBinding() {
        return toggleFullBrightKey;
    }

    public static KeyBinding getToggleTimeChangerKeyBinding() {
        return toggleTimeChangerKey;
    }

    public static KeyBinding getToggleProjectileTrailKeyBinding() {
        return toggleProjectileTrailKey;
    }

    public static KeyBinding getToggleStreamerModeKeyBinding() {
        return toggleStreamerModeKey;
    }

    public static KeyBinding getMacroKeyBinding(int index) {
        return index >= 0 && index < macroKeys.size() ? macroKeys.get(index) : null;
    }

    public static int getRegisteredMacroKeyCount() {
        return macroKeys.size();
    }

    public static void setKeyBinding(KeyBinding keyBinding, InputUtil.Key key) {
        if (keyBinding == null || key == null) {
            return;
        }
        keyBinding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.write();
        }
    }

    public static String keyBindingName(KeyBinding keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) {
            return "Unbound";
        }
        return keyBinding.getBoundKeyLocalizedText().getString();
    }

    private static void toggleAutoGg(MinecraftClient client) {
        if (CONFIG == null) return;
        CONFIG.pvp.autoGgEnabled = !CONFIG.pvp.autoGgEnabled;
        sendToggleMessage(client, "Auto GG", CONFIG.pvp.autoGgEnabled);
    }

    private static void toggleDualSpectate(MinecraftClient client) {
        if (CONFIG == null) return;
        CONFIG.pvp.dualSpectateEnabled = !CONFIG.pvp.dualSpectateEnabled;
        sendToggleMessage(client, "Dual Spectate Camera", CONFIG.pvp.dualSpectateEnabled);
    }

    private static void toggleFullBright(MinecraftClient client) {
        if (CONFIG == null) return;
        CONFIG.visual.fullBrightEnabled = !CONFIG.visual.fullBrightEnabled;
        sendToggleMessage(client, "Full Bright", CONFIG.visual.fullBrightEnabled);
    }

    private static void toggleTimeChanger(MinecraftClient client) {
        if (CONFIG == null) return;
        CONFIG.visual.timeChangerEnabled = !CONFIG.visual.timeChangerEnabled;
        sendToggleMessage(client, "Time Changer", CONFIG.visual.timeChangerEnabled);
    }

    private static void toggleProjectileTrail(MinecraftClient client) {
        if (CONFIG == null) return;
        CONFIG.visual.projectileTrailEnabled = !CONFIG.visual.projectileTrailEnabled;
        sendToggleMessage(client, "Projectile Trail", CONFIG.visual.projectileTrailEnabled);
    }

    private static void toggleStreamerMode(MinecraftClient client) {
        if (CONFIG == null) return;
        CONFIG.visual.streamerModeEnabled = !CONFIG.visual.streamerModeEnabled;
        sendToggleMessage(client, "Streamer Mode", CONFIG.visual.streamerModeEnabled);
    }

    private static void sendToggleMessage(MinecraftClient client, String label, boolean enabled) {
        saveConfigQuietly();
        if (client != null && client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(label + ": " + (enabled ? "ON" : "OFF")), true);
        }
    }

    private static void saveConfigQuietly() {
        if (CONFIG == null) return;
        try {
            CONFIG.save(FabricLoader.getInstance().getConfigDir().resolve("atomics_client.json"));
        } catch (Exception e) {
            LOGGER.warn("Failed to save keybind toggle state", e);
        }
    }

    public static void onTotemPop(Entity entity) {
        if (CONFIG == null || !CONFIG.enabled || entity == null) return;
        PvpStatsManager.recordTotemPop(entity);
        TotemPopEffects.play(entity);
    }

    public static boolean shouldCustomizeTotemPop(Entity entity) {
        if (CONFIG == null || !CONFIG.enabled || entity == null) return false;
        return !CONFIG.utility.onlyForSelf || entity == net.minecraft.client.MinecraftClient.getInstance().player;
    }

    public static boolean isTotemPopItemEnabled() {
        return CONFIG != null && CONFIG.enabled && CONFIG.item.handScaleEnabled;
    }

    public static float getHandScale() {
        return CONFIG == null ? 1.0f : CONFIG.item.handScale;
    }

    public static boolean isDroppedTotemScaleEnabled() {
        return CONFIG != null && CONFIG.enabled && CONFIG.item.droppedScaleEnabled;
    }

    public static float getDroppedTotemScale() {
        return CONFIG == null ? 1.0f : CONFIG.item.droppedScale;
    }

    public static float getPopScale() {
        return CONFIG == null ? 1.0f : CONFIG.popOverlay.popScale;
    }

    public static boolean isRetextureEnabled() {
        return CONFIG != null && CONFIG.enabled && CONFIG.retexture.enabled;
    }

    public static ItemStack getVisualTotemStack(ItemStack originalStack) {
        if (originalStack == null || !originalStack.isOf(Items.TOTEM_OF_UNDYING) || !isRetextureEnabled()) {
            return originalStack;
        }

        Item replacement = getReplacementItem();
        if (replacement == Items.AIR) {
            return originalStack;
        }

        ItemStack replacementStack = replacement.getDefaultStack();
        if (replacementStack.isEmpty()) {
            replacementStack = new ItemStack(replacement);
        }
        replacementStack.setCount(originalStack.getCount());
        return replacementStack;
    }

    private static Item getReplacementItem() {
        if (CONFIG == null || CONFIG.retexture == null) {
            return Items.AIR;
        }

        String itemId = CONFIG.retexture.itemId;
        if (itemId == null || itemId.isBlank()) {
            return Items.AIR;
        }

        if (itemId.equals(cachedReplacementItemId) && cachedReplacementItem != null) {
            return cachedReplacementItem;
        }

        cachedReplacementItemId = itemId;
        Identifier id = Identifier.tryParse(itemId);
        cachedReplacementItem = id == null ? Items.AIR : Registries.ITEM.get(id);
        return cachedReplacementItem;
    }


    public static boolean isEmptyBucketOverlayEnabled() {
        return CONFIG != null && CONFIG.enabled && CONFIG.misc != null && CONFIG.misc.emptyBucketOverlayEnabled;
    }

    public static boolean isShieldWarningOverlayEnabled() {
        return CONFIG != null && CONFIG.enabled && CONFIG.misc != null && CONFIG.misc.shieldWarningOverlayEnabled;
    }

    public static ItemStack getVisualItemStack(ItemStack originalStack) {
        return getVisualTotemStack(originalStack);
    }

    public static ItemStack getVisualHeldStack(LivingEntity entity, ItemStack originalStack) {
        return getVisualItemStack(originalStack);
    }

    public static void setRenderingLocalPlayerHeldItem(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        renderingLocalPlayerHeldItem.set(client != null && entity == client.player);
    }

    public static void clearRenderingLocalPlayerHeldItem() {
        renderingLocalPlayerHeldItem.remove();
    }

    public static boolean isRenderingLocalPlayerHeldItem() {
        return renderingLocalPlayerHeldItem.get();
    }

    public static boolean isEmptyBucketOverlayTarget(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.BUCKET);
    }

    public static int getLiveEmptyBucketOverlayColor() {
        if (!isEmptyBucketOverlayEnabled()) {
            return -1;
        }

        // Bucket overlay is a real item color tint, not enchant glint.
        // Full alpha means texture transparency is still preserved by the item texture itself.
        return EMPTY_BUCKET_OVERLAY_COLOR;
    }

    public static int getLiveShieldWarningOverlayColor() {
        if (!isRenderingLocalPlayerHeldItem() || !isShieldWarningOverlayEnabled() || !isShieldWarningActive()) {
            return -1;
        }

        return SHIELD_WARNING_OVERLAY_COLOR;
    }

    public static void recordLocalShieldDisabled() {
        lastLocalShieldDisabledMillis = System.currentTimeMillis();
    }

    private static boolean isShieldWarningActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastLocalShieldDisabledMillis < 5000L) {
            return true;
        }

        ItemStack offHandStack = client.player.getOffHandStack();
        ItemStack mainHandStack = client.player.getMainHandStack();
        ItemStack shieldStack = offHandStack.isOf(Items.SHIELD) ? offHandStack : mainHandStack;
        if (!shieldStack.isOf(Items.SHIELD)) {
            shieldStack = Items.SHIELD.getDefaultStack();
        }

        if (client.player.getItemCooldownManager().isCoolingDown(shieldStack)) {
            return true;
        }

        if (!client.player.isUsingItem() || !client.player.getActiveItem().isOf(Items.SHIELD)) {
            return false;
        }

        int delayTicks = getShieldBlockDelayTicks(client.player.getActiveItem());
        return delayTicks > 0 && client.player.getItemUseTime() < delayTicks;
    }

    private static int getShieldBlockDelayTicks(ItemStack stack) {
        BlocksAttacksComponent blocksAttacks = stack == null ? null : stack.get(DataComponentTypes.BLOCKS_ATTACKS);
        return blocksAttacks == null ? 5 : Math.max(0, blocksAttacks.getBlockDelayTicks());
    }

    public static int getItemColorOverlay(ItemStack stack) {
        if (stack == null || stack.isEmpty() || CONFIG == null || !CONFIG.enabled) {
            return -1;
        }

        if (isEmptyBucketOverlayTarget(stack)) {
            return getLiveEmptyBucketOverlayColor();
        }

        return -1;
    }

    public static boolean isTotemHueShiftCandidate(ItemStack stack) {
        if (stack == null || stack.isEmpty() || CONFIG == null || !CONFIG.enabled) {
            return false;
        }
        if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return true;
        }
        return stack.isOf(getReplacementItem());
    }

    public static boolean isTotemHueShiftTarget(ItemStack stack) {
        return isTotemHueShiftCandidate(stack) && CONFIG.retexture.colorOverlayEnabled;
    }

    public static float getLiveTotemHueShift() {
        if (CONFIG == null || !CONFIG.enabled || CONFIG.retexture == null || !CONFIG.retexture.colorOverlayEnabled) {
            return 0.0f;
        }
        return CONFIG.retexture.overlayHue;
    }

    public static float getTotemHueShift(ItemStack stack) {
        return isTotemHueShiftCandidate(stack) ? getLiveTotemHueShift() : 0.0f;
    }

    public static int applyHueShiftToArgb(int argb, float hueShiftDegrees) {
        if (hueShiftDegrees == 0.0f) {
            return argb;
        }

        int alpha = (argb >>> 24) & 255;
        if (alpha == 0) {
            return argb;
        }

        int r = (argb >>> 16) & 255;
        int g = (argb >>> 8) & 255;
        int b = argb & 255;

        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        hsb[0] = wrapHue(hsb[0] + hueShiftDegrees / 360.0f);
        int shifted = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return (alpha << 24) | (shifted & 0x00FFFFFF);
    }

    private static float wrapHue(float hue) {
        hue %= 1.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }
        return hue;
    }

    public static ItemStack getPreviewTotemStack() {
        return getVisualTotemStack(Items.TOTEM_OF_UNDYING.getDefaultStack());
    }
}
