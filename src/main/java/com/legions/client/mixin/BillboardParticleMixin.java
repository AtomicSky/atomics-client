package com.legions.client.mixin;

import com.legions.client.LegionsWorldBorder;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BillboardParticle.class)
public abstract class BillboardParticleMixin extends Particle {
    @Shadow
    protected Sprite sprite;

    protected BillboardParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void legions_client$captureWorldBorderGlitter(CallbackInfo ci) {
        LegionsWorldBorder.captureGlitterParticle(sprite, x, y, z);
    }

    @Inject(method = "render(Lnet/minecraft/client/particle/BillboardParticleSubmittable;Lnet/minecraft/client/render/Camera;F)V",
            at = @At("HEAD"), cancellable = true)
    private void legions_client$toggleWorldBorderGlitter(BillboardParticleSubmittable submittable,
                                                          Camera camera, float tickProgress,
                                                          CallbackInfo ci) {
        if (LegionsWorldBorder.shouldHideGlitterParticle(sprite)) {
            ci.cancel();
        }
    }
}
