package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.access.PlayerOverlayRenderStateAccess;
import com.atomics.client.ClientFeatureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void atomics_client$addWinOddsToNametag(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (player instanceof PlayerEntity playerEntity) {
            PlayerOverlayRenderStateAccess access = (PlayerOverlayRenderStateAccess) state;
            int color = AtomicsClient.getPlayerFriendFoeOverlayColor(playerEntity);
            int style = AtomicsClient.getPlayerFriendFoeOverlayStyle(playerEntity);
            access.atomics_client$setFriendFoeOverlayColor(color);
            access.atomics_client$setFriendFoeOverlayStyle(style);
            if (color != -1 && AtomicsClient.usesFriendFoeOutline(style)) {
                MinecraftClient client = MinecraftClient.getInstance();
                state.outlineColor = client.player != null && client.player.canSee(playerEntity)
                        ? ColorHelper.fullAlpha(color)
                        : 0;
            }
        } else {
            PlayerOverlayRenderStateAccess access = (PlayerOverlayRenderStateAccess) state;
            access.atomics_client$setFriendFoeOverlayColor(-1);
            access.atomics_client$setFriendFoeOverlayStyle(0);
        }

        if (!(player instanceof PlayerEntity playerEntity) || state.displayName == null) {
            return;
        }

        state.displayName = ClientFeatureManager.customizePlayerNametag(playerEntity, state.displayName);
    }
}
