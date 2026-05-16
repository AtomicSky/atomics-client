package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.minecraft.client.render.entity.state.TntEntityRenderState;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntEntityRenderer.class)
public abstract class TntEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/TntEntity;Lnet/minecraft/client/render/entity/state/TntEntityRenderState;F)V", at = @At("TAIL"))
    private void atomics_client$addFuseLabel(TntEntity tnt, TntEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (!ClientFeatureManager.shouldShowTntTimer(tnt)) {
            state.displayName = null;
            state.nameLabelPos = null;
            return;
        }

        state.displayName = ClientFeatureManager.tntTimerText(tnt);
        state.nameLabelPos = new Vec3d(0.0, tnt.getHeight() + 0.35, 0.0);
    }
}
