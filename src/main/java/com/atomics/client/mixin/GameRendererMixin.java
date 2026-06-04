package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class GameRendererMixin {
    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void atomics_client$applyZoom(float tickProgress, CallbackInfoReturnable<Float> cir) {
        float zoomMultiplier = ClientFeatureManager.getZoomFovMultiplier();
        if (zoomMultiplier < 0.999f) {
            cir.setReturnValue(cir.getReturnValue() * zoomMultiplier);
        }
    }
}
