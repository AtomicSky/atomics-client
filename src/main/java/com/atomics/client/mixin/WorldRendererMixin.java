package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Redirect(
            method = "renderEntities(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/RenderTickCounter;Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I")
    )
    private int atomics_client$useFriendFoeOutlineColor(Entity entity) {
        int color = AtomicsClient.getPlayerFriendFoeOutlineColor(entity);
        return color == -1 ? entity.getTeamColorValue() : color;
    }
}
