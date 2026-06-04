package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.atomics.client.config.ConfigPaths;
import com.atomics.client.gui.AtomicsClientScreen;
import com.atomics.client.render.FoodOverlayTextureCache;
import com.atomics.client.render.PlayerOverlayColorContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Mod(AtomicsClient.MOD_ID)
public class AtomicsClient {
    public static final String MOD_ID = "atomics_client";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TpsConfig CONFIG;
    private static KeyMapping.Category keyCategory;
    private static KeyMapping openStudioKey;
    private static KeyMapping zoomKey;
    private static KeyMapping freelookKey;
    private static KeyMapping toggleAutoGgKey;
    private static KeyMapping toggleDualSpectateKey;
    private static KeyMapping toggleFullBrightKey;
    private static KeyMapping toggleTimeChangerKey;
    private static KeyMapping toggleProjectileTrailKey;
    private static KeyMapping toggleStreamerModeKey;
    private static KeyMapping cycleFriendFoeKey;
    private static final List<KeyMapping> macroKeys = new ArrayList<>();
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

    public AtomicsClient(FMLJavaModLoadingContext context) {
        CONFIG = TpsConfig.load().normalize();
        RegisterKeyMappingsEvent.BUS.addListener(AtomicsClient::registerKeyBindings);
        RegisterClientReloadListenersEvent.BUS.addListener(AtomicsClient::registerReloadListeners);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> tick(Minecraft.getInstance()));
    }

    private static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        try {
            keyCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
            openStudioKey = registerKey(event, new KeyMapping(
                    "key.atomics_client.open_gui",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    keyCategory
            ));

            zoomKey = registerKey(event, new KeyMapping(
                    "key.atomics_client.zoom",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    keyCategory
            ));

            freelookKey = registerKey(event, new KeyMapping(
                    "key.atomics_client.freelook",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    keyCategory
            ));

            toggleAutoGgKey = registerUnboundKey(event, "key.atomics_client.toggle_auto_gg");
            toggleDualSpectateKey = registerUnboundKey(event, "key.atomics_client.toggle_dual_spectate");
            toggleFullBrightKey = registerUnboundKey(event, "key.atomics_client.toggle_full_bright");
            toggleTimeChangerKey = registerUnboundKey(event, "key.atomics_client.toggle_time_changer");
            toggleProjectileTrailKey = registerUnboundKey(event, "key.atomics_client.toggle_projectile_trail");
            toggleStreamerModeKey = registerUnboundKey(event, "key.atomics_client.toggle_streamer_mode");
            cycleFriendFoeKey = registerUnboundKey(event, "key.atomics_client.cycle_friend_foe");

            registerMacroKeys(event, TpsConfig.MAX_MACRO_SLOTS);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to register keybindings", e);
        }
    }

    private static KeyMapping registerKey(RegisterKeyMappingsEvent event, KeyMapping keyBinding) {
        event.register(keyBinding);
        return keyBinding;
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> FoodOverlayTextureCache.clear());
    }

    private static void tick(Minecraft client) {
        while (openStudioKey != null && openStudioKey.consumeClick()) {
            client.setScreen(new AtomicsClientScreen(client.screen));
        }
        while (toggleAutoGgKey != null && toggleAutoGgKey.consumeClick()) {
            toggleAutoGg(client);
        }
        while (toggleDualSpectateKey != null && toggleDualSpectateKey.consumeClick()) {
            toggleDualSpectate(client);
        }
        while (toggleFullBrightKey != null && toggleFullBrightKey.consumeClick()) {
            toggleFullBright(client);
        }
        while (toggleTimeChangerKey != null && toggleTimeChangerKey.consumeClick()) {
            toggleTimeChanger(client);
        }
        while (toggleProjectileTrailKey != null && toggleProjectileTrailKey.consumeClick()) {
            toggleProjectileTrail(client);
        }
        while (toggleStreamerModeKey != null && toggleStreamerModeKey.consumeClick()) {
            toggleStreamerMode(client);
        }
        while (cycleFriendFoeKey != null && cycleFriendFoeKey.consumeClick()) {
            cycleLookedAtPlayerFriendFoe(client);
        }
        for (int i = 0; i < macroKeys.size(); i++) {
            KeyMapping macroKey = macroKeys.get(i);
            while (macroKey != null && macroKey.consumeClick()) {
                ClientFeatureManager.runMacro(client, i);
            }
        }
        TotemPopEffects.tick(client);
        PvpNametagStatsManager.tick(client);
        DualSpectateCamera.tick(client);
        FreelookManager.tick(client);
        ClientFeatureManager.tick(client);
    }

    private static KeyMapping registerUnboundKey(RegisterKeyMappingsEvent event, String translationKey) {
        return registerKey(event, new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                keyCategory
        ));
    }

    private static void registerMacroKeys(RegisterKeyMappingsEvent event, int count) {
        if (keyCategory == null) return;
        while (macroKeys.size() < count) {
            int index = macroKeys.size();
            macroKeys.add(registerKey(event, new KeyMapping(
                    "key.atomics_client.macro_" + (index + 1),
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    keyCategory
            )));
        }
    }

    public static boolean isZoomKeyPressed() {
        return zoomKey != null && zoomKey.isDown();
    }

    public static boolean isFreelookEnabled() {
        return CONFIG != null
                && CONFIG.enabled
                && CONFIG.visual != null
                && CONFIG.visual.freelookEnabled;
    }

    public static boolean isFreelookKeyPressed() {
        return freelookKey != null && freelookKey.isDown();
    }

    public static boolean isFreelookToggleMode() {
        return CONFIG != null
                && CONFIG.visual != null
                && CONFIG.visual.freelookToggleMode;
    }

    public static KeyMapping getOpenStudioKeyBinding() {
        return openStudioKey;
    }

    public static KeyMapping getZoomKeyBinding() {
        return zoomKey;
    }

    public static KeyMapping getFreelookKeyBinding() {
        return freelookKey;
    }

    public static KeyMapping getToggleAutoGgKeyBinding() {
        return toggleAutoGgKey;
    }

    public static KeyMapping getToggleDualSpectateKeyBinding() {
        return toggleDualSpectateKey;
    }

    public static KeyMapping getToggleFullBrightKeyBinding() {
        return toggleFullBrightKey;
    }

    public static KeyMapping getToggleTimeChangerKeyBinding() {
        return toggleTimeChangerKey;
    }

    public static KeyMapping getToggleProjectileTrailKeyBinding() {
        return toggleProjectileTrailKey;
    }

    public static KeyMapping getToggleStreamerModeKeyBinding() {
        return toggleStreamerModeKey;
    }

    public static KeyMapping getCycleFriendFoeKeyBinding() {
        return cycleFriendFoeKey;
    }

    public static KeyMapping getMacroKeyBinding(int index) {
        return index >= 0 && index < macroKeys.size() ? macroKeys.get(index) : null;
    }

    public static int getRegisteredMacroKeyCount() {
        return macroKeys.size();
    }

    public static void setKeyBinding(KeyMapping keyBinding, InputConstants.Key key) {
        if (keyBinding == null || key == null) {
            return;
        }
        keyBinding.setKey(key);
        KeyMapping.resetMapping();
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null) {
            client.options.save();
        }
    }

    public static String keyBindingName(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) {
            return "Unbound";
        }
        return keyBinding.getTranslatedKeyMessage().getString();
    }

    private static void toggleAutoGg(Minecraft client) {
        if (CONFIG == null) return;
        CONFIG.pvp.autoGgEnabled = !CONFIG.pvp.autoGgEnabled;
        sendToggleMessage(client, "Auto GG", CONFIG.pvp.autoGgEnabled);
    }

    private static void toggleDualSpectate(Minecraft client) {
        if (CONFIG == null) return;
        CONFIG.pvp.dualSpectateEnabled = !CONFIG.pvp.dualSpectateEnabled;
        sendToggleMessage(client, "Dual Spectate Camera", CONFIG.pvp.dualSpectateEnabled);
    }

    private static void toggleFullBright(Minecraft client) {
        if (CONFIG == null) return;
        CONFIG.visual.fullBrightEnabled = !CONFIG.visual.fullBrightEnabled;
        sendToggleMessage(client, "Full Bright", CONFIG.visual.fullBrightEnabled);
    }

    private static void toggleTimeChanger(Minecraft client) {
        if (CONFIG == null) return;
        CONFIG.visual.timeChangerEnabled = !CONFIG.visual.timeChangerEnabled;
        sendToggleMessage(client, "Time Changer", CONFIG.visual.timeChangerEnabled);
    }

    private static void toggleProjectileTrail(Minecraft client) {
        if (CONFIG == null) return;
        CONFIG.visual.projectileTrailEnabled = !CONFIG.visual.projectileTrailEnabled;
        sendToggleMessage(client, "Projectile Trail", CONFIG.visual.projectileTrailEnabled);
    }

    private static void toggleStreamerMode(Minecraft client) {
        if (CONFIG == null) return;
        CONFIG.visual.streamerModeEnabled = !CONFIG.visual.streamerModeEnabled;
        sendToggleMessage(client, "Streamer Mode", CONFIG.visual.streamerModeEnabled);
    }

    private static void sendToggleMessage(Minecraft client, String label, boolean enabled) {
        saveConfigQuietly();
        if (client != null && client.player != null) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(label + ": " + (enabled ? "ON" : "OFF")), true);
        }
    }

    private static void cycleLookedAtPlayerFriendFoe(Minecraft client) {
        Player player = findLookedAtPlayer(client);
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

    private static Player findLookedAtPlayer(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return null;
        }

        if (client.hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof Player player
                && player != client.player) {
            return player;
        }
        if (client.crosshairPickEntity instanceof Player player && player != client.player) {
            return player;
        }

        Entity cameraEntity = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        Vec3 start = cameraEntity.getEyePosition(1.0f);
        Vec3 direction = cameraEntity.getViewVector(1.0f);
        double range = 96.0;
        Vec3 end = start.add(direction.scale(range));

        Player best = null;
        double bestDistanceSq = range * range;
        for (Player candidate : client.level.players()) {
            if (candidate == client.player || !candidate.isAlive() || candidate.isSpectator()) {
                continue;
            }

            AABB box = candidate.getBoundingBox().inflate(Math.max(0.3, candidate.getPickRadius() + 0.25));
            Optional<Vec3> hit = box.clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }

            double distanceSq = start.distanceToSqr(hit.get());
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private static void sendActionMessage(Minecraft client, String message) {
        if (client != null && client.player != null) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
        }
    }

    private static void saveConfigQuietly() {
        if (CONFIG == null) return;
        try {
            CONFIG.save(ConfigPaths.atomicsClient());
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
        return !CONFIG.utility.onlyForSelf || entity == net.minecraft.client.Minecraft.getInstance().player;
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
        if (originalStack == null || !originalStack.is(Items.TOTEM_OF_UNDYING) || !isRetextureEnabled()) {
            return originalStack;
        }

        Item replacement = getReplacementItem();
        if (replacement == Items.AIR) {
            return originalStack;
        }

        ItemStack replacementStack = replacement.getDefaultInstance();
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
        cachedReplacementItem = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
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
        Minecraft client = Minecraft.getInstance();
        renderingLocalPlayerHeldItem = client != null && entity == client.player;
    }

    public static void clearRenderingLocalPlayerHeldItem() {
        renderingLocalPlayerHeldItem = false;
    }

    public static boolean isRenderingLocalPlayerHeldItem() {
        return renderingLocalPlayerHeldItem;
    }

    public static boolean isEmptyBucketOverlayTarget(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.BUCKET);
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastLocalShieldDisabledMillis < 5000L) {
            return true;
        }

        ItemStack offHandStack = client.player.getOffhandItem();
        ItemStack mainHandStack = client.player.getMainHandItem();
        ItemStack shieldStack = offHandStack.is(Items.SHIELD) ? offHandStack : mainHandStack;
        if (!shieldStack.is(Items.SHIELD)) {
            shieldStack = Items.SHIELD.getDefaultInstance();
        }

        if (client.player.getCooldowns().isOnCooldown(shieldStack)) {
            return true;
        }

        if (!client.player.isUsingItem() || !client.player.getUseItem().is(Items.SHIELD)) {
            return false;
        }

        int delayTicks = getShieldBlockDelayTicks(client.player.getUseItem());
        return delayTicks > 0 && client.player.getTicksUsingItem() < delayTicks;
    }

    private static int getShieldBlockDelayTicks(ItemStack stack) {
        BlocksAttacks blocksAttacks = stack == null ? null : stack.get(DataComponents.BLOCKS_ATTACKS);
        return blocksAttacks == null ? 5 : Math.max(0, blocksAttacks.blockDelayTicks());
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

    public static int getPlayerFriendFoeOverlayColor(Player player) {
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

    public static int getPlayerFriendFoeOverlayStyle(Player player) {
        return getPlayerFriendFoeOverlayColor(player) == -1
                ? PlayerOverlayColorContext.STYLE_FULL
                : cachedFriendFoeOverlayStyle;
    }

    public static boolean usesFriendFoeOutline(int style) {
        return style == PlayerOverlayColorContext.STYLE_OUTLINE
                || style == PlayerOverlayColorContext.STYLE_OUTLINE_FULL;
    }

    public static boolean shouldBlockFriendAttack(Player target) {
        syncFriendFoeCache();
        return target != null
                && cachedFriendFoeOverlayEnabled
                && isFriend(target);
    }

    public static void notifyFriendAttackBlocked(Player target) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFriendAttackBlockedMillis < 750L) {
            return;
        }
        lastFriendAttackBlockedMillis = now;
        String name = getPlayerProfileName(target);
        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Blocked attack on friend" + (name == null || name.isBlank() ? "" : ": " + name)), true);
    }

    private static boolean isFriend(Player player) {
        String name = getPlayerProfileName(player);
        String normalizedName = normalizeName(name);
        return !normalizedName.isEmpty() && cachedFriendNames.contains(normalizedName);
    }

    private static String getPlayerProfileName(Player player) {
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
        if (stack.is(Items.TOTEM_OF_UNDYING)) {
            return true;
        }
        return stack.is(getReplacementItem());
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
        return getVisualTotemStack(Items.TOTEM_OF_UNDYING.getDefaultInstance());
    }
}