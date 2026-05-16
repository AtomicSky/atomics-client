package com.atomics.client.mixin;

import com.atomics.client.PvpStatsManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void atomics_client$trackAttackSwing(CallbackInfoReturnable<Boolean> cir) {
        PvpStatsManager.recordAttackSwing();
    }
}
