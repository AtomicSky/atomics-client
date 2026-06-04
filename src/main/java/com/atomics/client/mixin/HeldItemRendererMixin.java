package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
    @Unique
    private static LivingEntity atomics_client$currentEntity;

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD")
    )
    private void atomics_client$captureHeldEntity(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, PoseStack matrices, SubmitNodeCollector queue, int light, CallbackInfo ci) {
        atomics_client$currentEntity = entity;
        AtomicsClient.setRenderingLocalPlayerHeldItem(entity);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("RETURN")
    )
    private void atomics_client$clearHeldEntity(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, PoseStack matrices, SubmitNodeCollector queue, int light, CallbackInfo ci) {
        atomics_client$currentEntity = null;
        AtomicsClient.clearRenderingLocalPlayerHeldItem();
    }

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"
            ),
            index = 1
    )
    private ItemStack atomics_client$retextureHeldTotem(ItemStack stack) {
        return AtomicsClient.getVisualHeldStack(atomics_client$currentEntity, stack);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
    )
    private void atomics_client$scaleTotemInHand(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, PoseStack matrices, SubmitNodeCollector queue, int light, CallbackInfo ci) {
        if (AtomicsClient.CONFIG != null && AtomicsClient.CONFIG.enabled && AtomicsClient.CONFIG.misc != null && stack.is(Items.SHIELD)) {
            boolean raised = entity != null && entity.isUsingItem() && entity.getUseItem().is(Items.SHIELD);
            boolean enabled = raised ? AtomicsClient.CONFIG.misc.shieldUpEnabled : AtomicsClient.CONFIG.misc.shieldDownEnabled;
            if (enabled) {
                float x = raised ? AtomicsClient.CONFIG.misc.shieldUpX : AtomicsClient.CONFIG.misc.shieldDownX;
                float y = raised ? AtomicsClient.CONFIG.misc.shieldUpY : AtomicsClient.CONFIG.misc.shieldDownY;
                float z = raised ? AtomicsClient.CONFIG.misc.shieldUpZ : AtomicsClient.CONFIG.misc.shieldDownZ;
                float rotX = raised ? AtomicsClient.CONFIG.misc.shieldUpRotX : AtomicsClient.CONFIG.misc.shieldDownRotX;
                float rotY = raised ? AtomicsClient.CONFIG.misc.shieldUpRotY : AtomicsClient.CONFIG.misc.shieldDownRotY;
                float rotZ = raised ? AtomicsClient.CONFIG.misc.shieldUpRotZ : AtomicsClient.CONFIG.misc.shieldDownRotZ;

                matrices.translate(x, y, z);
                matrices.mulPose(Axis.XP.rotationDegrees(rotX));
                matrices.mulPose(Axis.YP.rotationDegrees(rotY));
                matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
            }
        }

        if (!AtomicsClient.isTotemPopItemEnabled()) return;
        if (!stack.is(Items.TOTEM_OF_UNDYING)) return;
        float scale = AtomicsClient.getHandScale();
        matrices.scale(scale, scale, scale);
    }
}
