package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void atomics_client$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ClientFeatureManager.onZoomScroll(vertical)) {
            ci.cancel();
        }
    }
}
