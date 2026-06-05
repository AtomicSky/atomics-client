package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
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
