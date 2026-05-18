package com.atomics.client.render;

public final class PlayerOverlayColorContext {
    private static int color = -1;

    private PlayerOverlayColorContext() {
    }

    public static void set(int color) {
        PlayerOverlayColorContext.color = color;
    }

    public static void clear() {
        color = -1;
    }

    public static int activeColor() {
        return color;
    }

    public static int apply(int originalColor) {
        int overlayColor = activeColor();
        if (overlayColor == -1) {
            return originalColor;
        }

        int baseColor = originalColor == -1 ? 0xFFFFFFFF : originalColor;
        int alpha = (overlayColor >>> 24) & 255;
        if (alpha <= 0) {
            return originalColor;
        }
        if (alpha >= 255) {
            return overlayColor;
        }

        int inverseAlpha = 255 - alpha;
        int baseAlpha = (baseColor >>> 24) & 255;
        int outAlpha = alpha + baseAlpha * inverseAlpha / 255;
        int r = (((overlayColor >>> 16) & 255) * alpha + ((baseColor >>> 16) & 255) * inverseAlpha) / 255;
        int g = (((overlayColor >>> 8) & 255) * alpha + ((baseColor >>> 8) & 255) * inverseAlpha) / 255;
        int b = ((overlayColor & 255) * alpha + (baseColor & 255) * inverseAlpha) / 255;
        return (outAlpha << 24) | (r << 16) | (g << 8) | b;
    }
}
