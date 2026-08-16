package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.ClientFeatureManager;
import com.atomics.client.VillagerProfessionLeveler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "interactEntity", at = @At("HEAD"))
    private void atomics_client$trackInteractedVillager(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (entity instanceof VillagerEntity) {
            VillagerProfessionLeveler.rememberInteractedVillager(entity);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"))
    private void atomics_client$trackInteractedVillagerAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (entity instanceof VillagerEntity) {
            VillagerProfessionLeveler.rememberInteractedVillager(entity);
        }
    }
}
