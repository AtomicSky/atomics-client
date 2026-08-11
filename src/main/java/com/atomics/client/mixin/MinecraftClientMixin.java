package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("RETURN"), cancellable = true)
    private void atomics_client$hasFriendFoeOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && AtomicsClient.shouldRenderFriendFoeOutline(entity)) {
            cir.setReturnValue(true);
        }
    }
}
