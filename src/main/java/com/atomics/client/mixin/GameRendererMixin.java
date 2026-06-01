package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import com.atomics.client.AtomicsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
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
    private void atomics_client$applyZoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        float zoomMultiplier = ClientFeatureManager.getZoomFovMultiplier();
        if (zoomMultiplier < 0.999f) {
            cir.setReturnValue(cir.getReturnValue() * zoomMultiplier);
        }
    }

    @ModifyVariable(method = "displayItemActivation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack atomics_client$retextureActivationItem(ItemStack stack) {
        return AtomicsClient.getVisualTotemStack(stack);
    }

    @ModifyConstant(method = "displayItemActivation", constant = @Constant(intValue = 40))
    private int atomics_client$activationDuration(int defaultTicks) {
        return atomics_client$configuredActivationTicks(defaultTicks);
    }

    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(intValue = 40))
    private int atomics_client$activationElapsedDuration(int defaultTicks) {
        return atomics_client$configuredActivationTicks(defaultTicks);
    }

    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(floatValue = 40.0f))
    private float atomics_client$activationProgressDuration(float defaultTicks) {
        return atomics_client$configuredActivationTicks(Math.round(defaultTicks));
    }

    @Redirect(
            method = "renderItemActivationAnimation",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
    )
    private void atomics_client$scaleActivationItem(PoseStack matrices, float x, float y, float z) {
        float scale = 1.0f;
        if (AtomicsClient.CONFIG != null
                && AtomicsClient.CONFIG.enabled
                && AtomicsClient.CONFIG.popOverlay.scaleEnabled) {
            scale = AtomicsClient.getPopScale();
        }
        matrices.scale(x * scale, y * scale, z * scale);
    }

    private static int atomics_client$configuredActivationTicks(int defaultTicks) {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled) {
            return defaultTicks;
        }
        return Math.max(1, AtomicsClient.CONFIG.popOverlay.animationTicks);
    }
}
