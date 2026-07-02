package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.VillagerProfessionLeveler;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntityRenderer.class)
public class VillagerEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void atomics_client$highlightLevelerTarget(VillagerEntity villager, VillagerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        int highlightColor = AtomicsClient.getRendererOutlineColor(VillagerProfessionLeveler.highlightColor());
        if (VillagerProfessionLeveler.isMarkedLevelerTarget(villager)) {
            state.outlineColor = highlightColor;
        } else if (state.outlineColor == highlightColor) {
            state.outlineColor = 0;
        }
    }
}
