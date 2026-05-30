package com.atomics.client.render;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FoodOverlayTextureCache {
    private static final int SATURATION_YELLOW = 0xFFFFD800;
    private static final int SHAPE_ALPHA_CUTOFF = 24;
    private static final Map<Identifier, Identifier> TEXTURES = new HashMap<>();
    private static final Map<Identifier, Boolean> VANILLA_SHAPES = new HashMap<>();

    private FoodOverlayTextureCache() {
    }

    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            for (Identifier textureId : TEXTURES.values()) {
                client.getTextureManager().destroyTexture(textureId);
            }
        }
        TEXTURES.clear();
        VANILLA_SHAPES.clear();
    }

    public static Identifier get(Identifier vanillaSpriteId) {
        Identifier cached = TEXTURES.get(vanillaSpriteId);
        if (cached != null) {
            return cached;
        }

        Identifier textureId = Identifier.of(AtomicsClient.MOD_ID, "generated/food_overlay/" + vanillaSpriteId.getPath().replace('/', '_'));
        NativeImage image = loadYellowImage(vanillaSpriteId);
        if (image == null) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            image.close();
            return null;
        }

        client.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(() -> textureId.toString(), image));
        TEXTURES.put(vanillaSpriteId, textureId);
        return textureId;
    }

    public static boolean hasVanillaShape(Identifier vanillaSpriteId) {
        Boolean cached = VANILLA_SHAPES.get(vanillaSpriteId);
        if (cached != null) {
            return cached;
        }

        boolean vanillaShape = loadVanillaShapeMatch(vanillaSpriteId);
        VANILLA_SHAPES.put(vanillaSpriteId, vanillaShape);
        return vanillaShape;
    }

    private static NativeImage loadYellowImage(Identifier vanillaSpriteId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ResourceManager resourceManager = client == null ? null : client.getResourceManager();
        if (resourceManager == null) {
            return null;
        }

        Identifier resourceId = Identifier.of(vanillaSpriteId.getNamespace(), "textures/gui/sprites/" + vanillaSpriteId.getPath() + ".png");
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return null;
        }

        try (InputStream stream = resource.get().getInputStream(); NativeImage source = NativeImage.read(stream)) {
            NativeImage shifted = new NativeImage(source.getWidth(), source.getHeight(), false);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    shifted.setColorArgb(x, y, yellowMask(source.getColorArgb(x, y)));
                }
            }
            return shifted;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean loadVanillaShapeMatch(Identifier vanillaSpriteId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ResourceManager resourceManager = client == null ? null : client.getResourceManager();
        if (resourceManager == null) {
            return false;
        }

        Identifier resourceId = Identifier.of(vanillaSpriteId.getNamespace(), "textures/gui/sprites/" + vanillaSpriteId.getPath() + ".png");
        List<Resource> resources = resourceManager.getAllResources(resourceId);
        if (resources.isEmpty()) {
            return false;
        }
        if (resources.size() == 1) {
            return true;
        }

        try (NativeImage first = readImage(resources.get(0)); NativeImage last = readImage(resources.get(resources.size() - 1))) {
            return sameAlphaShape(first, last);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static NativeImage readImage(Resource resource) throws Exception {
        try (InputStream stream = resource.getInputStream()) {
            return NativeImage.read(stream);
        }
    }

    private static boolean sameAlphaShape(NativeImage first, NativeImage second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return false;
        }

        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                boolean firstVisible = ((first.getColorArgb(x, y) >>> 24) & 255) >= SHAPE_ALPHA_CUTOFF;
                boolean secondVisible = ((second.getColorArgb(x, y) >>> 24) & 255) >= SHAPE_ALPHA_CUTOFF;
                if (firstVisible != secondVisible) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int yellowMask(int argb) {
        int alpha = (argb >>> 24) & 255;
        if (alpha == 0) {
            return argb;
        }
        return (alpha << 24) | (SATURATION_YELLOW & 0x00FFFFFF);
    }
}
