package com.atomics.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class TpsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("atomics_client");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String DEFAULT_PARTICLE_ID = "minecraft:totem_of_undying";
    public static final int DEFAULT_PARTICLE_COUNT = 80;
    public static final String DEFAULT_PARTICLE_SHAPE = "random";
    public static final double DEFAULT_PARTICLE_SPREAD = 0.65;
    public static final double DEFAULT_PARTICLE_VERTICAL_SPREAD = 0.95;
    public static final double DEFAULT_PARTICLE_SPEED = 0.12;
    public static final boolean DEFAULT_TOTEM_PARTICLE_COLOR_ENABLED = false;
    public static final int DEFAULT_TOTEM_PARTICLE_COLOR_R = 85;
    public static final int DEFAULT_TOTEM_PARTICLE_COLOR_G = 255;
    public static final int DEFAULT_TOTEM_PARTICLE_COLOR_B = 120;
    public static final float DEFAULT_TOTEM_PARTICLE_COLOR_SCALE = 1.0f;
    public static final String DEFAULT_SOUND_ID = "minecraft:item.totem.use";
    public static final float DEFAULT_SOUND_VOLUME = 1.0f;
    public static final float DEFAULT_SOUND_PITCH = 1.0f;
    public static final int DEFAULT_SOUND_DELAY_TICKS = 0;
    public static final float DEFAULT_HAND_SCALE = 1.0f;
    public static final float DEFAULT_DROPPED_SCALE = 1.0f;
    public static final float DEFAULT_POP_SCALE = 1.0f;
    public static final int DEFAULT_POP_ANIMATION_TICKS = 40;
    public static final String DEFAULT_RETEXTURE_ITEM_ID = "minecraft:totem_of_undying";
    public static final boolean DEFAULT_TOTEM_COLOR_OVERLAY_ENABLED = false;
    public static final float DEFAULT_TOTEM_OVERLAY_HUE = 0.0f;
    public static final float DEFAULT_TOTEM_OVERLAY_ALPHA = 1.0f;
    public static final boolean DEFAULT_MISC_SHIELD_DOWN_ENABLED = false;
    public static final boolean DEFAULT_MISC_SHIELD_UP_ENABLED = false;
    public static final boolean DEFAULT_SHIELD_WARNING_OVERLAY_ENABLED = true;
    public static final boolean DEFAULT_MISC_FIRE_OVERLAY_ENABLED = false;
    public static final boolean DEFAULT_EMPTY_BUCKET_OVERLAY_ENABLED = false;
    public static final boolean DEFAULT_REACH_DISPLAY_ENABLED = false;
    public static final boolean DEFAULT_OPPONENT_INFO_ENABLED = false;
    public static final boolean DEFAULT_DUAL_SPECTATE_OVERHEAD_ENABLED = false;
    public static final float DEFAULT_DUAL_SPECTATE_OVERHEAD_GROUP_DISTANCE = 24.0f;
    public static final float DEFAULT_DUAL_SPECTATE_MAX_Y_DIFFERENCE = 12.0f;
    public static final boolean DEFAULT_FRIEND_FOE_OVERLAY_ENABLED = false;
    public static final int DEFAULT_FRIEND_OVERLAY_R = 60;
    public static final int DEFAULT_FRIEND_OVERLAY_G = 255;
    public static final int DEFAULT_FRIEND_OVERLAY_B = 110;
    public static final float DEFAULT_FRIEND_OVERLAY_ALPHA = 0.35f;
    public static final int DEFAULT_FOE_OVERLAY_R = 255;
    public static final int DEFAULT_FOE_OVERLAY_G = 60;
    public static final int DEFAULT_FOE_OVERLAY_B = 60;
    public static final float DEFAULT_FOE_OVERLAY_ALPHA = 0.35f;
    public static final boolean DEFAULT_TEAM_COUNT_OVERLAY_ENABLED = false;
    public static final boolean DEFAULT_TEAM_COUNT_OVERLAY_SERVER_FILTER_ENABLED = false;
    public static final int DEFAULT_TEAM_COUNT_OVERLAY_X = -1;
    public static final int DEFAULT_TEAM_COUNT_OVERLAY_Y = -1;
    public static final String FRIEND_FOE_STYLE_FULL = "full";
    public static final String FRIEND_FOE_STYLE_OUTLINE = "outline";
    public static final String FRIEND_FOE_STYLE_OUTLINE_FULL = "outline_full";
    public static final String FRIEND_FOE_STYLE_PULSE = "pulse";
    public static final String DEFAULT_FRIEND_FOE_OVERLAY_STYLE = FRIEND_FOE_STYLE_FULL;
    public static final String NAMETAG_ITEM_WIN_ODDS = "win_odds";
    public static final String NAMETAG_ITEM_TOTEM_POPS = "totem_pops";
    public static final String NAMETAG_ITEM_OPPONENT_STATS = "opponent_stats";
    public static final String NAMETAG_ITEM_PING = "ping";
    public static final boolean DEFAULT_PING_NAMETAG_ENABLED = false;
    public static final boolean DEFAULT_TOTEM_POP_NAMETAG_ENABLED = false;
    public static final boolean DEFAULT_OPPONENT_STATS_NAMETAG_ENABLED = false;
    public static final String OPPONENT_STATS_NAMETAG_ICON_TIER = "icon_tier";
    public static final String OPPONENT_STATS_NAMETAG_TIER = "tier";
    public static final String OPPONENT_STATS_NAMETAG_MODE_TIER = "mode_tier";
    public static final String DEFAULT_OPPONENT_STATS_NAMETAG_FORMAT = OPPONENT_STATS_NAMETAG_ICON_TIER;
    public static final boolean DEFAULT_FULL_BRIGHT_ENABLED = false;
    public static final boolean DEFAULT_ARMOR_HUD_ENABLED = true;
    public static final boolean DEFAULT_ARMOR_HUD_AUTO_POSITION = false;
    public static final boolean DEFAULT_ARMOR_DURABILITY_WARNING_ENABLED = true;
    public static final int DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT = 10;
    public static final int DEFAULT_ARMOR_HUD_X = -1;
    public static final int DEFAULT_ARMOR_HUD_Y = -1;
    public static final boolean DEFAULT_ARMOR_HUD_VERTICAL = false;
    public static final int DEFAULT_ARMOR_HUD_SPACING = 20;
    public static final boolean DEFAULT_ARMOR_HUD_HOTBAR_BORDER = true;
    public static final String ARMOR_HUD_DURABILITY_PERCENT = "percent";
    public static final String ARMOR_HUD_DURABILITY_NUMBER = "number";
    public static final String ARMOR_HUD_DURABILITY_BAR = "bar";
    public static final String DEFAULT_ARMOR_HUD_DURABILITY_MODE = ARMOR_HUD_DURABILITY_BAR;
    public static final boolean DEFAULT_TIME_CHANGER_ENABLED = false;
    public static final int DEFAULT_TIME_OF_DAY = 6000;
    public static final boolean DEFAULT_TNT_TIMER_ENABLED = false;
    public static final int DEFAULT_TNT_TIMER_RANGE = 64;
    public static final boolean DEFAULT_PROJECTILE_TRAIL_ENABLED = false;
    public static final String DEFAULT_PROJECTILE_TRAIL_PARTICLE_ID = "minecraft:crit";
    public static final int DEFAULT_PROJECTILE_TRAIL_PARTICLE_COUNT = 1;
    public static final double DEFAULT_PROJECTILE_TRAIL_SPREAD = 0.03;
    public static final double DEFAULT_PROJECTILE_TRAIL_SPEED = 0.0;
    public static final boolean DEFAULT_FOOD_OVERLAY_ENABLED = false;
    public static final float DEFAULT_FOOD_OVERLAY_HUE = 100.0f;
    public static final float DEFAULT_FOOD_OVERLAY_ALPHA = 0.5f;
    public static final boolean DEFAULT_PARTIAL_STATUS_ICONS_ENABLED = true;
    public static final boolean DEFAULT_STREAMER_MODE_ENABLED = false;
    public static final float MIN_ZOOM_MULTIPLIER = 1.0f;
    public static final float MAX_ZOOM_MULTIPLIER = 300.0f;
    public static final boolean DEFAULT_ZOOM_ENABLED = true;
    public static final float DEFAULT_ZOOM_MULTIPLIER = 4.0f;
    public static final boolean DEFAULT_FREELOOK_ENABLED = true;
    public static final boolean DEFAULT_FREELOOK_TOGGLE_MODE = false;
    public static final int MIN_MACRO_SLOTS = 4;
    public static final int MAX_MACRO_SLOTS = 12;
    public static final boolean DEFAULT_AUTO_VILLAGER_TRADER_ENABLED = false;
    public static final String DEFAULT_AUTO_VILLAGER_TRADER_PROFESSION = "minecraft:librarian";
    public static final String DEFAULT_AUTO_VILLAGER_TRADER_TRADE = "enchanted_book";
    public static final String DEFAULT_AUTO_VILLAGER_TRADER_ENCHANTMENT = "";
    public static final boolean DEFAULT_AUTO_VILLAGER_TRADER_CHECK_CHESTS = false;
    public static final boolean DEFAULT_AUTO_VILLAGER_TRADER_AUTO_CLOSE_MERCHANT = false;
    public static final double DEFAULT_AUTO_VILLAGER_TRADER_RANGE = 4.5;
    public static final String DEFAULT_VILLAGER_LEVELER_TARGET_UUID = "";
    public static final boolean DEFAULT_VILLAGER_LEVELER_AUTO_DROP_ITEMS = false;
    public static final double MIN_AUTO_VILLAGER_TRADER_RANGE = 2.0;
    public static final double MAX_AUTO_VILLAGER_TRADER_RANGE = 6.0;
    public static final float DEFAULT_SHIELD_DOWN_X = 0.0f;
    public static final float DEFAULT_SHIELD_DOWN_Y = 0.0f;
    public static final float DEFAULT_SHIELD_DOWN_Z = 0.0f;
    public static final float DEFAULT_SHIELD_DOWN_ROT_X = 0.0f;
    public static final float DEFAULT_SHIELD_DOWN_ROT_Y = 0.0f;
    public static final float DEFAULT_SHIELD_DOWN_ROT_Z = 0.0f;
    public static final float DEFAULT_SHIELD_UP_X = 0.0f;
    public static final float DEFAULT_SHIELD_UP_Y = 0.0f;
    public static final float DEFAULT_SHIELD_UP_Z = 0.0f;
    public static final float DEFAULT_SHIELD_UP_ROT_X = 0.0f;
    public static final float DEFAULT_SHIELD_UP_ROT_Y = 0.0f;
    public static final float DEFAULT_SHIELD_UP_ROT_Z = 0.0f;
    public static final float DEFAULT_FIRE_OVERLAY_HEIGHT = 0.0f;
    public static final int DEFAULT_SHIELD_WARNING_OVERLAY_R = 255;
    public static final int DEFAULT_SHIELD_WARNING_OVERLAY_G = 35;
    public static final int DEFAULT_SHIELD_WARNING_OVERLAY_B = 35;
    public static final float DEFAULT_SHIELD_WARNING_OVERLAY_ALPHA = 1.0f;
    public static final int DEFAULT_EMPTY_BUCKET_OVERLAY_R = 255;
    public static final int DEFAULT_EMPTY_BUCKET_OVERLAY_G = 40;
    public static final int DEFAULT_EMPTY_BUCKET_OVERLAY_B = 40;
    public static final float DEFAULT_EMPTY_BUCKET_OVERLAY_ALPHA = 1.0f;

    public boolean enabled = true;

    public ParticleSettings particles = new ParticleSettings();
    public SoundSettings sounds = new SoundSettings();
    public ItemSettings item = new ItemSettings();
    public PopOverlaySettings popOverlay = new PopOverlaySettings();
    public RetextureSettings retexture = new RetextureSettings();
    public UtilitySettings utility = new UtilitySettings();
    public MiscSettings misc = new MiscSettings();
    public PvpSettings pvp = new PvpSettings();
    public CombatSettings combat = new CombatSettings();
    public VisualSettings visual = new VisualSettings();
    public MacroSettings macros = new MacroSettings();
    public InventorySorterSettings inventorySorter = new InventorySorterSettings();
    public UiSettings ui = new UiSettings();

    public static TpsConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("atomics_client.json");
        try {
            if (!Files.exists(path)) {
                TpsConfig cfg = new TpsConfig();
                cfg.save(path);
                return cfg;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                TpsConfig cfg = GSON.fromJson(reader, TpsConfig.class);
                return cfg == null ? new TpsConfig() : cfg.normalize();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
            return new TpsConfig();
        }
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(this, writer);
        }
    }

    public TpsConfig normalize() {
        if (particles == null) particles = new ParticleSettings();
        if (sounds == null) sounds = new SoundSettings();
        if (item == null) item = new ItemSettings();
        if (popOverlay == null) popOverlay = new PopOverlaySettings();
        if (retexture == null) retexture = new RetextureSettings();
        if (utility == null) utility = new UtilitySettings();
        if (misc == null) misc = new MiscSettings();
        if (pvp == null) pvp = new PvpSettings();
        if (combat == null) combat = new CombatSettings();
        if (visual == null) visual = new VisualSettings();
        if (macros == null) macros = new MacroSettings();
        if (inventorySorter == null) inventorySorter = new InventorySorterSettings();
        if (ui == null) ui = new UiSettings();
        visual.timeOfDay = clampInt(visual.timeOfDay, 0, 24000);
        visual.tntTimerRange = clampInt(visual.tntTimerRange, 8, 128);
        visual.zoomMultiplier = clampFloat(visual.zoomMultiplier, MIN_ZOOM_MULTIPLIER, MAX_ZOOM_MULTIPLIER);
        visual.foodOverlayHue = clampFloat(visual.foodOverlayHue, -180.0f, 180.0f);
        visual.foodOverlayAlpha = clampFloat(visual.foodOverlayAlpha, 0.0f, 1.0f);
        visual.armorDurabilityWarningPercent = DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
        visual.armorHudX = clampInt(visual.armorHudX, -1, 10000);
        visual.armorHudY = clampInt(visual.armorHudY, -1, 10000);
        visual.armorHudSpacing = clampInt(visual.armorHudSpacing, 20, 64);
        if (!ARMOR_HUD_DURABILITY_PERCENT.equals(visual.armorHudDurabilityMode)
                && !ARMOR_HUD_DURABILITY_NUMBER.equals(visual.armorHudDurabilityMode)
                && !ARMOR_HUD_DURABILITY_BAR.equals(visual.armorHudDurabilityMode)) {
            visual.armorHudDurabilityMode = DEFAULT_ARMOR_HUD_DURABILITY_MODE;
        }
        if (macros.messages == null
                || macros.messages.length < MIN_MACRO_SLOTS
                || macros.messages.length > MAX_MACRO_SLOTS) {
            int size = macros.messages == null
                    ? MIN_MACRO_SLOTS
                    : Math.max(MIN_MACRO_SLOTS, Math.min(MAX_MACRO_SLOTS, macros.messages.length));
            String[] normalized = new String[size];
            if (macros.messages != null) {
                System.arraycopy(macros.messages, 0, normalized, 0, Math.min(macros.messages.length, normalized.length));
            }
            macros.messages = normalized;
        }
        for (int i = 0; i < macros.messages.length; i++) {
            if (macros.messages[i] == null) {
                macros.messages[i] = "";
            } else if (macros.messages[i].length() > 256) {
                macros.messages[i] = macros.messages[i].substring(0, 256);
            }
        }
        if (ui.collapsedSections == null) {
            ui.collapsedSections = new ArrayList<>();
        } else {
            ui.collapsedSections.removeIf(id -> id == null || id.isBlank());
            ui.collapsedSections = new ArrayList<>(new LinkedHashSet<>(ui.collapsedSections));
        }
        utility.autoVillagerTraderProfession = normalizeBoundedText(utility.autoVillagerTraderProfession, 64);
        utility.autoVillagerTraderTrade = normalizeBoundedText(utility.autoVillagerTraderTrade, 128);
        utility.autoVillagerTraderEnchantment = normalizeBoundedText(utility.autoVillagerTraderEnchantment, 64);
        utility.autoVillagerTraderRange = DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
        utility.villagerLevelerTargetUuid = normalizeBoundedText(utility.villagerLevelerTargetUuid, 64);
        normalizeInventorySorter();
        pvp.spoofedHealthMode = PVP_HEALTH_MODE_PREFER_SRV;
        if (pvp.dualSpectatePlayerOne == null) pvp.dualSpectatePlayerOne = "";
        if (pvp.dualSpectatePlayerTwo == null) pvp.dualSpectatePlayerTwo = "";
        if (pvp.friendNames == null) pvp.friendNames = new ArrayList<>();
        if (pvp.foeNames == null) pvp.foeNames = new ArrayList<>();
        normalizeNameList(pvp.friendNames);
        normalizeNameList(pvp.foeNames);
        pvp.friendOverlayR = clampInt(pvp.friendOverlayR, 0, 255);
        pvp.friendOverlayG = clampInt(pvp.friendOverlayG, 0, 255);
        pvp.friendOverlayB = clampInt(pvp.friendOverlayB, 0, 255);
        pvp.friendOverlayAlpha = clampFloat(pvp.friendOverlayAlpha, 0.0f, 1.0f);
        pvp.friendFoeOverlayStyle = normalizeFriendFoeStyle(pvp.friendFoeOverlayStyle);
        pvp.foeOverlayR = clampInt(pvp.foeOverlayR, 0, 255);
        pvp.foeOverlayG = clampInt(pvp.foeOverlayG, 0, 255);
        pvp.foeOverlayB = clampInt(pvp.foeOverlayB, 0, 255);
        pvp.foeOverlayAlpha = clampFloat(pvp.foeOverlayAlpha, 0.0f, 1.0f);
        pvp.nametagItemOrder = normalizeNametagItems(pvp.nametagItemOrder, false);
        pvp.nametagItemsBeforeName = normalizeNametagItems(pvp.nametagItemsBeforeName, true);
        pvp.opponentStatsNametagFormat = normalizeOpponentStatsNametagFormat(pvp.opponentStatsNametagFormat);
        pvp.dualSpectatePadding = Math.max(1.0f, Math.min(2.5f, pvp.dualSpectatePadding));
        pvp.dualSpectateMinDistance = Math.max(2.0f, Math.min(30.0f, pvp.dualSpectateMinDistance));
        pvp.dualSpectateMaxDistance = Math.max(10.0f, Math.min(160.0f, pvp.dualSpectateMaxDistance));
        pvp.dualSpectateOverheadGroupDistance = clampFloat(pvp.dualSpectateOverheadGroupDistance, 4.0f, 80.0f);
        pvp.dualSpectateMaxYDifference = clampFloat(pvp.dualSpectateMaxYDifference, 2.0f, 48.0f);
        if (pvp.dualSpectateMaxDistance < pvp.dualSpectateMinDistance) {
            pvp.dualSpectateMaxDistance = pvp.dualSpectateMinDistance;
        }
        pvp.teamCountOverlayX = clampInt(pvp.teamCountOverlayX, -1, 10000);
        pvp.teamCountOverlayY = clampInt(pvp.teamCountOverlayY, -1, 10000);
        pvp.teamCountOverlayAllowedServers = normalizeServerList(pvp.teamCountOverlayAllowedServers);
        if (particles.disabledParticleIds == null) {
            particles.disabledParticleIds = new ArrayList<>();
        } else {
            particles.disabledParticleIds.removeIf(id -> id == null || id.isBlank());
            particles.disabledParticleIds = new ArrayList<>(new LinkedHashSet<>(particles.disabledParticleIds));
        }
        if (particles.bursts == null || particles.bursts.isEmpty()) {
            particles.bursts = new ArrayList<>(List.of(defaultParticleBurst()));
        }
        for (ParticleBurst burst : particles.bursts) {
            if (burst.particle == null || burst.particle.isBlank()) {
                burst.particle = DEFAULT_PARTICLE_ID;
            }
            if (burst.shape == null || burst.shape.isBlank()) {
                burst.shape = DEFAULT_PARTICLE_SHAPE;
            }
            burst.count = clampInt(burst.count, 0, 1000);
            burst.spreadX = clampDouble(burst.spreadX, 0.0, 8.0);
            burst.spreadY = clampDouble(burst.spreadY, 0.0, 8.0);
            burst.spreadZ = clampDouble(burst.spreadZ, 0.0, 8.0);
            burst.speed = clampDouble(burst.speed, 0.0, 4.0);
        }
        if (visual.projectileTrailParticles == null || visual.projectileTrailParticles.isEmpty()) {
            visual.projectileTrailParticles = new ArrayList<>(List.of(defaultProjectileTrailParticle()));
        }
        for (ParticleBurst burst : visual.projectileTrailParticles) {
            if (burst.particle == null || burst.particle.isBlank()) {
                burst.particle = DEFAULT_PROJECTILE_TRAIL_PARTICLE_ID;
            }
            if (burst.shape == null || burst.shape.isBlank()) {
                burst.shape = DEFAULT_PARTICLE_SHAPE;
            }
            burst.count = clampInt(burst.count, 0, 1000);
            burst.spreadX = clampDouble(burst.spreadX, 0.0, 8.0);
            burst.spreadY = clampDouble(burst.spreadY, 0.0, 8.0);
            burst.spreadZ = clampDouble(burst.spreadZ, 0.0, 8.0);
            burst.speed = clampDouble(burst.speed, 0.0, 4.0);
        }
        if (sounds.sounds == null || sounds.sounds.isEmpty()) {
            sounds.sounds = new ArrayList<>(List.of(defaultSoundPlay()));
        }
        for (SoundPlay sound : sounds.sounds) {
            if (sound.sound == null || sound.sound.isBlank()) {
                sound.sound = DEFAULT_SOUND_ID;
            }
            sound.delayTicks = Math.max(0, sound.delayTicks);
        }
        if (retexture.itemId == null || retexture.itemId.isBlank()) {
            retexture.itemId = DEFAULT_RETEXTURE_ITEM_ID;
        }
        misc.shieldWarningOverlayR = clampInt(misc.shieldWarningOverlayR, 0, 255);
        misc.shieldWarningOverlayG = clampInt(misc.shieldWarningOverlayG, 0, 255);
        misc.shieldWarningOverlayB = clampInt(misc.shieldWarningOverlayB, 0, 255);
        misc.shieldWarningOverlayAlpha = clampFloat(misc.shieldWarningOverlayAlpha, 0.0f, 1.0f);
        misc.emptyBucketOverlayR = clampInt(misc.emptyBucketOverlayR, 0, 255);
        misc.emptyBucketOverlayG = clampInt(misc.emptyBucketOverlayG, 0, 255);
        misc.emptyBucketOverlayB = clampInt(misc.emptyBucketOverlayB, 0, 255);
        misc.emptyBucketOverlayAlpha = clampFloat(misc.emptyBucketOverlayAlpha, 0.0f, 1.0f);
        return this;
    }

    private void normalizeInventorySorter() {
        if (inventorySorter.kits == null) {
            inventorySorter.kits = new ArrayList<>();
            return;
        }

        LinkedHashSet<String> seenIds = new LinkedHashSet<>();
        for (int i = 0; i < inventorySorter.kits.size(); i++) {
            InventorySortKit kit = inventorySorter.kits.get(i);
            if (kit == null) {
                kit = new InventorySortKit();
                inventorySorter.kits.set(i, kit);
            }

            kit.id = normalizeKitId(kit.id, seenIds);
            kit.name = normalizeKitName(kit.name, i + 1);
            kit.serverAddress = kit.serverAddress == null ? "" : kit.serverAddress.trim();
            kit.beforeSlots = normalizeInventorySortSlots(kit.beforeSlots);
            kit.afterSlots = normalizeInventorySortSlots(kit.afterSlots);
        }
    }

    private static String normalizeKitId(String id, LinkedHashSet<String> seenIds) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty() || seenIds.contains(normalized)) {
            normalized = "kit-" + System.currentTimeMillis() + "-" + seenIds.size();
            while (seenIds.contains(normalized)) {
                normalized = normalized + "x";
            }
        }
        seenIds.add(normalized);
        return normalized;
    }

    private static String normalizeKitName(String name, int fallbackIndex) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            normalized = "Kit " + fallbackIndex;
        }
        return normalized.length() > 48 ? normalized.substring(0, 48) : normalized;
    }

    private static List<String> normalizeInventorySortSlots(List<String> slots) {
        ArrayList<String> normalized = new ArrayList<>(InventorySortKit.SLOT_COUNT);
        if (slots != null) {
            for (String slot : slots) {
                normalized.add(slot == null ? "" : slot);
                if (normalized.size() >= InventorySortKit.SLOT_COUNT) {
                    break;
                }
            }
        }
        while (normalized.size() < InventorySortKit.SLOT_COUNT) {
            normalized.add("");
        }
        return normalized;
    }

    public static String normalizeFriendFoeStyle(String value) {
        if (FRIEND_FOE_STYLE_OUTLINE.equals(value)
                || FRIEND_FOE_STYLE_OUTLINE_FULL.equals(value)
                || FRIEND_FOE_STYLE_PULSE.equals(value)
                || FRIEND_FOE_STYLE_FULL.equals(value)) {
            return value;
        }
        return DEFAULT_FRIEND_FOE_OVERLAY_STYLE;
    }

    public static List<String> defaultNametagItemOrder() {
        return new ArrayList<>(List.of(NAMETAG_ITEM_WIN_ODDS, NAMETAG_ITEM_TOTEM_POPS, NAMETAG_ITEM_OPPONENT_STATS, NAMETAG_ITEM_PING));
    }

    public static boolean isKnownNametagItem(String item) {
        return NAMETAG_ITEM_WIN_ODDS.equals(item)
                || NAMETAG_ITEM_TOTEM_POPS.equals(item)
                || NAMETAG_ITEM_OPPONENT_STATS.equals(item)
                || NAMETAG_ITEM_PING.equals(item);
    }

    private static List<String> normalizeNametagItems(List<String> items, boolean omitMissingDefaults) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (items != null) {
            for (String item : items) {
                if (isKnownNametagItem(item)) {
                    normalized.add(item);
                }
            }
        }
        if (!omitMissingDefaults) {
            for (String item : defaultNametagItemOrder()) {
                normalized.add(item);
            }
        }
        return new ArrayList<>(normalized);
    }

    public static String normalizeOpponentStatsNametagFormat(String value) {
        if (OPPONENT_STATS_NAMETAG_TIER.equals(value)
                || OPPONENT_STATS_NAMETAG_MODE_TIER.equals(value)
                || OPPONENT_STATS_NAMETAG_ICON_TIER.equals(value)) {
            return value;
        }
        return DEFAULT_OPPONENT_STATS_NAMETAG_FORMAT;
    }
    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeBoundedText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private static void normalizeNameList(List<String> names) {
        if (names == null) {
            return;
        }
        names.removeIf(name -> name == null || name.isBlank());
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).trim();
            names.set(i, name.length() > 16 ? name.substring(0, 16) : name);
        }
        names.removeIf(name -> !seen.add(name.toLowerCase(java.util.Locale.ROOT)));
    }

    private static String normalizeServerList(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        LinkedHashSet<String> servers = new LinkedHashSet<>();
        for (String server : value.split("[,;\\s]+")) {
            String normalized = server == null ? "" : server.trim();
            if (!normalized.isEmpty()) {
                servers.add(normalized.length() > 128 ? normalized.substring(0, 128) : normalized);
            }
        }
        return String.join(", ", servers);
    }

    public static class ParticleSettings {
        public boolean enabled = true;
        /**
         * Only recolors bursts that use minecraft:totem_of_undying.
         * Other particles in the editor keep their normal color.
         */
        public boolean customTotemColorEnabled = DEFAULT_TOTEM_PARTICLE_COLOR_ENABLED;
        public int totemColorR = DEFAULT_TOTEM_PARTICLE_COLOR_R;
        public int totemColorG = DEFAULT_TOTEM_PARTICLE_COLOR_G;
        public int totemColorB = DEFAULT_TOTEM_PARTICLE_COLOR_B;
        public float totemColorScale = DEFAULT_TOTEM_PARTICLE_COLOR_SCALE;
        public List<String> disabledParticleIds = new ArrayList<>();
        public List<ParticleBurst> bursts = new ArrayList<>(List.of(
                defaultParticleBurst()
        ));
    }

    public static class ParticleBurst {
        public String particle;
        public int count;
        public String shape;
        public double spreadX;
        public double spreadY;
        public double spreadZ;
        public double speed;
        public ParticleBurst(String particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
            this(particle, count, DEFAULT_PARTICLE_SHAPE, spreadX, spreadY, spreadZ, speed);
        }
        public ParticleBurst(String particle, int count, String shape, double spreadX, double spreadY, double spreadZ, double speed) {
            this.particle = particle; this.count = count; this.shape = shape; this.spreadX = spreadX; this.spreadY = spreadY; this.spreadZ = spreadZ; this.speed = speed;
        }
    }

    public static class SoundSettings {
        public boolean enabled = true;
        public List<SoundPlay> sounds = new ArrayList<>(List.of(
                defaultSoundPlay()
        ));
    }

    public static class SoundPlay {
        public String sound;
        public float volume;
        public float pitch;
        public int delayTicks;
        public SoundPlay(String sound, float volume, float pitch) { this(sound, volume, pitch, DEFAULT_SOUND_DELAY_TICKS); }
        public SoundPlay(String sound, float volume, float pitch, int delayTicks) { this.sound = sound; this.volume = volume; this.pitch = pitch; this.delayTicks = delayTicks; }
    }

    public static class ItemSettings {
        public boolean handScaleEnabled = false;
        public float handScale = DEFAULT_HAND_SCALE;
        public boolean droppedScaleEnabled = false;
        public float droppedScale = DEFAULT_DROPPED_SCALE;
    }

    public static class PopOverlaySettings {
        public boolean scaleEnabled = false;
        public float popScale = DEFAULT_POP_SCALE;
        public int animationTicks = DEFAULT_POP_ANIMATION_TICKS;
    }

    public static class RetextureSettings {
        public boolean enabled = false;
        public String itemId = DEFAULT_RETEXTURE_ITEM_ID;
        public boolean colorOverlayEnabled = DEFAULT_TOTEM_COLOR_OVERLAY_ENABLED;
        /** Hue shift in degrees. 0 = unchanged, 180 = opposite color. */
        public float overlayHue = DEFAULT_TOTEM_OVERLAY_HUE;
        public float overlayAlpha = DEFAULT_TOTEM_OVERLAY_ALPHA;
    }

    public static class UtilitySettings {
        public boolean onlyForSelf = false;
        public boolean printDebugToChat = false;
        public boolean replaceVanillaParticles = true;
        public boolean replaceVanillaSounds = true;
        public boolean autoVillagerTraderEnabled = DEFAULT_AUTO_VILLAGER_TRADER_ENABLED;
        public String autoVillagerTraderProfession = DEFAULT_AUTO_VILLAGER_TRADER_PROFESSION;
        public String autoVillagerTraderTrade = DEFAULT_AUTO_VILLAGER_TRADER_TRADE;
        public String autoVillagerTraderEnchantment = DEFAULT_AUTO_VILLAGER_TRADER_ENCHANTMENT;
        public boolean autoVillagerTraderCheckChests = DEFAULT_AUTO_VILLAGER_TRADER_CHECK_CHESTS;
        public boolean autoVillagerTraderAutoCloseMerchant = DEFAULT_AUTO_VILLAGER_TRADER_AUTO_CLOSE_MERCHANT;
        public double autoVillagerTraderRange = DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
        public String villagerLevelerTargetUuid = DEFAULT_VILLAGER_LEVELER_TARGET_UUID;
        public boolean villagerLevelerAutoDropItems = DEFAULT_VILLAGER_LEVELER_AUTO_DROP_ITEMS;
    }

    public static class MiscSettings {
        public boolean shieldDownEnabled = DEFAULT_MISC_SHIELD_DOWN_ENABLED;
        public boolean shieldUpEnabled = DEFAULT_MISC_SHIELD_UP_ENABLED;
        public boolean shieldWarningOverlayEnabled = DEFAULT_SHIELD_WARNING_OVERLAY_ENABLED;
        public boolean fireOverlayEnabled = DEFAULT_MISC_FIRE_OVERLAY_ENABLED;
        public boolean emptyBucketOverlayEnabled = DEFAULT_EMPTY_BUCKET_OVERLAY_ENABLED;
        public int shieldWarningOverlayR = DEFAULT_SHIELD_WARNING_OVERLAY_R;
        public int shieldWarningOverlayG = DEFAULT_SHIELD_WARNING_OVERLAY_G;
        public int shieldWarningOverlayB = DEFAULT_SHIELD_WARNING_OVERLAY_B;
        public float shieldWarningOverlayAlpha = DEFAULT_SHIELD_WARNING_OVERLAY_ALPHA;
        public int emptyBucketOverlayR = DEFAULT_EMPTY_BUCKET_OVERLAY_R;
        public int emptyBucketOverlayG = DEFAULT_EMPTY_BUCKET_OVERLAY_G;
        public int emptyBucketOverlayB = DEFAULT_EMPTY_BUCKET_OVERLAY_B;
        public float emptyBucketOverlayAlpha = DEFAULT_EMPTY_BUCKET_OVERLAY_ALPHA;

        public float shieldDownX = DEFAULT_SHIELD_DOWN_X;
        public float shieldDownY = DEFAULT_SHIELD_DOWN_Y;
        public float shieldDownZ = DEFAULT_SHIELD_DOWN_Z;
        public float shieldDownRotX = DEFAULT_SHIELD_DOWN_ROT_X;
        public float shieldDownRotY = DEFAULT_SHIELD_DOWN_ROT_Y;
        public float shieldDownRotZ = DEFAULT_SHIELD_DOWN_ROT_Z;

        public float shieldUpX = DEFAULT_SHIELD_UP_X;
        public float shieldUpY = DEFAULT_SHIELD_UP_Y;
        public float shieldUpZ = DEFAULT_SHIELD_UP_Z;
        public float shieldUpRotX = DEFAULT_SHIELD_UP_ROT_X;
        public float shieldUpRotY = DEFAULT_SHIELD_UP_ROT_Y;
        public float shieldUpRotZ = DEFAULT_SHIELD_UP_ROT_Z;

        public float fireOverlayHeight = DEFAULT_FIRE_OVERLAY_HEIGHT;
    }

    public static class CombatSettings {
        public boolean reachDisplayEnabled = DEFAULT_REACH_DISPLAY_ENABLED;
        public boolean opponentInfoEnabled = DEFAULT_OPPONENT_INFO_ENABLED;
    }

    public static class VisualSettings {
        public boolean fullBrightEnabled = DEFAULT_FULL_BRIGHT_ENABLED;
        public boolean armorHudEnabled = DEFAULT_ARMOR_HUD_ENABLED;
        public boolean armorHudAutoPosition = DEFAULT_ARMOR_HUD_AUTO_POSITION;
        public boolean armorDurabilityWarningEnabled = DEFAULT_ARMOR_DURABILITY_WARNING_ENABLED;
        public int armorDurabilityWarningPercent = DEFAULT_ARMOR_DURABILITY_WARNING_PERCENT;
        public int armorHudX = DEFAULT_ARMOR_HUD_X;
        public int armorHudY = DEFAULT_ARMOR_HUD_Y;
        public boolean armorHudVertical = DEFAULT_ARMOR_HUD_VERTICAL;
        public int armorHudSpacing = DEFAULT_ARMOR_HUD_SPACING;
        public boolean armorHudHotbarBorder = DEFAULT_ARMOR_HUD_HOTBAR_BORDER;
        public String armorHudDurabilityMode = DEFAULT_ARMOR_HUD_DURABILITY_MODE;
        public boolean timeChangerEnabled = DEFAULT_TIME_CHANGER_ENABLED;
        public int timeOfDay = DEFAULT_TIME_OF_DAY;
        public boolean tntTimerEnabled = DEFAULT_TNT_TIMER_ENABLED;
        public int tntTimerRange = DEFAULT_TNT_TIMER_RANGE;
        public boolean projectileTrailEnabled = DEFAULT_PROJECTILE_TRAIL_ENABLED;
        public List<ParticleBurst> projectileTrailParticles = new ArrayList<>(List.of(
                defaultProjectileTrailParticle()
        ));
        public boolean foodOverlayEnabled = DEFAULT_FOOD_OVERLAY_ENABLED;
        public float foodOverlayHue = DEFAULT_FOOD_OVERLAY_HUE;
        public float foodOverlayAlpha = DEFAULT_FOOD_OVERLAY_ALPHA;
        public boolean partialStatusIconsEnabled = DEFAULT_PARTIAL_STATUS_ICONS_ENABLED;
        public boolean streamerModeEnabled = DEFAULT_STREAMER_MODE_ENABLED;
        public boolean zoomEnabled = DEFAULT_ZOOM_ENABLED;
        public float zoomMultiplier = DEFAULT_ZOOM_MULTIPLIER;
        public boolean freelookEnabled = DEFAULT_FREELOOK_ENABLED;
        public boolean freelookToggleMode = DEFAULT_FREELOOK_TOGGLE_MODE;
    }

    public static class MacroSettings {
        public boolean enabled = false;
        public String[] messages = new String[]{"", "", "", ""};
    }

    public static class InventorySorterSettings {
        public boolean enabled = false;
        public List<InventorySortKit> kits = new ArrayList<>();
    }

    public static class InventorySortKit {
        public static final int SLOT_COUNT = 41;

        public String id = "kit-" + System.currentTimeMillis();
        public String name = "Kit";
        public String serverAddress = "";
        public boolean enabled = true;
        public List<String> beforeSlots = emptySlots();
        public List<String> afterSlots = emptySlots();

        public static List<String> emptySlots() {
            ArrayList<String> slots = new ArrayList<>(SLOT_COUNT);
            for (int i = 0; i < SLOT_COUNT; i++) {
                slots.add("");
            }
            return slots;
        }
    }

    public static class UiSettings {
        public List<String> collapsedSections = new ArrayList<>();
    }

    public static final String PVP_HEALTH_MODE_PREFER_SRV = "prefer_srv";
    public static final String PVP_HEALTH_MODE_ESTIMATION_ONLY = "estimation_only";

    public static class PvpSettings {
        public boolean winOddsEnabled = true;
        public boolean totemPopNametagEnabled = DEFAULT_TOTEM_POP_NAMETAG_ENABLED;
        public boolean opponentStatsNametagEnabled = DEFAULT_OPPONENT_STATS_NAMETAG_ENABLED;
        public String opponentStatsNametagFormat = DEFAULT_OPPONENT_STATS_NAMETAG_FORMAT;
        public boolean pingNametagEnabled = DEFAULT_PING_NAMETAG_ENABLED;
        public List<String> nametagItemOrder = defaultNametagItemOrder();
        public List<String> nametagItemsBeforeName = new ArrayList<>(List.of(NAMETAG_ITEM_OPPONENT_STATS));
        public String spoofedHealthMode = PVP_HEALTH_MODE_PREFER_SRV;
        public boolean tierWeightEnabled = true;
        public float tierWeightScale = 0.18f;
        public boolean dualSpectateEnabled = false;
        public boolean dualSpectateAutoFill = false;
        public String dualSpectatePlayerOne = "";
        public String dualSpectatePlayerTwo = "";
        public boolean dualSpectateLockPlayerOne = false;
        public boolean dualSpectateLockPlayerTwo = false;
        public boolean dualSpectateForceThirdPerson = true;
        public float dualSpectatePadding = 1.35f;
        public float dualSpectateMinDistance = 6.0f;
        public float dualSpectateMaxDistance = 80.0f;
        public boolean dualSpectateOverheadEnabled = DEFAULT_DUAL_SPECTATE_OVERHEAD_ENABLED;
        public float dualSpectateOverheadGroupDistance = DEFAULT_DUAL_SPECTATE_OVERHEAD_GROUP_DISTANCE;
        public float dualSpectateMaxYDifference = DEFAULT_DUAL_SPECTATE_MAX_Y_DIFFERENCE;
        public boolean friendFoeOverlayEnabled = DEFAULT_FRIEND_FOE_OVERLAY_ENABLED;
        public String friendFoeOverlayStyle = DEFAULT_FRIEND_FOE_OVERLAY_STYLE;
        public boolean teamCountOverlayEnabled = DEFAULT_TEAM_COUNT_OVERLAY_ENABLED;
        public boolean teamCountOverlayServerFilterEnabled = DEFAULT_TEAM_COUNT_OVERLAY_SERVER_FILTER_ENABLED;
        public String teamCountOverlayAllowedServers = "";
        public int teamCountOverlayX = DEFAULT_TEAM_COUNT_OVERLAY_X;
        public int teamCountOverlayY = DEFAULT_TEAM_COUNT_OVERLAY_Y;
        public List<String> friendNames = new ArrayList<>();
        public List<String> foeNames = new ArrayList<>();
        public int friendOverlayR = DEFAULT_FRIEND_OVERLAY_R;
        public int friendOverlayG = DEFAULT_FRIEND_OVERLAY_G;
        public int friendOverlayB = DEFAULT_FRIEND_OVERLAY_B;
        public float friendOverlayAlpha = DEFAULT_FRIEND_OVERLAY_ALPHA;
        public int foeOverlayR = DEFAULT_FOE_OVERLAY_R;
        public int foeOverlayG = DEFAULT_FOE_OVERLAY_G;
        public int foeOverlayB = DEFAULT_FOE_OVERLAY_B;
        public float foeOverlayAlpha = DEFAULT_FOE_OVERLAY_ALPHA;
    }
    public static ParticleBurst defaultParticleBurst() {
        return new ParticleBurst(
                DEFAULT_PARTICLE_ID,
                DEFAULT_PARTICLE_COUNT,
                DEFAULT_PARTICLE_SHAPE,
                DEFAULT_PARTICLE_SPREAD,
                DEFAULT_PARTICLE_VERTICAL_SPREAD,
                DEFAULT_PARTICLE_SPREAD,
                DEFAULT_PARTICLE_SPEED
        );
    }

    public static ParticleBurst defaultProjectileTrailParticle() {
        return new ParticleBurst(
                DEFAULT_PROJECTILE_TRAIL_PARTICLE_ID,
                DEFAULT_PROJECTILE_TRAIL_PARTICLE_COUNT,
                DEFAULT_PARTICLE_SHAPE,
                DEFAULT_PROJECTILE_TRAIL_SPREAD,
                DEFAULT_PROJECTILE_TRAIL_SPREAD,
                DEFAULT_PROJECTILE_TRAIL_SPREAD,
                DEFAULT_PROJECTILE_TRAIL_SPEED
        );
    }

    public static SoundPlay defaultSoundPlay() {
        return new SoundPlay(DEFAULT_SOUND_ID, DEFAULT_SOUND_VOLUME, DEFAULT_SOUND_PITCH);
    }
}
