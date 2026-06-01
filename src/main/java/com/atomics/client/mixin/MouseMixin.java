package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import com.atomics.client.FreelookManager;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void atomics_client$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ClientFeatureManager.onZoomScroll(vertical)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            )
    )
    private void atomics_client$applyFreelook(LocalPlayer player, double cursorDeltaX, double cursorDeltaY) {
        if (FreelookManager.isActive()) {
            FreelookManager.onMouseLook(cursorDeltaX, cursorDeltaY);
        } else {
            player.turn(cursorDeltaX, cursorDeltaY);
        }
    }
}
