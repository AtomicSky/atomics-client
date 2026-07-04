package com.atomics.client.mixin;

import com.atomics.client.AtomicsClient;
import com.atomics.client.render.LegionsWarningParticle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Shadow
    protected ClientWorld world;

    @Inject(method = "createParticle", at = @At("RETURN"), cancellable = true)
    private <T extends ParticleEffect> void atomics_client$replaceLegionsEndRodParticle(
            T parameters,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfoReturnable<Particle> cir
    ) {
        Particle particle = cir.getReturnValue();
        if (particle == null
                || parameters.getType() != ParticleTypes.END_ROD
                || !AtomicsClient.areLegionsEndRodWarningsEnabled(MinecraftClient.getInstance())
                || !(particle instanceof BillboardParticle billboardParticle)) {
            return;
        }

        Sprite sprite = ((BillboardParticleAccessor) billboardParticle).atomics_client$getSprite();
        if (sprite == null) {
            return;
        }

        cir.setReturnValue(new LegionsWarningParticle(
                this.world,
                x,
                y,
                z,
                velocityX,
                velocityY,
                velocityZ,
                sprite,
                particle.getMaxAge()
        ));
    }
}
