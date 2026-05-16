package com.atomics.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("red")
    void atomics_client$setRed(float red);

    @Accessor("green")
    void atomics_client$setGreen(float green);

    @Accessor("blue")
    void atomics_client$setBlue(float blue);
}
