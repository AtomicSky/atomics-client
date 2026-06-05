package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void atomics_client$applyZoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float zoomMultiplier = ClientFeatureManager.getZoomFovMultiplier();
        if (zoomMultiplier < 0.999f) {
            cir.setReturnValue(cir.getReturnValue() * zoomMultiplier);
        }
    }

    @ModifyVariable(
            method = "showFloatingItem",
            at = @At("HEAD"),
            argsOnly = true
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

    @ModifyConstant(method = "showFloatingItem", constant = @Constant(intValue = 40))
    private int atomics_client$useConfiguredOverlayDuration(int defaultTicks) {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled) {
            return defaultTicks;
        }
        return Math.max(1, AtomicsClient.CONFIG.popOverlay.animationTicks);
    }
}
