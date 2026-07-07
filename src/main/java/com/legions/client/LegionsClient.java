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

public class LegionsClient implements ClientModInitializer {
    public static final String MOD_ID = "legions_client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static LegionsConfig CONFIG;

    private static final boolean ATOMICS_CLIENT_LOADED = FabricLoader.getInstance().isModLoaded("atomics_client");
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        CONFIG = LegionsConfig.load().normalize();

        if (!ATOMICS_CLIENT_LOADED) {
            KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
            openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.legions_client.open_config",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    category
            ));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey != null && openConfigKey.wasPressed()) {
                client.setScreen(new LegionsClientScreen(client.currentScreen));
            }
            LegionsFeatures.tick(client);
            LegionsPingController.tick(client);
            LegionsSpectateLock.tick(client);
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, parameters, timestamp) -> {
            if (!LegionsPingController.shouldCleanReceivedPingText(message)) {
                return true;
            }

            LegionsPingController.receiveChatPing(message, sender);
            if (MinecraftClient.getInstance().inGameHud != null) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(LegionsPingController.cleanReceivedPingText(message));
            }
            return false;
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, parameters, timestamp) ->
                LegionsPingController.receiveChatPing(message, sender)
        );
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (!LegionsPingController.shouldCleanReceivedPingText(message)) {
                return message;
            }

            LegionsPingController.receiveChatPing(message, null);
            return LegionsPingController.cleanReceivedPingText(message);
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
}
