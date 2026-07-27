package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import net.minecraft.client.particle.EndRodParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndRodParticle.class)
public class EndRodParticleMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void legions_client$makeLegionsEndRodsClearer(CallbackInfo ci) {
        if (!LegionsClient.warningParticlesEnabled()) {
            return;
        }
        EndRodParticle particle = (EndRodParticle) (Object) this;
        particle.scale(1.65f);
        particle.setColor(1.0f, 1.0f, 1.0f);
    }
}
