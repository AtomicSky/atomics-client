package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ClientFeatureManager {
    private static final long REACH_DISPLAY_MS = 1400L;
    private static final float ZOOM_MIN = 1.5f;
    private static final float ZOOM_MAX = 8.0f;
    private static final float ZOOM_SCROLL_STEP = 0.25f;
    private static final int ARMOR_HUD_EMPTY_OFFHAND_SHIFT = 28;
    private static final Identifier HOTBAR_SPRITE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");

    private static long lastReachMillis;
    private static String lastReachTextString = "";
    private static Component lastReachText = Component.empty();
    private static int lastReachTextWidth = -1;
    private static int trailTick;
    private static boolean fullBrightApplied;
    private static boolean timeChangerApplied;
    private static float currentZoomMultiplier = 1.0f;
    private static float targetZoomMultiplier = 1.0f;
    private static long lastZoomUpdateNanos;
    private static long lastZoomFeedbackMillis;
    private static long lastArmorWarningMillis;
    private static String lastArmorWarningKey = "";
    private static final Map<UUID, String> MASKED_PLAYER_NAMES = new HashMap<>();
    private static final Component[] TNT_TIMER_TEXT_CACHE = new Component[2001];
    private static final List<PreparedParticleBurst> PREPARED_PROJECTILE_TRAIL_PARTICLES = new ArrayList<>();
    private static boolean projectileTrailParticleCacheInitialized;
    private static int projectileTrailParticleFingerprint;

    private ClientFeatureManager() {
    }

    public static void tick(Minecraft client) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || client == null || client.player == null || client.level == null) {
            updateZoom(cfg);
            if (client == null || client.level == null) {
                MASKED_PLAYER_NAMES.clear();
            }
            if (client != null && client.player != null) {
                disableFullBrightIfNeeded(client);
            }
            if (client != null && client.level != null) {
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
            client.level.setTimeFromServer(cfg.visual.timeOfDay);
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
    public static void onReachAttack(Player player, Entity target) {
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
        lastReachText = Component.literal(lastReachTextString);
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

    private static double calculateClosestPointReach(Player player, Entity target) {
        Vec3 eyePos = player.getEyePosition();
        AABB box = target.getBoundingBox();

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

    public static void renderHud(GuiGraphicsExtractor context) {
        TpsConfig cfg = liveConfig();
        Minecraft client = Minecraft.getInstance();
        if (cfg == null || client == null || client.player == null || client.level == null) {
            return;
        }

        if (cfg.combat.reachDisplayEnabled) {
            renderReachDisplay(context, client);
        }
        if (cfg.visual.armorHudEnabled || cfg.visual.armorDurabilityWarningEnabled) {
            renderArmorHudAndWarnings(context, client, cfg.visual);
        }
    }

    public static boolean shouldMaskPlayerNames() {
        TpsConfig cfg = liveConfig();
        return cfg != null && cfg.visual.streamerModeEnabled;
    }

    public static String maskedPlayerName(Entity entity) {
        if (entity == null) return "Player";
        UUID uuid = entity.getUUID();
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

    public static Component customizePlayerNametag(Player player, Component originalDisplayName) {
        TpsConfig cfg = liveConfig();
        if (player == null || originalDisplayName == null || cfg == null || cfg.pvp == null) {
            return originalDisplayName;
        }

        MutableComponent baseName = shouldMaskPlayerNames()
                ? Component.literal(maskedPlayerName(player))
                : originalDisplayName.copy();

        List<NametagItem> before = new ArrayList<>();
        List<NametagItem> after = new ArrayList<>();
        List<String> order = cfg.pvp.nametagItemOrder == null ? TpsConfig.defaultNametagItemOrder() : cfg.pvp.nametagItemOrder;
        for (String itemId : order) {
            Component itemText = getNametagItemText(itemId, player, cfg);
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

        MutableComponent result = Component.empty();
        appendNametagItems(result, before);
        if (!before.isEmpty()) {
            result.append(Component.literal(" "));
        }
        result.append(baseName);
        if (!after.isEmpty()) {
            result.append(Component.literal(" "));
            appendNametagItems(result, after);
        }
        return result;
    }

    private static void appendNametagItems(MutableComponent target, List<NametagItem> items) {
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                target.append(Component.literal(" "));
            }
            target.append(items.get(i).text);
        }
    }

    private static Component getNametagItemText(String itemId, Player player, TpsConfig cfg) {
        if (TpsConfig.NAMETAG_ITEM_WIN_ODDS.equals(itemId)) {
            return PvpStatsManager.getWinOddsNameSuffix(player);
        }
        if (TpsConfig.NAMETAG_ITEM_TOTEM_POPS.equals(itemId)) {
            return PvpStatsManager.getTotemPopNameSuffix(player);
        }
        if (TpsConfig.NAMETAG_ITEM_OPPONENT_STATS.equals(itemId)) {
            return TierWeightManager.getNameSuffix(player);
        }
        if (TpsConfig.NAMETAG_ITEM_PING.equals(itemId)) {
            return getPingNametagText(player, cfg);
        }
        return null;
    }

    private static Component getPingNametagText(Player player, TpsConfig cfg) {
        if (cfg == null || cfg.pvp == null || !cfg.pvp.pingNametagEnabled || player == null) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return null;
        }
        PlayerInfo entry = client.getConnection().getPlayerInfo(player.getUUID());
        if (entry == null || entry.getLatency() < 0) {
            return null;
        }
        int ping = entry.getLatency();
        ChatFormatting color = ping <= 75 ? ChatFormatting.GREEN : ping <= 150 ? ChatFormatting.YELLOW : ping <= 250 ? ChatFormatting.GOLD : ChatFormatting.RED;
        return Component.literal(ping + "ms").withStyle(color);
    }

    public static boolean isZoomActive() {
        updateZoom(liveConfig());
        return currentZoomMultiplier > 1.01f || targetZoomMultiplier > 1.01f;
    }

    private record NametagItem(String id, Component text) {
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
        Minecraft client = Minecraft.getInstance();
        TpsConfig cfg = liveConfig();
        if (client == null || client.screen != null || cfg == null || !cfg.visual.zoomEnabled || !AtomicsClient.isZoomKeyPressed()) {
            return false;
        }
        if (Math.abs(verticalAmount) < 0.0001) {
            return false;
        }

        cfg.visual.zoomMultiplier = clampFloat((float) (cfg.visual.zoomMultiplier + verticalAmount * ZOOM_SCROLL_STEP), ZOOM_MIN, ZOOM_MAX);
        if (client.player != null) {
            long now = System.currentTimeMillis();
            if (now - lastZoomFeedbackMillis > 70L) {
                AtomicsClient.sendClientMessage(client.player, Component.literal("Zoom: " + String.format(Locale.US, "%.2fx", cfg.visual.zoomMultiplier)).withStyle(ChatFormatting.AQUA), true);
                lastZoomFeedbackMillis = now;
            }
        }
        return true;
    }

    public static void runMacro(Minecraft client, int index) {
        TpsConfig cfg = liveConfig();
        if (cfg == null || !cfg.macros.enabled || client == null || client.getConnection() == null) {
            return;
        }
        if (index < 0 || index >= cfg.macros.messages.length) {
            return;
        }

        String message = cfg.macros.messages[index];
        if (message == null || message.isBlank()) {
            if (client.player != null) {
                AtomicsClient.sendClientMessage(client.player, Component.literal("Macro " + (index + 1) + " is empty").withStyle(ChatFormatting.YELLOW), true);
            }
            return;
        }

        message = message.trim();
        if (message.startsWith("/") && message.length() > 1) {
            client.getConnection().sendCommand(message.substring(1));
        } else {
            client.getConnection().sendChat(message);
        }
    }

    private static void renderReachDisplay(GuiGraphicsExtractor context, Minecraft client) {
        long age = System.currentTimeMillis() - lastReachMillis;
        if (lastReachMillis <= 0L || age > REACH_DISPLAY_MS) {
            return;
        }

        float alpha = 1.0f - Math.max(0.0f, (age - 900L) / 500.0f);
        int a = Math.max(30, Math.min(150, Math.round(alpha * 150.0f)));
        int color = (a << 24) | 0xB8B8B8;
        float scale = 0.72f;
        if (lastReachTextWidth < 0) {
            lastReachTextWidth = client.font.width(lastReachTextString);
        }
        int x = client.getWindow().getGuiScaledWidth() / 2 - Math.round(lastReachTextWidth * scale / 2.0f);
        int y = client.getWindow().getGuiScaledHeight() / 2 + 12;
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        context.text(client.font, lastReachText, Math.round(x / scale), Math.round(y / scale), color);
        context.pose().popMatrix();
    }

    private static void renderArmorHudAndWarnings(GuiGraphicsExtractor context, Minecraft client, TpsConfig.VisualSettings visual) {
        ArmorPiece[] armor = armorPieces(client.player);
        if (visual.armorHudEnabled) {
            renderArmorHud(context, client, armor, visual);
        }
        if (visual.armorDurabilityWarningEnabled) {
            warnLowArmorDurability(client, armor, TpsConfig.DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT);
        }
    }

    private static void renderArmorHud(GuiGraphicsExtractor context, Minecraft client, ArmorPiece[] armor, TpsConfig.VisualSettings visual) {
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
            x = client.getWindow().getGuiScaledWidth() / 2 - 127 - hudWidth;
            y = client.getWindow().getGuiScaledHeight() - 22;
            if (vertical) {
                y = client.getWindow().getGuiScaledHeight() / 2 - hudHeight / 2;
            } else if (autoPosition && client.player != null && client.player.getOffhandItem().isEmpty()) {
                x += ARMOR_HUD_EMPTY_OFFHAND_SHIFT;
            }
        }
        x = Math.max(0, Math.min(client.getWindow().getGuiScaledWidth() - hudWidth, x));
        y = Math.max(0, Math.min(client.getWindow().getGuiScaledHeight() - hudHeight, y));

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
            context.item(piece.stack(), itemX, itemY);
            context.itemDecorations(client.font, piece.stack(), itemX, itemY);

            String durability = armorDurabilityText(piece, visual.armorHudDurabilityMode);
            if (!durability.isEmpty()) {
                int color = armorDurabilityColor(piece.percent(), warningPercent);
                float scale = 0.68f;
                int textWidth = client.font.width(durability);
                int textX = Math.round((itemX + 8.0f - textWidth * scale / 2.0f) / scale);
                int textY = Math.round((itemY + 18) / scale);
                context.pose().pushMatrix();
                context.pose().scale(scale, scale);
                context.text(client.font, durability, textX, textY, color);
                context.pose().popMatrix();
            }
            if (vertical) {
                y += spacing;
            } else {
                x += spacing;
            }
        }
    }

    private static void renderArmorHudSlot(GuiGraphicsExtractor context, int x, int y, int visible) {
        context.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE_TEXTURE, x, y, 0.0f, 0.0f, 22, 22, 182, 22);
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

    private static void warnLowArmorDurability(Minecraft client, ArmorPiece[] armor, int warningPercent) {
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
            AtomicsClient.sendClientMessage(client.player, Component.literal("Low armor durability: " + lowest.slotName() + " " + lowest.remaining() + "/" + lowest.maxDamage())
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            lastArmorWarningKey = key;
            lastArmorWarningMillis = now;
        }
    }

    private static ArmorPiece[] armorPieces(Player player) {
        return new ArmorPiece[] {
                armorPiece(player, EquipmentSlot.HEAD, "Helmet"),
                armorPiece(player, EquipmentSlot.CHEST, "Chestplate"),
                armorPiece(player, EquipmentSlot.LEGS, "Leggings"),
                armorPiece(player, EquipmentSlot.FEET, "Boots")
        };
    }

    private static ArmorPiece armorPiece(Player player, EquipmentSlot slot, String slotName) {
        if (player == null) {
            return new ArmorPiece(ItemStack.EMPTY, slotName, 0, 0, 100);
        }
        ItemStack stack = player.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) {
            return new ArmorPiece(ItemStack.EMPTY, slotName, 0, 0, 100);
        }
        if (!stack.isDamageableItem()) {
            return new ArmorPiece(stack, slotName, 0, 0, 100);
        }
        int maxDamage = Math.max(1, stack.getMaxDamage());
        int remaining = Math.max(0, maxDamage - stack.getDamageValue());
        int percent = Math.round(remaining * 100.0f / maxDamage);
        return new ArmorPiece(stack, slotName, remaining, maxDamage, percent);
    }

    private static int armorDurabilityColor(int percent, int warningPercent) {
        if (percent <= warningPercent) return 0xFFFF5555;
        if (percent <= Math.max(warningPercent + 15, 40)) return 0xFFFFAA00;
        return 0xFF55FF55;
    }

    public static boolean shouldShowTntTimer(PrimedTnt tnt) {
        TpsConfig cfg = liveConfig();
        Minecraft client = Minecraft.getInstance();
        if (cfg == null || client == null || client.player == null || tnt == null || !cfg.visual.tntTimerEnabled) {
            return false;
        }
        double rangeSq = cfg.visual.tntTimerRange * cfg.visual.tntTimerRange;
        return tnt.distanceToSqr(client.player) <= rangeSq;
    }

    public static Component tntTimerText(PrimedTnt tnt) {
        int fuse = Math.max(0, tnt.getFuse());
        if (fuse < TNT_TIMER_TEXT_CACHE.length) {
            Component cached = TNT_TIMER_TEXT_CACHE[fuse];
            if (cached != null) {
                return cached;
            }
            Component created = createTntTimerText(fuse);
            TNT_TIMER_TEXT_CACHE[fuse] = created;
            return created;
        }
        return createTntTimerText(fuse);
    }

    private static Component createTntTimerText(int fuse) {
        float seconds = fuse / 20.0f;
        ChatFormatting color = seconds <= 1.0f ? ChatFormatting.RED : seconds <= 2.0f ? ChatFormatting.GOLD : ChatFormatting.GRAY;
        return Component.literal(String.format(Locale.US, "%.1fs", seconds)).withStyle(color);
    }

    private static void spawnProjectileTrails(Minecraft client, TpsConfig.VisualSettings visual) {
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
        RandomSource random = client.level.getRandom();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Projectile) || entity.distanceToSqr(client.player) > rangeSq) {
                continue;
            }
            if (entity.getDeltaMovement().lengthSqr() < 0.0004) {
                continue;
            }

            for (PreparedParticleBurst prepared : preparedParticles) {
                TpsConfig.ParticleBurst burst = prepared.burst;
                ParticleOptions effect = prepared.effect;
                int count = prepared.count;
                for (int i = 0; i < count; i++) {
                    double x = entity.getX() + (random.nextDouble() - 0.5) * burst.spreadX;
                    double y = entity.getY() + (random.nextDouble() - 0.5) * burst.spreadY;
                    double z = entity.getZ() + (random.nextDouble() - 0.5) * burst.spreadZ;
                    double vx = (random.nextDouble() - 0.5) * burst.speed;
                    double vy = (random.nextDouble() - 0.5) * burst.speed;
                    double vz = (random.nextDouble() - 0.5) * burst.speed;
                    client.particleEngine.createParticle(effect, x, y, z, vx, vy, vz);
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
            ParticleOptions effect = TotemPopEffects.getParticleEffect(burst.particle);
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
        targetZoomMultiplier = cfg != null && cfg.visual.zoomEnabled && AtomicsClient.isZoomKeyPressed()
                ? clampFloat(cfg.visual.zoomMultiplier, ZOOM_MIN, ZOOM_MAX)
                : 1.0f;
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

    private static void applyFullBrightIfNeeded(Minecraft client) {
        MobEffectInstance effect = client.player.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null
                || effect.getAmplifier() != 0
                || effect.getDuration() < 220
                || effect.isVisible()
                || effect.showIcon()) {
            client.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, false));
        }
        fullBrightApplied = true;
    }

    private static void disableFullBrightIfNeeded(Minecraft client) {
        if (!fullBrightApplied || client.player == null) {
            return;
        }
        MobEffectInstance effect = client.player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null
                && effect.getAmplifier() == 0
                && effect.getDuration() <= 280
                && !effect.isVisible()
                && !effect.showIcon()) {
            client.player.removeEffect(MobEffects.NIGHT_VISION);
        }
        fullBrightApplied = false;
    }

    private static void disableTimeChangerIfNeeded(Minecraft client) {
        if (!timeChangerApplied || client.level == null) {
            return;
        }
        client.level.setTimeFromServer(client.level.getOverworldClockTime());
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

    private record PreparedParticleBurst(TpsConfig.ParticleBurst burst, ParticleOptions effect, int count) {
    }
}
