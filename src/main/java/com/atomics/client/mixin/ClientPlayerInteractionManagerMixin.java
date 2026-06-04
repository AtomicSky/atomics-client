package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void atomics_client$trackAttackTarget(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity targetPlayer && AtomicsClient.shouldBlockFriendAttack(targetPlayer)) {
            AtomicsClient.notifyFriendAttackBlocked(targetPlayer);
            ci.cancel();
            return;
        }
        ClientFeatureManager.onReachAttack(player, target);
    }
}