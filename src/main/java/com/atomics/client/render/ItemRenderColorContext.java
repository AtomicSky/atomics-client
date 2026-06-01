package com.atomics.client.render;

public final class ItemRenderColorContext {
    public static final int NO_COLOR_TINT = -1;
    public static final float NO_HUE_SHIFT = 0.0f;

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
        return colorTint != NO_COLOR_TINT || hueShift != NO_HUE_SHIFT;
    }

    public static int tintColor() {
        if (colorTint != NO_COLOR_TINT) {
            return colorTint;
        }

        if (hueShift == NO_HUE_SHIFT) {
            return 0xFFFFFFFF;
        }

        float hue = ((hueShift % 360.0f) + 360.0f) % 360.0f / 360.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public static float applyToChannel(float original, int shift) {
        if (!active()) {
            return original;
        }

        int tint = tintColor();
        int alpha = (tint >>> 24) & 255;
        int channel = (tint >>> shift) & 255;
        float multiplier = (channel * alpha + 255 * (255 - alpha)) / (255.0f * 255.0f);
        return original * multiplier;
    }
}
