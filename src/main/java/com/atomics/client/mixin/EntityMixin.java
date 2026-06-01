package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void atomics_client$applyFriendFoeOutlineColor(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        int color = AtomicsClient.getPlayerFriendFoeOverlayColor(player);
        int style = AtomicsClient.getPlayerFriendFoeOverlayStyle(player);
        if (color != -1 && AtomicsClient.usesFriendFoeOutline(style)) {
            cir.setReturnValue(color);
        }
    }
}
