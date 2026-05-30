package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import com.atomics.client.FreelookManager;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void atomics_client$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ClientFeatureManager.onZoomScroll(vertical)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void atomics_client$applyFreelook(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
        if (FreelookManager.isActive()) {
            FreelookManager.onMouseLook(cursorDeltaX, cursorDeltaY);
        } else {
            player.changeLookDirection(cursorDeltaX, cursorDeltaY);
        }
    }
}
