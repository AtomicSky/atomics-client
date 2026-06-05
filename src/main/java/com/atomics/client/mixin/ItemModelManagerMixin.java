package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.access.ItemRenderStateAccess;
import com.atomics.client.render.ItemRenderColorContext;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @ModifyVariable(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack atomics_client$retextureTotemEverywhere(ItemStack stack) {
        return AtomicsClient.getVisualItemStack(stack);
    }

    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("TAIL")
    )
    private void atomics_client$setColorOverlay(ItemRenderState state, ItemStack stack, ModelTransformationMode displayContext, boolean leftHanded, World world, LivingEntity entity, int seed, CallbackInfo ci) {
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
