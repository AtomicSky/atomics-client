package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ClientFeatureManager {
    private static final long REACH_DISPLAY_MS = 1400L;
    private static final float ZOOM_MIN = TpsConfig.MIN_ZOOM_MULTIPLIER;
    private static final float ZOOM_MAX = TpsConfig.MAX_ZOOM_MULTIPLIER;
    private static final double ZOOM_SCROLL_FACTOR = 1.25;
    private static final int ARMOR_HUD_EMPTY_OFFHAND_SHIFT = 28;
    private static final Identifier HOTBAR_SPRITE_TEXTURE = Identifier.ofVanilla("textures/gui/sprites/hud/hotbar.png");
    private static final String TEAM_COUNT_TITLE = "Teams Left";
    private static final int TEAM_COUNT_MIN_WIDTH = 78;
    private static final int TEAM_COUNT_HEADER_HEIGHT = 13;
    private static final int TEAM_COUNT_ROW_HEIGHT = 10;
    private static final int TEAM_COUNT_PADDING_X = 5;
    private static final int TEAM_COUNT_MARGIN = 8;
    private static final int TEAM_COUNT_DEFAULT_Y = 36;

    private static long lastReachMillis;
    private static String lastReachTextString = "";
    private static Text lastReachText = Text.empty();
    private static int lastReachTextWidth = -1;
    private static int trailTick;
    private static boolean fullBrightApplied;
    private static boolean timeChangerApplied;
    private static float currentZoomMultiplier = 1.0f;
    private static float targetZoomMultiplier = 1.0f;
    private static float zoomSessionMultiplier = 1.0f;
    private static boolean zoomSessionActive;
    private static long lastZoomUpdateNanos;
    private static long lastZoomFeedbackMillis;
    private static long lastArmorWarningMillis;
    private static String lastArmorWarningKey = "";
    private static final Map<UUID, String> MASKED_PLAYER_NAMES = new HashMap<>();
    private static final Text[] TNT_TIMER_TEXT_CACHE = new Text[2001];
    private static final List<PreparedParticleBurst> PREPARED_PROJECTILE_TRAIL_PARTICLES = new ArrayList<>();
    private static boolean projectileTrailParticleCacheInitialized;
    private static int projectileTrailParticleFingerprint;

    private ClientFeatureManager() {
    }

    public static void tick(MinecraftClient client) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || client == null || client.player == null || client.world == null) {
            updateZoom(cfg);
            if (client == null || client.world == null) {
                MASKED_PLAYER_NAMES.clear();
            }
            if (client != null && client.player != null) {
                disableFullBrightIfNeeded(client);
            }
            if (client != null && client.world != null) {
                disableTimeChangerIfNeeded(client);
            }
            return;
        }

        updateZoom(cfg);

        if (cfg.visual.fullBrightEnabled) {
            applyFullBrightIfNeeded(client);
        } else {
            disableFullBrightIfNeeded(client);
        }

        if (cfg.visual.timeChangerEnabled) {
            client.world.setTime(client.world.getTime(), cfg.visual.timeOfDay, false);
            timeChangerApplied = true;
        } else {
            disableTimeChangerIfNeeded(client);
        }

        if (cfg.visual.projectileTrailEnabled) {
            spawnProjectileTrails(client, cfg.visual);
        }
    }

    /**
     * Records reach the same way ReachDisplay does: distance from the attacking
     * player's eye position to the closest point on the target's bounding box.
     *
     * This avoids entity-center / feet-to-feet distances, which can incorrectly
     * show values above normal Minecraft melee reach.
     */
    public static void onReachAttack(PlayerEntity player, Entity target) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || player == null || target == null || !cfg.combat.reachDisplayEnabled) {
            return;
        }

        double reach = calculateClosestPointReach(player, target);
        if (!Double.isFinite(reach)) {
            return;
        }

        lastReachMillis = System.currentTimeMillis();
        lastReachTextString = String.format(Locale.US, "%.2fm", reach);
        lastReachText = Text.literal(lastReachTextString);
        lastReachTextWidth = -1;
    }

    /**
     * Kept for older call sites, but reach should be captured at attack time.
     * Server-confirmed damage can arrive after the target has moved, which is
     * what caused impossible displayed reach values.
     */
    public static void onHitLanded(Entity target) {
        // Intentionally unused. Reach is recorded from ClientPlayerInteractionManagerMixin.
    }

    private static double calculateClosestPointReach(PlayerEntity player, Entity target) {
        Vec3d eyePos = player.getEyePos();
        Box box = target.getBoundingBox();

        double closestX = clampDouble(eyePos.x, box.minX, box.maxX);
        double closestY = clampDouble(eyePos.y, box.minY, box.maxY);
        double closestZ = clampDouble(eyePos.z, box.minZ, box.maxZ);

        double dx = eyePos.x - closestX;
        double dy = eyePos.y - closestY;
        double dz = eyePos.z - closestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static void renderHud(DrawContext context) {
        TpsConfig cfg = liveConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cfg == null || client == null || client.player == null || client.world == null) {
            return;
        }

        if (cfg.combat.reachDisplayEnabled) {
            renderReachDisplay(context, client);
        }
        if (cfg.visual.armorHudEnabled || cfg.visual.armorDurabilityWarningEnabled) {
            renderArmorHudAndWarnings(context, client, cfg.visual);
        }
        if (cfg.pvp.teamCountOverlayEnabled) {
            renderTeamCountOverlay(context, client, cfg.pvp.teamCountOverlayX, cfg.pvp.teamCountOverlayY, false);
        }
    }

    public static int teamCountOverlayPreviewWidth(MinecraftClient client) {
        MinecraftClient renderClient = client == null ? MinecraftClient.getInstance() : client;
        return teamCountOverlayWidth(renderClient, displayTeamCounts(client, true));
    }

    public static int teamCountOverlayPreviewHeight(MinecraftClient client) {
        return teamCountOverlayHeight(displayTeamCounts(client, true));
    }

    public static int defaultTeamCountOverlayX(MinecraftClient client) {
        MinecraftClient renderClient = client == null ? MinecraftClient.getInstance() : client;
        int screenWidth = renderClient.getWindow().getScaledWidth();
        return Math.max(0, screenWidth - teamCountOverlayPreviewWidth(renderClient) - TEAM_COUNT_MARGIN);
    }

    public static int defaultTeamCountOverlayY() {
        return TEAM_COUNT_DEFAULT_Y;
    }

    public static void renderTeamCountOverlayPreview(DrawContext context, MinecraftClient client, int x, int y) {
        MinecraftClient renderClient = client == null ? MinecraftClient.getInstance() : client;
        renderTeamCountOverlay(context, renderClient, x, y, true);
    }

    public static boolean shouldMaskPlayerNames() {
        TpsConfig cfg = liveConfig();
        return cfg != null && cfg.visual.streamerModeEnabled;
    }

    public static String maskedPlayerName(Entity entity) {
        if (entity == null) return "Player";
        UUID uuid = entity.getUuid();
        String cached = MASKED_PLAYER_NAMES.get(uuid);
        if (cached != null) {
            return cached;
        }

        if (MASKED_PLAYER_NAMES.size() > 512) {
            MASKED_PLAYER_NAMES.clear();
        }
        String maskedName = "Player " + (Math.floorMod(uuid.hashCode(), 900) + 100);
        MASKED_PLAYER_NAMES.put(uuid, maskedName);
        return maskedName;
    }

    public static Text customizePlayerNametag(PlayerEntity player, Text originalDisplayName) {
        TpsConfig cfg = liveConfig();
        if (player == null || originalDisplayName == null || cfg == null || cfg.pvp == null) {
            return originalDisplayName;
        }

        MutableText baseName = shouldMaskPlayerNames()
                ? Text.literal(maskedPlayerName(player))
                : originalDisplayName.copy();

        List<NametagItem> before = new ArrayList<>();
        List<NametagItem> after = new ArrayList<>();
        List<String> order = cfg.pvp.nametagItemOrder == null ? TpsConfig.defaultNametagItemOrder() : cfg.pvp.nametagItemOrder;
        for (String itemId : order) {
            Text itemText = getNametagItemText(itemId, player, cfg);
            if (itemText == null) {
                continue;
            }
            NametagItem item = new NametagItem(itemId, itemText);
            if (cfg.pvp.nametagItemsBeforeName != null && cfg.pvp.nametagItemsBeforeName.contains(itemId)) {
                before.add(item);
            } else {
                after.add(item);
            }
        }

        if (before.isEmpty() && after.isEmpty()) {
            return baseName;
        }

        MutableText result = Text.empty();
        appendNametagItems(result, before);
        if (!before.isEmpty()) {
            result.append(Text.literal(" "));
        }
        result.append(baseName);
        if (!after.isEmpty()) {
            result.append(Text.literal(" "));
            appendNametagItems(result, after);
        }
        return result;
    }

    private static void appendNametagItems(MutableText target, List<NametagItem> items) {
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                target.append(Text.literal(" "));
            }
            target.append(items.get(i).text);
        }
    }

    private static Text getNametagItemText(String itemId, PlayerEntity player, TpsConfig cfg) {
        if (TpsConfig.NAMETAG_ITEM_WIN_ODDS.equals(itemId)) {
            return PvpNametagStatsManager.getWinPercentNameSuffix(player);
        }
        if (TpsConfig.NAMETAG_ITEM_TOTEM_POPS.equals(itemId)) {
            return PvpNametagStatsManager.getTotemPopNameSuffix(player);
        }
        if (TpsConfig.NAMETAG_ITEM_OPPONENT_STATS.equals(itemId)) {
            return TierWeightManager.getNameSuffix(player);
        }
        if (TpsConfig.NAMETAG_ITEM_PING.equals(itemId)) {
            return getPingNametagText(player, cfg);
        }
        return null;
    }

    private static Text getPingNametagText(PlayerEntity player, TpsConfig cfg) {
        if (cfg == null || cfg.pvp == null || !cfg.pvp.pingNametagEnabled || player == null) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return null;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null || entry.getLatency() < 0) {
            return null;
        }
        int ping = entry.getLatency();
        Formatting color = ping <= 75 ? Formatting.GREEN : ping <= 150 ? Formatting.YELLOW : ping <= 250 ? Formatting.GOLD : Formatting.RED;
        return Text.literal(ping + "ms").formatted(color);
    }

    public static boolean isZoomActive() {
        updateZoom(liveConfig());
        return currentZoomMultiplier > 1.01f || targetZoomMultiplier > 1.01f;
    }

    private record NametagItem(String id, Text text) {
    }

    private record ArmorPiece(ItemStack stack, String slotName, int remaining, int maxDamage, int percent) {
        boolean isPresent() {
            return stack != null && !stack.isEmpty();
        }

        boolean isDamageable() {
            return isPresent() && maxDamage > 0;
        }
    }

    public static float getZoomFovMultiplier() {
        updateZoom(liveConfig());
        return 1.0f / Math.max(1.0f, currentZoomMultiplier);
    }

    public static boolean onZoomScroll(double verticalAmount) {
        MinecraftClient client = MinecraftClient.getInstance();
        TpsConfig cfg = liveConfig();
        if (client == null || client.currentScreen != null || cfg == null || !cfg.visual.zoomEnabled || !AtomicsClient.isZoomKeyPressed()) {
            return false;
        }
        if (Math.abs(verticalAmount) < 0.0001) {
            return false;
        }

        ensureZoomSessionStarted(cfg);
        zoomSessionMultiplier = adjustZoomByScroll(zoomSessionMultiplier, verticalAmount);
        if (client.player != null) {
            long now = System.currentTimeMillis();
            if (now - lastZoomFeedbackMillis > 70L) {
                client.player.sendMessage(Text.literal("Zoom: " + String.format(Locale.US, "%.2fx", zoomSessionMultiplier)).formatted(Formatting.AQUA), true);
                lastZoomFeedbackMillis = now;
            }
        }
        return true;
    }

    public static void runMacro(MinecraftClient client, int index) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || !cfg.macros.enabled || client == null || client.getNetworkHandler() == null) {
            return;
        }
        if (index < 0 || index >= cfg.macros.messages.length) {
            return;
        }

        String message = cfg.macros.messages[index];
        if (message == null || message.isBlank()) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Macro " + (index + 1) + " is empty").formatted(Formatting.YELLOW), true);
            }
            return;
        }

        message = message.trim();
        if (message.startsWith("/") && message.length() > 1) {
            client.getNetworkHandler().sendChatCommand(message.substring(1));
        } else {
            client.getNetworkHandler().sendChatMessage(message);
        }
    }

    private static void renderReachDisplay(DrawContext context, MinecraftClient client) {
        long age = System.currentTimeMillis() - lastReachMillis;
        if (lastReachMillis <= 0L || age > REACH_DISPLAY_MS) {
            return;
        }

        float alpha = 1.0f - Math.max(0.0f, (age - 900L) / 500.0f);
        int a = Math.max(30, Math.min(150, Math.round(alpha * 150.0f)));
        int color = (a << 24) | 0xB8B8B8;
        float scale = 0.72f;
        if (lastReachTextWidth < 0) {
            lastReachTextWidth = client.textRenderer.getWidth(lastReachTextString);
        }
        int x = client.getWindow().getScaledWidth() / 2 - Math.round(lastReachTextWidth * scale / 2.0f);
        int y = client.getWindow().getScaledHeight() / 2 + 12;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(client.textRenderer, lastReachText, Math.round(x / scale), Math.round(y / scale), color);
        context.getMatrices().popMatrix();
    }

    private static void renderTeamCountOverlay(DrawContext context, MinecraftClient client, int configuredX, int configuredY, boolean preview) {
        List<TeamCount> counts = displayTeamCounts(client, preview);
        if (counts.isEmpty()) {
            return;
        }

        int width = teamCountOverlayWidth(client, counts);
        int height = teamCountOverlayHeight(counts);
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = configuredX;
        int y = configuredY;
        if (x < 0 || y < 0) {
            x = Math.max(0, screenWidth - width - TEAM_COUNT_MARGIN);
            y = Math.min(Math.max(0, TEAM_COUNT_DEFAULT_Y), Math.max(0, screenHeight - height));
        }
        x = Math.max(0, Math.min(screenWidth - width, x));
        y = Math.max(0, Math.min(screenHeight - height, y));

        context.fill(x, y, x + width, y + height, 0x66000000);
        context.fill(x, y, x + width, y + TEAM_COUNT_HEADER_HEIGHT, 0x99000000);
        int titleX = x + width / 2 - client.textRenderer.getWidth(TEAM_COUNT_TITLE) / 2;
        context.drawTextWithShadow(client.textRenderer, TEAM_COUNT_TITLE, titleX, y + 2, 0xFFFFFF55);

        int rowY = y + TEAM_COUNT_HEADER_HEIGHT + 2;
        for (int i = 0; i < counts.size(); i++) {
            TeamCount count = counts.get(i);
            int rowTop = rowY + i * TEAM_COUNT_ROW_HEIGHT;
            context.fill(x, rowTop, x + width, rowTop + TEAM_COUNT_ROW_HEIGHT, (i & 1) == 0 ? 0x52000000 : 0x3F000000);
            context.drawTextWithShadow(client.textRenderer, count.name(), x + TEAM_COUNT_PADDING_X, rowTop + 1, count.color());
            String value = String.valueOf(count.count());
            int valueX = x + width - TEAM_COUNT_PADDING_X - client.textRenderer.getWidth(value);
            context.drawTextWithShadow(client.textRenderer, value, valueX, rowTop + 1, 0xFFFFFFFF);
        }
    }

    private static int teamCountOverlayWidth(MinecraftClient client, List<TeamCount> counts) {
        int width = client.textRenderer.getWidth(TEAM_COUNT_TITLE) + TEAM_COUNT_PADDING_X * 2;
        for (TeamCount count : counts) {
            String value = String.valueOf(count.count());
            int rowWidth = client.textRenderer.getWidth(count.name()) + client.textRenderer.getWidth(value) + TEAM_COUNT_PADDING_X * 3 + 8;
            width = Math.max(width, rowWidth);
        }
        return Math.max(TEAM_COUNT_MIN_WIDTH, width);
    }

    private static int teamCountOverlayHeight(List<TeamCount> counts) {
        return TEAM_COUNT_HEADER_HEIGHT + 4 + counts.size() * TEAM_COUNT_ROW_HEIGHT;
    }

    private static List<TeamCount> displayTeamCounts(MinecraftClient client, boolean preview) {
        List<TeamCount> counts = collectTeamCounts(client);
        if (preview && counts.isEmpty()) {
            return sampleTeamCounts();
        }
        return counts;
    }

    private static List<TeamCount> collectTeamCounts(MinecraftClient client) {
        LinkedHashMap<String, TeamCount> counts = new LinkedHashMap<>();
        if (client == null || client.world == null) {
            return new ArrayList<>();
        }

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == null || player.isSpectator() || player.isDead() || !player.isAlive()) {
                continue;
            }

            Team team = player.getScoreboardTeam();
            if (team == null || team.getName() == null || team.getName().isBlank()) {
                continue;
            }

            String key = team.getName();
            TeamCount current = counts.get(key);
            if (current == null) {
                counts.put(key, new TeamCount(teamDisplayName(team), teamTextColor(team), 1));
            } else {
                counts.put(key, new TeamCount(current.name(), current.color(), current.count() + 1));
            }
        }
        return new ArrayList<>(counts.values());
    }

    private static List<TeamCount> sampleTeamCounts() {
        ArrayList<TeamCount> counts = new ArrayList<>();
        counts.add(new TeamCount("BLUE", 0xFF5555FF, 8));
        counts.add(new TeamCount("RED", 0xFFFF5555, 7));
        return counts;
    }

    private static String teamDisplayName(Team team) {
        String name = team.getDisplayName() == null ? "" : team.getDisplayName().getString();
        if (name.isBlank()) {
            name = team.getName();
        }
        return formatTeamName(name);
    }

    private static String formatTeamName(String name) {
        if (name == null || name.isBlank()) {
            return "Team";
        }
        return name.length() <= 18 ? name : name.substring(0, 18);
    }

    private static int teamTextColor(Team team) {
        Formatting formatting = team.getColor();
        if (formatting == null) {
            return 0xFFFFFFFF;
        }

        Integer rgb = formatting.getColorValue();
        if (rgb == null) {
            return 0xFFFFFFFF;
        }

        int color = rgb & 0xFFFFFF;
        return color <= 0x202020 ? 0xFFAAAAAA : 0xFF000000 | color;
    }

    private static void renderArmorHudAndWarnings(DrawContext context, MinecraftClient client, TpsConfig.VisualSettings visual) {
        ArmorPiece[] armor = armorPieces(client.player);
        if (visual.armorHudEnabled) {
            renderArmorHud(context, client, armor, visual);
        }
        if (visual.armorDurabilityWarningEnabled) {
            warnLowArmorDurability(client, armor, TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT);
        }
    }

    private static void renderArmorHud(DrawContext context, MinecraftClient client, ArmorPiece[] armor, TpsConfig.VisualSettings visual) {
        int visible = 0;
        for (ArmorPiece piece : armor) {
            if (piece != null && piece.isPresent()) {
                visible++;
            }
        }
        if (visible <= 0) {
            return;
        }

        boolean autoPosition = visual.armorHudAutoPosition;
        boolean vertical = !autoPosition && visual.armorHudVertical;
        int spacing = autoPosition ? TpsConfig.DEFAULT_ARMOR_HUD_SPACING : Math.max(20, Math.min(64, visual.armorHudSpacing));
        int slotSize = visual.armorHudHotbarBorder ? 22 : 16;
        int itemOffset = visual.armorHudHotbarBorder ? 3 : 0;
        int itemHeight = TpsConfig.ARMOR_HUD_DURABILITY_BAR.equals(visual.armorHudDurabilityMode) ? 16 : 28;
        int hudWidth = vertical ? slotSize : slotSize + (visible - 1) * spacing;
        int hudHeight = vertical ? Math.max(slotSize, itemHeight) + (visible - 1) * spacing : Math.max(slotSize, itemHeight);
        int x = visual.armorHudX;
        int y = visual.armorHudY;
        if (autoPosition || x < 0 || y < 0) {
            x = client.getWindow().getScaledWidth() / 2 - 127 - hudWidth;
            y = client.getWindow().getScaledHeight() - 22;
            if (vertical) {
                y = client.getWindow().getScaledHeight() / 2 - hudHeight / 2;
            } else if (autoPosition && client.player != null && client.player.getOffHandStack().isEmpty()) {
                x += ARMOR_HUD_EMPTY_OFFHAND_SHIFT;
            }
        }
        x = Math.max(0, Math.min(client.getWindow().getScaledWidth() - hudWidth, x));
        y = Math.max(0, Math.min(client.getWindow().getScaledHeight() - hudHeight, y));

        int warningPercent = TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
        for (ArmorPiece piece : armor) {
            if (piece == null || !piece.isPresent()) {
                continue;
            }
            if (visual.armorHudHotbarBorder) {
                renderArmorHudSlot(context, x, y, visible);
            }
            int itemX = x + itemOffset;
            int itemY = y + itemOffset;
            context.drawItem(piece.stack(), itemX, itemY);
            context.drawStackOverlay(client.textRenderer, piece.stack(), itemX, itemY);

            String durability = armorDurabilityText(piece, visual.armorHudDurabilityMode);
            if (!durability.isEmpty()) {
                int color = armorDurabilityColor(piece.percent(), warningPercent);
                float scale = 0.68f;
                int textWidth = client.textRenderer.getWidth(durability);
                int textX = Math.round((itemX + 8.0f - textWidth * scale / 2.0f) / scale);
                int textY = Math.round((itemY + 18) / scale);
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(scale, scale);
                context.drawTextWithShadow(client.textRenderer, durability, textX, textY, color);
                context.getMatrices().popMatrix();
            }
            if (vertical) {
                y += spacing;
            } else {
                x += spacing;
            }
        }
    }

    private static void renderArmorHudSlot(DrawContext context, int x, int y, int visible) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE_TEXTURE, x, y, 0.0f, 0.0f, 22, 22, 182, 22);
    }

    private static String armorDurabilityText(ArmorPiece piece, String mode) {
        if (!piece.isDamageable()) {
            return "";
        }
        if (TpsConfig.ARMOR_HUD_DURABILITY_PERCENT.equals(mode)) {
            return piece.percent() + "%";
        }
        if (TpsConfig.ARMOR_HUD_DURABILITY_BAR.equals(mode)) {
            return "";
        }
        return String.valueOf(piece.remaining());
    }

    private static void warnLowArmorDurability(MinecraftClient client, ArmorPiece[] armor, int warningPercent) {
        ArmorPiece lowest = null;
        for (ArmorPiece piece : armor) {
            if (piece == null || !piece.isDamageable() || piece.percent() > warningPercent) {
                continue;
            }
            if (lowest == null || piece.percent() < lowest.percent()) {
                lowest = piece;
            }
        }
        if (lowest == null || client.player == null) {
            lastArmorWarningKey = "";
            return;
        }

        long now = System.currentTimeMillis();
        String key = lowest.slotName() + lowest.remaining();
        if (!key.equals(lastArmorWarningKey) || now - lastArmorWarningMillis > 2500L) {
            client.player.sendMessage(Text.literal("Low armor durability: " + lowest.slotName() + " " + lowest.remaining() + "/" + lowest.maxDamage())
                    .formatted(Formatting.RED, Formatting.BOLD), true);
            lastArmorWarningKey = key;
            lastArmorWarningMillis = now;
        }
    }

    private static ArmorPiece[] armorPieces(PlayerEntity player) {
        return new ArmorPiece[] {
                armorPiece(player, EquipmentSlot.HEAD, "Helmet"),
                armorPiece(player, EquipmentSlot.CHEST, "Chestplate"),
                armorPiece(player, EquipmentSlot.LEGS, "Leggings"),
                armorPiece(player, EquipmentSlot.FEET, "Boots")
        };
    }

    private static ArmorPiece armorPiece(PlayerEntity player, EquipmentSlot slot, String slotName) {
        if (player == null) {
            return new ArmorPiece(ItemStack.EMPTY, slotName, 0, 0, 100);
        }
        ItemStack stack = player.getEquippedStack(slot);
        if (stack == null || stack.isEmpty()) {
            return new ArmorPiece(ItemStack.EMPTY, slotName, 0, 0, 100);
        }
        if (!stack.isDamageable()) {
            return new ArmorPiece(stack, slotName, 0, 0, 100);
        }
        int maxDamage = Math.max(1, stack.getMaxDamage());
        int remaining = Math.max(0, maxDamage - stack.getDamage());
        int percent = Math.round(remaining * 100.0f / maxDamage);
        return new ArmorPiece(stack, slotName, remaining, maxDamage, percent);
    }

    private static int armorDurabilityColor(int percent, int warningPercent) {
        if (percent <= warningPercent) return 0xFFFF5555;
        if (percent <= Math.max(warningPercent + 15, 40)) return 0xFFFFAA00;
        return 0xFF55FF55;
    }

    public static boolean shouldShowTntTimer(TntEntity tnt) {
        TpsConfig cfg = liveConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cfg == null || client == null || client.player == null || tnt == null || !cfg.visual.tntTimerEnabled) {
            return false;
        }
        double rangeSq = cfg.visual.tntTimerRange * cfg.visual.tntTimerRange;
        return tnt.squaredDistanceTo(client.player) <= rangeSq;
    }

    public static Text tntTimerText(TntEntity tnt) {
        int fuse = Math.max(0, tnt.getFuse());
        if (fuse < TNT_TIMER_TEXT_CACHE.length) {
            Text cached = TNT_TIMER_TEXT_CACHE[fuse];
            if (cached != null) {
                return cached;
            }
            Text created = createTntTimerText(fuse);
            TNT_TIMER_TEXT_CACHE[fuse] = created;
            return created;
        }
        return createTntTimerText(fuse);
    }

    private static Text createTntTimerText(int fuse) {
        float seconds = fuse / 20.0f;
        Formatting color = seconds <= 1.0f ? Formatting.RED : seconds <= 2.0f ? Formatting.GOLD : Formatting.GRAY;
        return Text.literal(String.format(Locale.US, "%.1fs", seconds)).formatted(color);
    }

    private static void spawnProjectileTrails(MinecraftClient client, TpsConfig.VisualSettings visual) {
        if (++trailTick % 2 != 0) {
            return;
        }

        if (visual.projectileTrailParticles == null || visual.projectileTrailParticles.isEmpty()) {
            return;
        }
        List<PreparedParticleBurst> preparedParticles = prepareProjectileTrailParticles(visual.projectileTrailParticles);
        if (preparedParticles.isEmpty()) {
            return;
        }

        double rangeSq = 128.0 * 128.0;
        Random random = client.world.random;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity) || entity.squaredDistanceTo(client.player) > rangeSq) {
                continue;
            }
            if (entity.getVelocity().lengthSquared() < 0.0004) {
                continue;
            }

            for (PreparedParticleBurst prepared : preparedParticles) {
                TpsConfig.ParticleBurst burst = prepared.burst;
                ParticleEffect effect = prepared.effect;
                int count = prepared.count;
                for (int i = 0; i < count; i++) {
                    double x = entity.getX() + (random.nextDouble() - 0.5) * burst.spreadX;
                    double y = entity.getY() + (random.nextDouble() - 0.5) * burst.spreadY;
                    double z = entity.getZ() + (random.nextDouble() - 0.5) * burst.spreadZ;
                    double vx = (random.nextDouble() - 0.5) * burst.speed;
                    double vy = (random.nextDouble() - 0.5) * burst.speed;
                    double vz = (random.nextDouble() - 0.5) * burst.speed;
                    client.particleManager.addParticle(effect, x, y, z, vx, vy, vz);
                }
            }
        }
    }

    private static List<PreparedParticleBurst> prepareProjectileTrailParticles(List<TpsConfig.ParticleBurst> bursts) {
        int fingerprint = particleBurstFingerprint(bursts);
        if (projectileTrailParticleCacheInitialized && fingerprint == projectileTrailParticleFingerprint) {
            return PREPARED_PROJECTILE_TRAIL_PARTICLES;
        }

        projectileTrailParticleCacheInitialized = true;
        projectileTrailParticleFingerprint = fingerprint;
        PREPARED_PROJECTILE_TRAIL_PARTICLES.clear();
        for (TpsConfig.ParticleBurst burst : bursts) {
            if (burst == null) {
                continue;
            }
            int count = Math.max(0, burst.count);
            if (count <= 0) {
                continue;
            }
            ParticleEffect effect = TotemPopEffects.getParticleEffect(burst.particle);
            if (effect != null) {
                PREPARED_PROJECTILE_TRAIL_PARTICLES.add(new PreparedParticleBurst(burst, effect, count));
            }
        }
        return PREPARED_PROJECTILE_TRAIL_PARTICLES;
    }

    private static int particleBurstFingerprint(List<TpsConfig.ParticleBurst> bursts) {
        int result = bursts.size();
        for (TpsConfig.ParticleBurst burst : bursts) {
            if (burst == null) {
                result = 31 * result;
                continue;
            }
            result = 31 * result + (burst.particle == null ? 0 : burst.particle.hashCode());
            result = 31 * result + burst.count;
            result = 31 * result + Double.hashCode(burst.spreadX);
            result = 31 * result + Double.hashCode(burst.spreadY);
            result = 31 * result + Double.hashCode(burst.spreadZ);
            result = 31 * result + Double.hashCode(burst.speed);
        }
        return result;
    }

    private static void updateZoom(TpsConfig cfg) {
        if (cfg != null && cfg.visual.zoomEnabled && AtomicsClient.isZoomKeyPressed()) {
            ensureZoomSessionStarted(cfg);
            targetZoomMultiplier = zoomSessionMultiplier;
        } else {
            zoomSessionActive = false;
            zoomSessionMultiplier = 1.0f;
            targetZoomMultiplier = 1.0f;
        }
        if (targetZoomMultiplier == 1.0f && currentZoomMultiplier == 1.0f) {
            lastZoomUpdateNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        float elapsedSeconds = lastZoomUpdateNanos == 0L ? 0.05f : Math.min(0.2f, (now - lastZoomUpdateNanos) / 1_000_000_000.0f);
        lastZoomUpdateNanos = now;
        float amount = Math.min(1.0f, Math.max(0.0f, elapsedSeconds * 14.0f));
        currentZoomMultiplier += (targetZoomMultiplier - currentZoomMultiplier) * amount;
        if (Math.abs(targetZoomMultiplier - currentZoomMultiplier) < 0.01f) {
            currentZoomMultiplier = targetZoomMultiplier;
        }
    }

    private static void ensureZoomSessionStarted(TpsConfig cfg) {
        if (!zoomSessionActive) {
            zoomSessionMultiplier = clampFloat(cfg.visual.zoomMultiplier, ZOOM_MIN, ZOOM_MAX);
            zoomSessionActive = true;
        }
    }

    private static float adjustZoomByScroll(float currentMultiplier, double verticalAmount) {
        double factor = Math.pow(ZOOM_SCROLL_FACTOR, Math.abs(verticalAmount));
        double adjusted = verticalAmount > 0.0
                ? currentMultiplier * factor
                : currentMultiplier / factor;
        return clampFloat((float) adjusted, ZOOM_MIN, ZOOM_MAX);
    }

    private static void applyFullBrightIfNeeded(MinecraftClient client) {
        StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (effect == null
                || effect.getAmplifier() != 0
                || effect.getDuration() < 220
                || effect.shouldShowParticles()
                || effect.shouldShowIcon()) {
            client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, false, false, false));
        }
        fullBrightApplied = true;
    }

    private static void disableFullBrightIfNeeded(MinecraftClient client) {
        if (!fullBrightApplied || client.player == null) {
            return;
        }
        StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (effect != null
                && effect.getAmplifier() == 0
                && effect.getDuration() <= 280
                && !effect.shouldShowParticles()
                && !effect.shouldShowIcon()) {
            client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        fullBrightApplied = false;
    }

    private static void disableTimeChangerIfNeeded(MinecraftClient client) {
        if (!timeChangerApplied || client.world == null) {
            return;
        }
        client.world.setTime(client.world.getTime(), client.world.getTimeOfDay(), true);
        timeChangerApplied = false;
    }

    private static float clampFloat(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static TpsConfig liveConfig() {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled) {
            return null;
        }
        return AtomicsClient.CONFIG;
    }

    private record TeamCount(String name, int color, int count) {
    }

    private record PreparedParticleBurst(TpsConfig.ParticleBurst burst, ParticleEffect effect, int count) {
    }
}
