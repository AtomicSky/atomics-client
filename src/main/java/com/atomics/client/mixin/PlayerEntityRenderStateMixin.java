package com.atomics.client.mixin;

import com.atomics.client.access.PlayerOverlayRenderStateAccess;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerOverlayRenderStateAccess {
    @Unique
    private int atomics_client$friendFoeOverlayColor = -1;

    @Override
    public void atomics_client$setFriendFoeOverlayColor(int color) {
        this.atomics_client$friendFoeOverlayColor = color;
    }

    @Override
    public int atomics_client$getFriendFoeOverlayColor() {
        return this.atomics_client$friendFoeOverlayColor;
    }
}
