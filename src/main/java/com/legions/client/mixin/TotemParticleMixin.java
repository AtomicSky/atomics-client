package com.legions.client.mixin;

import com.legions.client.LegionsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.TotemParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemParticle.class)
public class TotemParticleMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void legions_client$makeLegionsTotemsClearer(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.warningParticlesEnabled) {
            return;
        }
        TotemParticle particle = (TotemParticle) (Object) this;
        particle.scale(1.65f);
        particle.setColor(1.0f, 1.0f, 1.0f);
    }
}
