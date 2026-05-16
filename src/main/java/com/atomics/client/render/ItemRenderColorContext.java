package com.atomics.client.render;

import com.atomics.client.AtomicsClient;

import java.util.HashMap;
import java.util.Map;

public final class ItemRenderColorContext {
    public static final int NO_COLOR_TINT = -1;
    public static final int DYNAMIC_EMPTY_BUCKET_COLOR = -2;
    public static final float NO_HUE_SHIFT = 0.0f;
    public static final float DYNAMIC_TOTEM_HUE_SHIFT = Float.NaN;

    private static final ThreadLocal<Integer> COLOR_TINT = ThreadLocal.withInitial(() -> NO_COLOR_TINT);
    private static final ThreadLocal<Float> HUE_SHIFT = ThreadLocal.withInitial(() -> NO_HUE_SHIFT);
    private static final Map<Integer, int[]> TINT_ARRAY_CACHE = new HashMap<>();

    private ItemRenderColorContext() {}

    public static void set(int colorTint, float hueShift) {
        COLOR_TINT.set(colorTint);
        HUE_SHIFT.set(hueShift);
    }

    public static void clear() {
        COLOR_TINT.set(NO_COLOR_TINT);
        HUE_SHIFT.set(NO_HUE_SHIFT);
    }

    public static boolean active() {
        int colorTint = COLOR_TINT.get();
        if (colorTint == DYNAMIC_EMPTY_BUCKET_COLOR) {
            return AtomicsClient.getLiveEmptyBucketOverlayColor() != NO_COLOR_TINT;
        }
        if (colorTint != NO_COLOR_TINT) {
            return true;
        }

        float hueShift = HUE_SHIFT.get();
        if (Float.isNaN(hueShift)) {
            return AtomicsClient.getLiveTotemHueShift() != NO_HUE_SHIFT;
        }
        return hueShift != NO_HUE_SHIFT;
    }

    public static int tintColor() {
        int colorTint = COLOR_TINT.get();
        if (colorTint == DYNAMIC_EMPTY_BUCKET_COLOR) {
            int liveBucketColor = AtomicsClient.getLiveEmptyBucketOverlayColor();
            return liveBucketColor == NO_COLOR_TINT ? 0xFFFFFFFF : liveBucketColor;
        }
        if (colorTint != NO_COLOR_TINT) {
            return colorTint;
        }

        float hueShift = HUE_SHIFT.get();
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
        return TINT_ARRAY_CACHE.computeIfAbsent(color, cachedColor -> new int[] { cachedColor });
    }
}
