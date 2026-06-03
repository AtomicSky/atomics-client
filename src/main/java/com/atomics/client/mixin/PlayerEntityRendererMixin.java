package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.access.PlayerOverlayRenderStateAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import com.atomics.client.ClientFeatureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void atomics_client$addWinOddsToNametag(Avatar player, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
        if (player instanceof Player playerEntity) {
            PlayerOverlayRenderStateAccess access = (PlayerOverlayRenderStateAccess) state;
            int color = AtomicsClient.getPlayerFriendFoeOverlayColor(playerEntity);
            int style = AtomicsClient.getPlayerFriendFoeOverlayStyle(playerEntity);
            access.atomics_client$setFriendFoeOverlayColor(color);
            access.atomics_client$setFriendFoeOverlayStyle(style);
            if (color != -1 && AtomicsClient.usesFriendFoeOutline(style)) {
                Minecraft client = Minecraft.getInstance();
                state.outlineColor = client.player != null && client.player.hasLineOfSight(playerEntity)
                        ? ARGB.opaque(color)
                        : 0;
            }
        } else {
            PlayerOverlayRenderStateAccess access = (PlayerOverlayRenderStateAccess) state;
            access.atomics_client$setFriendFoeOverlayColor(-1);
            access.atomics_client$setFriendFoeOverlayStyle(0);
        }

        if (!(player instanceof Player playerEntity) || state.nameTag == null) {
            return;
        }

        state.nameTag = ClientFeatureManager.customizePlayerNametag(playerEntity, state.nameTag);
    }
}
