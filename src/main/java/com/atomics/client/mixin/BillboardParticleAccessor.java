package com.atomics.client.mixin;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BillboardParticle.class)
public interface BillboardParticleAccessor {
    @Accessor("sprite")
    Sprite atomics_client$getSprite();
}
