package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.atomics.client.gui.AtomicsClientScreen;
import com.atomics.client.render.FoodOverlayTextureCache;
import com.atomics.client.render.PlayerOverlayColorContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class AtomicsClient implements ClientModInitializer {
    public static final String MOD_ID = "atomics_client";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TpsConfig CONFIG;
    private static KeyBinding.Category keyCategory;
    private static KeyBinding openStudioKey;
    private static KeyBinding zoomKey;
    private static KeyBinding freelookKey;
    private static KeyBinding toggleAutoGgKey;
    private static KeyBinding toggleDualSpectateKey;
    private static KeyBinding toggleFullBrightKey;
    private static KeyBinding toggleTimeChangerKey;
    private static KeyBinding toggleProjectileTrailKey;
    private static KeyBinding toggleStreamerModeKey;
    private static KeyBinding cycleFriendFoeKey;
    private static final List<KeyBinding> macroKeys = new ArrayList<>();
    private static String cachedReplacementItemId;
    private static Item cachedReplacementItem;
    private static long lastLocalShieldDisabledMillis;
    private static long lastFriendAttackBlockedMillis;
    private static boolean renderingLocalPlayerHeldItem;
    private static final Set<String> cachedFriendNames = new HashSet<>();
    private static final Set<String> cachedFoeNames = new HashSet<>();
    private static boolean friendFoeCacheInitialized;
    private static int friendFoeCacheFingerprint;
    private static boolean cachedFriendFoeOverlayEnabled;
    private static int cachedFriendFoeOverlayStyle = PlayerOverlayColorContext.STYLE_FULL;
    private static int cachedFriendOverlayColor = -1;
    private static int cachedFoeOverlayColor = -1;

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


            zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.atomics_client.zoom",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    keyCategory
            ));

            freelookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.atomics_client.freelook",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    keyCategory
            ));

            toggleAutoGgKey = registerUnboundKey("key.atomics_client.toggle_auto_gg");
            toggleDualSpectateKey = registerUnboundKey("key.atomics_client.toggle_dual_spectate");
            toggleFullBrightKey = registerUnboundKey("key.atomics_client.toggle_full_bright");
            toggleTimeChangerKey = registerUnboundKey("key.atomics_client.toggle_time_changer");
            toggleProjectileTrailKey = registerUnboundKey("key.atomics_client.toggle_projectile_trail");
            toggleStreamerModeKey = registerUnboundKey("key.atomics_client.toggle_streamer_mode");
            cycleFriendFoeKey = registerUnboundKey("key.atomics_client.cycle_friend_foe");

            registerMacroKeys(TpsConfig.MAX_MACRO_SLOTS);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to register keybindings", e);
        }

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(MOD_ID, "food_overlay_textures");
            }

            @Override
            public void reload(ResourceManager manager) {
                FoodOverlayTextureCache.clear();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openStudioKey != null && openStudioKey.wasPressed()) {
                client.setScreen(new AtomicsClientScreen(client.currentScreen));
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
            while (cycleFriendFoeKey != null && cycleFriendFoeKey.wasPressed()) {
                cycleLookedAtPlayerFriendFoe(client);
            }
            for (int i = 0; i < macroKeys.size(); i++) {
                KeyBinding macroKey = macroKeys.get(i);
                while (macroKey != null && macroKey.wasPressed()) {
                    ClientFeatureManager.runMacro(client, i);
                }
            }
            TotemPopEffects.tick(client);
            DualSpectateCamera.tick(client);
            FreelookManager.tick(client);
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

    public static boolean isFreelookEnabled() {
        return CONFIG != null
                && CONFIG.enabled
                && CONFIG.visual != null
                && CONFIG.visual.freelookEnabled;
    }

    public static boolean isFreelookKeyPressed() {
        return freelookKey != null && freelookKey.isPressed();
    }

    public static boolean isFreelookToggleMode() {
        return CONFIG != null
                && CONFIG.visual != null
                && CONFIG.visual.freelookToggleMode;
    }

    public static KeyBinding getOpenStudioKeyBinding() {
        return openStudioKey;
    }


    public static KeyBinding getZoomKeyBinding() {
        return zoomKey;
    }

    public static KeyBinding getFreelookKeyBinding() {
        return freelookKey;
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

    public static KeyBinding getCycleFriendFoeKeyBinding() {
        return cycleFriendFoeKey;
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

    private static void cycleLookedAtPlayerFriendFoe(MinecraftClient client) {
        PlayerEntity player = findLookedAtPlayer(client);
        if (player == null) {
            sendActionMessage(client, "Look at a player to mark them");
            return;
        }
        if (CONFIG == null) {
            CONFIG = new TpsConfig();
        }
        CONFIG.normalize();
        String name = player.getGameProfile() == null ? player.getName().getString() : player.getGameProfile().name();
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            sendActionMessage(client, "Could not read player name");
            return;
        }

        String lowerName = normalized.toLowerCase(Locale.ROOT);
        boolean friend = containsName(CONFIG.pvp.friendNames, lowerName);
        boolean foe = containsName(CONFIG.pvp.foeNames, lowerName);
        removeName(CONFIG.pvp.friendNames, lowerName);
        removeName(CONFIG.pvp.foeNames, lowerName);

        String state;
        if (!friend && !foe) {
            CONFIG.pvp.friendNames.add(normalized);
            state = "Friend";
        } else if (friend) {
            CONFIG.pvp.foeNames.add(normalized);
            state = "Foe";
        } else {
            state = "Neutral";
        }
        CONFIG.pvp.friendFoeOverlayEnabled = true;
        CONFIG.normalize();
        saveConfigQuietly();
        sendActionMessage(client, normalized + ": " + state);
    }

    private static PlayerEntity findLookedAtPlayer(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }

        if (client.crosshairTarget instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof PlayerEntity player
                && player != client.player) {
            return player;
        }
        if (client.targetedEntity instanceof PlayerEntity player && player != client.player) {
            return player;
        }

        Entity cameraEntity = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        Vec3d start = cameraEntity.getCameraPosVec(1.0f);
        Vec3d direction = cameraEntity.getRotationVec(1.0f);
        double range = 96.0;
        Vec3d end = start.add(direction.multiply(range));

        PlayerEntity best = null;
        double bestDistanceSq = range * range;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == client.player || !candidate.isAlive() || candidate.isSpectator()) {
                continue;
            }

            Box box = candidate.getBoundingBox().expand(Math.max(0.3, candidate.getTargetingMargin() + 0.25));
            Optional<Vec3d> hit = box.raycast(start, end);
            if (hit.isEmpty()) {
                continue;
            }

            double distanceSq = start.squaredDistanceTo(hit.get());
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private static void sendActionMessage(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(message), true);
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
        renderingLocalPlayerHeldItem = client != null && entity == client.player;
    }

    public static void clearRenderingLocalPlayerHeldItem() {
        renderingLocalPlayerHeldItem = false;
    }

    public static boolean isRenderingLocalPlayerHeldItem() {
        return renderingLocalPlayerHeldItem;
    }

    public static boolean isEmptyBucketOverlayTarget(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.BUCKET);
    }

    public static int getLiveEmptyBucketOverlayColor() {
        if (!isEmptyBucketOverlayEnabled()) {
            return -1;
        }

        TpsConfig.MiscSettings misc = CONFIG == null ? null : CONFIG.misc;
        return misc == null
                ? -1
                : colorWithAlpha(misc.emptyBucketOverlayR, misc.emptyBucketOverlayG, misc.emptyBucketOverlayB, misc.emptyBucketOverlayAlpha);
    }

    public static int getLiveShieldWarningOverlayColor() {
        if (!isRenderingLocalPlayerHeldItem() || !isShieldWarningOverlayEnabled() || !isShieldWarningActive()) {
            return -1;
        }

        TpsConfig.MiscSettings misc = CONFIG == null ? null : CONFIG.misc;
        return misc == null
                ? -1
                : colorWithAlpha(misc.shieldWarningOverlayR, misc.shieldWarningOverlayG, misc.shieldWarningOverlayB, misc.shieldWarningOverlayAlpha);
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

    public static int getPlayerFriendFoeOverlayColor(PlayerEntity player) {
        syncFriendFoeCache();
        if (player == null || !cachedFriendFoeOverlayEnabled) {
            return -1;
        }

        String name = getPlayerProfileName(player);
        String normalizedName = normalizeName(name);
        if (normalizedName.isEmpty()) {
            return -1;
        }

        if (cachedFriendNames.contains(normalizedName)) {
            return cachedFriendOverlayColor;
        }
        if (cachedFoeNames.contains(normalizedName)) {
            return cachedFoeOverlayColor;
        }
        return -1;
    }

    public static int getPlayerFriendFoeOverlayStyle(PlayerEntity player) {
        return getPlayerFriendFoeOverlayColor(player) == -1
                ? PlayerOverlayColorContext.STYLE_FULL
                : cachedFriendFoeOverlayStyle;
    }

    public static boolean usesFriendFoeOutline(int style) {
        return style == PlayerOverlayColorContext.STYLE_OUTLINE
                || style == PlayerOverlayColorContext.STYLE_OUTLINE_FULL;
    }

    public static boolean shouldBlockFriendAttack(PlayerEntity target) {
        syncFriendFoeCache();
        return target != null
                && cachedFriendFoeOverlayEnabled
                && isFriend(target);
    }

    public static void notifyFriendAttackBlocked(PlayerEntity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFriendAttackBlockedMillis < 750L) {
            return;
        }
        lastFriendAttackBlockedMillis = now;
        String name = getPlayerProfileName(target);
        client.player.sendMessage(net.minecraft.text.Text.literal("Blocked attack on friend" + (name == null || name.isBlank() ? "" : ": " + name)), true);
    }

    private static boolean isFriend(PlayerEntity player) {
        String name = getPlayerProfileName(player);
        String normalizedName = normalizeName(name);
        return !normalizedName.isEmpty() && cachedFriendNames.contains(normalizedName);
    }

    private static String getPlayerProfileName(PlayerEntity player) {
        if (player == null) {
            return null;
        }
        return player.getGameProfile() == null ? player.getName().getString() : player.getGameProfile().name();
    }

    private static boolean containsName(List<String> names, String normalizedName) {
        if (names == null || normalizedName == null) {
            return false;
        }
        for (String name : names) {
            if (name != null && normalizedName.equals(name.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static void removeName(List<String> names, String normalizedName) {
        if (names == null || normalizedName == null) {
            return;
        }
        names.removeIf(name -> name != null && normalizedName.equals(name.trim().toLowerCase(Locale.ROOT)));
    }

    private static void syncFriendFoeCache() {
        TpsConfig cfg = CONFIG;
        TpsConfig.PvpSettings pvp = cfg == null ? null : cfg.pvp;
        boolean enabled = cfg != null && cfg.enabled && pvp != null && pvp.friendFoeOverlayEnabled;
        int fingerprint = friendFoeFingerprint(pvp, enabled);
        if (friendFoeCacheInitialized && fingerprint == friendFoeCacheFingerprint) {
            return;
        }

        friendFoeCacheInitialized = true;
        friendFoeCacheFingerprint = fingerprint;
        cachedFriendFoeOverlayEnabled = enabled;
        cachedFriendFoeOverlayStyle = PlayerOverlayColorContext.STYLE_FULL;
        cachedFriendNames.clear();
        cachedFoeNames.clear();
        cachedFriendOverlayColor = -1;
        cachedFoeOverlayColor = -1;
        if (!enabled) {
            return;
        }

        addNormalizedNames(pvp.friendNames, cachedFriendNames);
        addNormalizedNames(pvp.foeNames, cachedFoeNames);
        cachedFriendFoeOverlayStyle = friendFoeStyleId(pvp.friendFoeOverlayStyle);
        cachedFriendOverlayColor = colorWithAlpha(pvp.friendOverlayR, pvp.friendOverlayG, pvp.friendOverlayB, pvp.friendOverlayAlpha);
        cachedFoeOverlayColor = colorWithAlpha(pvp.foeOverlayR, pvp.foeOverlayG, pvp.foeOverlayB, pvp.foeOverlayAlpha);
    }

    private static int friendFoeFingerprint(TpsConfig.PvpSettings pvp, boolean enabled) {
        int result = Boolean.hashCode(enabled);
        if (pvp == null) {
            return result;
        }
        result = 31 * result + pvp.friendOverlayR;
        result = 31 * result + pvp.friendOverlayG;
        result = 31 * result + pvp.friendOverlayB;
        result = 31 * result + Float.floatToIntBits(pvp.friendOverlayAlpha);
        result = 31 * result + (pvp.friendFoeOverlayStyle == null ? 0 : pvp.friendFoeOverlayStyle.hashCode());
        result = 31 * result + pvp.foeOverlayR;
        result = 31 * result + pvp.foeOverlayG;
        result = 31 * result + pvp.foeOverlayB;
        result = 31 * result + Float.floatToIntBits(pvp.foeOverlayAlpha);
        result = 31 * result + listFingerprint(pvp.friendNames);
        result = 31 * result + listFingerprint(pvp.foeNames);
        return result;
    }

    private static int friendFoeStyleId(String style) {
        return switch (TpsConfig.normalizeFriendFoeStyle(style)) {
            case TpsConfig.FRIEND_FOE_STYLE_OUTLINE -> PlayerOverlayColorContext.STYLE_OUTLINE;
            case TpsConfig.FRIEND_FOE_STYLE_OUTLINE_FULL -> PlayerOverlayColorContext.STYLE_OUTLINE_FULL;
            case TpsConfig.FRIEND_FOE_STYLE_PULSE -> PlayerOverlayColorContext.STYLE_PULSE;
            default -> PlayerOverlayColorContext.STYLE_FULL;
        };
    }

    private static int listFingerprint(List<String> names) {
        if (names == null) {
            return 0;
        }
        int result = names.size();
        for (String name : names) {
            result = 31 * result + (name == null ? 0 : name.hashCode());
        }
        return result;
    }

    private static void addNormalizedNames(List<String> names, Set<String> target) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            String normalizedName = normalizeName(name);
            if (!normalizedName.isEmpty()) {
                target.add(normalizedName);
            }
        }
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? "" : trimmed.toLowerCase(Locale.ROOT);
    }

    private static int colorWithAlpha(int r, int g, int b, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        int cr = Math.max(0, Math.min(255, r));
        int cg = Math.max(0, Math.min(255, g));
        int cb = Math.max(0, Math.min(255, b));
        return (a << 24) | (cr << 16) | (cg << 8) | cb;
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