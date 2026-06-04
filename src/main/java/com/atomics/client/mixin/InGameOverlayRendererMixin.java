package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public class InGameOverlayRendererMixin {
    @ModifyArg(
            method = "renderItemActivationAnimation(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"
            ),
            index = 1
    )
    private ItemStack atomics_client$retextureFloatingItem(ItemStack stack) {
        return AtomicsClient.getVisualTotemStack(stack);
    }

    @Redirect(
            method = "renderItemActivationAnimation(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
    )
    private void atomics_client$scaleFloatingItem(PoseStack matrices, float x, float y, float z) {
        float scale = 1.0f;
        if (AtomicsClient.CONFIG != null
                && AtomicsClient.CONFIG.enabled
                && AtomicsClient.CONFIG.popOverlay.scaleEnabled) {
            scale = AtomicsClient.getPopScale();
        }
        matrices.scale(x * scale, y * scale, z * scale);
    }

    @ModifyConstant(method = "displayItemActivation(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/util/RandomSource;)V", constant = @Constant(intValue = 40))
    private int atomics_client$useConfiguredOverlayDuration(int defaultTicks) {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled) {
            return defaultTicks;
        }
        return Math.max(1, AtomicsClient.CONFIG.popOverlay.animationTicks);
    }
    @ModifyArg(
            method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
            index = 1
    )
    private static float atomics_client$adjustFireOverlayHeight(float y) {
        if (AtomicsClient.CONFIG == null
                || !AtomicsClient.CONFIG.enabled
                || AtomicsClient.CONFIG.misc == null
                || !AtomicsClient.CONFIG.misc.fireOverlayEnabled) {
            return y;
        }
        return y + AtomicsClient.CONFIG.misc.fireOverlayHeight;
    }

}
