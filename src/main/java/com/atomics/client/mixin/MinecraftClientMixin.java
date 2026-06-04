package com.atomics.client.mixin;

import com.atomics.client.PvpStatsManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "startAttack()Z", at = @At("HEAD"))
    private void atomics_client$trackAttackSwing(CallbackInfoReturnable<Boolean> cir) {
        PvpStatsManager.recordAttackSwing();
    }
}
