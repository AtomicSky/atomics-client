package com.atomics.client.mixin;

import com.atomics.client.TotemPopEffects;
import com.atomics.client.PvpStatsManager;
import com.atomics.client.AtomicsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void atomics_client$recordConfirmedHit(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        Entity entity = packet.getEntity(client.world);
        if (packet.getStatus() == 2) {
            PvpStatsManager.recordEntityHurt(entity);
        } else if (packet.getStatus() == 3) {
            // Some servers disable death messages or otherwise skip the normal client-side death path.
            // The entity-status death packet still arrives, so use it as a fallback.
            PvpStatsManager.recordDeathStatus(entity);
        } else if (packet.getStatus() == 30 && entity == client.player) {
            AtomicsClient.recordLocalShieldDisabled();
        } else if (packet.getStatus() == 35) {
            PvpStatsManager.recordTotemPop(entity);
        }
    }

    @Inject(method = "onHealthUpdate", at = @At("HEAD"))
    private void atomics_client$recordLocalDeathFromHealthPacket(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (packet.getHealth() <= 0.0f) {
            PvpStatsManager.recordLocalDeathFromHealthPacket();
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void atomics_client$recordOutcomeFromGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        PvpStatsManager.recordOutcomeFromServerMessage(packet.content());
    }

    @Inject(method = "onProfilelessChatMessage", at = @At("HEAD"))
    private void atomics_client$recordOutcomeFromProfilelessChat(ProfilelessChatMessageS2CPacket packet, CallbackInfo ci) {
        PvpStatsManager.recordOutcomeFromServerMessage(packet.message());
    }


    @Redirect(
            method = "onEntityStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleManager;addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V"
            )
    )
    private void atomics_client$playConfiguredTotemParticles(ParticleManager manager, Entity entity, ParticleEffect effect, int maxAge, EntityStatusS2CPacket packet) {
        if (packet.getStatus() != 35 || !AtomicsClient.shouldCustomizeTotemPop(entity)) {
            manager.addEmitter(entity, effect, maxAge);
            return;
        }

        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.particles.enabled) {
            manager.addEmitter(entity, effect, maxAge);
            return;
        }

        boolean replaceVanilla = AtomicsClient.CONFIG.utility.replaceVanillaParticles;
        if (!replaceVanilla) {
            manager.addEmitter(entity, effect, maxAge);
        }
        TotemPopEffects.playParticles(entity);
    }

    @Redirect(
            method = "onEntityStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;playSoundClient(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"
            )
    )
    private void atomics_client$playConfiguredTotemSound(ClientWorld world, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance, EntityStatusS2CPacket packet) {
        Entity entity = packet.getEntity(world);
        if (packet.getStatus() != 35 || !AtomicsClient.shouldCustomizeTotemPop(entity)) {
            world.playSoundClient(x, y, z, sound, category, volume, pitch, useDistance);
            return;
        }

        boolean replaceVanilla = AtomicsClient.CONFIG == null || AtomicsClient.CONFIG.utility.replaceVanillaSounds;
        if (!replaceVanilla) {
            world.playSoundClient(x, y, z, sound, category, volume, pitch, useDistance);
        }
        if (AtomicsClient.CONFIG != null && AtomicsClient.CONFIG.sounds.enabled && entity != null) {
            TotemPopEffects.playSounds(entity);
        }
    }
}
