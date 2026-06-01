package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.render.PlayerOverlayColorContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void atomics_client$prepareFriendFoeTint(
            LivingEntity entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        PlayerOverlayColorContext.clear();
        if (entity instanceof Player player) {
            int color = AtomicsClient.getPlayerFriendFoeOverlayColor(player);
            if (color != -1) {
                PlayerOverlayColorContext.set(color, AtomicsClient.getPlayerFriendFoeOverlayStyle(player));
            }
        }
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 4
    )
    private float atomics_client$applyFriendFoeRed(float original) {
        return atomics_client$friendFoeColorComponent(original, 16);
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 5
    )
    private float atomics_client$applyFriendFoeGreen(float original) {
        return atomics_client$friendFoeColorComponent(original, 8);
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 6
    )
    private float atomics_client$applyFriendFoeBlue(float original) {
        return atomics_client$friendFoeColorComponent(original, 0);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private void atomics_client$clearFriendFoeTint(
            LivingEntity entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        PlayerOverlayColorContext.clear();
    }

    private static float atomics_client$friendFoeColorComponent(float original, int shift) {
        if (PlayerOverlayColorContext.activeColor() == -1) {
            return original;
        }
        int tinted = PlayerOverlayColorContext.apply(0xFFFFFFFF);
        return ((tinted >>> shift) & 255) / 255.0f;
    }
}
