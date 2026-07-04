package com.atomics.client.render;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LegionsWarningParticle extends BillboardParticle {
    private static final int EDGE_SEGMENTS = 7;
    private static final float HALF_WIDTH = 0.48f;
    private static final float TOP_Y = 0.54f;
    private static final float BOTTOM_Y = -0.36f;
    private static final float CELL_SIZE = 0.055f;

    public LegionsWarningParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Sprite sprite, int maxAge) {
        super(world, x, y, z, velocityX, velocityY, velocityZ, sprite);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.velocityMultiplier = 0.88f;
        this.gravityStrength = 0.0f;
        this.collidesWithWorld = false;
        this.scale = 1.0f;
        this.maxAge = Math.max(24, Math.min(80, maxAge));
        setBoundingBoxSpacing(0.75f, 0.75f);
    }

    @Override
    public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
        Quaternionf rotation = new Quaternionf();
        getRotator().setRotation(rotation, camera, tickProgress);

        Vec3d cameraPos = camera.getCameraPos();
        float centerX = (float) (MathHelper.lerp((double) tickProgress, this.lastX, this.x) - cameraPos.getX());
        float centerY = (float) (MathHelper.lerp((double) tickProgress, this.lastY, this.y) - cameraPos.getY());
        float centerZ = (float) (MathHelper.lerp((double) tickProgress, this.lastZ, this.z) - cameraPos.getZ());

        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(rotation);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(rotation);

        float life = ((float) this.age + tickProgress) / (float) this.maxAge;
        float fade = Math.max(0.0f, Math.min(1.0f, Math.min(life * 5.0f, (1.0f - life) * 3.0f)));
        if (fade <= 0.0f) {
            return;
        }
        float pulse = 0.9f + MathHelper.sin(((float) this.age + tickProgress) * 0.35f) * 0.08f;
        float size = CELL_SIZE * fade * pulse;
        float radius = fade * pulse;
        int light = getBrightness(tickProgress);
        int outlineColor = ColorHelper.fromFloats(0.92f * fade, 1.0f, 0.88f, 0.08f);
        int markColor = ColorHelper.fromFloats(0.98f * fade, 1.0f, 0.28f, 0.05f);

        drawLine(submittable, rotation, right, up, centerX, centerY, centerZ, 0.0f, TOP_Y, -HALF_WIDTH, BOTTOM_Y, radius, size, outlineColor, light);
        drawLine(submittable, rotation, right, up, centerX, centerY, centerZ, -HALF_WIDTH, BOTTOM_Y, HALF_WIDTH, BOTTOM_Y, radius, size, outlineColor, light);
        drawLine(submittable, rotation, right, up, centerX, centerY, centerZ, HALF_WIDTH, BOTTOM_Y, 0.0f, TOP_Y, radius, size, outlineColor, light);

        drawMarker(submittable, rotation, right, up, centerX, centerY, centerZ, 0.0f, 0.15f, radius, size * 1.05f, markColor, light);
        drawMarker(submittable, rotation, right, up, centerX, centerY, centerZ, 0.0f, 0.02f, radius, size * 1.05f, markColor, light);
        drawMarker(submittable, rotation, right, up, centerX, centerY, centerZ, 0.0f, -0.11f, radius, size * 1.05f, markColor, light);
        drawMarker(submittable, rotation, right, up, centerX, centerY, centerZ, 0.0f, -0.27f, radius, size * 1.2f, markColor, light);
    }

    private void drawLine(
            BillboardParticleSubmittable submittable,
            Quaternionf rotation,
            Vector3f right,
            Vector3f up,
            float centerX,
            float centerY,
            float centerZ,
            float startX,
            float startY,
            float endX,
            float endY,
            float radius,
            float size,
            int color,
            int light
    ) {
        for (int i = 0; i <= EDGE_SEGMENTS; i++) {
            float progress = i / (float) EDGE_SEGMENTS;
            drawMarker(
                    submittable,
                    rotation,
                    right,
                    up,
                    centerX,
                    centerY,
                    centerZ,
                    MathHelper.lerp(progress, startX, endX),
                    MathHelper.lerp(progress, startY, endY),
                    radius,
                    size,
                    color,
                    light
            );
        }
    }

    private void drawMarker(
            BillboardParticleSubmittable submittable,
            Quaternionf rotation,
            Vector3f right,
            Vector3f up,
            float centerX,
            float centerY,
            float centerZ,
            float localX,
            float localY,
            float radius,
            float size,
            int color,
            int light
    ) {
        float offsetX = localX * radius;
        float offsetY = localY * radius;
        float x = centerX + right.x() * offsetX + up.x() * offsetY;
        float y = centerY + right.y() * offsetX + up.y() * offsetY;
        float z = centerZ + right.z() * offsetX + up.z() * offsetY;
        submittable.render(
                getRenderType(),
                x,
                y,
                z,
                rotation.x,
                rotation.y,
                rotation.z,
                rotation.w,
                size,
                getMinU(),
                getMaxU(),
                getMinV(),
                getMaxV(),
                color,
                light
        );
    }

    @Override
    protected RenderType getRenderType() {
        return RenderType.PARTICLE_ATLAS_TRANSLUCENT;
    }
}
