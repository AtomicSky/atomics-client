package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void atomics_client$applyZoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float zoomMultiplier = ClientFeatureManager.getZoomFovMultiplier();
        if (zoomMultiplier < 0.999f) {
            cir.setReturnValue(cir.getReturnValue() * zoomMultiplier);
        }
    }
}
