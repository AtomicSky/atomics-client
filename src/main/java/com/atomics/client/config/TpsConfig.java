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
    public static final boolean DEFAULT_FRIEND_FOE_OVERLAY_ENABLED = false;
    public static final int DEFAULT_FRIEND_OVERLAY_R = 60;
    public static final int DEFAULT_FRIEND_OVERLAY_G = 255;
    public static final int DEFAULT_FRIEND_OVERLAY_B = 110;
    public static final float DEFAULT_FRIEND_OVERLAY_ALPHA = 0.35f;
    public static final int DEFAULT_FOE_OVERLAY_R = 255;
    public static final int DEFAULT_FOE_OVERLAY_G = 60;
    public static final int DEFAULT_FOE_OVERLAY_B = 60;
    public static final float DEFAULT_FOE_OVERLAY_ALPHA = 0.35f;
    public static final boolean DEFAULT_FULL_BRIGHT_ENABLED = false;
    public static final boolean DEFAULT_TIME_CHANGER_ENABLED = false;
    public static final int DEFAULT_TIME_OF_DAY = 6000;
    public static final boolean DEFAULT_TNT_TIMER_ENABLED = false;
    public static final int DEFAULT_TNT_TIMER_RANGE = 64;
    public static final boolean DEFAULT_PROJECTILE_TRAIL_ENABLED = false;
    public static final String DEFAULT_PROJECTILE_TRAIL_PARTICLE_ID = "minecraft:crit";
    public static final int DEFAULT_PROJECTILE_TRAIL_PARTICLE_COUNT = 1;
    public static final double DEFAULT_PROJECTILE_TRAIL_SPREAD = 0.03;
    public static final double DEFAULT_PROJECTILE_TRAIL_SPEED = 0.0;
    public static final boolean DEFAULT_STREAMER_MODE_ENABLED = false;
    public static final boolean DEFAULT_ZOOM_ENABLED = true;
    public static final float DEFAULT_ZOOM_MULTIPLIER = 4.0f;
    public static final int MIN_MACRO_SLOTS = 4;
    public static final int MAX_MACRO_SLOTS = 12;
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
        if (ui == null) ui = new UiSettings();
        visual.timeOfDay = clampInt(visual.timeOfDay, 0, 24000);
        visual.tntTimerRange = clampInt(visual.tntTimerRange, 8, 128);
        visual.zoomMultiplier = clampFloat(visual.zoomMultiplier, 1.5f, 8.0f);
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
        pvp.spoofedHealthMode = PVP_HEALTH_MODE_PREFER_SRV;
        pvp.deathRecapEnabled = false;
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
        pvp.foeOverlayR = clampInt(pvp.foeOverlayR, 0, 255);
        pvp.foeOverlayG = clampInt(pvp.foeOverlayG, 0, 255);
        pvp.foeOverlayB = clampInt(pvp.foeOverlayB, 0, 255);
        pvp.foeOverlayAlpha = clampFloat(pvp.foeOverlayAlpha, 0.0f, 1.0f);
        if (pvp.autoGgMessage == null || pvp.autoGgMessage.isBlank()) pvp.autoGgMessage = "gg";
        if (pvp.autoGgWinMessage == null || pvp.autoGgWinMessage.isBlank()) pvp.autoGgWinMessage = pvp.autoGgMessage;
        if (pvp.autoGgLoseMessage == null || pvp.autoGgLoseMessage.isBlank()) pvp.autoGgLoseMessage = pvp.autoGgMessage;
        if (pvp.autoGgMessage.length() > 64) pvp.autoGgMessage = pvp.autoGgMessage.substring(0, 64);
        if (pvp.autoGgWinMessage.length() > 64) pvp.autoGgWinMessage = pvp.autoGgWinMessage.substring(0, 64);
        if (pvp.autoGgLoseMessage.length() > 64) pvp.autoGgLoseMessage = pvp.autoGgLoseMessage.substring(0, 64);
        if (pvp.dailyStatsDate == null) pvp.dailyStatsDate = "";
        if (pvp.weeklyStatsKey == null) pvp.weeklyStatsKey = "";
        if (pvp.monthlyStatsKey == null) pvp.monthlyStatsKey = "";
        if (!isStatsTimeframe(pvp.statsNumbersTimeframe)) pvp.statsNumbersTimeframe = "session";
        if (!isStatsTimeframe(pvp.statsBarGraphTimeframe)) pvp.statsBarGraphTimeframe = "session";
        if (pvp.statSessions == null) {
            pvp.statSessions = new ArrayList<>();
        } else {
            pvp.statSessions.removeIf(session -> session == null || session.sessionId == null || session.sessionId.isBlank());
            for (SessionStatsSnapshot session : pvp.statSessions) {
                if (session.label == null || session.label.isBlank()) session.label = "Session";
                session.deaths = Math.max(0, session.deaths);
                session.kills = Math.max(0, session.kills);
                session.totemPops = Math.max(0, session.totemPops);
                session.attackClicks = Math.max(0, session.attackClicks);
                session.hitsLanded = Math.max(0, session.hitsLanded);
                session.damageTaken = Math.max(0.0f, session.damageTaken);
            }
            while (pvp.statSessions.size() > 50) {
                pvp.statSessions.remove(0);
            }
        }
        pvp.dualSpectatePadding = Math.max(1.0f, Math.min(2.5f, pvp.dualSpectatePadding));
        pvp.dualSpectateMinDistance = Math.max(2.0f, Math.min(30.0f, pvp.dualSpectateMinDistance));
        pvp.dualSpectateMaxDistance = Math.max(10.0f, Math.min(160.0f, pvp.dualSpectateMaxDistance));
        if (pvp.dualSpectateMaxDistance < pvp.dualSpectateMinDistance) {
            pvp.dualSpectateMaxDistance = pvp.dualSpectateMinDistance;
        }
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
        return this;
    }

    private static boolean isStatsTimeframe(String value) {
        return "session".equals(value)
                || "daily".equals(value)
                || "weekly".equals(value)
                || "monthly".equals(value)
                || "all_time".equals(value);
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
    }

    public static class MiscSettings {
        public boolean shieldDownEnabled = DEFAULT_MISC_SHIELD_DOWN_ENABLED;
        public boolean shieldUpEnabled = DEFAULT_MISC_SHIELD_UP_ENABLED;
        public boolean shieldWarningOverlayEnabled = DEFAULT_SHIELD_WARNING_OVERLAY_ENABLED;
        public boolean fireOverlayEnabled = DEFAULT_MISC_FIRE_OVERLAY_ENABLED;
        public boolean emptyBucketOverlayEnabled = DEFAULT_EMPTY_BUCKET_OVERLAY_ENABLED;

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
        public boolean timeChangerEnabled = DEFAULT_TIME_CHANGER_ENABLED;
        public int timeOfDay = DEFAULT_TIME_OF_DAY;
        public boolean tntTimerEnabled = DEFAULT_TNT_TIMER_ENABLED;
        public int tntTimerRange = DEFAULT_TNT_TIMER_RANGE;
        public boolean projectileTrailEnabled = DEFAULT_PROJECTILE_TRAIL_ENABLED;
        public List<ParticleBurst> projectileTrailParticles = new ArrayList<>(List.of(
                defaultProjectileTrailParticle()
        ));
        public boolean streamerModeEnabled = DEFAULT_STREAMER_MODE_ENABLED;
        public boolean zoomEnabled = DEFAULT_ZOOM_ENABLED;
        public float zoomMultiplier = DEFAULT_ZOOM_MULTIPLIER;
    }

    public static class MacroSettings {
        public boolean enabled = false;
        public String[] messages = new String[]{"", "", "", ""};
    }

    public static class UiSettings {
        public List<String> collapsedSections = new ArrayList<>();
    }

    public static final String PVP_HEALTH_MODE_PREFER_SRV = "prefer_srv";
    public static final String PVP_HEALTH_MODE_ESTIMATION_ONLY = "estimation_only";

    public static class PvpSettings {
        public boolean deathRecapEnabled = false;
        public boolean sessionStatsEnabled = true;
        public boolean allTimeStatsEnabled = true;
        public boolean winOddsEnabled = true;
        public boolean autoGgEnabled = false;
        public String autoGgMessage = "gg";
        public String autoGgWinMessage = "gg";
        public String autoGgLoseMessage = "gg";
        public String spoofedHealthMode = PVP_HEALTH_MODE_PREFER_SRV;
        public boolean tierWeightEnabled = true;
        public float tierWeightScale = 0.18f;
        public boolean dualSpectateEnabled = false;
        public boolean dualSpectateAutoFill = false;
        public String dualSpectatePlayerOne = "";
        public String dualSpectatePlayerTwo = "";
        public boolean dualSpectateForceThirdPerson = true;
        public float dualSpectatePadding = 1.35f;
        public float dualSpectateMinDistance = 6.0f;
        public float dualSpectateMaxDistance = 80.0f;
        public boolean friendFoeOverlayEnabled = DEFAULT_FRIEND_FOE_OVERLAY_ENABLED;
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

        public int allTimeDeaths = 0;
        public int allTimeKills = 0;
        public int allTimeTotemPops = 0;
        public int allTimeAttackClicks = 0;
        public int allTimeHitsLanded = 0;
        public float allTimeDamageTaken = 0.0f;

        public String dailyStatsDate = "";
        public int dailyBaseDeaths = 0;
        public int dailyBaseKills = 0;
        public int dailyBaseTotemPops = 0;
        public int dailyBaseAttackClicks = 0;
        public int dailyBaseHitsLanded = 0;
        public float dailyBaseDamageTaken = 0.0f;

        public String weeklyStatsKey = "";
        public int weeklyBaseDeaths = 0;
        public int weeklyBaseKills = 0;
        public int weeklyBaseTotemPops = 0;
        public int weeklyBaseAttackClicks = 0;
        public int weeklyBaseHitsLanded = 0;
        public float weeklyBaseDamageTaken = 0.0f;

        public String monthlyStatsKey = "";
        public int monthlyBaseDeaths = 0;
        public int monthlyBaseKills = 0;
        public int monthlyBaseTotemPops = 0;
        public int monthlyBaseAttackClicks = 0;
        public int monthlyBaseHitsLanded = 0;
        public float monthlyBaseDamageTaken = 0.0f;

        public String statsNumbersTimeframe = "session";
        public String statsBarGraphTimeframe = "session";
        public boolean statsGraphKillsVisible = true;
        public boolean statsGraphDeathsVisible = true;
        public boolean statsGraphTotemPopsVisible = true;
        public boolean statsGraphAttackClicksVisible = true;
        public boolean statsGraphHitsLandedVisible = true;
        public boolean statsGraphDamageTakenVisible = true;
        public boolean statsGraphKdRatioVisible = true;
        public boolean statsGraphAccuracyVisible = true;
        public List<SessionStatsSnapshot> statSessions = new ArrayList<>();
    }

    public static class SessionStatsSnapshot {
        public String sessionId = "";
        public String label = "Session";
        public int deaths = 0;
        public int kills = 0;
        public int totemPops = 0;
        public int attackClicks = 0;
        public int hitsLanded = 0;
        public float damageTaken = 0.0f;
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
