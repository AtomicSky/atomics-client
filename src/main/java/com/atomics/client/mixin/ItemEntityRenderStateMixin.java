package com.atomics.client.mixin;

import com.atomics.client.access.ItemEntityRenderStateAccess;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemEntityRenderStateAccess {
    @Unique
    private boolean atomics_client$droppedTotem;

    @Override
    public boolean atomics_client$isDroppedTotem() {
        return atomics_client$droppedTotem;
    }

    @Override
    public void atomics_client$setDroppedTotem(boolean droppedTotem) {
        this.atomics_client$droppedTotem = droppedTotem;
    }
}
