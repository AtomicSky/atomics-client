package com.atomics.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPos")
    void atomics_client$setPos(Vec3d pos);

    @Invoker("setRotation")
    void atomics_client$setRotation(float yaw, float pitch);

    @Invoker("clipToSpace")
    float atomics_client$clipToSpace(float desiredCameraDistance);

    @Invoker("moveBy")
    void atomics_client$moveBy(float x, float y, float z);

    @Accessor("cameraY")
    float atomics_client$getCameraY();

    @Accessor("lastCameraY")
    float atomics_client$getLastCameraY();
}
