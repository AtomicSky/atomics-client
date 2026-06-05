package com.atomics.client.mixin;

import com.atomics.client.render.PlayerOverlayColorContext;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Model.class)
public class RenderCommandQueueMixin {
    @ModifyArg(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            ),
            index = 4
    )
    private int atomics_client$applyPlayerFriendFoeOverlay(int originalColor) {
        return PlayerOverlayColorContext.apply(originalColor);
    }
}
