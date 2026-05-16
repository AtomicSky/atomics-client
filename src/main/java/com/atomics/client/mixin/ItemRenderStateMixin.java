package com.atomics.client.mixin;

import com.atomics.client.access.ItemRenderStateAccess;
import com.atomics.client.render.ItemRenderColorContext;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderState.class)
public class ItemRenderStateMixin implements ItemRenderStateAccess {
    @Unique
    private int atomics_client$colorOverlay = -1;

    @Unique
    private float atomics_client$hueShift = 0.0f;

    @Override
    public void atomics_client$setColorOverlay(int color) {
        this.atomics_client$colorOverlay = color;
    }

    @Override
    public int atomics_client$getColorOverlay() {
        return this.atomics_client$colorOverlay;
    }

    @Override
    public void atomics_client$setHueShift(float hueShift) {
        this.atomics_client$hueShift = hueShift;
    }

    @Override
    public float atomics_client$getHueShift() {
        return this.atomics_client$hueShift;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V",
            at = @At("HEAD")
    )
    private void atomics_client$beginColoredItemRender(MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, int overlay, int int2, CallbackInfo ci) {
        ItemRenderColorContext.set(this.atomics_client$colorOverlay, this.atomics_client$hueShift);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V",
            at = @At("RETURN")
    )
    private void atomics_client$endColoredItemRender(MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, int overlay, int int2, CallbackInfo ci) {
        ItemRenderColorContext.clear();
    }
}
