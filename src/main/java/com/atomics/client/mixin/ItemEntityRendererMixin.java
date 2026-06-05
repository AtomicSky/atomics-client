package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.access.ItemEntityRenderStateAccess;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @ModifyArg(
            method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;update(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/item/ItemModelManager;)V"
            ),
            index = 1
    )
    private ItemStack atomics_client$retextureDroppedTotem(ItemStack stack) {
        return AtomicsClient.getVisualItemStack(stack);
    }

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void atomics_client$trackDroppedTotem(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ((ItemEntityRenderStateAccess) state).atomics_client$setDroppedTotem(entity.getStack().isOf(Items.TOTEM_OF_UNDYING));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/ItemEntityRenderer;renderStack(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/ItemStackEntityRenderState;Lnet/minecraft/util/math/random/Random;)V"
            )
    )
    private void atomics_client$scaleDroppedTotem(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!AtomicsClient.isDroppedTotemScaleEnabled()) {
            return;
        }
        if (!((ItemEntityRenderStateAccess) state).atomics_client$isDroppedTotem()) {
            return;
        }

        float scale = AtomicsClient.getDroppedTotemScale();
        matrices.scale(scale, scale, scale);
    }
}
