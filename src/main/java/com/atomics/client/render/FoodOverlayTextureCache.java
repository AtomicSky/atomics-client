package com.atomics.client.render;

import com.atomics.client.AtomicsClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class FoodOverlayTextureCache {
    private static final int SATURATION_GRADIENT_START = 0xFF9F8609;
    private static final int SATURATION_GRADIENT_END = 0xFFE9D262;
    private static final int SHAPE_ALPHA_CUTOFF = 24;
    private static final Map<Identifier, Identifier> TEXTURES = new HashMap<>();

    private FoodOverlayTextureCache() {
    }

    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            for (Identifier textureId : TEXTURES.values()) {
                client.getTextureManager().release(textureId);
            }
        }
        TEXTURES.clear();
    }

    public static Identifier get(Identifier vanillaSpriteId) {
        Identifier cached = TEXTURES.get(vanillaSpriteId);
        if (cached != null) {
            return cached;
        }

        Identifier textureId = Identifier.fromNamespaceAndPath(AtomicsClient.MOD_ID, "generated/food_overlay/" + vanillaSpriteId.getPath().replace('/', '_'));
        NativeImage image = loadYellowOutlineImage(vanillaSpriteId);
        if (image == null) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            image.close();
            return null;
        }

        client.getTextureManager().register(textureId, new DynamicTexture(() -> textureId.toString(), image));
        TEXTURES.put(vanillaSpriteId, textureId);
        return textureId;
    }

    private static NativeImage loadYellowOutlineImage(Identifier vanillaSpriteId) {
        Minecraft client = Minecraft.getInstance();
        ResourceManager resourceManager = client == null ? null : client.getResourceManager();
        if (resourceManager == null) {
            return null;
        }

        Identifier resourceId = Identifier.fromNamespaceAndPath(vanillaSpriteId.getNamespace(), "textures/gui/sprites/" + vanillaSpriteId.getPath() + ".png");
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return null;
        }

        try (InputStream stream = resource.get().open(); NativeImage source = NativeImage.read(stream)) {
            NativeImage shifted = new NativeImage(source.getWidth(), source.getHeight(), false);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    shifted.setPixel(x, y, yellowOutline(source, x, y));
                }
            }
            return shifted;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int yellowOutline(NativeImage image, int x, int y) {
        int argb = image.getPixel(x, y);
        int alpha = (argb >>> 24) & 255;
        if (alpha < SHAPE_ALPHA_CUTOFF || !isEdgePixel(image, x, y)) {
            return 0;
        }
        return (alpha << 24) | (gradientColor(image, x, y) & 0x00FFFFFF);
    }

    private static int gradientColor(NativeImage image, int x, int y) {
        float width = Math.max(1.0f, image.getWidth() - 1.0f);
        float height = Math.max(1.0f, image.getHeight() - 1.0f);
        float horizontal = x / width;
        float vertical = 1.0f - y / height;
        float amount = Math.max(0.0f, Math.min(1.0f, vertical * 0.82f + horizontal * 0.18f));
        return lerpColor(SATURATION_GRADIENT_START, SATURATION_GRADIENT_END, amount);
    }

    private static int lerpColor(int from, int to, float amount) {
        int r = lerpChannel((from >>> 16) & 255, (to >>> 16) & 255, amount);
        int g = lerpChannel((from >>> 8) & 255, (to >>> 8) & 255, amount);
        int b = lerpChannel(from & 255, to & 255, amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

    private static boolean isEdgePixel(NativeImage image, int x, int y) {
        return !isVisible(image, x - 1, y)
                || !isVisible(image, x + 1, y)
                || !isVisible(image, x, y - 1)
                || !isVisible(image, x, y + 1);
    }

    private static boolean isVisible(NativeImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return false;
        }
        return ((image.getPixel(x, y) >>> 24) & 255) >= SHAPE_ALPHA_CUTOFF;
    }
}
