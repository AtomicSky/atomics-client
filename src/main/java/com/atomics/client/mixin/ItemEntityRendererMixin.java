package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void atomics_client$scaleDroppedTotem(ItemEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (AtomicsClient.isDroppedTotemScaleEnabled() && entity.getItem().is(Items.TOTEM_OF_UNDYING)) {
            float scale = AtomicsClient.getDroppedTotemScale();
            matrices.scale(scale, scale, scale);
        }
    }
}
