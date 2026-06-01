package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.render.ItemRenderColorContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @ModifyVariable(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack atomics_client$retextureItem(ItemStack stack) {
        return AtomicsClient.getVisualItemStack(stack);
    }

    @ModifyVariable(method = "renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack atomics_client$retextureItemWithoutEntity(ItemStack stack) {
        return AtomicsClient.getVisualItemStack(stack);
    }

    @Inject(
            method = "renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD")
    )
    private void atomics_client$prepareItemTint(
            PoseStack matrices,
            VertexConsumer vertices,
            List<BakedQuad> quads,
            ItemStack stack,
            int light,
            int overlay,
            CallbackInfo ci
    ) {
        ItemRenderColorContext.set(AtomicsClient.getItemColorOverlay(stack), AtomicsClient.getTotemHueShift(stack));
    }

    @Redirect(
            method = "renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFII)V"
            )
    )
    private void atomics_client$applyItemTint(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BakedQuad quad,
            float red,
            float green,
            float blue,
            int light,
            int overlay
    ) {
        vertices.putBulkData(
                pose,
                quad,
                ItemRenderColorContext.applyToChannel(red, 16),
                ItemRenderColorContext.applyToChannel(green, 8),
                ItemRenderColorContext.applyToChannel(blue, 0),
                light,
                overlay
        );
    }

    @Inject(
            method = "renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("RETURN")
    )
    private void atomics_client$clearItemTint(
            PoseStack matrices,
            VertexConsumer vertices,
            List<BakedQuad> quads,
            ItemStack stack,
            int light,
            int overlay,
            CallbackInfo ci
    ) {
        ItemRenderColorContext.clear();
    }
}
