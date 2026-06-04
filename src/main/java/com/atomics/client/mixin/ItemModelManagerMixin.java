package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.access.ItemRenderStateAccess;
import com.atomics.client.render.ItemRenderColorContext;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class ItemModelManagerMixin {
    @ModifyVariable(
            method = "updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack atomics_client$retextureTotemEverywhere(ItemStack stack) {
        return AtomicsClient.getVisualItemStack(stack);
    }

    @Inject(
            method = "updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("TAIL")
    )
    private void atomics_client$setColorOverlay(ItemStackRenderState state, ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext, Level world, ItemOwner heldItemContext, int seed, CallbackInfo ci) {
        ItemRenderStateAccess access = (ItemRenderStateAccess) state;

        // Store dynamic targets instead of baked values. Minecraft can cache item
        // render states, so baked hue/overlay values would not update until the
        // item/model got recreated. Dynamic sentinels make config changes visible
        // immediately in inventory, hotbar, hand, dropped item, and pop overlay.
        access.atomics_client$setColorOverlay(
                AtomicsClient.isEmptyBucketOverlayTarget(stack)
                        ? ItemRenderColorContext.DYNAMIC_EMPTY_BUCKET_COLOR
                        : AtomicsClient.getItemColorOverlay(stack)
        );
        access.atomics_client$setHueShift(
                AtomicsClient.isTotemHueShiftCandidate(stack)
                        ? ItemRenderColorContext.DYNAMIC_TOTEM_HUE_SHIFT
                        : AtomicsClient.getTotemHueShift(stack)
        );
    }
}
