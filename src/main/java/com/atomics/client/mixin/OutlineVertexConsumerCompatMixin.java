package com.atomics.client.mixin;

import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.render.OutlineVertexConsumerProvider$OutlineVertexConsumer", priority = 900)
public class OutlineVertexConsumerCompatMixin {
    @ModifyArg(
            method = "vertex(FFFIFFIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/vulkanmod/interfaces/ExtendedVertexBuilder;vertex(FFFIFFIII)V",
                    remap = false
            ),
            index = 3,
            require = 0,
            remap = false
    )
    private int atomics_client$convertVulkanOutlineColor(int color) {
        return ColorHelper.toAbgr(color);
    }
}
