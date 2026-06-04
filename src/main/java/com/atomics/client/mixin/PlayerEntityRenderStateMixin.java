package com.atomics.client.mixin;

import com.atomics.client.access.PlayerOverlayRenderStateAccess;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerOverlayRenderStateAccess {
    @Unique
    private int atomics_client$friendFoeOverlayColor = -1;
    @Unique
    private int atomics_client$friendFoeOverlayStyle = 0;

    @Override
    public void atomics_client$setFriendFoeOverlayColor(int color) {
        this.atomics_client$friendFoeOverlayColor = color;
    }

    @Override
    public int atomics_client$getFriendFoeOverlayColor() {
        return this.atomics_client$friendFoeOverlayColor;
    }

    @Override
    public void atomics_client$setFriendFoeOverlayStyle(int style) {
        this.atomics_client$friendFoeOverlayStyle = style;
    }

    @Override
    public int atomics_client$getFriendFoeOverlayStyle() {
        return this.atomics_client$friendFoeOverlayStyle;
    }
}
