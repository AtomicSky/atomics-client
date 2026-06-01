package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BlockEntityWithoutLevelRenderer.class)
public class BlockEntityWithoutLevelRendererMixin {
    @ModifyArg(
            method = "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 4
    )
    private float atomics_client$applyShieldWarningRed(float original) {
        return atomics_client$applyShieldWarningColor(original, 16);
    }

    @ModifyArg(
            method = "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 5
    )
    private float atomics_client$applyShieldWarningGreen(float original) {
        return atomics_client$applyShieldWarningColor(original, 8);
    }

    @ModifyArg(
            method = "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 6
    )
    private float atomics_client$applyShieldWarningBlue(float original) {
        return atomics_client$applyShieldWarningColor(original, 0);
    }

    private static float atomics_client$applyShieldWarningColor(float original, int shift) {
        int tint = AtomicsClient.getLiveShieldWarningOverlayColor();
        if (tint == -1) {
            return original;
        }

        int alpha = (tint >>> 24) & 255;
        int channel = (tint >>> shift) & 255;
        float multiplier = (channel * alpha + 255 * (255 - alpha)) / (255.0f * 255.0f);
        return original * multiplier;
    }
}
