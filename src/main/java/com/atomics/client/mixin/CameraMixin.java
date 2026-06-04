package com.atomics.client.mixin;

import com.atomics.client.DualSpectateCamera;
import com.atomics.client.FreelookManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow
    private boolean detached;

    @Inject(
            method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void atomics_client$applyCustomCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        float tickProgress = camera.getCameraEntityPartialTicks(deltaTracker);
        if (DualSpectateCamera.isActive()) {
            this.detached = false;
            DualSpectateCamera.applyToCamera(camera, Minecraft.getInstance(), tickProgress);
        } else if (FreelookManager.isActive()) {
            this.detached = true;
            FreelookManager.applyToCamera(camera, camera.entity(), tickProgress);
        }
    }
}
