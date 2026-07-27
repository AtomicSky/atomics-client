package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void legions_client$blockDisabledWarningParticle(ParticleEffect effect, double x, double y, double z,
                                                             double velocityX, double velocityY, double velocityZ,
                                                             CallbackInfoReturnable<Particle> cir) {
        if (legions_client$shouldBlockWarningParticle(effect)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V",
            at = @At("HEAD"), cancellable = true)
    private void legions_client$blockDisabledWarningEmitter(Entity entity, ParticleEffect effect, CallbackInfo ci) {
        if (legions_client$shouldBlockWarningParticle(effect)) {
            ci.cancel();
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V",
            at = @At("HEAD"), cancellable = true)
    private void legions_client$blockDisabledWarningEmitter(Entity entity, ParticleEffect effect, int maxAge, CallbackInfo ci) {
        if (legions_client$shouldBlockWarningParticle(effect)) {
            ci.cancel();
        }
    }

    private static boolean legions_client$shouldBlockWarningParticle(ParticleEffect effect) {
        MinecraftClient client = MinecraftClient.getInstance();
        return LegionsClient.enabled(client)
                && LegionsClient.CONFIG != null
                && !LegionsClient.CONFIG.warningParticlesEnabled
                && effect != null
                && (effect.getType() == ParticleTypes.END_ROD || effect.getType() == ParticleTypes.TOTEM_OF_UNDYING);
    }
}
