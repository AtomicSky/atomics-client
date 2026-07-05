package com.legions.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsPingManager {
    private static final double RANGE = 96.0;
    private static final long DOUBLE_PRESS_WINDOW_MILLIS = 300L;
    private static final float BLOCK_PING_OUTER_EXPAND = 0.025f;
    private static final float BLOCK_PING_INNER_EXPAND = 0.04f;
    private static final float BLOCK_PING_OUTER_WIDTH = 3.5f;
    private static final float BLOCK_PING_INNER_WIDTH = 1.75f;
    private static final float BLOCK_PING_FAR_MARKER_SIZE = 7.0f;
    private static final double BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED = 40.0 * 40.0;
    private static final int BLOCK_PING_OUTER_COLOR = 0x99FFD84A;
    private static final int BLOCK_PING_INNER_COLOR = 0xFFFFFFB0;
    private static final int BLOCK_PING_MARKER_COLOR = 0xFFFFD84A;
    private static final Pattern BLOCK_PATTERN = Pattern.compile("\\[LC:B:([A-Za-z0-9_]{3,16}):(-?\\d+):(-?\\d+):(-?\\d+)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_PATTERN = Pattern.compile("\\[LC:P:([A-Za-z0-9_]{3,16}):([A-Za-z0-9_]{3,16})]", Pattern.CASE_INSENSITIVE);

    private static final Map<BlockPos, Long> markedBlocks = new HashMap<>();
    private static final Map<String, Long> markedPlayers = new HashMap<>();
    private static long pendingBlockPressAt;
    private static BlockPos pendingBlockPingPos;

    private LegionsPingManager() {
    }

    public static void handlePingKeyPress(MinecraftClient client) {
        if (!canUsePing(client)) {
            clearPendingBlockPing();
            return;
        }

        long now = System.currentTimeMillis();
        if (pendingBlockPressAt > 0L && now - pendingBlockPressAt <= DOUBLE_PRESS_WINDOW_MILLIS) {
            clearPendingBlockPing();
            pingLookPlayer(client);
            return;
        }

        flushPendingBlockPing(client, now);
        pendingBlockPressAt = now;
        pendingBlockPingPos = raycastBlockPos(client);
    }

    private static void pingLookPlayer(MinecraftClient client) {
        String sender = LegionsFeatures.realUsername(client.player);
        PlayerEntity targetPlayer = raycastPlayer(client);
        if (targetPlayer != null) {
            String target = LegionsFeatures.realUsername(targetPlayer);
            sendChat(client, "Focus " + target + " [LC:P:" + sender + ":" + target + "]");
            markPlayer(target);
        }
    }

    private static void pingBlock(MinecraftClient client, BlockPos pos) {
        String sender = LegionsFeatures.realUsername(client.player);
        sendChat(client, "Go to " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " [LC:B:" + sender + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + "]");
        markBlock(pos);
    }

    public static void receiveChatPing(Text message, GameProfile senderProfile) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || message == null) {
            return;
        }
        String raw = message.getString();

        if (senderProfile == null) {
            return;
        }

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
            clearPendingBlockPing();
            return;
        }

        long now = System.currentTimeMillis();
        if (!canUsePing(client)) {
            clearPendingBlockPing();
        } else {
            flushPendingBlockPing(client, now);
        }

        long ttl = pingTtlMillis();
        markedBlocks.entrySet().removeIf(entry -> now - entry.getValue() > ttl);
        markedPlayers.entrySet().removeIf(entry -> now - entry.getValue() > ttl);
    }

    public static boolean isMarkedPlayer(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return markedPlayers.containsKey(LegionsFeatures.realUsername(player).toLowerCase(Locale.ROOT));
    }

    public static void renderBlockHighlights() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || client.world == null || markedBlocks.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long ttl = pingTtlMillis();
        for (Map.Entry<BlockPos, Long> entry : markedBlocks.entrySet()) {
            if (now - entry.getValue() > ttl) {
                continue;
            }
            BlockPos pos = entry.getKey();
            renderBlockPingOutline(client, pos);
        }
    }

    private static void renderBlockPingOutline(MinecraftClient client, BlockPos pos) {
        GizmoDrawing.box(pos, BLOCK_PING_OUTER_EXPAND, DrawStyle.stroked(BLOCK_PING_OUTER_COLOR, BLOCK_PING_OUTER_WIDTH)).ignoreOcclusion();
        GizmoDrawing.box(pos, BLOCK_PING_INNER_EXPAND, DrawStyle.stroked(BLOCK_PING_INNER_COLOR, BLOCK_PING_INNER_WIDTH)).ignoreOcclusion();

        Entity camera = client.getCameraEntity();
        Vec3d center = Vec3d.ofCenter(pos);
        if (camera != null && camera.squaredDistanceTo(center) > BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED) {
            GizmoDrawing.point(center, BLOCK_PING_MARKER_COLOR, BLOCK_PING_FAR_MARKER_SIZE).ignoreOcclusion();
        }
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

    private static void flushPendingBlockPing(MinecraftClient client, long now) {
        if (pendingBlockPressAt <= 0L || now - pendingBlockPressAt < DOUBLE_PRESS_WINDOW_MILLIS) {
            return;
        }

        BlockPos pos = pendingBlockPingPos;
        clearPendingBlockPing();
        if (pos != null && canUsePing(client)) {
            pingBlock(client, pos);
        }
    }

    private static void clearPendingBlockPing() {
        pendingBlockPressAt = 0L;
        pendingBlockPingPos = null;
    }

    private static long pingTtlMillis() {
        return Math.max(3, Math.min(10, LegionsClient.CONFIG.pingDurationSeconds)) * 1000L;
    }

    private static boolean canUsePing(MinecraftClient client) {
        return LegionsClient.enabled(client)
                && LegionsClient.CONFIG.teamPingEnabled
                && client.player != null
                && client.world != null;
    }

    private static boolean canAcceptFrom(MinecraftClient client, GameProfile signedSender, String embeddedSender) {
        if (client.player == null) {
            return false;
        }
        String senderName = signedSender != null ? signedSender.name() : embeddedSender;
        if (senderName == null || senderName.equalsIgnoreCase(LegionsFeatures.realUsername(client.player))) {
            return true;
        }
        PlayerEntity sender = findPlayer(client, senderName);
        return sender != null && !LegionsFeatures.isSpectatorTeam(sender) && LegionsFeatures.isTeammate(client.player, sender);
    }

    private static PlayerEntity findPlayer(MinecraftClient client, String name) {
        if (client.world == null || name == null) {
            return null;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (LegionsFeatures.realUsername(player).equalsIgnoreCase(name)) {
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

    private static BlockPos raycastBlockPos(MinecraftClient client) {
        BlockHitResult blockHit = raycastBlock(client);
        if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return blockHit.getBlockPos().toImmutable();
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
