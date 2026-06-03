package com.atomics.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void atomics_client$setPos(Vec3 pos);

    @Invoker("setRotation")
    void atomics_client$setRotation(float yaw, float pitch);

    @Invoker("getMaxZoom")
    float atomics_client$clipToSpace(float desiredCameraDistance);

    @Invoker("move")
    void atomics_client$moveBy(float x, float y, float z);

    @Accessor("eyeHeight")
    float atomics_client$getCameraY();

    @Accessor("eyeHeightOld")
    float atomics_client$getLastCameraY();
}
