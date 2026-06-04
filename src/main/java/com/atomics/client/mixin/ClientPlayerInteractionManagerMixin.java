package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void atomics_client$trackAttackTarget(Player player, Entity target, CallbackInfo ci) {
        if (target instanceof Player targetPlayer && AtomicsClient.shouldBlockFriendAttack(targetPlayer)) {
            AtomicsClient.notifyFriendAttackBlocked(targetPlayer);
            ci.cancel();
            return;
        }
        ClientFeatureManager.onReachAttack(player, target);
    }
}
