package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import net.minecraft.client.particle.ParticleSpriteManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ParticleSpriteManager.class)
public class ParticleSpriteManagerMixin {
    private static final Identifier LEGIONS_END_ROD = Identifier.of("minecraft", "end_rod");
    private static final Identifier LEGIONS_WORLD_BORDER_PARTICLE = Identifier.of(LegionsClient.MOD_ID, "world_border_end_rod");

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private void legions_client$forceWorldBorderEndRodSprite(Identifier id, Resource resource, CallbackInfoReturnable<Optional<List<Identifier>>> cir) {
        if (LEGIONS_END_ROD.equals(id)) {
            cir.setReturnValue(Optional.of(List.of(LEGIONS_WORLD_BORDER_PARTICLE)));
        }
    }
}
