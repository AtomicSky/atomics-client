package com.atomics.client.mixin;

import com.atomics.client.render.ItemRenderColorContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class LayerRenderStateMixin {
    @Unique
    private static final Map<List<BakedQuad>, List<BakedQuad>> atomics_client$tintedQuadCache = new IdentityHashMap<>();

    @ModifyArg(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
            ),
            index = 5
    )
    private int[] atomics_client$forceTintArray(int[] original) {
        return ItemRenderColorContext.active() ? ItemRenderColorContext.tintArray() : original;
    }

    @ModifyArg(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
            ),
            index = 6
    )
    private List<BakedQuad> atomics_client$forceEveryQuadTinted(List<BakedQuad> original) {
        if (!ItemRenderColorContext.active() || original == null || original.isEmpty()) {
            return original;
        }

        List<BakedQuad> cached = atomics_client$tintedQuadCache.get(original);
        if (cached != null) {
            return cached;
        }

        boolean needsRetint = false;
        for (BakedQuad quad : original) {
            if (quad.tintIndex() != 0) {
                needsRetint = true;
                break;
            }
        }
        if (!needsRetint) {
            atomics_client$tintedQuadCache.put(original, original);
            return original;
        }

        List<BakedQuad> tinted = new ArrayList<>(original.size());
        for (BakedQuad quad : original) {
            tinted.add(atomics_client$withTintIndexZero(quad));
        }
        atomics_client$tintedQuadCache.put(original, tinted);
        return tinted;
    }

    @Unique
    private static BakedQuad atomics_client$withTintIndexZero(BakedQuad quad) {
        if (quad.tintIndex() == 0) {
            return quad;
        }
        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                0,
                quad.direction(),
                quad.sprite(),
                quad.shade(),
                quad.lightEmission()
        );
    }
}
