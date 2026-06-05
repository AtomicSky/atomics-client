package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShieldModelRenderer.class)
public class ShieldModelRendererMixin {
    @Redirect(
            method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"
            )
    )
    private void atomics_client$applyShieldWarningOverlay(ModelPart part, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay) {
        int warningColor = AtomicsClient.getLiveShieldWarningOverlayColor();
        if (warningColor == -1) {
            part.render(matrices, vertexConsumer, light, overlay);
            return;
        }
        part.render(matrices, vertexConsumer, light, overlay, warningColor);
    }
}
