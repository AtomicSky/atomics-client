package com.atomics.client.mixin;

import com.atomics.client.render.ItemRenderColorContext;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ItemRenderer.class)
public class LayerRenderStateMixin {
    @Unique
    private static final Map<List<BakedQuad>, List<BakedQuad>> atomics_client$tintedQuadCache = new IdentityHashMap<>();

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;[IIILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"
            ),
            index = 1
    )
    private static int[] atomics_client$forceTintArray(int[] original) {
        return ItemRenderColorContext.active() ? ItemRenderColorContext.tintArray() : original;
    }

    @ModifyArg(
            method = "renderBakedItemModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemQuads(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Ljava/util/List;[III)V"
            ),
            index = 2
    )
    private static List<BakedQuad> atomics_client$forceEveryQuadTinted(List<BakedQuad> original) {
        if (!ItemRenderColorContext.active() || original == null || original.isEmpty()) {
            return original;
        }

        List<BakedQuad> cached = atomics_client$tintedQuadCache.get(original);
        if (cached != null) {
            return cached;
        }

        boolean needsRetint = false;
        for (BakedQuad quad : original) {
            if (!quad.hasTint() || quad.getTintIndex() != 0) {
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
        if (quad.hasTint() && quad.getTintIndex() == 0) {
            return quad;
        }
        return new BakedQuad(
                quad.getVertexData().clone(),
                0,
                quad.getFace(),
                quad.getSprite(),
                quad.hasShade(),
                quad.getLightEmission()
        );
    }
}
