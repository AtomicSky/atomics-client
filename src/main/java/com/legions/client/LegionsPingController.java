package com.legions.client;

import com.legions.client.config.LegionsConfig;
import com.legions.client.config.LegionsConfig.PingRow;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.util.InputUtil;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsPingController {
    private static final double RANGE = 100.0;
    private static final double PLAYER_PING_RAY_RADIUS = 3.0;
    private static final double PLAYER_PING_RAY_RADIUS_SQUARED =
            PLAYER_PING_RAY_RADIUS * PLAYER_PING_RAY_RADIUS;
    private static final long PRESS_WINDOW_MILLIS = 350L;
    private static final float BLOCK_PING_EXPAND = 0.02f;
    private static final float BLOCK_PING_STROKE_WIDTH = 2.25f;
    private static final float BLOCK_PING_FAR_MARKER_SIZE = 7.0f;
    private static final double BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED = 40.0 * 40.0;
    private static final int DEFAULT_PING_COLOR = 0xFFFFD84A;
    private static final float BLOCK_PING_LABEL_SCALE = 0.65f;
    private static final Pattern MACHINE_PAYLOAD_PATTERN = Pattern.compile("\\[LC:([PB]):([^\\]]+)]", Pattern.CASE_INSENSITIVE);

    private static final Map<BlockPos, PingMark> markedBlocks = new HashMap<>();
    private static final Map<String, PingMark> markedPlayers = new HashMap<>();
    private static final Map<PingKey, Boolean> keyDownStates = new HashMap<>();
    private static PendingPress pendingPress;
    private static UUID lastAttackedPlayerUuid;
    private static String lastAttackedPlayerName;
    private static long lastAttackedPlayerAt;
    private static UUID lastAttackerPlayerUuid;
    private static String lastAttackerPlayerName;
    private static long lastAttackerPlayerAt;

    private LegionsPingController() {
    }

    public static void recordAttackedEntity(MinecraftClient client, Entity target) {
        if (!(target instanceof PlayerEntity player) || client == null || client.player == null
                || player.getUuid().equals(client.player.getUuid())) {
            return;
        }

        lastAttackedPlayerUuid = player.getUuid();
        lastAttackedPlayerName = LegionsFeatures.realUsername(player);
        lastAttackedPlayerAt = System.currentTimeMillis();
    }

    public static void recordAttacker(MinecraftClient client, Entity attacker) {
        if (!(attacker instanceof PlayerEntity player) || client == null || client.player == null
                || player.getUuid().equals(client.player.getUuid())) {
            return;
        }

        lastAttackerPlayerUuid = player.getUuid();
        lastAttackerPlayerName = LegionsFeatures.realUsername(player);
        lastAttackerPlayerAt = System.currentTimeMillis();
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            markedBlocks.clear();
            markedPlayers.clear();
            clearInputState();
            clearRecentTargets();
            return;
        }

        long now = System.currentTimeMillis();
        if (!canUsePing(client)) {
            clearInputState();
        } else {
            List<PingRow> rows = configuredRows();
            flushPendingPress(client, rows, now);
            pollConfiguredKeys(client, rows, now);
            flushPendingPress(client, rows, now);
        }

        long ttl = pingTtlMillis();
        markedBlocks.entrySet().removeIf(entry -> now - entry.getValue().markedAt() > ttl);
        markedPlayers.entrySet().removeIf(entry -> now - entry.getValue().markedAt() > ttl);
    }

    public static void receiveChatPing(Text message, GameProfile senderProfile) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || message == null) {
            return;
        }

        String raw = message.getString();
        Matcher matcher = MACHINE_PAYLOAD_PATTERN.matcher(raw);
        while (matcher.find()) {
            ReceivedPing ping = parseReceivedPing(matcher.group(1), matcher.group(2));
            if (ping != null && canAcceptFrom(client, senderProfile, ping.sender(), ping.audience())) {
                if (ping.playerTarget() != null) {
                    markPlayer(ping.playerTarget(), ping.color());
                } else if (ping.blockPos() != null) {
                    markBlock(ping.blockPos(), ping.color());
                }
            }
        }
    }

    public static boolean shouldCleanReceivedPingText(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        return LegionsClient.enabled(client)
                && LegionsClient.CONFIG.teamPingEnabled
                && hasMachinePayload(message);
    }

    public static boolean hasMachinePayload(Text message) {
        return message != null && MACHINE_PAYLOAD_PATTERN.matcher(message.getString()).find();
    }

    public static Text cleanReceivedPingText(Text message) {
        if (message == null) {
            return Text.empty();
        }
        return stripMachinePayload(message);
    }

    public static boolean isMarkedPlayer(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return markedPlayers.containsKey(LegionsFeatures.realUsername(player).toLowerCase(Locale.ROOT));
    }

    public static int markedPlayerColor(PlayerEntity player) {
        if (player == null) {
            return DEFAULT_PING_COLOR;
        }
        PingMark mark = markedPlayers.get(LegionsFeatures.realUsername(player).toLowerCase(Locale.ROOT));
        return mark == null ? DEFAULT_PING_COLOR : mark.color();
    }

    public static void renderBlockHighlights() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.teamPingEnabled || client.world == null
                || markedBlocks.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long ttl = pingTtlMillis();
        for (Map.Entry<BlockPos, PingMark> entry : markedBlocks.entrySet()) {
            if (now - entry.getValue().markedAt() > ttl) {
                continue;
            }
            renderBlockPingOutline(client, entry.getKey(), entry.getValue().color());
        }
    }

    private static void pollConfiguredKeys(MinecraftClient client, List<PingRow> rows, long now) {
        Set<PingKey> configuredKeys = new HashSet<>();
        for (PingRow row : rows) {
            PingKey key = new PingKey(row.keyType, row.keyCode);
            configuredKeys.add(key);
            boolean down = isKeyDown(client, key);
            boolean wasDown = keyDownStates.getOrDefault(key, false);
            if (down && !wasDown) {
                recordPress(client, rows, key, now);
            }
            keyDownStates.put(key, down);
        }
        keyDownStates.keySet().removeIf(key -> !configuredKeys.contains(key));
    }

    private static void recordPress(MinecraftClient client, List<PingRow> rows, PingKey key, long now) {
        boolean continuingPress = pendingPress != null
                && pendingPress.key().equals(key)
                && now - pendingPress.pressedAt() <= PRESS_WINDOW_MILLIS;
        int presses = continuingPress ? pendingPress.presses() + 1 : 1;
        AimSnapshot aim = continuingPress ? pendingPress.aim() : captureAim(client);
        pendingPress = new PendingPress(key, presses, now, aim);

        PingRow row = findRow(rows, key, presses);
        if (row == null && !hasHigherPressRow(rows, key, presses)) {
            pendingPress = null;
            return;
        }

        if (row != null && !hasHigherPressRow(rows, key, presses)) {
            PendingPress press = pendingPress;
            pendingPress = null;
            dispatchRow(client, row, press.aim());
        }
    }

    private static void flushPendingPress(MinecraftClient client, List<PingRow> rows, long now) {
        if (pendingPress == null || now - pendingPress.pressedAt() < PRESS_WINDOW_MILLIS) {
            return;
        }

        PendingPress press = pendingPress;
        pendingPress = null;
        PingRow row = findRow(rows, press.key(), press.presses());
        if (row != null) {
            dispatchRow(client, row, press.aim());
        }
    }

    private static void dispatchRow(MinecraftClient client, PingRow row, AimSnapshot aim) {
        PingTarget target = resolveTarget(client, row, aim);
        if (target == null) {
            return;
        }

        int color = parseColor(row.color);
        if (target.blockPos() != null) {
            pingBlock(client, row, target.blockPos(), color);
        } else if (target.player() != null) {
            pingPlayer(client, row, target.player(), color);
        }
    }

    private static PingTarget resolveTarget(MinecraftClient client, PingRow row, AimSnapshot aim) {
        if (row.targetType == PingRow.TARGET_TYPE_BLOCKS_ONLY) {
            BlockPos pos = aim == null ? null : aim.blockPos();
            if (pos == null) {
                showFeedback(client, "No block targeted");
                return null;
            }
            return PingTarget.block(pos);
        }

        PlayerEntity target = switch (row.targetSource) {
            case PingRow.TARGET_SOURCE_LAST_ATTACKER -> recentPlayer(client, lastAttackerPlayerUuid, lastAttackerPlayerName, lastAttackerPlayerAt);
            case PingRow.TARGET_SOURCE_LAST_ATTACKED -> recentPlayer(client, lastAttackedPlayerUuid, lastAttackedPlayerName, lastAttackedPlayerAt);
            case PingRow.TARGET_SOURCE_SELF -> client.player;
            default -> raycastPlayer(client, row, aim);
        };
        if (target == null || !isAllowedPlayerTarget(client, row, target)) {
            showFeedback(client, "No matching player target");
            return null;
        }
        return PingTarget.player(target);
    }

    private static void pingPlayer(MinecraftClient client, PingRow row, PlayerEntity target, int color) {
        String sender = LegionsFeatures.realUsername(client.player);
        String targetName = LegionsFeatures.realUsername(target);
        String visibleMessage = formatPlayerMessage(client, row, target, sender, targetName);
        sendChat(client, visibleMessage + " [LC:P:" + sender + ":" + targetName
                + ":color=" + hexColor(color) + ":audience=" + audienceName(row.visualAudience) + "]");
        markPlayer(targetName, color);
    }

    private static void pingBlock(MinecraftClient client, PingRow row, BlockPos pos, int color) {
        String sender = LegionsFeatures.realUsername(client.player);
        String visibleMessage = formatBlockMessage(row.message, sender, pos);
        sendChat(client, visibleMessage + " [LC:B:" + sender + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ()
                + ":color=" + hexColor(color) + ":audience=" + audienceName(row.visualAudience) + "]");
        markBlock(pos, color);
    }

    private static String formatPlayerMessage(MinecraftClient client, PingRow row, PlayerEntity target, String sender, String targetName) {
        String template;
        if (row.targetType == PingRow.TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE) {
            template = LegionsFeatures.isTeammate(client.player, target) ? row.teammateMessage : row.enemyMessage;
        } else {
            template = row.message;
        }
        return formatPlayerMessage(template, sender, targetName);
    }

    private static String formatPlayerMessage(String template, String sender, String targetName) {
        return cleanVisibleMessage(template)
                .replace("{sender}", sender)
                .replace("{player}", targetName);
    }

    private static String formatBlockMessage(String template, String sender, BlockPos pos) {
        return cleanVisibleMessage(template)
                .replace("{sender}", sender)
                .replace("{x}", Integer.toString(pos.getX()))
                .replace("{y}", Integer.toString(pos.getY()))
                .replace("{z}", Integer.toString(pos.getZ()));
    }

    private static String cleanVisibleMessage(String template) {
        String message = template == null || template.isBlank() ? "Ping" : template.trim();
        return message.length() > 190 ? message.substring(0, 190) : message;
    }

    private static ReceivedPing parseReceivedPing(String type, String body) {
        String[] parts = body.split(":");
        if ("P".equalsIgnoreCase(type)) {
            if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return null;
            }
            PayloadOptions options = parsePayloadOptions(parts, 2);
            return ReceivedPing.player(parts[0], parts[1], options.color(), options.audience());
        }

        if (parts.length < 4 || parts[0].isBlank()) {
            return null;
        }
        try {
            PayloadOptions options = parsePayloadOptions(parts, 4);
            return ReceivedPing.block(parts[0], new BlockPos(
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            ), options.color(), options.audience());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static PayloadOptions parsePayloadOptions(String[] parts, int start) {
        int color = DEFAULT_PING_COLOR;
        int audience = PingRow.VISUAL_AUDIENCE_TEAMMATES;
        for (int i = start; i < parts.length; i++) {
            String part = parts[i];
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = part.substring(0, equals).toLowerCase(Locale.ROOT);
            String value = part.substring(equals + 1);
            if ("color".equals(key)) {
                color = parseColor(value);
            } else if ("audience".equals(key)) {
                audience = parseAudience(value);
            }
        }
        return new PayloadOptions(color, audience);
    }

    private static boolean canAcceptFrom(MinecraftClient client, GameProfile signedSender, String embeddedSender, int audience) {
        if (client.player == null) {
            return false;
        }
        String senderName = signedSender != null ? signedSender.name() : embeddedSender;
        if (senderName == null || senderName.equalsIgnoreCase(LegionsFeatures.realUsername(client.player))) {
            return true;
        }
        if (LegionsFeatures.isSpectatorTeam(client.player)) {
            return true;
        }
        if (audience == PingRow.VISUAL_AUDIENCE_EVERYONE) {
            return true;
        }

        PlayerEntity sender = findPlayer(client, senderName);
        if (sender == null || LegionsFeatures.isSpectatorTeam(sender)) {
            return false;
        }
        return audience == PingRow.VISUAL_AUDIENCE_OPPONENTS
                ? LegionsFeatures.isOpponent(sender, client.player)
                : LegionsFeatures.isTeammate(sender, client.player);
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
        ArrayList<Range> ranges = new ArrayList<>();
        Matcher matcher = MACHINE_PAYLOAD_PATTERN.matcher(raw);
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
        ranges.sort((first, second) -> Integer.compare(first.start, second.start));
        return mergeRanges(ranges);
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

    private static void renderBlockPingOutline(MinecraftClient client, BlockPos pos, int color) {
        renderBlockShapeOutline(client, pos, color);
        Vec3d center = Vec3d.ofCenter(pos);
        if (LegionsClient.CONFIG.blockPingDistanceLabelEnabled) {
            GizmoDrawing.blockLabel(bracketDistanceLabel(client, center), pos, 0, color, BLOCK_PING_LABEL_SCALE).ignoreOcclusion();
        }

        Entity camera = client.getCameraEntity();
        if (camera != null && camera.squaredDistanceTo(center) > BLOCK_PING_FAR_MARKER_DISTANCE_SQUARED) {
            GizmoDrawing.point(center, color, BLOCK_PING_FAR_MARKER_SIZE).ignoreOcclusion();
        }
    }

    private static void renderBlockShapeOutline(MinecraftClient client, BlockPos pos, int color) {
        if (client.world == null) {
            return;
        }

        BlockState state = client.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.absent());
        if (shape.isEmpty()) {
            renderBox(pos, color);
            return;
        }

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(pos);
            renderBox(box, color);
        });
    }

    private static void renderBox(BlockPos pos, int color) {
        GizmoDrawing.box(pos, BLOCK_PING_EXPAND, blockPingDrawStyle(color)).ignoreOcclusion();
    }

    private static void renderBox(Box box, int color) {
        GizmoDrawing.box(box.expand(BLOCK_PING_EXPAND), blockPingDrawStyle(color)).ignoreOcclusion();
    }

    private static DrawStyle blockPingDrawStyle(int color) {
        return DrawStyle.filledAndStroked(color, BLOCK_PING_STROKE_WIDTH, (color & 0x00FFFFFF) | 0x35000000);
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

    private static void markBlock(BlockPos pos, int color) {
        markedBlocks.put(pos.toImmutable(), new PingMark(System.currentTimeMillis(), opaque(color)));
    }

    private static void markPlayer(String name, int color) {
        if (name != null && !name.isBlank()) {
            markedPlayers.put(name.toLowerCase(Locale.ROOT), new PingMark(System.currentTimeMillis(), opaque(color)));
        }
    }

    private static List<PingRow> configuredRows() {
        if (LegionsClient.CONFIG == null) {
            return LegionsConfig.defaultPingRows();
        }
        return LegionsClient.CONFIG.normalize().pingRows;
    }

    private static PingRow findRow(List<PingRow> rows, PingKey key, int presses) {
        for (PingRow row : rows) {
            if (matches(row, key) && row.presses == presses) {
                return row;
            }
        }
        return null;
    }

    private static boolean hasHigherPressRow(List<PingRow> rows, PingKey key, int presses) {
        for (PingRow row : rows) {
            if (matches(row, key) && row.presses > presses) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(PingRow row, PingKey key) {
        return row.keyType == key.keyType() && row.keyCode == key.keyCode();
    }

    private static boolean isKeyDown(MinecraftClient client, PingKey key) {
        if (client == null || client.getWindow() == null) {
            return false;
        }
        if (key.keyType() == PingRow.KEY_TYPE_MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.keyCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(client.getWindow(), key.keyCode());
    }

    private static void clearInputState() {
        keyDownStates.clear();
        pendingPress = null;
    }

    private static long pingTtlMillis() {
        return Math.max(1, Math.min(25, LegionsClient.CONFIG.pingDurationSeconds)) * 1000L;
    }

    private static long recentTargetTtlMillis() {
        return Math.max(1, Math.min(60, LegionsClient.CONFIG.pingRecentTargetTimeoutSeconds)) * 1000L;
    }

    private static boolean canUsePing(MinecraftClient client) {
        return LegionsClient.enabled(client)
                && LegionsClient.CONFIG.teamPingEnabled
                && client.currentScreen == null
                && client.player != null
                && client.world != null;
    }

    private static PlayerEntity recentPlayer(MinecraftClient client, UUID uuid, String name, long markedAt) {
        if (client.world == null || name == null || System.currentTimeMillis() - markedAt > recentTargetTtlMillis()) {
            return null;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (uuid != null && uuid.equals(player.getUuid()) && isRecentTargetCandidate(client, player)) {
                return player;
            }
        }

        PlayerEntity player = findPlayer(client, name);
        return isRecentTargetCandidate(client, player) ? player : null;
    }

    private static boolean isRecentTargetCandidate(MinecraftClient client, PlayerEntity player) {
        return player != null
                && client.player != null
                && !player.getUuid().equals(client.player.getUuid())
                && player.isAlive()
                && !player.isSpectator();
    }

    private static void clearRecentTargets() {
        lastAttackedPlayerUuid = null;
        lastAttackedPlayerName = null;
        lastAttackedPlayerAt = 0L;
        lastAttackerPlayerUuid = null;
        lastAttackerPlayerName = null;
        lastAttackerPlayerAt = 0L;
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

    private static PlayerEntity findPlayer(MinecraftClient client, UUID uuid, String name) {
        if (client.world == null) {
            return null;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (uuid != null && uuid.equals(player.getUuid())) {
                return player;
            }
        }
        return findPlayer(client, name);
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

    private static AimSnapshot captureAim(MinecraftClient client) {
        return new AimSnapshot(raycastBlockPos(client), raycastPlayerHits(client));
    }

    private static List<PlayerRayHit> raycastPlayerHits(MinecraftClient client) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null || client.player == null) {
            return List.of();
        }
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d look = camera.getRotationVec(1.0f).normalize();
        ArrayList<PlayerRayHit> hits = new ArrayList<>();

        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate.getUuid().equals(client.player.getUuid()) || !isFirstPressRaycastCandidate(candidate)) {
                continue;
            }

            RayDistance distance = closestDistanceToRay(start, look, candidate);
            if (distance.rayDistance() < 0.0 || distance.rayDistance() > RANGE
                    || distance.distanceSquared() > PLAYER_PING_RAY_RADIUS_SQUARED) {
                continue;
            }

            hits.add(new PlayerRayHit(
                    candidate.getUuid(),
                    LegionsFeatures.realUsername(candidate),
                    distance.distanceSquared(),
                    distance.rayDistance()
            ));
        }

        return hits;
    }

    private static PlayerEntity raycastPlayer(MinecraftClient client, PingRow row, AimSnapshot aim) {
        if (aim == null) {
            return null;
        }

        PlayerEntity best = null;
        double bestDistanceSq = PLAYER_PING_RAY_RADIUS_SQUARED;
        double bestRayDistance = Double.POSITIVE_INFINITY;

        for (PlayerRayHit hit : aim.playerHits()) {
            PlayerEntity candidate = findPlayer(client, hit.uuid(), hit.name());
            if (!isRaycastTargetCandidate(client, row, candidate)) {
                continue;
            }

            if (hit.distanceSquared() < bestDistanceSq
                    || hit.distanceSquared() == bestDistanceSq && hit.rayDistance() < bestRayDistance) {
                best = candidate;
                bestDistanceSq = hit.distanceSquared();
                bestRayDistance = hit.rayDistance();
            }
        }

        return best;
    }

    private static boolean isFirstPressRaycastCandidate(PlayerEntity player) {
        return player != null
                && player.isAlive()
                && !player.isSpectator();
    }

    private static boolean isRaycastTargetCandidate(MinecraftClient client, PingRow row, PlayerEntity player) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && isAllowedPlayerTarget(client, row, player);
    }

    private static boolean isAllowedPlayerTarget(MinecraftClient client, PingRow row, PlayerEntity player) {
        if (client.player == null || player == null || LegionsFeatures.isSpectatorTeam(player)) {
            return false;
        }
        return switch (row.targetType) {
            case PingRow.TARGET_TYPE_TEAMMATES_ONLY -> player == client.player || LegionsFeatures.isTeammate(client.player, player);
            case PingRow.TARGET_TYPE_ENEMIES_ONLY -> LegionsFeatures.isOpponent(client.player, player);
            case PingRow.TARGET_TYPE_ALL_PLAYERS_SAME_MESSAGE, PingRow.TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE -> true;
            default -> false;
        };
    }

    private static RayDistance closestDistanceToRay(Vec3d start, Vec3d direction, PlayerEntity player) {
        RayDistance eyeDistance = closestDistanceToRay(start, direction, player.getEyePos());
        RayDistance centerDistance = closestDistanceToRay(start, direction, player.getBoundingBox().getCenter());
        return eyeDistance.distanceSquared() <= centerDistance.distanceSquared() ? eyeDistance : centerDistance;
    }

    private static RayDistance closestDistanceToRay(Vec3d start, Vec3d direction, Vec3d point) {
        Vec3d offset = point.subtract(start);
        double rayDistance = offset.dotProduct(direction);
        Vec3d closest = start.add(direction.multiply(Math.max(0.0, Math.min(RANGE, rayDistance))));
        return new RayDistance(point.squaredDistanceTo(closest), rayDistance);
    }

    private static void sendChat(MinecraftClient client, String message) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatMessage(message);
        }
    }

    private static void showFeedback(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    private static int parseAudience(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("opponents".equals(normalized) || "opponent".equals(normalized) || "enemies".equals(normalized)) {
            return PingRow.VISUAL_AUDIENCE_OPPONENTS;
        }
        if ("all".equals(normalized) || "everyone".equals(normalized)) {
            return PingRow.VISUAL_AUDIENCE_EVERYONE;
        }
        return PingRow.VISUAL_AUDIENCE_TEAMMATES;
    }

    private static String audienceName(int audience) {
        return switch (audience) {
            case PingRow.VISUAL_AUDIENCE_OPPONENTS -> "opponents";
            case PingRow.VISUAL_AUDIENCE_EVERYONE -> "all";
            default -> "team";
        };
    }

    private static int parseColor(String value) {
        if (value == null) {
            return DEFAULT_PING_COLOR;
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        if (!cleaned.matches("[0-9a-f]{6}")) {
            return DEFAULT_PING_COLOR;
        }
        return 0xFF000000 | Integer.parseInt(cleaned, 16);
    }

    private static String hexColor(int color) {
        return String.format(Locale.ROOT, "%06x", color & 0x00FFFFFF);
    }

    private static int opaque(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    private record PingKey(int keyType, int keyCode) {
    }

    private record PendingPress(PingKey key, int presses, long pressedAt, AimSnapshot aim) {
    }

    private record AimSnapshot(BlockPos blockPos, List<PlayerRayHit> playerHits) {
    }

    private record PlayerRayHit(UUID uuid, String name, double distanceSquared, double rayDistance) {
    }

    private record PingMark(long markedAt, int color) {
    }

    private record PingTarget(PlayerEntity player, BlockPos blockPos) {
        private static PingTarget player(PlayerEntity player) {
            return new PingTarget(player, null);
        }

        private static PingTarget block(BlockPos pos) {
            return new PingTarget(null, pos);
        }
    }

    private record ReceivedPing(String sender, String playerTarget, BlockPos blockPos, int color, int audience) {
        private static ReceivedPing player(String sender, String target, int color, int audience) {
            return new ReceivedPing(sender, target, null, color, audience);
        }

        private static ReceivedPing block(String sender, BlockPos pos, int color, int audience) {
            return new ReceivedPing(sender, null, pos, color, audience);
        }
    }

    private record PayloadOptions(int color, int audience) {
    }

    private record Range(int start, int end) {
    }

    private record RayDistance(double distanceSquared, double rayDistance) {
    }
}
