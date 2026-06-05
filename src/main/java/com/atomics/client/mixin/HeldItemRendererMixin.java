package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Unique
    private static LivingEntity atomics_client$currentEntity;

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void atomics_client$captureHeldEntity(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        atomics_client$currentEntity = entity;
        AtomicsClient.setRenderingLocalPlayerHeldItem(entity);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void atomics_client$clearHeldEntity(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        atomics_client$currentEntity = null;
        AtomicsClient.clearRenderingLocalPlayerHeldItem();
    }

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"
            ),
            index = 1
    )
    private ItemStack atomics_client$retextureHeldTotem(ItemStack stack) {
        return AtomicsClient.getVisualHeldStack(atomics_client$currentEntity, stack);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"
            )
    )
    private void atomics_client$scaleTotemInHand(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (AtomicsClient.CONFIG != null && AtomicsClient.CONFIG.enabled && AtomicsClient.CONFIG.misc != null && stack.isOf(Items.SHIELD)) {
            boolean raised = entity != null && entity.isUsingItem() && entity.getActiveItem().isOf(Items.SHIELD);
            boolean enabled = raised ? AtomicsClient.CONFIG.misc.shieldUpEnabled : AtomicsClient.CONFIG.misc.shieldDownEnabled;
            if (enabled) {
                float x = raised ? AtomicsClient.CONFIG.misc.shieldUpX : AtomicsClient.CONFIG.misc.shieldDownX;
                float y = raised ? AtomicsClient.CONFIG.misc.shieldUpY : AtomicsClient.CONFIG.misc.shieldDownY;
                float z = raised ? AtomicsClient.CONFIG.misc.shieldUpZ : AtomicsClient.CONFIG.misc.shieldDownZ;
                float rotX = raised ? AtomicsClient.CONFIG.misc.shieldUpRotX : AtomicsClient.CONFIG.misc.shieldDownRotX;
                float rotY = raised ? AtomicsClient.CONFIG.misc.shieldUpRotY : AtomicsClient.CONFIG.misc.shieldDownRotY;
                float rotZ = raised ? AtomicsClient.CONFIG.misc.shieldUpRotZ : AtomicsClient.CONFIG.misc.shieldDownRotZ;

                matrices.translate(x, y, z);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
            }
        }

        if (!AtomicsClient.isTotemPopItemEnabled()) return;
        if (!stack.isOf(Items.TOTEM_OF_UNDYING)) return;
        float scale = AtomicsClient.getHandScale();
        matrices.scale(scale, scale, scale);
    }
}
