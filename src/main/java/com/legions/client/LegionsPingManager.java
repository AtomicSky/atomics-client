package com.legions.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsPingManager {
    private static final double RANGE = 100.0;
    private static final double PLAYER_PING_RAY_RADIUS = 3.0;
    private static final double PLAYER_PING_RAY_RADIUS_SQUARED =
            PLAYER_PING_RAY_RADIUS * PLAYER_PING_RAY_RADIUS;
    private static final long DOUBLE_PRESS_WINDOW_MILLIS = 300L;
    private static final float BLOCK_PING_EXPAND = 0.02f;
    private static final float BLOCK_PING_STROKE_WIDTH = 2.25f;
    private static final float BLOCK_PING_FAR_MARKER_SIZE = 7.0f;
    private static final double BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED = 40.0 * 40.0;
    private static final int BLOCK_PING_STROKE_COLOR = 0xFFFFD84A;
    private static final int BLOCK_PING_FILL_COLOR = 0x35FFD84A;
    private static final int BLOCK_PING_MARKER_COLOR = 0xFFFFD84A;
    private static final int PING_LABEL_COLOR = 0xFF20FF4A;
    private static final float BLOCK_PING_LABEL_SCALE = 0.65f;
    private static final DrawStyle BLOCK_PING_DRAW_STYLE = DrawStyle.filledAndStroked(BLOCK_PING_STROKE_COLOR, BLOCK_PING_STROKE_WIDTH, BLOCK_PING_FILL_COLOR);
    private static final Pattern BLOCK_PATTERN = Pattern.compile("\\[LC:B:([A-Za-z0-9_]{3,16}):(-?\\d+):(-?\\d+):(-?\\d+)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_PATTERN = Pattern.compile("\\[LC:P:([A-Za-z0-9_]{3,16}):([A-Za-z0-9_]{3,16})]", Pattern.CASE_INSENSITIVE);

    private static final Map<BlockPos, Long> markedBlocks = new HashMap<>();
    private static final Map<String, Long> markedPlayers = new HashMap<>();
    private static long pendingPlayerPressAt;
    private static String pendingPlayerPingName;
    private static BlockPos pendingBlockPingPos;
    private static UUID lastAttackedPlayerUuid;
    private static String lastAttackedPlayerName;

    private LegionsPingManager() {
    }

    public static void handlePingKeyPress(MinecraftClient client) {
        if (!canUsePing(client)) {
            clearPendingPlayerPing();
            return;
        }

        long now = System.currentTimeMillis();
        if (pendingPlayerPressAt > 0L && now - pendingPlayerPressAt <= DOUBLE_PRESS_WINDOW_MILLIS) {
            BlockPos pos = pendingBlockPingPos;
            clearPendingPlayerPing();
            if (pos != null) {
                pingBlock(client, pos);
            }
            return;
        }

        flushPendingPlayerPing(client, now);
        pendingPlayerPressAt = now;
        PlayerEntity target = LegionsClient.CONFIG.pingLastAttackedPlayerEnabled ? lastAttackedPlayer(client) : raycastPlayer(client);
        pendingPlayerPingName = target == null ? null : LegionsFeatures.realUsername(target);
        pendingBlockPingPos = raycastBlockPos(client);
    }

    public static void recordAttackedEntity(MinecraftClient client, Entity target) {
        if (!(target instanceof PlayerEntity player) || client == null || client.player == null
                || player.getUuid().equals(client.player.getUuid())) {
            return;
        }

        lastAttackedPlayerUuid = player.getUuid();
        lastAttackedPlayerName = LegionsFeatures.realUsername(player);
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

    public static boolean shouldCleanReceivedPingText(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        return LegionsClient.enabled(client)
                && LegionsClient.CONFIG.teamPingEnabled
                && hasMachinePayload(message);
    }

    public static boolean hasMachinePayload(Text message) {
        if (message == null) {
            return false;
        }
        String raw = message.getString();
        return PLAYER_PATTERN.matcher(raw).find() || BLOCK_PATTERN.matcher(raw).find();
    }

    public static Text cleanReceivedPingText(Text message) {
        if (message == null) {
            return Text.empty();
        }
        return stripMachinePayload(message);
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            markedBlocks.clear();
            markedPlayers.clear();
            clearPendingPlayerPing();
            clearLastAttackedPlayer();
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
        Vec3d center = Vec3d.ofCenter(pos);
        if (LegionsClient.CONFIG.blockPingDistanceLabelEnabled) {
            GizmoDrawing.blockLabel(bracketDistanceLabel(client, center), pos, 0, PING_LABEL_COLOR, BLOCK_PING_LABEL_SCALE);
        }

        Entity camera = client.getCameraEntity();
        if (camera != null && camera.squaredDistanceTo(center) > BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED) {
            GizmoDrawing.point(center, BLOCK_PING_MARKER_COLOR, BLOCK_PING_FAR_MARKER_SIZE);
        }
    }

    private static void renderBlockShapeOutline(MinecraftClient client, BlockPos pos) {
        if (client.world == null) {
            return;
        }

        BlockState state = client.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.absent());
        if (shape.isEmpty()) {
            renderBox(pos);
            return;
        }

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(pos);
            renderBox(box);
        });
    }

    private static void renderBox(BlockPos pos) {
        GizmoDrawing.box(pos, BLOCK_PING_EXPAND, blockPingDrawStyle());
    }

    private static void renderBox(Box box) {
        GizmoDrawing.box(box.expand(BLOCK_PING_EXPAND), blockPingDrawStyle());
    }

    private static DrawStyle blockPingDrawStyle() {
        return BLOCK_PING_DRAW_STYLE;
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

    private static Text stripMachinePayload(Text message) {
        String raw = message.getString();
        List<Range> ranges = machinePayloadRanges(raw);
        if (ranges.isEmpty()) {
            return message.copy();
        }

        MutableText clean = Text.empty();
        int[] cursor = new int[]{0};
        message.visit((style, text) -> {
            appendCleanSegment(clean, text, style, cursor[0], ranges);
            cursor[0] += text.length();
            return Optional.empty();
        }, Style.EMPTY);
        return clean;
    }

    private static void appendCleanSegment(MutableText clean, String text, Style style, int segmentStart, List<Range> ranges) {
        int segmentEnd = segmentStart + text.length();
        int cursor = 0;
        for (Range range : ranges) {
            if (range.end <= segmentStart) {
                continue;
            }
            if (range.start >= segmentEnd) {
                break;
            }

            int localStart = Math.max(0, range.start - segmentStart);
            int localEnd = Math.min(text.length(), range.end - segmentStart);
            if (localStart > cursor) {
                clean.append(Text.literal(text.substring(cursor, localStart)).setStyle(style));
            }
            cursor = Math.max(cursor, localEnd);
        }
        if (cursor < text.length()) {
            clean.append(Text.literal(text.substring(cursor)).setStyle(style));
        }
    }

    private static List<Range> machinePayloadRanges(String raw) {
        List<Range> ranges = new ArrayList<>();
        addPayloadRanges(ranges, raw, PLAYER_PATTERN.matcher(raw));
        addPayloadRanges(ranges, raw, BLOCK_PATTERN.matcher(raw));
        ranges.sort((first, second) -> Integer.compare(first.start, second.start));
        return mergeRanges(ranges);
    }

    private static void addPayloadRanges(List<Range> ranges, String raw, Matcher matcher) {
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start > 0 && Character.isWhitespace(raw.charAt(start - 1))) {
                start--;
            }
            while (end < raw.length() && Character.isWhitespace(raw.charAt(end))) {
                end++;
            }
            ranges.add(new Range(start, end));
        }
    }

    private static List<Range> mergeRanges(List<Range> ranges) {
        if (ranges.size() < 2) {
            return ranges;
        }

        List<Range> merged = new ArrayList<>();
        Range current = ranges.getFirst();
        for (int i = 1; i < ranges.size(); i++) {
            Range next = ranges.get(i);
            if (next.start <= current.end) {
                current = new Range(current.start, Math.max(current.end, next.end));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private record Range(int start, int end) {
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
        pendingBlockPingPos = null;
    }

    private static long pingTtlMillis() {
        return Math.max(1, Math.min(25, LegionsClient.CONFIG.pingDurationSeconds)) * 1000L;
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

    private static PlayerEntity lastAttackedPlayer(MinecraftClient client) {
        if (client.world == null || lastAttackedPlayerName == null) {
            return null;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (lastAttackedPlayerUuid != null && lastAttackedPlayerUuid.equals(player.getUuid()) && isPlayerPingCandidate(client, player)) {
                return player;
            }
        }

        PlayerEntity player = findPlayer(client, lastAttackedPlayerName);
        return isPlayerPingCandidate(client, player) ? player : null;
    }

    private static void clearLastAttackedPlayer() {
        lastAttackedPlayerUuid = null;
        lastAttackedPlayerName = null;
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
        Vec3d look = camera.getRotationVec(1.0f).normalize();
        PlayerEntity best = null;
        double bestDistanceSq = PLAYER_PING_RAY_RADIUS_SQUARED;
        double bestRayDistance = Double.POSITIVE_INFINITY;

        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (!isPlayerPingCandidate(client, candidate)) {
                continue;
            }

            RayDistance distance = closestDistanceToRay(start, look, candidate);
            if (distance.distanceSquared() > PLAYER_PING_RAY_RADIUS_SQUARED) {
                continue;
            }

            if (distance.distanceSquared() < bestDistanceSq
                    || distance.distanceSquared() == bestDistanceSq && distance.rayDistance() < bestRayDistance) {
                best = candidate;
                bestDistanceSq = distance.distanceSquared();
                bestRayDistance = distance.rayDistance();
            }
        }

        return best;
    }

    private static boolean isPlayerPingCandidate(MinecraftClient client, PlayerEntity player) {
        return player != null
                && client.player != null
                && !player.getUuid().equals(client.player.getUuid())
                && player.isAlive()
                && !player.isSpectator();
    }

    private static RayDistance closestDistanceToRay(Vec3d start, Vec3d direction, PlayerEntity player) {
        RayDistance eyeDistance = closestDistanceToRay(start, direction, player.getEyePos());
        RayDistance centerDistance = closestDistanceToRay(start, direction, player.getBoundingBox().getCenter());
        return eyeDistance.distanceSquared() <= centerDistance.distanceSquared() ? eyeDistance : centerDistance;
    }

    private static RayDistance closestDistanceToRay(Vec3d start, Vec3d direction, Vec3d point) {
        Vec3d offset = point.subtract(start);
        double rayDistance = Math.max(0.0, Math.min(RANGE, offset.dotProduct(direction)));
        Vec3d closest = start.add(direction.multiply(rayDistance));
        return new RayDistance(point.squaredDistanceTo(closest), rayDistance);
    }

    private record RayDistance(double distanceSquared, double rayDistance) {
    }
}
