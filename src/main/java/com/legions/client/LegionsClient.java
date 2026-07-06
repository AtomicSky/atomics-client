package com.legions.client;

import com.legions.client.config.LegionsConfig;
import com.legions.client.gui.LegionsClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

public class LegionsClient implements ClientModInitializer {
    public static final String MOD_ID = "legions_client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static LegionsConfig CONFIG;

    private static final boolean ATOMICS_CLIENT_LOADED = FabricLoader.getInstance().isModLoaded("atomics_client");
    private static KeyBinding openConfigKey;
    private static KeyBinding pingTargetKey;
    private static KeyBinding lockDualSpectateKey;

    @Override
    public void onInitializeClient() {
        CONFIG = LegionsConfig.load().normalize();

        KeyBinding.Category category = ATOMICS_CLIENT_LOADED
                ? existingCategoryOrMisc(Identifier.of("atomics_client", "main"))
                : KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        if (!ATOMICS_CLIENT_LOADED) {
            openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.legions_client.open_config",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    category
            ));
        }
        pingTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.legions_client.ping_target",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
        ));
        if (ATOMICS_CLIENT_LOADED) {
            lockDualSpectateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.legions_client.lock_dual_spectate",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    category
            ));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey != null && openConfigKey.wasPressed()) {
                client.setScreen(new LegionsClientScreen(client.currentScreen));
            }
            while (pingTargetKey != null && pingTargetKey.wasPressed()) {
                LegionsPingManager.handlePingKeyPress(client);
            }
            while (lockDualSpectateKey != null && lockDualSpectateKey.wasPressed()) {
                LegionsSpectateLock.handleKeyPress(client);
            }
            LegionsFeatures.tick(client);
            LegionsPingManager.tick(client);
            LegionsSpectateLock.tick(client);
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, parameters, timestamp) -> {
            if (!LegionsPingManager.shouldCleanReceivedPingText(message)) {
                return true;
            }

            LegionsPingManager.receiveChatPing(message, sender);
            if (MinecraftClient.getInstance().inGameHud != null) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(LegionsPingManager.cleanReceivedPingText(message));
            }
            return false;
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, parameters, timestamp) ->
                LegionsPingManager.receiveChatPing(message, sender)
        );
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (!LegionsPingManager.shouldCleanReceivedPingText(message)) {
                return message;
            }

            LegionsPingManager.receiveChatPing(message, null);
            return LegionsPingManager.cleanReceivedPingText(message);
        });
        HudRenderCallback.EVENT.register((context, tickCounter) -> LegionsHud.renderHud(context));
    }

    public static void saveConfig() {
        CONFIG.normalize().save(FabricLoader.getInstance().getConfigDir().resolve("legions_client.json"));
    }

    public static boolean enabled(MinecraftClient client) {
        return CONFIG != null && CONFIG.enabled && LegionsFeatures.isLegionsServer(client);
    }

    public static boolean isAtomicsClientLoaded() {
        return ATOMICS_CLIENT_LOADED;
    }

    private static KeyBinding.Category existingCategoryOrMisc(Identifier id) {
        KeyBinding.Category category = findRegisteredCategory(id);
        if (category != null) {
            return category;
        }
        LOGGER.warn("Atomics Client keybind category {} was not registered yet; using Minecraft's Misc category for Legions keys.", id);
        return KeyBinding.Category.MISC;
    }

    private static KeyBinding.Category findRegisteredCategory(Identifier id) {
        for (Field field : KeyBinding.Category.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            KeyBinding.Category category = findRegisteredCategory(id, field);
            if (category != null) {
                return category;
            }
        }
        return null;
    }

    private static KeyBinding.Category findRegisteredCategory(Identifier id, Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            if (!(value instanceof List<?> categories)) {
                return null;
            }
            for (Object item : categories) {
                if (item instanceof KeyBinding.Category category && id.equals(category.id())) {
                    return category;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.debug("Skipped keybind category registry field {} while looking up {}.", field.getName(), id, e);
        }
        return null;
    }
}
