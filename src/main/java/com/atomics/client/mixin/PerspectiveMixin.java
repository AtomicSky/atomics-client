package com.atomics.client.mixin;

import com.atomics.client.DualSpectateCamera;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CameraType.class)
public class PerspectiveMixin {
    @Inject(method = "isFirstPerson()Z", at = @At("HEAD"), cancellable = true)
    private void atomics_client$dualSpectateIsNotFirstPerson(CallbackInfoReturnable<Boolean> cir) {
        if (DualSpectateCamera.isActive()) {
            cir.setReturnValue(false);
        }
    }
}
