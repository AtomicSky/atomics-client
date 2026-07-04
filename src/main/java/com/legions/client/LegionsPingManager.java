package com.legions.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsPingManager {
    private static final double RANGE = 96.0;
    private static final Pattern BLOCK_PATTERN = Pattern.compile("\\[LC]\\s+([A-Za-z0-9_]{3,16})\\s+block\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_PATTERN = Pattern.compile("\\[LC]\\s+([A-Za-z0-9_]{3,16})\\s+player\\s+([A-Za-z0-9_]{3,16})", Pattern.CASE_INSENSITIVE);

    private static final Map<BlockPos, Long> markedBlocks = new HashMap<>();
    private static final Map<String, Long> markedPlayers = new HashMap<>();
    private static int particleTick;

    private LegionsPingManager() {
    }

    public static void pingLookTarget(MinecraftClient client) {
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || client.player == null || client.world == null) {
            return;
        }

        String sender = client.player.getName().getString();
        PlayerEntity targetPlayer = raycastPlayer(client);
        if (targetPlayer != null) {
            String target = targetPlayer.getName().getString();
            sendChat(client, "[LC] " + sender + " player " + target);
            markPlayer(target);
            return;
        }

        BlockHitResult blockHit = raycastBlock(client);
        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            sendChat(client, "[LC] " + sender + " block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            markBlock(pos);
        }
    }

    public static void receiveChatPing(Text message, GameProfile senderProfile) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || message == null) {
            return;
        }
        String raw = message.getString();

        Matcher playerMatcher = PLAYER_PATTERN.matcher(raw);
        if (playerMatcher.find() && canAcceptFrom(client, senderProfile, playerMatcher.group(1))) {
            markPlayer(playerMatcher.group(2));
            return;
        }

        Matcher blockMatcher = BLOCK_PATTERN.matcher(raw);
        if (blockMatcher.find() && canAcceptFrom(client, senderProfile, blockMatcher.group(1))) {
            markBlock(new BlockPos(
                    Integer.parseInt(blockMatcher.group(2)),
                    Integer.parseInt(blockMatcher.group(3)),
                    Integer.parseInt(blockMatcher.group(4))
            ));
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            markedBlocks.clear();
            markedPlayers.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long ttl = Math.max(3, LegionsClient.CONFIG.pingDurationSeconds) * 1000L;
        markedBlocks.entrySet().removeIf(entry -> now - entry.getValue() > ttl);
        markedPlayers.entrySet().removeIf(entry -> now - entry.getValue() > ttl);

        particleTick++;
        if (particleTick % 4 != 0) {
            return;
        }

        for (BlockPos pos : markedBlocks.keySet()) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.05;
            double z = pos.getZ() + 0.5;
            client.particleManager.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.04, 0.0);
            client.particleManager.addParticle(ParticleTypes.END_ROD, x, y - 0.7, z, 0.0, 0.02, 0.0);
        }

        Iterator<String> names = markedPlayers.keySet().iterator();
        while (names.hasNext()) {
            String name = names.next();
            PlayerEntity player = findPlayer(client, name);
            if (player == null) {
                continue;
            }
            client.particleManager.addParticle(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + player.getHeight() + 0.2, player.getZ(), 0.0, 0.04, 0.0);
        }
    }

    public static boolean isMarkedPlayer(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return markedPlayers.containsKey(player.getName().getString().toLowerCase(Locale.ROOT));
    }

    private static void sendChat(MinecraftClient client, String message) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatMessage(message);
        }
    }

    private static void markBlock(BlockPos pos) {
        markedBlocks.put(pos.toImmutable(), System.currentTimeMillis());
    }

    private static void markPlayer(String name) {
        markedPlayers.put(name.toLowerCase(Locale.ROOT), System.currentTimeMillis());
    }

    private static boolean canAcceptFrom(MinecraftClient client, GameProfile signedSender, String embeddedSender) {
        if (client.player == null) {
            return false;
        }
        String senderName = signedSender != null ? signedSender.name() : embeddedSender;
        if (senderName == null || senderName.equalsIgnoreCase(client.player.getName().getString())) {
            return true;
        }
        PlayerEntity sender = findPlayer(client, senderName);
        return sender != null && LegionsFeatures.isTeammate(client.player, sender);
    }

    private static PlayerEntity findPlayer(MinecraftClient client, String name) {
        if (client.world == null || name == null) {
            return null;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private static BlockHitResult raycastBlock(MinecraftClient client) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) {
            return null;
        }
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d end = start.add(camera.getRotationVec(1.0f).multiply(RANGE));
        return client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, camera));
    }

    private static PlayerEntity raycastPlayer(MinecraftClient client) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) {
            return null;
        }
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d look = camera.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(RANGE));
        Box searchBox = camera.getBoundingBox().stretch(look.multiply(RANGE)).expand(1.0);
        EntityHitResult result = ProjectileUtil.raycast(camera, start, end, searchBox,
                entity -> entity instanceof PlayerEntity && entity.isAlive() && !entity.isSpectator(),
                RANGE * RANGE
        );
        if (result == null || !(result.getEntity() instanceof PlayerEntity player)) {
            return null;
        }
        BlockHitResult blockHit = raycastBlock(client);
        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK
                && blockHit.getPos().squaredDistanceTo(start) < result.getPos().squaredDistanceTo(start)) {
            return null;
        }
        UUID localUuid = client.player == null ? null : client.player.getUuid();
        return localUuid != null && localUuid.equals(player.getUuid()) ? null : player;
    }
}
