package com.legions.client.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.legions.client.LegionsClient;
import net.minecraft.client.particle.ParticleSpriteManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(ParticleSpriteManager.class)
public class ParticleSpriteManagerMixin {
    private static final Set<Identifier> LEGIONS_GLITTER_PARTICLE_IDS = Set.of(
            Identifier.of("minecraft", "end_rod"),
            Identifier.of("minecraft", "totem_of_undying")
    );
    private static final Set<Identifier> LEGIONS_GLITTER_TEXTURE_IDS = Set.of(
            Identifier.of("minecraft", "glitter_0"),
            Identifier.of("minecraft", "glitter_1"),
            Identifier.of("minecraft", "glitter_2"),
            Identifier.of("minecraft", "glitter_3"),
            Identifier.of("minecraft", "glitter_4"),
            Identifier.of("minecraft", "glitter_5"),
            Identifier.of("minecraft", "glitter_6"),
            Identifier.of("minecraft", "glitter_7")
    );
    private static final Identifier LEGIONS_WORLD_BORDER_PARTICLE = Identifier.of(LegionsClient.MOD_ID, "world_border_end_rod");

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private void legions_client$forceWorldBorderGlitterSprite(Identifier id, Resource resource, CallbackInfoReturnable<Optional<List<Identifier>>> cir) {
        if (legions_client$usesGlitterParticleSprite(id, resource)) {
            cir.setReturnValue(Optional.of(List.of(LEGIONS_WORLD_BORDER_PARTICLE)));
        }
    }

    private static boolean legions_client$usesGlitterParticleSprite(Identifier id, Resource resource) {
        if (LEGIONS_GLITTER_PARTICLE_IDS.contains(id)) {
            return true;
        }

        try (BufferedReader reader = resource.getReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return false;
            }
            JsonElement texturesElement = root.getAsJsonObject().get("textures");
            if (texturesElement == null || !texturesElement.isJsonArray()) {
                return false;
            }

            JsonArray textures = texturesElement.getAsJsonArray();
            for (JsonElement textureElement : textures) {
                if (!textureElement.isJsonPrimitive()) {
                    continue;
                }
                Identifier textureId = Identifier.tryParse(textureElement.getAsString());
                if (LEGIONS_GLITTER_TEXTURE_IDS.contains(textureId)) {
                    return true;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
        return false;
    }
}
