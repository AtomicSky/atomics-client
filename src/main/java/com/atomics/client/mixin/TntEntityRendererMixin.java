package com.atomics.client.mixin;

import com.atomics.client.ClientFeatureManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntRenderer.class)
public abstract class TntEntityRendererMixin extends EntityRenderer<PrimedTnt> {
    protected TntEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/item/PrimedTnt;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    private void atomics_client$renderFuseLabel(PrimedTnt tnt, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (ClientFeatureManager.shouldShowTntTimer(tnt)) {
            renderNameTag(tnt, ClientFeatureManager.tntTimerText(tnt), matrices, buffers, light);
        }
    }
}
