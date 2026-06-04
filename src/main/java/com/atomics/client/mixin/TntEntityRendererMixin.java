package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntRenderer.class)
public abstract class TntEntityRendererMixin {
    @Unique
    private static float atomics_client$cachedLabelHeight = Float.NaN;

    @Unique
    private static Vec3 atomics_client$cachedLabelPos;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/PrimedTnt;Lnet/minecraft/client/renderer/entity/state/TntRenderState;F)V", at = @At("TAIL"))
    private void atomics_client$addFuseLabel(PrimedTnt tnt, TntRenderState state, float tickProgress, CallbackInfo ci) {
        if (!ClientFeatureManager.shouldShowTntTimer(tnt)) {
            state.nameTag = null;
            state.nameTagAttachment = null;
            return;
        }

        state.nameTag = ClientFeatureManager.tntTimerText(tnt);
        float labelHeight = tnt.getBbHeight() + 0.35f;
        if (atomics_client$cachedLabelPos == null || Float.compare(labelHeight, atomics_client$cachedLabelHeight) != 0) {
            atomics_client$cachedLabelHeight = labelHeight;
            atomics_client$cachedLabelPos = new Vec3(0.0, labelHeight, 0.0);
        }
        state.nameTagAttachment = atomics_client$cachedLabelPos;
    }
}
