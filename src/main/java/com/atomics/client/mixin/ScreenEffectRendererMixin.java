package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @ModifyArg(
            method = "renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            ),
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
