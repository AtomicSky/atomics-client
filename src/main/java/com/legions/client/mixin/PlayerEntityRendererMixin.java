package com.legions.client.mixin;

import com.legions.client.LegionsFeatures;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerEntityRenderer.class, priority = 500)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void legions_client$addLegionsNametagAndOutline(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (!(player instanceof PlayerEntity playerEntity)) {
            return;
        }
        if (LegionsFeatures.shouldHidePlayerModel(playerEntity)) {
            state.invisible = true;
            state.invisibleToPlayer = true;
            state.displayName = null;
            state.nameLabelPos = null;
            state.shadowRadius = 0.0f;
            state.outlineColor = 0;
            return;
        }
        if (state.displayName != null) {
            state.displayName = LegionsFeatures.customizeNametag(playerEntity, state.displayName);
        }
        state.outlineColor = LegionsFeatures.getOutlineColor(playerEntity);
    }
}
