package com.legions.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsPingManager {
    private static final double RANGE = 100.0;
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
    private static final int PING_LABEL_COLOR = 0xFF20FF4A;
    private static final float BLOCK_PING_LABEL_SCALE = 0.65f;
    private static final Pattern BLOCK_PATTERN = Pattern.compile("\\[LC:B:([A-Za-z0-9_]{3,16}):(-?\\d+):(-?\\d+):(-?\\d+)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_PATTERN = Pattern.compile("\\[LC:P:([A-Za-z0-9_]{3,16}):([A-Za-z0-9_]{3,16})]", Pattern.CASE_INSENSITIVE);

    private static final Map<BlockPos, Long> markedBlocks = new HashMap<>();
    private static final Map<String, Long> markedPlayers = new HashMap<>();
    private static long pendingPlayerPressAt;
    private static String pendingPlayerPingName;

    private LegionsPingManager() {
    }

    public static void handlePingKeyPress(MinecraftClient client) {
        if (!canUsePing(client)) {
            clearPendingPlayerPing();
            return;
        }

        long now = System.currentTimeMillis();
        if (pendingPlayerPressAt > 0L && now - pendingPlayerPressAt <= DOUBLE_PRESS_WINDOW_MILLIS) {
            clearPendingPlayerPing();
            BlockPos pos = raycastBlockPos(client);
            if (pos != null) {
                pingBlock(client, pos);
            }
            return;
        }

        flushPendingPlayerPing(client, now);
        pendingPlayerPressAt = now;
        PlayerEntity target = raycastPlayer(client);
        pendingPlayerPingName = target == null ? null : LegionsFeatures.realUsername(target);
    }

    private static void pingPlayer(MinecraftClient client, String target) {
        if (target == null || target.isBlank()) {
            return;
        }
        String sender = LegionsFeatures.realUsername(client.player);
        PlayerEntity targetPlayer = findPlayer(client, target);
        if (targetPlayer != null) {
            String visibleMessage = LegionsFeatures.isTeammate(client.player, targetPlayer)
                    ? target + " needs help!"
                    : "Focus " + target;
            sendChat(client, visibleMessage + " [LC:P:" + sender + ":" + target + "]");
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
            clearPendingPlayerPing();
            return;
        }

        long now = System.currentTimeMillis();
        if (!canUsePing(client)) {
            clearPendingPlayerPing();
        } else {
            flushPendingPlayerPing(client, now);
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
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || client.world == null
                || markedBlocks.isEmpty()) {
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
        renderBlockShapeOutline(client, pos);
        GizmoDrawing.blockLabel(bracketDistanceLabel(client, Vec3d.ofCenter(pos)), pos, 0, PING_LABEL_COLOR, BLOCK_PING_LABEL_SCALE).ignoreOcclusion();

        Entity camera = client.getCameraEntity();
        Vec3d center = Vec3d.ofCenter(pos);
        if (camera != null && camera.squaredDistanceTo(center) > BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED) {
            GizmoDrawing.point(center, BLOCK_PING_MARKER_COLOR, BLOCK_PING_FAR_MARKER_SIZE).ignoreOcclusion();
        }
    }

    private static void renderBlockShapeOutline(MinecraftClient client, BlockPos pos) {
        if (client.world == null) {
            return;
        }

        BlockState state = client.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.absent());
        if (shape.isEmpty()) {
            renderBox(pos, BLOCK_PING_OUTER_EXPAND, BLOCK_PING_INNER_EXPAND);
            return;
        }

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(pos);
            renderBox(box, BLOCK_PING_OUTER_EXPAND, BLOCK_PING_INNER_EXPAND);
        });
    }

    private static void renderBox(BlockPos pos, float outerExpand, float innerExpand) {
        GizmoDrawing.box(pos, outerExpand, DrawStyle.stroked(BLOCK_PING_OUTER_COLOR, BLOCK_PING_OUTER_WIDTH));
        GizmoDrawing.box(pos, innerExpand, DrawStyle.stroked(BLOCK_PING_INNER_COLOR, BLOCK_PING_INNER_WIDTH));
    }

    private static void renderBox(Box box, float outerExpand, float innerExpand) {
        GizmoDrawing.box(box.expand(outerExpand), DrawStyle.stroked(BLOCK_PING_OUTER_COLOR, BLOCK_PING_OUTER_WIDTH));
        GizmoDrawing.box(box.expand(innerExpand), DrawStyle.stroked(BLOCK_PING_INNER_COLOR, BLOCK_PING_INNER_WIDTH));
    }

    private static String bracketDistanceLabel(MinecraftClient client, Vec3d pos) {
        return "[" + distanceLabel(client, pos) + "]";
    }

    private static String distanceLabel(MinecraftClient client, Vec3d pos) {
        Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        if (camera == null || pos == null) {
            return "?m";
        }
        return Math.round(camera.getCameraPosVec(1.0f).distanceTo(pos)) + "m";
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

    private static void flushPendingPlayerPing(MinecraftClient client, long now) {
        if (pendingPlayerPressAt <= 0L || now - pendingPlayerPressAt < DOUBLE_PRESS_WINDOW_MILLIS) {
            return;
        }

        String target = pendingPlayerPingName;
        clearPendingPlayerPing();
        if (target != null && canUsePing(client)) {
            pingPlayer(client, target);
        }
    }

    private static void clearPendingPlayerPing() {
        pendingPlayerPressAt = 0L;
        pendingPlayerPingName = null;
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
        UUID localUuid = client.player == null ? null : client.player.getUuid();
        return localUuid != null && localUuid.equals(player.getUuid()) ? null : player;
    }
}
