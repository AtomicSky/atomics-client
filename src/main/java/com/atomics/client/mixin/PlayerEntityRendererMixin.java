package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.access.PlayerOverlayRenderStateAccess;
import com.atomics.client.ClientFeatureManager;
import com.atomics.client.PvpStatsManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void atomics_client$addWinOddsToNametag(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (player instanceof PlayerEntity playerEntity) {
            ((PlayerOverlayRenderStateAccess) state).atomics_client$setFriendFoeOverlayColor(AtomicsClient.getPlayerFriendFoeOverlayColor(playerEntity));
        } else {
            ((PlayerOverlayRenderStateAccess) state).atomics_client$setFriendFoeOverlayColor(-1);
        }

        if (!(player instanceof PlayerEntity playerEntity) || state.displayName == null) {
            return;
        }

        boolean maskNames = ClientFeatureManager.shouldMaskPlayerNames();
        Text suffix = PvpStatsManager.getWinOddsNameSuffix(playerEntity);
        if (!maskNames && suffix == null) {
            return;
        }

        MutableText displayName = maskNames
                ? Text.literal(ClientFeatureManager.maskedPlayerName(playerEntity))
                : state.displayName.copy();
        state.displayName = suffix == null ? displayName : displayName.append(suffix);
    }
}
