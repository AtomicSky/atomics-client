package com.atomics.client.render;

import com.atomics.client.AtomicsClient;

import java.util.HashMap;
import java.util.Map;

public final class ItemRenderColorContext {
    public static final int NO_COLOR_TINT = -1;
    public static final int DYNAMIC_EMPTY_BUCKET_COLOR = -2;
    public static final float NO_HUE_SHIFT = 0.0f;
    public static final float DYNAMIC_TOTEM_HUE_SHIFT = Float.NaN;

    private static final Map<Integer, int[]> TINT_ARRAY_CACHE = new HashMap<>();
    private static int colorTint = NO_COLOR_TINT;
    private static float hueShift = NO_HUE_SHIFT;

    private ItemRenderColorContext() {}

    public static void set(int colorTint, float hueShift) {
        ItemRenderColorContext.colorTint = colorTint;
        ItemRenderColorContext.hueShift = hueShift;
    }

    public static void clear() {
        colorTint = NO_COLOR_TINT;
        hueShift = NO_HUE_SHIFT;
    }

    public static boolean active() {
        if (colorTint == DYNAMIC_EMPTY_BUCKET_COLOR) {
            return AtomicsClient.getLiveEmptyBucketOverlayColor() != NO_COLOR_TINT;
        }
        if (colorTint != NO_COLOR_TINT) {
            return true;
        }

        if (Float.isNaN(hueShift)) {
            return AtomicsClient.getLiveTotemHueShift() != NO_HUE_SHIFT;
        }
        return hueShift != NO_HUE_SHIFT;
    }

    public static int tintColor() {
        if (colorTint == DYNAMIC_EMPTY_BUCKET_COLOR) {
            int liveBucketColor = AtomicsClient.getLiveEmptyBucketOverlayColor();
            return liveBucketColor == NO_COLOR_TINT ? 0xFFFFFFFF : liveBucketColor;
        }
        if (colorTint != NO_COLOR_TINT) {
            return colorTint;
        }

        float hueShift = ItemRenderColorContext.hueShift;
        if (Float.isNaN(hueShift)) {
            hueShift = AtomicsClient.getLiveTotemHueShift();
        }
        if (hueShift == NO_HUE_SHIFT) {
            return 0xFFFFFFFF;
        }

        // Dynamic mode: the ItemRenderState can be cached by Minecraft, so the
        // current config value is read here at render time instead of being baked
        // into the render state when the item was first seen.
        float hue = ((hueShift % 360.0f) + 360.0f) % 360.0f / 360.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public static int[] tintArray() {
        int color = tintColor();
        int[] cached = TINT_ARRAY_CACHE.get(color);
        if (cached != null) {
            return cached;
        }

        int[] created = new int[] { color };
        TINT_ARRAY_CACHE.put(color, created);
        return created;
    }
}
