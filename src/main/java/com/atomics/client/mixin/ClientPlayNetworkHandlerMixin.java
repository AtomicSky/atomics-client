package com.atomics.client.mixin;

import com.atomics.client.TotemPopEffects;
import com.atomics.client.AtomicsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import com.atomics.client.PvpNametagStatsManager;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void atomics_client$recordShieldDisabled(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Entity entity = packet.getEntity(client.level);
        if (packet.getEventId() == 35) {
            PvpNametagStatsManager.recordTotemPop(entity);
        } else if (packet.getEventId() == 30 && entity == client.player) {
            AtomicsClient.recordLocalShieldDisabled();
        }
    }
    @Redirect(
            method = "handleEntityEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"
            )
    )
    private void atomics_client$playConfiguredTotemParticles(ParticleEngine manager, Entity entity, ParticleOptions effect, int maxAge, ClientboundEntityEventPacket packet) {
        if (packet.getEventId() != 35 || !AtomicsClient.shouldCustomizeTotemPop(entity)) {
            manager.createTrackingEmitter(entity, effect, maxAge);
            return;
        }

        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.particles.enabled) {
            manager.createTrackingEmitter(entity, effect, maxAge);
            return;
        }

        boolean replaceVanilla = AtomicsClient.CONFIG.utility.replaceVanillaParticles;
        if (!replaceVanilla) {
            manager.createTrackingEmitter(entity, effect, maxAge);
        }
        TotemPopEffects.playParticles(entity);
    }

    @Redirect(
            method = "handleEntityEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
            )
    )
    private void atomics_client$playConfiguredTotemSound(ClientLevel world, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance, ClientboundEntityEventPacket packet) {
        Entity entity = packet.getEntity(world);
        if (packet.getEventId() != 35 || !AtomicsClient.shouldCustomizeTotemPop(entity)) {
            world.playLocalSound(x, y, z, sound, category, volume, pitch, useDistance);
            return;
        }

        boolean replaceVanilla = AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.utility.replaceVanillaSounds;
        if (!replaceVanilla) {
            world.playLocalSound(x, y, z, sound, category, volume, pitch, useDistance);
        }
        if (AtomicsClient.CONFIG != null && AtomicsClient.CONFIG.sounds.enabled && entity != null) {
            TotemPopEffects.playSounds(entity);
        }
    }
}