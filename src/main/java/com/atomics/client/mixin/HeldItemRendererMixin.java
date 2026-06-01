package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void atomics_client$transformHeldItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack matrices,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        AtomicsClient.setRenderingLocalPlayerHeldItem(entity);

        if (AtomicsClient.CONFIG != null && AtomicsClient.CONFIG.enabled && AtomicsClient.CONFIG.misc != null && stack.is(Items.SHIELD)) {
            boolean raised = entity != null && entity.isUsingItem() && entity.getUseItem().is(Items.SHIELD);
            boolean enabled = raised ? AtomicsClient.CONFIG.misc.shieldUpEnabled : AtomicsClient.CONFIG.misc.shieldDownEnabled;
            if (enabled) {
                float x = raised ? AtomicsClient.CONFIG.misc.shieldUpX : AtomicsClient.CONFIG.misc.shieldDownX;
                float y = raised ? AtomicsClient.CONFIG.misc.shieldUpY : AtomicsClient.CONFIG.misc.shieldDownY;
                float z = raised ? AtomicsClient.CONFIG.misc.shieldUpZ : AtomicsClient.CONFIG.misc.shieldDownZ;
                float rotX = raised ? AtomicsClient.CONFIG.misc.shieldUpRotX : AtomicsClient.CONFIG.misc.shieldDownRotX;
                float rotY = raised ? AtomicsClient.CONFIG.misc.shieldUpRotY : AtomicsClient.CONFIG.misc.shieldDownRotY;
                float rotZ = raised ? AtomicsClient.CONFIG.misc.shieldUpRotZ : AtomicsClient.CONFIG.misc.shieldDownRotZ;

                matrices.translate(x, y, z);
                matrices.mulPose(Axis.XP.rotationDegrees(rotX));
                matrices.mulPose(Axis.YP.rotationDegrees(rotY));
                matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
            }
        }

        if (AtomicsClient.isTotemPopItemEnabled() && stack.is(Items.TOTEM_OF_UNDYING)) {
            float scale = AtomicsClient.getHandScale();
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void atomics_client$clearHeldItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack matrices,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        AtomicsClient.clearRenderingLocalPlayerHeldItem();
    }
}
