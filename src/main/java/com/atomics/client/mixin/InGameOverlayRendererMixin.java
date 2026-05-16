package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @ModifyArg(
            method = "renderFloatingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/item/ItemModelManager;clearAndUpdate(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/world/World;Lnet/minecraft/util/HeldItemContext;I)V"
            ),
            index = 1
    )
    private ItemStack atomics_client$retextureFloatingItem(ItemStack stack) {
        return AtomicsClient.getVisualTotemStack(stack);
    }

    @Redirect(
            method = "renderFloatingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V")
    )
    private void atomics_client$scaleFloatingItem(MatrixStack matrices, float x, float y, float z) {
        float scale = 1.0f;
        if (AtomicsClient.CONFIG != null
                && AtomicsClient.CONFIG.enabled
                && AtomicsClient.CONFIG.popOverlay.scaleEnabled) {
            scale = AtomicsClient.getPopScale();
        }
        matrices.scale(x * scale, y * scale, z * scale);
    }

    @ModifyConstant(method = "setFloatingItem", constant = @Constant(intValue = 40))
    private int atomics_client$useConfiguredOverlayDuration(int defaultTicks) {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled) {
            return defaultTicks;
        }
        return Math.max(1, AtomicsClient.CONFIG.popOverlay.animationTicks);
    }
    @ModifyArg(
            method = "renderFireOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"),
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
