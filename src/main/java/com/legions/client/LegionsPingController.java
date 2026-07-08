package com.legions.client;

import com.legions.client.config.LegionsConfig;
import com.legions.client.config.LegionsConfig.PingRow;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
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
    private static final float FIGHT_MARKER_SIZE = 8.0f;
    private static final int ARROW_SCREEN_MARGIN = 18;
    private static final int ARROW_STACK_DISTANCE = 22;
    private static final int ARROW_STACK_OFFSET = 10;
    private static final int FIGHT_DETECTION_RADIUS = 24;
    private static final int FIGHT_MIN_PLAYERS = 3;
    private static final int FIGHT_MIN_TEAMS = 2;
    private static final int FIGHT_REFRESH_TICKS = 5;
    private static final long FIGHT_MARKER_MIN_MOVE_MILLIS = 250L;
    private static final long FIGHT_MARKER_MAX_MOVE_MILLIS = 1200L;
    private static final int PING_LABEL_MAX_LENGTH = 14;
    private static final Pattern MACHINE_PAYLOAD_PATTERN = Pattern.compile("\\[LC:([PB]):([^\\]]+)]", Pattern.CASE_INSENSITIVE);

    private static final Map<BlockPos, PingMark> markedBlocks = new HashMap<>();
    private static final Map<String, PingMark> markedPlayers = new HashMap<>();
    private static final Map<PingKey, Boolean> keyDownStates = new HashMap<>();
    private static final ArrayList<FightMark> fightMarkers = new ArrayList<>();
    private static final ArrayList<PlayerFightNode> fightPlayerScratch = new ArrayList<>();
    private static final ArrayList<FightCandidate> fightCandidateScratch = new ArrayList<>();
    private static PendingPress pendingPress;
    private static long lastFightDetectionAt;
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
            fightMarkers.clear();
            clearInputState();
            clearRecentTargets();
            lastFightDetectionAt = 0L;
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
        updateTeamFightMarkers(client, now);

        long ttl = pingTtlMillis();
        markedBlocks.entrySet().removeIf(entry -> now - entry.getValue().markedAt() > ttl);
        markedPlayers.entrySet().removeIf(entry -> now - entry.getValue().markedAt() > ttl);
        fightMarkers.removeIf(mark -> fightMarkerOpacity(mark, now) <= 0);
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
                    markPlayer(ping.playerTarget(), ping.color(), ping.icon(), ping.label());
                } else if (ping.blockPos() != null) {
                    markBlock(ping.blockPos(), ping.color(), ping.icon(), ping.label());
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
        if (!LegionsClient.enabled(client) || client.world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long ttl = pingTtlMillis();
        if (LegionsClient.CONFIG.teamPingEnabled) {
            for (Map.Entry<BlockPos, PingMark> entry : markedBlocks.entrySet()) {
                if (now - entry.getValue().markedAt() > ttl) {
                    continue;
                }
                renderBlockPingOutline(client, entry.getKey(), entry.getValue().color());
            }
        }

        int fightColor = parseColor(LegionsClient.CONFIG.teamFightMarkerColor);
        for (FightMark mark : fightMarkers) {
            int opacity = fightMarkerOpacity(mark, now);
            if (opacity > 0) {
                renderFightMarker(client, mark, fightColor, opacity, now);
            }
        }
    }

    public static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client)
                || !LegionsClient.CONFIG.offscreenPingArrowsEnabled
                || client.world == null
                || client.player == null) {
            return;
        }

        ArrayList<ActiveMarker> markers = activeMarkers(client, System.currentTimeMillis());
        if (!markers.isEmpty()) {
            renderOffscreenArrows(context, client, markers);
        }
    }

    private static ArrayList<ActiveMarker> activeMarkers(MinecraftClient client, long now) {
        ArrayList<ActiveMarker> markers = new ArrayList<>();
        long pingTtl = pingTtlMillis();
        if (LegionsClient.CONFIG.teamPingEnabled) {
            for (Map.Entry<BlockPos, PingMark> entry : markedBlocks.entrySet()) {
                if (now - entry.getValue().markedAt() <= pingTtl) {
                    PingMark mark = entry.getValue();
                    markers.add(new ActiveMarker(Vec3d.ofCenter(entry.getKey()), mark.color(), mark.icon(), mark.label(), 100));
                }
            }
            for (Map.Entry<String, PingMark> entry : markedPlayers.entrySet()) {
                if (now - entry.getValue().markedAt() > pingTtl) {
                    continue;
                }
                PlayerEntity player = findPlayer(client, entry.getKey());
                if (player != null) {
                    if (client.player != null && player.getUuid().equals(client.player.getUuid())) {
                        continue;
                    }
                    PingMark mark = entry.getValue();
                    markers.add(new ActiveMarker(player.getBoundingBox().getCenter(), mark.color(), mark.icon(), mark.label(), 100));
                }
            }
        }

        int fightColor = parseColor(LegionsClient.CONFIG.teamFightMarkerColor);
        for (FightMark mark : fightMarkers) {
            int opacity = fightMarkerOpacity(mark, now);
            if (opacity > 0) {
                markers.add(new ActiveMarker(fightMarkerPosition(client, mark, now), fightColor, PingRow.ICON_FIRE, "Team Fight", opacity));
            }
        }
        return markers;
    }

    private static void renderOffscreenArrows(DrawContext context, MinecraftClient client, List<ActiveMarker> markers) {
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        ArrayList<ArrowPlacement> placements = new ArrayList<>();
        for (ActiveMarker marker : markers) {
            ScreenProjection projection = projectMarkerToScreenEdge(client, marker.pos(), screenWidth, screenHeight);
            if (projection == null || projection.visible()) {
                continue;
            }

            ArrowPlacement placement = stackedArrowPlacement(projection, placements, screenWidth, screenHeight);
            placements.add(placement);
            drawArrow(context, client, marker, placement);
        }
    }

    private static ScreenProjection projectMarkerToScreenEdge(MinecraftClient client, Vec3d pos, int screenWidth, int screenHeight) {
        Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        if (camera == null) {
            return null;
        }

        Vec3d cameraPos = camera.getCameraPosVec(1.0f);
        Vec3d offset = pos.subtract(cameraPos);
        if (offset.lengthSquared() < 0.0001) {
            return new ScreenProjection(true, screenWidth / 2.0, screenHeight / 2.0, 0.0, -1.0);
        }

        Vec3d look = camera.getRotationVec(1.0f).normalize();
        double yawRadians = Math.toRadians(camera.getYaw(1.0f));
        Vec3d right = new Vec3d(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians)).normalize();
        Vec3d up = right.crossProduct(look).normalize();

        double horizontal = offset.dotProduct(right);
        double vertical = offset.dotProduct(up);
        double forward = offset.dotProduct(look);
        double aspect = screenHeight <= 0 ? 1.0 : screenWidth / (double) screenHeight;
        double fov = client.options == null ? 70.0 : client.options.getFov().getValue();
        double verticalTan = Math.tan(Math.toRadians(clampDouble(fov, 30.0, 120.0) * 0.5));
        double horizontalTan = verticalTan * aspect;
        double screenX;
        double screenY;
        if (forward > 0.01) {
            screenX = horizontal / (forward * horizontalTan);
            screenY = -vertical / (forward * verticalTan);
        } else {
            double divisor = Math.max(1.0, Math.abs(forward));
            screenX = horizontal / (divisor * horizontalTan);
            screenY = -vertical / (divisor * verticalTan);
        }

        boolean visible = forward > 0.01 && Math.abs(screenX) <= 1.0 && Math.abs(screenY) <= 1.0;
        if (visible) {
            return new ScreenProjection(true, screenWidth / 2.0 + screenX * screenWidth / 2.0,
                    screenHeight / 2.0 + screenY * screenHeight / 2.0, screenX, screenY);
        }

        if (Math.abs(screenX) < 0.001 && Math.abs(screenY) < 0.001) {
            screenY = 1.0;
        }
        double scale = 1.0 / Math.max(Math.abs(screenX), Math.abs(screenY));
        double edgeX = screenX * scale;
        double edgeY = screenY * scale;
        int margin = Math.round(ARROW_SCREEN_MARGIN * arrowScale());
        double x = screenWidth / 2.0 + edgeX * Math.max(0.0, screenWidth / 2.0 - margin);
        double y = screenHeight / 2.0 + edgeY * Math.max(0.0, screenHeight / 2.0 - margin);
        double dirX = x - screenWidth / 2.0;
        double dirY = y - screenHeight / 2.0;
        double length = Math.sqrt(dirX * dirX + dirY * dirY);
        if (length < 0.001) {
            dirX = 0.0;
            dirY = 1.0;
        } else {
            dirX /= length;
            dirY /= length;
        }
        return new ScreenProjection(false, x, y, dirX, dirY);
    }

    private static ArrowPlacement stackedArrowPlacement(ScreenProjection projection, List<ArrowPlacement> placements,
                                                       int screenWidth, int screenHeight) {
        double x = projection.x();
        double y = projection.y();
        int overlap = 0;
        double stackDistanceSquared = Math.pow(ARROW_STACK_DISTANCE * arrowScale(), 2.0);
        for (ArrowPlacement placement : placements) {
            double dx = placement.x() - x;
            double dy = placement.y() - y;
            if (dx * dx + dy * dy < stackDistanceSquared) {
                overlap++;
            }
        }

        if (overlap > 0) {
            double side = overlap % 2 == 0 ? -1.0 : 1.0;
            double offset = side * (ARROW_STACK_OFFSET * arrowScale()) * ((overlap + 1) / 2.0);
            x += -projection.dirY() * offset;
            y += projection.dirX() * offset;
        }

        int margin = Math.round(ARROW_SCREEN_MARGIN * arrowScale());
        x = clampDouble(x, margin, Math.max(margin, screenWidth - margin));
        y = clampDouble(y, margin, Math.max(margin, screenHeight - margin));
        return new ArrowPlacement(x, y, projection.dirX(), projection.dirY());
    }

    private static void drawArrow(DrawContext context, MinecraftClient client, ActiveMarker marker, ArrowPlacement placement) {
        float scale = arrowScale();
        int opacity = arrowOpacity(client, marker);
        int color = applyOpacity(marker.color(), opacity);
        int fadeColor = applyOpacity(marker.color(), Math.max(10, opacity / 2));
        int shadowColor = applyOpacity(0xFF000000, Math.max(18, opacity / 2));
        double headLength = Math.max(10.0, 14.0 * scale);
        double headWidth = Math.max(8.0, 12.0 * scale);
        int tail = Math.max(2, Math.round(3.0f * scale));
        int x = (int) Math.round(placement.x());
        int y = (int) Math.round(placement.y());
        double perpX = -placement.dirY();
        double perpY = placement.dirX();
        double baseX = x - placement.dirX() * headLength;
        double baseY = y - placement.dirY() * headLength;

        drawFilledTriangle(context,
                x + 1.0, y + 1.0,
                baseX + perpX * headWidth * 0.5 + 1.0, baseY + perpY * headWidth * 0.5 + 1.0,
                baseX - perpX * headWidth * 0.5 + 1.0, baseY - perpY * headWidth * 0.5 + 1.0,
                shadowColor);
        drawFilledTriangle(context,
                x, y,
                baseX + perpX * headWidth * 0.5, baseY + perpY * headWidth * 0.5,
                baseX - perpX * headWidth * 0.5, baseY - perpY * headWidth * 0.5,
                color);
        for (int i = 1; i <= 3; i++) {
            int tailX = (int) Math.round(baseX - placement.dirX() * tail * 3.0 * i);
            int tailY = (int) Math.round(baseY - placement.dirY() * tail * 3.0 * i);
            context.fill(tailX - tail / 2, tailY - tail / 2, tailX + tail / 2 + 1, tailY + tail / 2 + 1, fadeColor);
        }

        drawArrowIcon(context, client.textRenderer, iconSymbol(marker.icon()),
                (int) Math.round(x - placement.dirX() * headLength * 2.25),
                (int) Math.round(y - placement.dirY() * headLength * 2.25),
                color,
                Math.max(1.0f, 1.35f * scale));
    }

    private static void drawArrowIcon(DrawContext context, TextRenderer renderer, String icon, int centerX, int centerY,
                                      int color, float iconScale) {
        int width = renderer.getWidth(icon);
        int height = renderer.fontHeight;
        int x = (int) Math.round(centerX - width * iconScale / 2.0f);
        int y = (int) Math.round(centerY - height * iconScale / 2.0f);
        x = clamp(x, 2, Math.max(2, context.getScaledWindowWidth() - (int) Math.ceil(width * iconScale) - 2));
        y = clamp(y, 2, Math.max(2, context.getScaledWindowHeight() - (int) Math.ceil(height * iconScale) - 2));

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(iconScale, iconScale);
        context.drawTextWithShadow(renderer, icon, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    private static int arrowOpacity(MinecraftClient client, ActiveMarker marker) {
        int minOpacity = Math.max(10, Math.min(100, LegionsClient.CONFIG.offscreenPingArrowMinOpacity));
        int maxOpacity = Math.max(minOpacity, Math.min(100, LegionsClient.CONFIG.offscreenPingArrowMaxOpacity));
        int opacity = maxOpacity;
        if (LegionsClient.CONFIG.offscreenPingArrowDistanceFadeEnabled) {
            Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
            double distance = camera == null ? 0.0 : camera.getCameraPosVec(1.0f).distanceTo(marker.pos());
            double fadeDistance = Math.max(96.0, client.options == null ? 160.0 : client.options.getViewDistance().getValue() * 16.0);
            double progress = clampDouble(distance / fadeDistance, 0.0, 1.0);
            opacity = (int) Math.round(maxOpacity - (maxOpacity - minOpacity) * progress);
        }
        return Math.max(0, Math.min(100, opacity * marker.opacity() / 100));
    }

    private static void drawFilledTriangle(DrawContext context,
                                           double x1, double y1,
                                           double x2, double y2,
                                           double x3, double y3,
                                           int color) {
        int minY = (int) Math.floor(Math.min(y1, Math.min(y2, y3)));
        int maxY = (int) Math.ceil(Math.max(y1, Math.max(y2, y3)));
        double[] intersections = new double[3];
        for (int y = minY; y <= maxY; y++) {
            double scanY = y + 0.5;
            int count = 0;
            count = addTriangleIntersection(intersections, count, x1, y1, x2, y2, scanY);
            count = addTriangleIntersection(intersections, count, x2, y2, x3, y3, scanY);
            count = addTriangleIntersection(intersections, count, x3, y3, x1, y1, scanY);
            if (count < 2) {
                continue;
            }
            sort(intersections, count);
            int left = (int) Math.floor(intersections[0]);
            int right = (int) Math.ceil(intersections[count - 1]);
            if (right >= left) {
                context.fill(left, y, right + 1, y + 1, color);
            }
        }
    }

    private static int addTriangleIntersection(double[] intersections, int count,
                                               double x1, double y1,
                                               double x2, double y2,
                                               double scanY) {
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);
        if (scanY < minY || scanY >= maxY || y1 == y2) {
            return count;
        }
        double progress = (scanY - y1) / (y2 - y1);
        intersections[count] = x1 + (x2 - x1) * progress;
        return count + 1;
    }

    private static void sort(double[] values, int count) {
        for (int i = 1; i < count; i++) {
            double value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value) {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    private static float arrowScale() {
        return Math.max(50, Math.min(200, LegionsClient.CONFIG.offscreenPingArrowScale)) / 100.0f;
    }

    private static int applyOpacity(int color, int opacityPercent) {
        int alpha = Math.max(0, Math.min(100, opacityPercent)) * 255 / 100;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void updateTeamFightMarkers(MinecraftClient client, long now) {
        if (!canUseTeamFightDetector(client)) {
            fightMarkers.clear();
            fightPlayerScratch.clear();
            fightCandidateScratch.clear();
            lastFightDetectionAt = 0L;
            return;
        }

        long refreshMillis = FIGHT_REFRESH_TICKS * 50L;
        if (lastFightDetectionAt > 0L && now - lastFightDetectionAt < refreshMillis) {
            return;
        }
        lastFightDetectionAt = now;

        collectFightCandidates(client);
        refreshFightMarkers(client, now);
    }

    private static boolean canUseTeamFightDetector(MinecraftClient client) {
        return LegionsClient.enabled(client)
                && LegionsClient.CONFIG.teamFightDetectorEnabled
                && client.world != null
                && client.player != null
                && (!LegionsClient.CONFIG.teamFightDetectorSpectatorOnly || LegionsFeatures.isSpectatorTeam(client.player));
    }

    private static void collectFightCandidates(MinecraftClient client) {
        fightPlayerScratch.clear();
        fightCandidateScratch.clear();
        Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();

        for (PlayerEntity player : client.world.getPlayers()) {
            PlayerFightNode node = fightNode(client, player);
            if (node == null) {
                continue;
            }
            fightPlayerScratch.add(node);
        }

        int count = fightPlayerScratch.size();
        if (count < FIGHT_MIN_PLAYERS) {
            return;
        }

        boolean[] visited = new boolean[count];
        double radiusSquared = (double) FIGHT_DETECTION_RADIUS * FIGHT_DETECTION_RADIUS;
        ArrayList<Integer> queue = new ArrayList<>();
        ArrayList<Integer> cluster = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (visited[i]) {
                continue;
            }

            queue.clear();
            cluster.clear();
            visited[i] = true;
            queue.add(i);
            for (int q = 0; q < queue.size(); q++) {
                int currentIndex = queue.get(q);
                cluster.add(currentIndex);
                Vec3d currentPos = fightPlayerScratch.get(currentIndex).pos();
                for (int otherIndex = 0; otherIndex < count; otherIndex++) {
                    if (!visited[otherIndex]
                            && currentPos.squaredDistanceTo(fightPlayerScratch.get(otherIndex).pos()) <= radiusSquared) {
                        visited[otherIndex] = true;
                        queue.add(otherIndex);
                    }
                }
            }
            maybeAddFightCandidate(client, cluster);
        }

        fightCandidateScratch.sort((first, second) -> {
            int players = Integer.compare(second.players(), first.players());
            if (players != 0) {
                return players;
            }
            int teams = Integer.compare(second.teams(), first.teams());
            if (teams != 0) {
                return teams;
            }
            return Double.compare(first.distanceSquared(), second.distanceSquared());
        });
    }

    private static PlayerFightNode fightNode(MinecraftClient client, PlayerEntity player) {
        if (player == null
                || !player.isAlive()
                || player.isSpectator()
                || LegionsFeatures.isSpectatorTeam(player)) {
            return null;
        }

        Team team = player.getScoreboardTeam();
        if (team == null || team.getName() == null || team.getName().isBlank()) {
            return null;
        }

        Vec3d pos = player.getBoundingBox().getCenter();
        return new PlayerFightNode(team.getName(), pos);
    }

    private static void maybeAddFightCandidate(MinecraftClient client, List<Integer> cluster) {
        if (cluster.size() < FIGHT_MIN_PLAYERS) {
            return;
        }

        HashSet<String> teams = new HashSet<>();
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        double minY = Double.POSITIVE_INFINITY;
        for (int index : cluster) {
            PlayerFightNode node = fightPlayerScratch.get(index);
            teams.add(node.team());
            x += node.pos().x;
            y += node.pos().y;
            z += node.pos().z;
            minY = Math.min(minY, node.pos().y);
        }

        if (teams.size() < FIGHT_MIN_TEAMS) {
            return;
        }

        double averageY = y / cluster.size();
        Vec3d center = groundedFightPosition(client,
                new Vec3d(x / cluster.size(), averageY, z / cluster.size()),
                minY,
                averageY);
        Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        double distanceSquared = camera == null ? 0.0 : camera.getCameraPosVec(1.0f).squaredDistanceTo(center);
        fightCandidateScratch.add(new FightCandidate(center, averageY, cluster.size(), teams.size(), distanceSquared));
    }

    private static void refreshFightMarkers(MinecraftClient client, long now) {
        int maxMarkers = Math.max(1, LegionsClient.CONFIG.teamFightMaxMarkers);
        int limit = Math.min(maxMarkers, fightCandidateScratch.size());
        ArrayList<FightMark> next = new ArrayList<>();
        boolean[] usedOldMarkers = new boolean[fightMarkers.size()];
        double matchDistanceSquared = Math.pow(FIGHT_DETECTION_RADIUS * 4.0, 2.0);

        for (int i = 0; i < limit; i++) {
            FightCandidate candidate = fightCandidateScratch.get(i);
            int match = nearestFightMarker(client, candidate.center(), usedOldMarkers, matchDistanceSquared, now);
            if (match >= 0) {
                usedOldMarkers[match] = true;
                FightMark old = fightMarkers.get(match);
                next.add(updatedFightMarker(client, old, candidate, now));
            } else {
                Vec3d target = candidate.center();
                next.add(new FightMark(target, target, candidate.averageY(),
                        now, 0L, now, 0L, candidate.players(), candidate.teams()));
            }
        }

        for (int i = 0; i < fightMarkers.size() && next.size() < maxMarkers; i++) {
            FightMark old = fightMarkers.get(i);
            if (!usedOldMarkers[i]) {
                long fadeStartedAt = old.fadeStartedAt() > 0L ? old.fadeStartedAt() : now;
                Vec3d current = fightMarkerPosition(client, old, now);
                FightMark fading = new FightMark(current, current, old.averageY(),
                        old.markedAt(), fadeStartedAt, now, 0L, old.players(), old.teams());
                if (fightMarkerOpacity(fading, now) > 0) {
                    next.add(fading);
                }
            }
        }

        fightMarkers.clear();
        fightMarkers.addAll(next);
    }

    private static FightMark updatedFightMarker(MinecraftClient client, FightMark old, FightCandidate candidate, long now) {
        Vec3d target = candidate.center();
        if (!LegionsClient.CONFIG.teamFightSmoothingEnabled) {
            return new FightMark(target, target, candidate.averageY(),
                    now, 0L, now, 0L, candidate.players(), candidate.teams());
        }

        if (old.targetPos().squaredDistanceTo(target) < 0.04) {
            return new FightMark(old.fromPos(), old.targetPos(), candidate.averageY(),
                    now, 0L, old.moveStartedAt(), old.moveDurationMillis(), candidate.players(), candidate.teams());
        }

        Vec3d current = fightMarkerPosition(client, old, now);
        return new FightMark(current, target, candidate.averageY(),
                now, 0L, now, fightMarkerMoveMillis(), candidate.players(), candidate.teams());
    }

    private static Vec3d fightMarkerPosition(MinecraftClient client, FightMark mark, long now) {
        Vec3d raw = mark.rawPos(now);
        return groundedFightPosition(client, raw,
                Math.min(raw.y, mark.averageY()) - FIGHT_DETECTION_RADIUS * 3.0,
                mark.averageY());
    }

    private static long fightMarkerMoveMillis() {
        if (!LegionsClient.CONFIG.teamFightSmoothingEnabled) {
            return 0L;
        }
        int strength = Math.max(5, Math.min(100, LegionsClient.CONFIG.teamFightSmoothingStrength));
        double speed = strength / 100.0;
        return Math.round(FIGHT_MARKER_MAX_MOVE_MILLIS
                - (FIGHT_MARKER_MAX_MOVE_MILLIS - FIGHT_MARKER_MIN_MOVE_MILLIS) * speed);
    }

    private static Vec3d groundedFightPosition(MinecraftClient client, Vec3d pos) {
        return groundedFightPosition(client, pos, pos.y - FIGHT_DETECTION_RADIUS, pos.y);
    }

    private static Vec3d groundedFightPosition(MinecraftClient client, Vec3d pos, double minY, double maxY) {
        if (client.world == null || client.player == null) {
            return pos;
        }

        double cappedY = Math.min(pos.y, maxY);
        double startY = maxY;
        double endY = Math.min(cappedY - FIGHT_DETECTION_RADIUS * 2.0, minY - 32.0);
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                new Vec3d(pos.x, startY, pos.z),
                new Vec3d(pos.x, endY, pos.z),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            double y = Math.min(hit.getPos().y + 0.04, maxY);
            return new Vec3d(hit.getPos().x, y, hit.getPos().z);
        }
        return new Vec3d(pos.x, cappedY, pos.z);
    }

    private static int nearestFightMarker(MinecraftClient client, Vec3d center, boolean[] usedOldMarkers, double maxDistanceSquared, long now) {
        int bestIndex = -1;
        double bestDistanceSquared = maxDistanceSquared;
        for (int i = 0; i < fightMarkers.size(); i++) {
            if (usedOldMarkers[i]) {
                continue;
            }
            double distanceSquared = fightMarkerPosition(client, fightMarkers.get(i), now).squaredDistanceTo(center);
            if (distanceSquared < bestDistanceSquared) {
                bestIndex = i;
                bestDistanceSquared = distanceSquared;
            }
        }
        return bestIndex;
    }

    private static Vec3d lerp(Vec3d from, Vec3d to, double progress) {
        return new Vec3d(
                from.x + (to.x - from.x) * progress,
                from.y + (to.y - from.y) * progress,
                from.z + (to.z - from.z) * progress
        );
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
        int icon = rowIcon(client, row, target);
        String label = shortRowLabel(client, row, target, icon);
        sendChat(client, visibleMessage + " [LC:P:" + sender + ":" + targetName
                + ":color=" + hexColor(color) + ":audience=" + audienceName(row.visualAudience)
                + ":icon=" + iconName(icon) + ":label=" + payloadLabel(label) + "]");
        markPlayer(targetName, color, icon, label);
    }

    private static void pingBlock(MinecraftClient client, PingRow row, BlockPos pos, int color) {
        String sender = LegionsFeatures.realUsername(client.player);
        String visibleMessage = formatBlockMessage(row.message, sender, pos);
        int icon = rowIcon(client, row, null);
        String label = shortRowLabel(client, row, null, icon);
        sendChat(client, visibleMessage + " [LC:B:" + sender + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ()
                + ":color=" + hexColor(color) + ":audience=" + audienceName(row.visualAudience)
                + ":icon=" + iconName(icon) + ":label=" + payloadLabel(label) + "]");
        markBlock(pos, color, icon, label);
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
                .replace("{SENDER}", sender)
                .replace("{player}", targetName)
                .replace("{PLAYER}", targetName);
    }

    private static String formatBlockMessage(String template, String sender, BlockPos pos) {
        return cleanVisibleMessage(template)
                .replace("{sender}", sender)
                .replace("{SENDER}", sender)
                .replace("{x}", Integer.toString(pos.getX()))
                .replace("{X}", Integer.toString(pos.getX()))
                .replace("{y}", Integer.toString(pos.getY()))
                .replace("{Y}", Integer.toString(pos.getY()))
                .replace("{z}", Integer.toString(pos.getZ()))
                .replace("{Z}", Integer.toString(pos.getZ()));
    }

    private static int rowIcon(MinecraftClient client, PingRow row, PlayerEntity target) {
        if (row.targetType == PingRow.TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE) {
            return target != null && LegionsFeatures.isTeammate(client.player, target)
                    ? row.teammateMessageIcon
                    : row.enemyMessageIcon;
        }
        return row.messageIcon;
    }

    private static String shortRowLabel(MinecraftClient client, PingRow row, PlayerEntity target, int icon) {
        if (row.targetType == PingRow.TARGET_TYPE_BLOCKS_ONLY) {
            return "Go";
        }
        if (row.targetType == PingRow.TARGET_TYPE_ALL_PLAYERS_DIFFERENT_MESSAGE
                && target != null
                && LegionsFeatures.isTeammate(client.player, target)) {
            return "Help";
        }
        return defaultIconLabel(icon);
    }

    private static String cleanVisibleMessage(String template) {
        String message = template == null || template.isBlank() ? "Ping" : template.trim();
        return message.length() > 190 ? message.substring(0, 190) : message;
    }

    private static String payloadLabel(String label) {
        return cleanPayloadLabel(label).replace(' ', '_');
    }

    private static String cleanPayloadLabel(String label) {
        return cleanMarkerLabel(label == null ? "" : label.replace('_', ' '), "");
    }

    private static String cleanMarkerLabel(String label, String fallback) {
        String cleaned = label == null ? "" : label.trim().replace('[', ' ').replace(']', ' ').replace(':', ' ');
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) {
            cleaned = fallback == null || fallback.isBlank() ? "Ping" : fallback;
        }
        return cleaned.length() <= PING_LABEL_MAX_LENGTH ? cleaned : cleaned.substring(0, PING_LABEL_MAX_LENGTH);
    }

    private static ReceivedPing parseReceivedPing(String type, String body) {
        String[] parts = body.split(":");
        if ("P".equalsIgnoreCase(type)) {
            if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return null;
            }
            PayloadOptions options = parsePayloadOptions(parts, 2, PingRow.ICON_STAR);
            return ReceivedPing.player(parts[0], parts[1], options.color(), options.audience(), options.icon(), options.label());
        }

        if (parts.length < 4 || parts[0].isBlank()) {
            return null;
        }
        try {
            PayloadOptions options = parsePayloadOptions(parts, 4, PingRow.ICON_PICKAXE);
            return ReceivedPing.block(parts[0], new BlockPos(
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            ), options.color(), options.audience(), options.icon(), options.label());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static PayloadOptions parsePayloadOptions(String[] parts, int start, int defaultIcon) {
        int color = DEFAULT_PING_COLOR;
        int audience = PingRow.VISUAL_AUDIENCE_TEAMMATES;
        int icon = defaultIcon;
        String label = "";
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
            } else if ("icon".equals(key)) {
                icon = parseIcon(value, icon);
            } else if ("label".equals(key)) {
                label = cleanPayloadLabel(value);
            }
        }
        return new PayloadOptions(color, audience, icon, label);
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

    private static void renderFightMarker(MinecraftClient client, FightMark mark, int color, int opacity, long now) {
        Vec3d center = fightMarkerPosition(client, mark, now);
        int fadedColor = applyOpacity(color, opacity);
        GizmoDrawing.point(center, fadedColor, FIGHT_MARKER_SIZE).ignoreOcclusion();
        GizmoDrawing.circle(center, Math.max(2.0f, FIGHT_DETECTION_RADIUS / 6.0f),
                blockPingDrawStyle(fadedColor)).ignoreOcclusion();
        if (LegionsClient.CONFIG.blockPingDistanceLabelEnabled) {
            String label = "Team fight [" + distanceLabel(client, center) + "]";
            GizmoDrawing.blockLabel(label, BlockPos.ofFloored(center), 0, fadedColor, BLOCK_PING_LABEL_SCALE).ignoreOcclusion();
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

    private static void markBlock(BlockPos pos, int color, int icon, String label) {
        markedBlocks.put(pos.toImmutable(), new PingMark(System.currentTimeMillis(), opaque(color), normalizeIcon(icon), cleanMarkerLabel(label, "Go")));
    }

    private static void markPlayer(String name, int color, int icon, String label) {
        if (name != null && !name.isBlank()) {
            markedPlayers.put(name.toLowerCase(Locale.ROOT), new PingMark(System.currentTimeMillis(), opaque(color), normalizeIcon(icon), cleanMarkerLabel(label, defaultIconLabel(icon))));
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

    private static int fightMarkerOpacity(FightMark mark, long now) {
        if (mark.fadeStartedAt() <= 0L) {
            return 100;
        }
        long fadeMillis = Math.max(1, LegionsClient.CONFIG.teamFightFadeOutSeconds) * 1000L;
        double progress = clampDouble((now - mark.fadeStartedAt()) / (double) fadeMillis, 0.0, 1.0);
        return (int) Math.round(100.0 * (1.0 - progress));
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

    private static int parseIcon(String value, int fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "default", "\u2666" -> PingRow.ICON_DEFAULT;
            case "axe", "\uD83E\uDE93" -> PingRow.ICON_AXE;
            case "pickaxe", "block", "\u26CF" -> PingRow.ICON_PICKAXE;
            case "sword", "focus", "enemy" -> PingRow.ICON_SWORD;
            case "bow" -> PingRow.ICON_BOW;
            case "star", "player" -> PingRow.ICON_STAR;
            case "fire", "fight", "\uD83D\uDD25" -> PingRow.ICON_FIRE;
            case "lightning", "warning", "danger", "\u26A1" -> PingRow.ICON_LIGHTNING;
            case "galaxy" -> PingRow.ICON_GALAXY;
            case "diamond", "\u25C6" -> PingRow.ICON_DIAMOND;
            case "dot" -> PingRow.ICON_DOT;
            case "heart", "shield", "help", "defend", "\u2764" -> PingRow.ICON_HEART;
            case "hourglass" -> PingRow.ICON_HOURGLASS;
            case "home", "flag", "go", "location" -> PingRow.ICON_HOME;
            case "comet" -> PingRow.ICON_COMET;
            default -> normalizeIcon(fallback);
        };
    }

    private static String iconName(int icon) {
        return switch (normalizeIcon(icon)) {
            case PingRow.ICON_AXE -> "axe";
            case PingRow.ICON_PICKAXE -> "pickaxe";
            case PingRow.ICON_SWORD -> "sword";
            case PingRow.ICON_BOW -> "bow";
            case PingRow.ICON_STAR -> "star";
            case PingRow.ICON_FIRE -> "fire";
            case PingRow.ICON_LIGHTNING -> "lightning";
            case PingRow.ICON_GALAXY -> "galaxy";
            case PingRow.ICON_DIAMOND -> "diamond";
            case PingRow.ICON_DOT -> "dot";
            case PingRow.ICON_HEART -> "heart";
            case PingRow.ICON_HOURGLASS -> "hourglass";
            case PingRow.ICON_HOME -> "home";
            case PingRow.ICON_COMET -> "comet";
            default -> "default";
        };
    }

    private static String iconSymbol(int icon) {
        return switch (normalizeIcon(icon)) {
            case PingRow.ICON_AXE -> "\uD83E\uDE93";
            case PingRow.ICON_PICKAXE -> "\u26CF";
            case PingRow.ICON_SWORD -> "\uD83D\uDDE1";
            case PingRow.ICON_BOW -> "\uD83C\uDFF9";
            case PingRow.ICON_STAR -> "\u2B50";
            case PingRow.ICON_FIRE -> "\uD83D\uDD25";
            case PingRow.ICON_LIGHTNING -> "\u26A1";
            case PingRow.ICON_GALAXY -> "\uD83C\uDF0C";
            case PingRow.ICON_DIAMOND -> "\u25C6";
            case PingRow.ICON_DOT -> "\u23FA";
            case PingRow.ICON_HEART -> "\u2764";
            case PingRow.ICON_HOURGLASS -> "\u23F3";
            case PingRow.ICON_HOME -> "\u2302";
            case PingRow.ICON_COMET -> "\u2604";
            default -> "\u2666";
        };
    }

    private static String defaultIconLabel(int icon) {
        return switch (normalizeIcon(icon)) {
            case PingRow.ICON_AXE -> "Attack";
            case PingRow.ICON_PICKAXE -> "Block";
            case PingRow.ICON_BOW -> "Bow";
            case PingRow.ICON_STAR -> "Team Fight";
            case PingRow.ICON_FIRE -> "Fight";
            case PingRow.ICON_LIGHTNING -> "Danger";
            case PingRow.ICON_GALAXY -> "Portal";
            case PingRow.ICON_DIAMOND -> "Point";
            case PingRow.ICON_DOT -> "Point";
            case PingRow.ICON_HEART -> "Help";
            case PingRow.ICON_HOURGLASS -> "Wait";
            case PingRow.ICON_HOME -> "Go";
            case PingRow.ICON_COMET -> "Move";
            default -> "Focus";
        };
    }

    private static int normalizeIcon(int icon) {
        return Math.max(PingRow.ICON_DEFAULT, Math.min(PingRow.ICON_COMET, icon));
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

    private record ActiveMarker(Vec3d pos, int color, int icon, String label, int opacity) {
    }

    private record ScreenProjection(boolean visible, double x, double y, double dirX, double dirY) {
    }

    private record ArrowPlacement(double x, double y, double dirX, double dirY) {
    }

    private record PlayerFightNode(String team, Vec3d pos) {
    }

    private record FightCandidate(Vec3d center, double averageY, int players, int teams, double distanceSquared) {
    }

    private record FightMark(Vec3d fromPos, Vec3d targetPos, double averageY,
                             long markedAt, long fadeStartedAt,
                             long moveStartedAt, long moveDurationMillis,
                             int players, int teams) {
        private Vec3d rawPos(long now) {
            if (moveDurationMillis <= 0L) {
                return targetPos;
            }
            double progress = clampDouble((now - moveStartedAt) / (double) moveDurationMillis, 0.0, 1.0);
            double eased = progress * progress * (3.0 - 2.0 * progress);
            return lerp(fromPos, targetPos, eased);
        }
    }

    private record PendingPress(PingKey key, int presses, long pressedAt, AimSnapshot aim) {
    }

    private record AimSnapshot(BlockPos blockPos, List<PlayerRayHit> playerHits) {
    }

    private record PlayerRayHit(UUID uuid, String name, double distanceSquared, double rayDistance) {
    }

    private record PingMark(long markedAt, int color, int icon, String label) {
    }

    private record PingTarget(PlayerEntity player, BlockPos blockPos) {
        private static PingTarget player(PlayerEntity player) {
            return new PingTarget(player, null);
        }

        private static PingTarget block(BlockPos pos) {
            return new PingTarget(null, pos);
        }
    }

    private record ReceivedPing(String sender, String playerTarget, BlockPos blockPos, int color, int audience, int icon, String label) {
        private static ReceivedPing player(String sender, String target, int color, int audience, int icon, String label) {
            return new ReceivedPing(sender, target, null, color, audience, icon, label);
        }

        private static ReceivedPing block(String sender, BlockPos pos, int color, int audience, int icon, String label) {
            return new ReceivedPing(sender, null, pos, color, audience, icon, label);
        }
    }

    private record PayloadOptions(int color, int audience, int icon, String label) {
    }

    private record Range(int start, int end) {
    }

    private record RayDistance(double distanceSquared, double rayDistance) {
    }
}
