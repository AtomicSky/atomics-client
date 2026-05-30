package com.atomics.client.mixin;

import com.atomics.client.DualSpectateCamera;
import com.atomics.client.FreelookManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void atomics_client$disableVanillaThirdPersonOffset(
            World world,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickProgress,
            CallbackInfo ci
    ) {
        if (DualSpectateCamera.isActive()) {
            this.thirdPerson = false;
            DualSpectateCamera.applyToCamera((Camera) (Object) this, MinecraftClient.getInstance(), tickProgress);
        } else if (FreelookManager.isActive()) {
            this.thirdPerson = true;
            FreelookManager.applyToCamera((Camera) (Object) this, focusedEntity, tickProgress);
        }
    }
}
