package com.atomics.client.mixin;

import com.atomics.client.access.PlayerOverlayRenderStateAccess;
import com.atomics.client.render.PlayerOverlayColorContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void atomics_client$beginPlayerFriendFoeOverlay(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (state instanceof AvatarRenderState) {
            PlayerOverlayRenderStateAccess access = (PlayerOverlayRenderStateAccess) state;
            PlayerOverlayColorContext.set(access.atomics_client$getFriendFoeOverlayColor(), access.atomics_client$getFriendFoeOverlayStyle());
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
    private void atomics_client$endPlayerFriendFoeOverlay(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (state instanceof AvatarRenderState) {
            PlayerOverlayColorContext.clear();
        }
    }
}
