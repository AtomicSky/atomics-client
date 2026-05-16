package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShieldModelRenderer.class)
public class ShieldModelRendererMixin {
    @ModifyArg(
            method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"
            ),
            index = 8
    )
    private int atomics_client$applyShieldWarningOverlay(int originalColor) {
        int warningColor = AtomicsClient.getLiveShieldWarningOverlayColor();
        return warningColor == -1 ? originalColor : warningColor;
    }
}
