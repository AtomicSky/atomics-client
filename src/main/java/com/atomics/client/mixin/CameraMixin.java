package com.atomics.client.mixin;

import com.atomics.client.DualSpectateCamera;
import com.atomics.client.FreelookManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow
    private boolean detached;

    @Inject(method = "setup", at = @At("TAIL"))
    private void atomics_client$disableVanillaThirdPersonOffset(
            Level world,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickProgress,
            CallbackInfo ci
    ) {
        if (DualSpectateCamera.isActive()) {
            this.detached = false;
            DualSpectateCamera.applyToCamera((Camera) (Object) this, Minecraft.getInstance(), tickProgress);
        } else if (FreelookManager.isActive()) {
            this.detached = true;
            FreelookManager.applyToCamera((Camera) (Object) this, focusedEntity, tickProgress);
        }
    }
}
