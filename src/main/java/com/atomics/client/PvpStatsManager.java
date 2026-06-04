package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PvpStatsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("atomics_client");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    private static final DateTimeFormatter SESSION_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.US);
    private static final String CURRENT_SESSION_ID = Long.toString(System.currentTimeMillis());
    private static final String CURRENT_SESSION_LABEL = LocalDateTime.now().format(SESSION_LABEL_FORMAT);
    private static final double WIN_ODDS_TRACKING_RADIUS = 100.0;
    private static final double WIN_ODDS_TRACKING_RADIUS_SQ = WIN_ODDS_TRACKING_RADIUS * WIN_ODDS_TRACKING_RADIUS;
    private static final float POP_HEALTH_WEIGHT = 20.0f;
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");
    private static final Pattern VANILLA_DEATH_MESSAGE_PATTERN = Pattern.compile(
            "\\b([*.]?[a-zA-Z0-9_]{2,16})\\s+(?:was .* by|blew themselves up|burnt|died|slain|smashed|fell|hit the ground|drowned|starved|suffocated|went up in flames|shot|killed)(?:\\s+([*.]?[a-zA-Z0-9_]{2,16}))?\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLAYER_KILLED_MESSAGE_PATTERN = Pattern.compile(
            "\\b([*.]?[a-zA-Z0-9_]{2,16})\\s+has\\s+killed\\s+([*.]?[a-zA-Z0-9_]{2,16})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WINNER_MESSAGE_PATTERN = Pattern.compile(
            "(?:Winner|Won by):\\s*(?:\\[[^\\]]+]\\s*)*(?:[*»>\\-]\\s*)?([*.]?[a-zA-Z0-9_]{2,16})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LOSER_MESSAGE_PATTERN = Pattern.compile(
            "Loser:\\s*(?:\\[[^\\]]+]\\s*)*(?:[*»>\\-]\\s*)?([*.]?[a-zA-Z0-9_]{2,16})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLAYER_WON_MESSAGE_PATTERN = Pattern.compile(
            "\\b([*.]?[a-zA-Z0-9_]{2,16})\\s+won\\s+(?:the\\s+)?(?:round|game|match|duel)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FLOWPVP_WINNER_MESSAGE_PATTERN = Pattern.compile(
            "(?:Winner|Won by):\\s*(?:\\[[^\\]]+]\\s*)*(?:[*.\\u2726\\u00BB>\\-]\\s*)?([*.]?[a-zA-Z0-9_]{2,16})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FLOWPVP_LOSER_MESSAGE_PATTERN = Pattern.compile(
            "Loser:\\s*(?:\\[[^\\]]+]\\s*)*(?:[*.\\u2726\\u00BB>\\-]\\s*)?([*.]?[a-zA-Z0-9_]{2,16})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VS_START_MESSAGE_PATTERN = Pattern.compile(
            "\\b([*.]?[a-zA-Z0-9_]{2,16})\\s+(?:vs\\.?|versus)\\s+([*.]?[a-zA-Z0-9_]{2,16})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern[] MATCH_START_OPPONENT_PATTERNS = new Pattern[] {
            Pattern.compile("\\bopponent\\s*(?:is|:|-)?\\s*([*.]?[a-zA-Z0-9_]{2,16})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:against|fighting)\\s+([*.]?[a-zA-Z0-9_]{2,16})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:duel|match|game)\\s+(?:against|versus|vs\\.?)\\s+([*.]?[a-zA-Z0-9_]{2,16})\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Counters SESSION = new Counters();
    private static final Map<UUID, Boolean> DEAD_STATE_BY_ENTITY = new HashMap<>();
    private static final Map<UUID, OpponentDuelStats> OPPONENT_STATS = new HashMap<>();
    private static final Map<UUID, WinOddsDisplay> WIN_ODDS_BY_OPPONENT = new HashMap<>();
    private static DeathRecap lastDeathRecap = DeathRecap.empty();
    private static float lastKnownHealth = -1.0f;
    private static boolean wasDead;
    private static int saveCooldownTicks;
    private static long lastLocalDeathMillis;
    private static UUID pendingAttackTargetUuid;
    private static LivingEntity pendingAttackTarget;
    private static float pendingAttackTargetHealth;
    private static long pendingAttackMillis;
    private static long lastConfirmedHitMillis;
    private static UUID activeOpponentUuid;
    private static LivingEntity activeOpponentEntity;
    private static int localDuelTotemPops;
    private static UUID lastRecordedKillUuid;
    private static long lastRecordedKillMillis;
    private static final Map<UUID, Long> RECENT_KILL_BY_VICTIM = new HashMap<>();
    private static int playerDeathScanCooldown;
    private static int localDuelHitsTaken;
    private static int opponentCacheCooldownTicks;
    private static int pendingAutoGgTicks;
    private static String pendingAutoGgMessage = "";
    private static long lastMatchEndMillis;
    private static String lastOpponentInfoName = "";
    private static long lastOpponentInfoMillis;
    private static long nextPeriodBaselineCheckMillis;
    private static String cachedServerAddressRaw = null;
    private static String cachedServerAddress = "";
    private static String cachedSpoofedHealthAddress = null;
    private static boolean cachedSpoofedHealthServer;

    private PvpStatsManager() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null) {
            if (saveCooldownTicks > 0) {
                saveCooldownTicks = 0;
                saveConfigQuietly();
            }
            lastKnownHealth = -1.0f;
            wasDead = false;
            DEAD_STATE_BY_ENTITY.clear();
            RECENT_KILL_BY_VICTIM.clear();
            OPPONENT_STATS.clear();
            WIN_ODDS_BY_OPPONENT.clear();
            activeOpponentUuid = null;
            activeOpponentEntity = null;
            opponentCacheCooldownTicks = 0;
            localDuelHitsTaken = 0;
            localDuelTotemPops = 0;
            pendingAutoGgTicks = 0;
            pendingAutoGgMessage = "";
            lastMatchEndMillis = 0L;
            lastOpponentInfoName = "";
            lastOpponentInfoMillis = 0L;
            return;
        }

        Player player = client.player;
        float health = Math.max(0.0f, player.getHealth() + player.getAbsorptionAmount());
        if (lastKnownHealth >= 0.0f && health < lastKnownHealth) {
            float damageAmount = lastKnownHealth - health;
            recordDamageTaken(damageAmount);
            recordLocalDuelHitTaken(damageAmount);
        }
        lastKnownHealth = health;

        boolean dead = player.isDeadOrDying() || !player.isAlive() || health <= 0.0f;
        if (dead && !wasDead) {
            recordDeath(player, player.getLastDamageSource());
        }
        wasDead = dead;

        if (saveCooldownTicks > 0 && --saveCooldownTicks == 0) {
            saveConfigQuietly();
        }

        tickOpponentCache(client);
        tickPendingHit();
        tickPlayerDeathFallbacks(client);
        tickAutoGg(client);
    }

    /**
     * Counts every attack swing so missed hits still lower accuracy.
     * Crystal attacks are removed again in recordAttackTarget once the target is known.
     */
    public static void recordAttackSwing() {
        recordAttackClick();
    }

    private static void recordAttackClick() {
        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;
        SESSION.attackClicks++;
        scheduleSave();
        if (pvp.allTimeStatsEnabled) {
            pvp.allTimeAttackClicks++;
        }
    }

    private static void undoAttackClick() {
        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;
        boolean changed = false;
        if (SESSION.attackClicks > 0) {
            SESSION.attackClicks--;
            changed = true;
        }
        if (pvp.allTimeStatsEnabled && pvp.allTimeAttackClicks > 0) {
            pvp.allTimeAttackClicks--;
            changed = true;
        }
        if (changed) {
            scheduleSave();
        }
    }

    public static void recordTotemPop(Entity entity) {
        if (entity == null) return;

        if (isLocalPlayer(entity)) {
            localDuelTotemPops++;
            TpsConfig.PvpSettings pvp = getPvpSettings();
            if (!pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;
            SESSION.totemPops++;
            scheduleSave();
            if (pvp.allTimeStatsEnabled) {
                pvp.allTimeTotemPops++;
            }
        } else if (entity instanceof Player player) {
            statsFor(player).totemPops++;
        }
    }

    public static void resetTotemCounters() {
        localDuelTotemPops = 0;
        for (OpponentDuelStats stats : OPPONENT_STATS.values()) {
            stats.totemPops = 0;
        }
        WIN_ODDS_BY_OPPONENT.clear();
    }

    public static void recordAttackTarget(Entity target) {
        if (target == null) {
            clearPendingHit();
            return;
        }
        if (target instanceof EndCrystal) {
            undoAttackClick();
            clearPendingHit();
            return;
        }
        if (!(target instanceof LivingEntity livingTarget)) {
            clearPendingHit();
            return;
        }

        if (target instanceof Player playerTarget && !isLocalPlayer(playerTarget)) {
            setActiveOpponent(playerTarget);
        }
        pendingAttackTargetUuid = target.getUUID();
        pendingAttackTarget = livingTarget;
        pendingAttackTargetHealth = livingTarget.getHealth() + livingTarget.getAbsorptionAmount();
        pendingAttackMillis = System.currentTimeMillis();
    }

    public static void recordGapConsumed(LivingEntity entity) {
        if (entity == null) return;
        if (isLocalPlayer(entity)) {
            return;
        } else if (entity instanceof Player player) {
            OpponentDuelStats stats = statsFor(player);
            stats.gaps++;
        }
    }

    public static Component getWinOddsNameSuffix(Player player) {
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (player == null || pvp == null || !pvp.winOddsEnabled || isLocalPlayer(player)) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        Player spectatedOpponent = DualSpectateCamera.getOtherSpectatedPlayer(player);
        if (spectatedOpponent == null && !isTrackableOpponent(client, player)) {
            return null;
        }

        WinOddsDisplay display;
        if (spectatedOpponent != null) {
            display = buildWinOddsDisplay(client, player, spectatedOpponent);
        } else {
            display = WIN_ODDS_BY_OPPONENT.get(player.getUUID());
        }
        if (display == null) {
            display = buildWinOddsDisplay(client, player);
            WIN_ODDS_BY_OPPONENT.put(player.getUUID(), display);
        }
        if (!display.available) {
            return null;
        }

        return display.suffix;
    }

    public static Component getTotemPopNameSuffix(Player player) {
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (player == null || pvp == null || !pvp.totemPopNametagEnabled || isLocalPlayer(player)) {
            return null;
        }

        OpponentDuelStats stats = statsFor(player);
        if (stats == null || stats.totemPops <= 0) {
            return null;
        }
        return Component.literal(formatPopCount(stats.totemPops)).withStyle(ChatFormatting.GOLD);
    }

    private static WinOddsDisplay buildWinOddsDisplay(Minecraft client, Player opponent) {
        if (client == null || client.player == null || opponent == null) return WinOddsDisplay.DEFAULT;

        OpponentDuelStats stats = statsFor(opponent);
        float localHealth = readCurrentHealth(client.player);
        HealthRead opponentHealth = readOpponentHealth(client, opponent, stats);

        if (!opponentHealth.available && hasAnyTotemPops(stats)) {
            int percent = getPopBasedWinOddsPercent(stats);
            return WinOddsDisplay.available(percent, 0.0f, true, stats.totemPops);
        }
        if (!opponentHealth.available) {
            return WinOddsDisplay.UNAVAILABLE;
        }

        int percent = getHealthAndPopWinOddsPercent(localHealth, opponentHealth.value, stats);
        boolean showPopCount = stats != null && stats.totemPops > 0;
        return WinOddsDisplay.available(percent, opponentHealth.value, showPopCount, stats.totemPops);
    }

    private static WinOddsDisplay buildWinOddsDisplay(Minecraft client, Player subject, Player opponent) {
        if (client == null || subject == null || opponent == null) return WinOddsDisplay.DEFAULT;

        OpponentDuelStats subjectStats = statsFor(subject);
        OpponentDuelStats opponentStats = statsFor(opponent);
        HealthRead subjectHealth = readOpponentHealth(client, subject, subjectStats);
        HealthRead opponentHealth = readOpponentHealth(client, opponent, opponentStats);
        boolean showPopCount = subjectStats.totemPops > 0;

        if ((!subjectHealth.available || !opponentHealth.available) && showPopCount) {
            int percent = getPopBasedWinOddsPercent(subjectStats.totemPops, opponentStats.totemPops);
            return WinOddsDisplay.available(percent, 0.0f, true, subjectStats.totemPops);
        }
        if (!subjectHealth.available || !opponentHealth.available) {
            return WinOddsDisplay.UNAVAILABLE;
        }

        int percent = getHealthAndPopWinOddsPercent(
                subjectHealth.value,
                opponentHealth.value,
                subjectStats.totemPops,
                opponentStats.totemPops
        );
        return WinOddsDisplay.available(percent, subjectHealth.value, showPopCount, subjectStats.totemPops);
    }

    private static HealthRead readOpponentHealth(Minecraft client, Player opponent, OpponentDuelStats stats) {
        if (client == null || client.player == null || opponent == null || stats == null) {
            return new HealthRead(0.0f, false);
        }

        if (!usesSpoofedOpponentHealth(client)) {
            return new HealthRead(readCurrentHealth(opponent), true);
        }

        // On spoofed-health servers, only trust direct below-name scoreboard health.
        // If the server does not expose that value, health-based odds are hidden.
        float belowNameHealth = readBelowNameHealth(client, opponent);
        if (isUsableHealth(belowNameHealth)) {
            return new HealthRead(Math.max(0.0f, Math.min(40.0f, belowNameHealth)), true);
        }

        return new HealthRead(0.0f, false);
    }

    private static boolean usesSpoofedOpponentHealth(Minecraft client) {
        String address = getCurrentServerAddress(client);
        if (address.equals(cachedSpoofedHealthAddress)) {
            return cachedSpoofedHealthServer;
        }

        cachedSpoofedHealthAddress = address;
        cachedSpoofedHealthServer = !address.isEmpty()
                && (hostMatches(address, "minemen.club")
                || hostMatches(address, "mcpvp.club")
                || hostMatches(address, "mcpvp.xyz")
                || hostMatches(address, "catpvp.xyz")
                || hostMatches(address, "catpvp.com")
                || hostMatches(address, "catpvp.minehut.gg")
                || hostMatches(address, "flowpvp.gg"));
        return cachedSpoofedHealthServer;
    }

    public static void recordOutcomeFromServerMessage(Component text) {
        if (text == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        String message = cleanServerMessage(text.getString());
        if (message.isEmpty()) return;

        String localName = client.player.getGameProfile().name();
        if (localName == null || localName.isEmpty()) return;

        recordOpponentInfoFromStartMessage(client, message, localName);

        if (!usesSpoofedOpponentHealth(client)) {
            return;
        }

        Matcher deathMatcher = VANILLA_DEATH_MESSAGE_PATTERN.matcher(message);
        if (deathMatcher.find()) {
            String victim = cleanPlayerName(deathMatcher.group(1));
            String attacker = cleanPlayerName(deathMatcher.group(2));
            if (playerNameMatches(victim, localName)) {
                recordLocalDeathFromHealthPacket();
                recordDetectedMatchEnd(false, true);
                return;
            }
            if (playerNameMatches(attacker, localName)) {
                recordActiveOpponentKillFromServerMessage();
                recordDetectedMatchEnd(true, true);
                return;
            }
        }

        Matcher killedMatcher = PLAYER_KILLED_MESSAGE_PATTERN.matcher(message);
        if (killedMatcher.find()) {
            String attacker = cleanPlayerName(killedMatcher.group(1));
            String victim = cleanPlayerName(killedMatcher.group(2));
            if (playerNameMatches(victim, localName)) {
                recordLocalDeathFromHealthPacket();
                recordDetectedMatchEnd(false, true);
                return;
            }
            if (playerNameMatches(attacker, localName)) {
                recordActiveOpponentKillFromServerMessage();
                recordDetectedMatchEnd(true, true);
                return;
            }
        }

        if (messageMentionsLocalLoss(message, localName)) {
            recordLocalDeathFromHealthPacket();
            recordDetectedMatchEnd(false, true);
            return;
        }

        String winner = findWinnerName(message);
        String loser = findLoserName(message);
        if (loser != null && playerNameMatches(loser, localName)) {
            recordLocalDeathFromHealthPacket();
            recordDetectedMatchEnd(false, true);
            return;
        }

        if (winner == null) return;
        if (playerNameMatches(winner, localName)) {
            recordActiveOpponentKillFromServerMessage();
            recordDetectedMatchEnd(true, true);
        } else if (messageMentionsLocalPlayer(message, localName) || isActiveOpponentName(winner)) {
            recordLocalDeathFromHealthPacket();
            recordDetectedMatchEnd(false, true);
        }
    }

    private static String cleanServerMessage(String message) {
        if (message == null) return "";
        return FORMATTING_CODE_PATTERN.matcher(message).replaceAll("").trim();
    }

    private static String findWinnerName(String message) {
        Matcher flowWinnerMatcher = FLOWPVP_WINNER_MESSAGE_PATTERN.matcher(message);
        if (flowWinnerMatcher.find()) {
            return cleanPlayerName(flowWinnerMatcher.group(1));
        }

        Matcher winnerMatcher = WINNER_MESSAGE_PATTERN.matcher(message);
        if (winnerMatcher.find()) {
            return cleanPlayerName(winnerMatcher.group(1));
        }

        Matcher wonMatcher = PLAYER_WON_MESSAGE_PATTERN.matcher(message);
        if (wonMatcher.find()) {
            return cleanPlayerName(wonMatcher.group(1));
        }
        return null;
    }

    private static String findLoserName(String message) {
        Matcher flowLoserMatcher = FLOWPVP_LOSER_MESSAGE_PATTERN.matcher(message);
        if (flowLoserMatcher.find()) {
            return cleanPlayerName(flowLoserMatcher.group(1));
        }

        Matcher loserMatcher = LOSER_MESSAGE_PATTERN.matcher(message);
        if (loserMatcher.find()) {
            return cleanPlayerName(loserMatcher.group(1));
        }
        return null;
    }

    private static boolean messageMentionsLocalLoss(String message, String localName) {
        String lower = message.toLowerCase(Locale.ROOT);
        String cleanedLocalName = cleanPlayerName(localName).toLowerCase(Locale.ROOT);
        return lower.contains("you died")
                || lower.contains("you lost")
                || lower.contains("you were defeated")
                || lower.contains("you have been defeated")
                || (!cleanedLocalName.isEmpty()
                && (lower.contains(cleanedLocalName + " lost")
                || lower.contains(cleanedLocalName + " was defeated")));
    }

    private static boolean messageMentionsLocalPlayer(String message, String localName) {
        String cleanedLocalName = cleanPlayerName(localName);
        if (cleanedLocalName.isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT);
        String lowerName = cleanedLocalName.toLowerCase(Locale.ROOT);
        int index = lowerMessage.indexOf(lowerName);
        while (index >= 0) {
            int before = index - 1;
            int after = index + lowerName.length();
            boolean leftBoundary = before < 0 || !isNameCharacter(lowerMessage.charAt(before));
            boolean rightBoundary = after >= lowerMessage.length() || !isNameCharacter(lowerMessage.charAt(after));
            if (leftBoundary && rightBoundary) {
                return true;
            }
            index = lowerMessage.indexOf(lowerName, index + 1);
        }
        return false;
    }

    private static void recordOpponentInfoFromStartMessage(Minecraft client, String message, String localName) {
        if (!isLikelyMatchStartMessage(message)) {
            return;
        }

        String opponentName = findOpponentNameFromStartMessage(message, localName);
        if ((opponentName == null || opponentName.isBlank()) && isStrongStartSignal(message)) {
            Player onlyOpponent = findOnlyTrackableOpponent(client);
            if (onlyOpponent != null) {
                opponentName = onlyOpponent.getScoreboardName();
            }
        }
        if (opponentName == null || opponentName.isBlank()) {
            return;
        }
        Player opponent = findWorldPlayer(client, opponentName);
        if (opponent != null) {
            setActiveOpponent(opponent);
        }
        sendOpponentInfoChat(client, opponentName);
    }

    private static boolean isLikelyMatchStartMessage(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("winner")
                || lower.contains("won by")
                || lower.contains("loser")
                || lower.contains(" lost")
                || lower.contains(" killed ")
                || lower.contains(" died")) {
            return false;
        }
        return lower.contains("opponent")
                || lower.contains(" versus ")
                || lower.contains(" vs ")
                || lower.contains(" vs. ")
                || lower.contains("against")
                || lower.contains("fighting");
    }

    private static boolean isStrongStartSignal(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("starts in")
                || lower.contains("starting in")
                || lower.contains("match starts")
                || lower.contains("match starting")
                || lower.contains("game starts")
                || lower.contains("game starting")
                || lower.contains("duel starts")
                || lower.contains("duel starting")
                || lower.equals("fight")
                || lower.equals("fight!");
    }

    private static String findOpponentNameFromStartMessage(String message, String localName) {
        Matcher vsMatcher = VS_START_MESSAGE_PATTERN.matcher(message);
        if (vsMatcher.find()) {
            String first = cleanPlayerName(vsMatcher.group(1));
            String second = cleanPlayerName(vsMatcher.group(2));
            if (playerNameMatches(first, localName) && isLikelyOpponentName(second, localName)) {
                return second;
            }
            if (playerNameMatches(second, localName) && isLikelyOpponentName(first, localName)) {
                return first;
            }
        }

        for (Pattern pattern : MATCH_START_OPPONENT_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String candidate = cleanPlayerName(matcher.group(1));
                if (isLikelyOpponentName(candidate, localName)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isLikelyOpponentName(String candidate, String localName) {
        String cleaned = cleanPlayerName(candidate);
        if (cleaned.length() < 2 || playerNameMatches(cleaned, localName)) {
            return false;
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        return !lower.equals("starting")
                && !lower.equals("starts")
                && !lower.equals("start")
                && !lower.equals("begins")
                && !lower.equals("begin")
                && !lower.equals("match")
                && !lower.equals("duel")
                && !lower.equals("game")
                && !lower.equals("round");
    }

    private static Player findWorldPlayer(Minecraft client, String name) {
        if (client == null || client.level == null || name == null || name.isBlank()) {
            return null;
        }
        for (Player player : client.level.players()) {
            if (player != null && playerNameMatches(player.getScoreboardName(), name)) {
                return player;
            }
        }
        return null;
    }

    private static Player findOnlyTrackableOpponent(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            return null;
        }
        Player found = null;
        for (Player player : client.level.players()) {
            if (!isTrackableOpponent(client, player)) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = player;
        }
        return found;
    }

    private static void sendOpponentInfoChat(Minecraft client, String opponentName) {
        if (client == null
                || AtomicsClient.CONFIG == null
                || !AtomicsClient.CONFIG.enabled
                || AtomicsClient.CONFIG.combat == null
                || !AtomicsClient.CONFIG.combat.opponentInfoEnabled) {
            return;
        }

        String cleaned = cleanPlayerName(opponentName);
        if (cleaned.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (cleaned.equalsIgnoreCase(lastOpponentInfoName) && now - lastOpponentInfoMillis < 15000L) {
            return;
        }

        lastOpponentInfoName = cleaned;
        lastOpponentInfoMillis = now;
        TierWeightManager.sendOpponentInfoChat(cleaned);
    }

    private static boolean isActiveOpponentName(String name) {
        if (name == null || activeOpponentEntity == null) return false;
        if (activeOpponentEntity instanceof Player player) {
            return playerNameMatches(name, player.getGameProfile().name())
                    || playerNameMatches(name, player.getScoreboardName());
        }
        return false;
    }

    private static boolean playerNameMatches(String seenName, String expectedName) {
        String seen = cleanPlayerName(seenName);
        String expected = cleanPlayerName(expectedName);
        return !seen.isEmpty() && seen.equalsIgnoreCase(expected);
    }

    private static String cleanPlayerName(String name) {
        if (name == null) return "";
        int start = 0;
        int end = name.length();
        while (start < end && Character.isWhitespace(name.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(name.charAt(end - 1))) {
            end--;
        }
        while (start < end && !isNameCharacter(name.charAt(start))) {
            start++;
        }
        return start >= end ? "" : name.substring(start, end);
    }

    private static boolean isNameCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String getCurrentServerAddress(Minecraft client) {
        if (client == null) return "";
        ServerData serverInfo = client.getCurrentServer();
        if (serverInfo == null || serverInfo.ip == null) return "";
        String rawAddress = serverInfo.ip;
        if (rawAddress.equals(cachedServerAddressRaw)) {
            return cachedServerAddress;
        }
        cachedServerAddressRaw = rawAddress;
        cachedServerAddress = rawAddress.trim().toLowerCase(Locale.ROOT);
        return cachedServerAddress;
    }

    private static boolean hostMatches(String address, String domain) {
        String host = address;
        int slashIndex = host.indexOf('/');
        if (slashIndex >= 0) host = host.substring(0, slashIndex);
        int colonIndex = host.indexOf(':');
        if (colonIndex >= 0) host = host.substring(0, colonIndex);

        domain = domain.toLowerCase(Locale.ROOT);
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static void tickOpponentCache(Minecraft client) {
        if (client == null || client.level == null || client.player == null) return;
        TpsConfig.PvpSettings pvp = livePvpSettings();
        if (pvp == null || !pvp.winOddsEnabled) {
            if (!WIN_ODDS_BY_OPPONENT.isEmpty()) {
                WIN_ODDS_BY_OPPONENT.clear();
            }
            return;
        }
        if (opponentCacheCooldownTicks > 0) {
            opponentCacheCooldownTicks--;
            return;
        }
        opponentCacheCooldownTicks = 4;

        WIN_ODDS_BY_OPPONENT.clear();
        for (Player other : client.level.players()) {
            if (!isTrackableOpponent(client, other)) {
                continue;
            }
            WIN_ODDS_BY_OPPONENT.put(other.getUUID(), buildWinOddsDisplay(client, other));
        }
    }

    private static float readCurrentHealth(Player player) {
        if (player == null || player.isDeadOrDying() || !player.isAlive()) return 0.0f;

        float health = player.getHealth();
        if (!Float.isFinite(health)) return 0.0f;
        return Math.max(0.0f, health);
    }

    private static float readBelowNameHealth(Minecraft client, Player player) {
        if (client == null || client.level == null || player == null) return Float.NaN;
        try {
            Scoreboard scoreboard = client.level.getScoreboard();
            if (scoreboard == null) return Float.NaN;

            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (objective == null) return Float.NaN;

            ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(player.getScoreboardName()), objective);
            if (score == null) return Float.NaN;

            float value = score.value();
            return isUsableHealth(value) ? value : Float.NaN;
        } catch (Throwable ignored) {
            return Float.NaN;
        }
    }

    private static boolean isUsableHealth(float health) {
        return Float.isFinite(health) && health >= 0.0f && health <= 40.0f;
    }

    private static boolean isTrackableOpponent(Minecraft client, Player other) {
        if (client == null || client.player == null || other == null || isLocalPlayer(other) || other.isSpectator() || other.isDeadOrDying() || !other.isAlive()) {
            return false;
        }

        Player local = client.player;
        double dx = other.getX() - local.getX();
        double dy = other.getY() - local.getY();
        double dz = other.getZ() - local.getZ();
        return dx * dx + dy * dy + dz * dz <= WIN_ODDS_TRACKING_RADIUS_SQ;
    }

    private static boolean hasAnyTotemPops(OpponentDuelStats stats) {
        return localDuelTotemPops > 0 || (stats != null && stats.totemPops > 0);
    }

    private static int getHealthAndPopWinOddsPercent(float localHealth, float opponentHealth, OpponentDuelStats stats) {
        int localPops = Math.max(0, localDuelTotemPops);
        int opponentPops = Math.max(0, stats == null ? 0 : stats.totemPops);
        return getHealthAndPopWinOddsPercent(localHealth, opponentHealth, localPops, opponentPops);
    }

    private static int getHealthAndPopWinOddsPercent(float localHealth, float opponentHealth, int localPops, int opponentPops) {
        float localScore = Math.max(0.0f, localHealth) + opponentPops * POP_HEALTH_WEIGHT;
        float opponentScore = Math.max(0.0f, opponentHealth) + localPops * POP_HEALTH_WEIGHT;
        float total = localScore + opponentScore;
        if (total <= 0.0f || !Float.isFinite(total)) {
            return 50;
        }

        int percent = Math.round((localScore / total) * 100.0f);
        return Math.max(0, Math.min(100, percent));
    }

    private static int getPopBasedWinOddsPercent(OpponentDuelStats stats) {
        int localPops = Math.max(0, localDuelTotemPops);
        int opponentPops = Math.max(0, stats == null ? 0 : stats.totemPops);
        return getPopBasedWinOddsPercent(localPops, opponentPops);
    }

    private static int getPopBasedWinOddsPercent(int localPops, int opponentPops) {
        int percent = Math.round(((opponentPops + 1.0f) / (localPops + opponentPops + 2.0f)) * 100.0f);
        return Math.max(0, Math.min(100, percent));
    }

    private static void setActiveOpponent(Player opponent) {
        if (opponent == null) return;
        if (activeOpponentUuid != null && activeOpponentUuid.equals(opponent.getUUID())) {
            activeOpponentEntity = opponent;
            return;
        }
        activeOpponentUuid = opponent.getUUID();
        activeOpponentEntity = opponent;
        statsFor(opponent);
        sendOpponentInfoChat(Minecraft.getInstance(), opponent.getScoreboardName());
    }

    private static OpponentDuelStats statsFor(Player opponent) {
        UUID uuid = opponent.getUUID();
        OpponentDuelStats stats = OPPONENT_STATS.get(uuid);
        if (stats == null) {
            stats = new OpponentDuelStats();
            OPPONENT_STATS.put(uuid, stats);
        }
        return stats;
    }

    public static void recordEntityHurt(Entity entity) {
        if (entity == null || entity instanceof EndCrystal || pendingAttackTargetUuid == null || !pendingAttackTargetUuid.equals(entity.getUUID())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - pendingAttackMillis > 900L || now - lastConfirmedHitMillis < 100L) {
            return;
        }

        lastConfirmedHitMillis = now;
        Entity hitEntity = entity;
        clearPendingHit();
        recordHitLanded(hitEntity);
    }

    private static void tickPendingHit() {
        if (pendingAttackTarget == null) return;

        long now = System.currentTimeMillis();
        if (now - pendingAttackMillis > 900L || !pendingAttackTarget.isAlive()) {
            clearPendingHit();
            return;
        }

        float currentHealth = pendingAttackTarget.getHealth() + pendingAttackTarget.getAbsorptionAmount();
        if (pendingAttackTarget.hurtTime > 0 || currentHealth < pendingAttackTargetHealth) {
            if (now - lastConfirmedHitMillis >= 100L) {
                lastConfirmedHitMillis = now;
                LivingEntity hitEntity = pendingAttackTarget;
                clearPendingHit();
                recordHitLanded(hitEntity);
            }
        }
    }

    private static void tickPlayerDeathFallbacks(Minecraft client) {
        if (client.level == null || ++playerDeathScanCooldown < 2) return;
        playerDeathScanCooldown = 0;

        for (Player other : client.level.players()) {
            if (other == null || isLocalPlayer(other)) continue;
            boolean dead = isPlayerDeadByClientSignal(client, other);
            Boolean wasDeadBefore = DEAD_STATE_BY_ENTITY.put(other.getUUID(), dead);
            if (dead && !Boolean.TRUE.equals(wasDeadBefore)) {
                recordDeathStatus(other);
            } else if (!dead) {
                RECENT_KILL_BY_VICTIM.remove(other.getUUID());
            }
        }

        if (activeOpponentEntity instanceof Player activePlayer && activeOpponentUuid != null) {
            boolean activeDead = isPlayerDeadByClientSignal(client, activePlayer);
            Boolean wasDeadBefore = DEAD_STATE_BY_ENTITY.put(activeOpponentUuid, activeDead);
            if (activeDead && !Boolean.TRUE.equals(wasDeadBefore)) {
                recordOpponentDeathFallback(activeOpponentEntity);
            } else if (!activeDead) {
                RECENT_KILL_BY_VICTIM.remove(activeOpponentUuid);
            }
        }
    }

    private static boolean isPlayerDeadByClientSignal(Minecraft client, Player player) {
        if (player == null) return true;

        // Normal vanilla/client-side death signals.
        if (player.isDeadOrDying() || !player.isAlive() || player.getHealth() <= 0.0f) {
            return true;
        }

        // On servers that spoof entity health, getHealth() may never reach 0 for
        // opponents. If the server exposes real HP through the below-name heart
        // scoreboard, treat 0 hearts there as a death signal too.
        // Keep this limited to known spoofed-health servers so random below-name
        // scoreboards with a value of 0 do not cause false death detections.
        if (usesSpoofedOpponentHealth(client)) {
            float belowNameHealth = readBelowNameHealth(client, player);
            return Float.isFinite(belowNameHealth) && belowNameHealth <= 0.0f;
        }

        return false;
    }

    private static void clearPendingHit() {
        pendingAttackTargetUuid = null;
        pendingAttackTarget = null;
        pendingAttackTargetHealth = 0.0f;
    }

    public static void recordDeathStatus(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;
        recordOpponentDeathFallback(livingEntity);
        recordDeath(livingEntity, livingEntity.getLastDamageSource());
    }

    public static void recordLocalDeathFromHealthPacket() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            recordDeath(client.player, client.player.getLastDamageSource());
        }
    }

    private static void recordActiveOpponentKillFromServerMessage() {
        if (activeOpponentUuid != null) {
            recordKill(activeOpponentUuid);
        }
    }

    private static void recordDetectedMatchEnd(boolean won) {
        recordDetectedMatchEnd(won, false);
    }

    private static void recordDetectedMatchEnd(boolean won, boolean authoritative) {
        Minecraft client = Minecraft.getInstance();
        if (!usesSpoofedOpponentHealth(client)) return;

        long now = System.currentTimeMillis();
        if (now - lastMatchEndMillis < 5000L) {
            if (authoritative && pendingAutoGgTicks > 0) {
                queueAutoGg(won);
            }
            return;
        }
        lastMatchEndMillis = now;

        resetTotemCounters();
        lastOpponentInfoName = "";
        lastOpponentInfoMillis = 0L;
        queueAutoGg(won);
    }

    private static void queueAutoGg(boolean won) {
        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.autoGgEnabled) return;

        String configuredMessage = won ? pvp.autoGgWinMessage : pvp.autoGgLoseMessage;
        String message = configuredMessage == null ? "" : configuredMessage.trim();
        if (message.isEmpty()) return;
        if (message.length() > 64) {
            message = message.substring(0, 64);
        }

        pendingAutoGgMessage = message;
        pendingAutoGgTicks = 12;
    }

    private static void tickAutoGg(Minecraft client) {
        if (pendingAutoGgTicks <= 0) return;
        if (--pendingAutoGgTicks > 0) return;
        if (client == null || client.getConnection() == null) return;

        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.autoGgEnabled) return;

        String message = pendingAutoGgMessage == null ? "" : pendingAutoGgMessage.trim();
        pendingAutoGgMessage = "";
        if (message.isEmpty()) return;
        if (message.length() > 64) {
            message = message.substring(0, 64);
        }
        client.getConnection().sendChat(message);
    }

    public static void recordDeath(LivingEntity entity, DamageSource source) {
        recordPossibleKill(entity, source);
        if (!isLocalPlayer(entity)) return;
        long now = System.currentTimeMillis();
        if (now - lastLocalDeathMillis < 1000L) {
            return;
        }
        lastLocalDeathMillis = now;

        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.deathRecapEnabled && !pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;

        SESSION.deaths++;
        if (pvp.allTimeStatsEnabled) {
            pvp.allTimeDeaths++;
        }

        lastDeathRecap = DeathRecap.from(entity, source);
        if (pvp.deathRecapEnabled && entity instanceof Player player) {
            AtomicsClient.sendClientMessage(player, Component.literal("[Atomics] " + lastDeathRecap.shortLine()).withStyle(ChatFormatting.GRAY), false);
        }
        scheduleSave();
        recordDetectedMatchEnd(false);
    }

    private static void recordPossibleKill(LivingEntity victim, DamageSource source) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || victim == null || victim.getUUID().equals(client.player.getUUID()) || source == null) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || !attacker.getUUID().equals(client.player.getUUID())) {
            return;
        }
        recordKill(victim.getUUID());
    }

    private static void recordOpponentDeathFallback(LivingEntity victim) {
        if (victim == null || !isActiveOpponent(victim)) return;
        long now = System.currentTimeMillis();
        if (now - Math.max(lastConfirmedHitMillis, pendingAttackMillis) > 8000L) return;
        recordKill(victim.getUUID());
    }

    private static void recordKill(UUID victimUuid) {
        if (victimUuid == null) return;
        long now = System.currentTimeMillis();

        Long lastVictimKill = RECENT_KILL_BY_VICTIM.get(victimUuid);
        if (lastVictimKill != null && now - lastVictimKill < 15000L) {
            return;
        }
        if (victimUuid.equals(lastRecordedKillUuid) && now - lastRecordedKillMillis < 15000L) {
            return;
        }

        RECENT_KILL_BY_VICTIM.put(victimUuid, now);
        lastRecordedKillUuid = victimUuid;
        lastRecordedKillMillis = now;

        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;
        SESSION.kills++;
        scheduleSave();
        if (pvp.allTimeStatsEnabled) {
            pvp.allTimeKills++;
        }
        recordDetectedMatchEnd(true);
    }

    public static Counters session() {
        return SESSION;
    }

    public static Counters allTime() {
        TpsConfig.PvpSettings pvp = getPvpSettings();
        Counters counters = new Counters();
        counters.deaths = pvp.allTimeDeaths;
        counters.kills = pvp.allTimeKills;
        counters.totemPops = pvp.allTimeTotemPops;
        counters.attackClicks = pvp.allTimeAttackClicks;
        counters.hitsLanded = pvp.allTimeHitsLanded;
        counters.damageTaken = pvp.allTimeDamageTaken;
        return counters;
    }

    public static Counters dailyDifference() {
        TpsConfig.PvpSettings pvp = getPvpSettings();

        return differenceFrom(
                pvp,
                pvp.dailyBaseDeaths,
                pvp.dailyBaseKills,
                pvp.dailyBaseTotemPops,
                pvp.dailyBaseAttackClicks,
                pvp.dailyBaseHitsLanded,
                pvp.dailyBaseDamageTaken
        );
    }

    public static Counters weeklyDifference() {
        TpsConfig.PvpSettings pvp = getPvpSettings();

        return differenceFrom(
                pvp,
                pvp.weeklyBaseDeaths,
                pvp.weeklyBaseKills,
                pvp.weeklyBaseTotemPops,
                pvp.weeklyBaseAttackClicks,
                pvp.weeklyBaseHitsLanded,
                pvp.weeklyBaseDamageTaken
        );
    }

    public static Counters monthlyDifference() {
        TpsConfig.PvpSettings pvp = getPvpSettings();

        return differenceFrom(
                pvp,
                pvp.monthlyBaseDeaths,
                pvp.monthlyBaseKills,
                pvp.monthlyBaseTotemPops,
                pvp.monthlyBaseAttackClicks,
                pvp.monthlyBaseHitsLanded,
                pvp.monthlyBaseDamageTaken
        );
    }

    public static String dailyStatsDate() {
        TpsConfig.PvpSettings pvp = getPvpSettings();
        return pvp.dailyStatsDate;
    }

    public static List<SessionSnapshot> sessionHistory() {
        TpsConfig.PvpSettings pvp = getPvpSettings();
        updateCurrentSessionHistory(pvp);

        List<SessionSnapshot> history = new ArrayList<>();
        if (pvp.statSessions != null) {
            for (TpsConfig.SessionStatsSnapshot session : pvp.statSessions) {
                if (session == null) continue;
                history.add(new SessionSnapshot(
                        session.label == null || session.label.isBlank() ? "Session" : session.label,
                        session.kills,
                        session.deaths,
                        session.totemPops,
                        session.attackClicks,
                        session.hitsLanded,
                        session.damageTaken
                ));
            }
        }
        return List.copyOf(history);
    }

    public static DeathRecap lastDeathRecap() {
        return lastDeathRecap;
    }

    public static TpsConfig.PvpSettings getPvpSettings() {
        if (AtomicsClient.CONFIG == null) {
            AtomicsClient.CONFIG = new TpsConfig().normalize();
        }
        if (AtomicsClient.CONFIG.pvp == null) {
            AtomicsClient.CONFIG.normalize();
        }
        TpsConfig.PvpSettings pvp = AtomicsClient.CONFIG.pvp;
        long now = System.currentTimeMillis();
        if (now >= nextPeriodBaselineCheckMillis) {
            nextPeriodBaselineCheckMillis = now + 30_000L;
            ensurePeriodBaselines(pvp);
        }
        return pvp;
    }

    private static TpsConfig.PvpSettings livePvpSettings() {
        if (AtomicsClient.CONFIG == null || !AtomicsClient.CONFIG.enabled || AtomicsClient.CONFIG.pvp == null) {
            return null;
        }
        return AtomicsClient.CONFIG.pvp;
    }

    private static void ensurePeriodBaselines(TpsConfig.PvpSettings pvp) {
        if (pvp == null) return;
        boolean changed = false;
        String today = LocalDate.now().toString();
        if (!today.equals(pvp.dailyStatsDate)) {
            pvp.dailyStatsDate = today;
            pvp.dailyBaseDeaths = pvp.allTimeDeaths;
            pvp.dailyBaseKills = pvp.allTimeKills;
            pvp.dailyBaseTotemPops = pvp.allTimeTotemPops;
            pvp.dailyBaseAttackClicks = pvp.allTimeAttackClicks;
            pvp.dailyBaseHitsLanded = pvp.allTimeHitsLanded;
            pvp.dailyBaseDamageTaken = pvp.allTimeDamageTaken;
            changed = true;
        }

        String week = currentWeekKey();
        if (!week.equals(pvp.weeklyStatsKey)) {
            pvp.weeklyStatsKey = week;
            pvp.weeklyBaseDeaths = pvp.allTimeDeaths;
            pvp.weeklyBaseKills = pvp.allTimeKills;
            pvp.weeklyBaseTotemPops = pvp.allTimeTotemPops;
            pvp.weeklyBaseAttackClicks = pvp.allTimeAttackClicks;
            pvp.weeklyBaseHitsLanded = pvp.allTimeHitsLanded;
            pvp.weeklyBaseDamageTaken = pvp.allTimeDamageTaken;
            changed = true;
        }

        String month = currentMonthKey();
        if (!month.equals(pvp.monthlyStatsKey)) {
            pvp.monthlyStatsKey = month;
            pvp.monthlyBaseDeaths = pvp.allTimeDeaths;
            pvp.monthlyBaseKills = pvp.allTimeKills;
            pvp.monthlyBaseTotemPops = pvp.allTimeTotemPops;
            pvp.monthlyBaseAttackClicks = pvp.allTimeAttackClicks;
            pvp.monthlyBaseHitsLanded = pvp.allTimeHitsLanded;
            pvp.monthlyBaseDamageTaken = pvp.allTimeDamageTaken;
            changed = true;
        }

        if (changed) {
            scheduleSave();
        }
    }

    private static String currentWeekKey() {
        LocalDate today = LocalDate.now();
        return String.format(Locale.US, "%04d-W%02d", today.get(IsoFields.WEEK_BASED_YEAR), today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static String currentMonthKey() {
        LocalDate today = LocalDate.now();
        return String.format(Locale.US, "%04d-%02d", today.getYear(), today.getMonthValue());
    }

    private static Counters differenceFrom(TpsConfig.PvpSettings pvp, int deaths, int kills, int totemPops, int attackClicks, int hitsLanded, float damageTaken) {
        Counters counters = new Counters();
        counters.deaths = Math.max(0, pvp.allTimeDeaths - deaths);
        counters.kills = Math.max(0, pvp.allTimeKills - kills);
        counters.totemPops = Math.max(0, pvp.allTimeTotemPops - totemPops);
        counters.attackClicks = Math.max(0, pvp.allTimeAttackClicks - attackClicks);
        counters.hitsLanded = Math.max(0, pvp.allTimeHitsLanded - hitsLanded);
        counters.damageTaken = Math.max(0.0f, pvp.allTimeDamageTaken - damageTaken);
        return counters;
    }

    private static void updateCurrentSessionHistory(TpsConfig.PvpSettings pvp) {
        if (pvp == null) return;
        if (pvp.statSessions == null) {
            pvp.statSessions = new ArrayList<>();
        }

        TpsConfig.SessionStatsSnapshot current = null;
        for (TpsConfig.SessionStatsSnapshot session : pvp.statSessions) {
            if (session != null && CURRENT_SESSION_ID.equals(session.sessionId)) {
                current = session;
                break;
            }
        }

        if (current == null) {
            current = new TpsConfig.SessionStatsSnapshot();
            current.sessionId = CURRENT_SESSION_ID;
            current.label = CURRENT_SESSION_LABEL;
            pvp.statSessions.add(current);
        }

        current.kills = SESSION.kills;
        current.deaths = SESSION.deaths;
        current.totemPops = SESSION.totemPops;
        current.attackClicks = SESSION.attackClicks;
        current.hitsLanded = SESSION.hitsLanded;
        current.damageTaken = SESSION.damageTaken;

        while (pvp.statSessions.size() > 50) {
            pvp.statSessions.remove(0);
        }
    }

    private static void recordDamageTaken(float amount) {
        if (amount <= 0.0f || !Float.isFinite(amount)) return;
        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;

        SESSION.damageTaken += amount;
        scheduleSave();
        if (pvp.allTimeStatsEnabled) {
            pvp.allTimeDamageTaken += amount;
        }
    }

    private static void recordLocalDuelHitTaken(float damageAmount) {
        if (damageAmount <= 0.0f || !Float.isFinite(damageAmount)) return;
        if (usesSpoofedOpponentHealth(Minecraft.getInstance())) {
            localDuelHitsTaken++;
        }
    }

    private static void recordHitLanded(Entity hitEntity) {
        ClientFeatureManager.onHitLanded(hitEntity);

        if (usesSpoofedOpponentHealth(Minecraft.getInstance()) && hitEntity instanceof Player player) {
            statsFor(player).hitsTaken++;
        }

        TpsConfig.PvpSettings pvp = getPvpSettings();
        if (!pvp.sessionStatsEnabled && !pvp.allTimeStatsEnabled) return;
        SESSION.hitsLanded++;
        scheduleSave();
        if (pvp.allTimeStatsEnabled) {
            pvp.allTimeHitsLanded++;
        }
    }

    private static boolean isLocalPlayer(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        return entity != null && client.player != null && entity.getUUID().equals(client.player.getUUID());
    }

    private static boolean isActiveOpponent(Entity entity) {
        return entity != null && activeOpponentUuid != null && activeOpponentUuid.equals(entity.getUUID());
    }

    private static void scheduleSave() {
        saveCooldownTicks = 80;
    }

    private static void saveConfigQuietly() {
        if (AtomicsClient.CONFIG == null) return;
        try {
            AtomicsClient.CONFIG.normalize();
            updateCurrentSessionHistory(AtomicsClient.CONFIG.pvp);
            AtomicsClient.CONFIG.save(FabricLoader.getInstance().getConfigDir().resolve("atomics_client.json"));
        } catch (IOException e) {
            LOGGER.error("Failed to save PvP stats", e);
        }
    }

    private static final class OpponentDuelStats {
        int totemPops;
        int gaps;
        int hitsTaken;
    }

    private record HealthRead(float value, boolean available) {
    }

    private record WinOddsDisplay(int percent, float opponentHealth, boolean showPopCount, int opponentPops,
                                  boolean available, Component suffix) {
        private static final WinOddsDisplay UNAVAILABLE = new WinOddsDisplay(50, 0.0f, false, 0, false, null);
        private static final WinOddsDisplay DEFAULT = UNAVAILABLE;

        private static WinOddsDisplay available(int percent, float opponentHealth, boolean showPopCount, int opponentPops) {
            int color = winOddsGradientColor(percent);
            Component suffix = Component.literal(percent + "%").withColor(color);
            return new WinOddsDisplay(percent, opponentHealth, showPopCount, opponentPops, true, suffix);
        }
    }

    public static class Counters {
        public int deaths;
        public int kills;
        public int totemPops;
        public int attackClicks;
        public int hitsLanded;
        public float damageTaken;

        public float accuracyPercent() {
            return attackClicks <= 0 ? 0.0f : hitsLanded * 100.0f / attackClicks;
        }

        public float kdRatio() {
            return deaths <= 0 ? kills : kills / (float) deaths;
        }
    }

    public record SessionSnapshot(String label, int kills, int deaths, int totemPops, int attackClicks,
                                  int hitsLanded, float damageTaken) {
        public float accuracyPercent() {
            return attackClicks <= 0 ? 0.0f : hitsLanded * 100.0f / attackClicks;
        }

        public float kdRatio() {
            return deaths <= 0 ? kills : kills / (float) deaths;
        }
    }

    public record DeathRecap(String time, String cause, String attacker, String position, float damageTaken,
                             PlayerSnapshot attackerSnapshot) {
        public static DeathRecap empty() {
            return new DeathRecap("", "", "", "", 0.0f, PlayerSnapshot.empty());
        }

        public static DeathRecap from(LivingEntity entity, DamageSource source) {
            String cause = source == null ? "unknown" : source.getMsgId();
            String attacker = "none";
            PlayerSnapshot attackerSnapshot = PlayerSnapshot.empty();
            if (source != null && source.getEntity() != null) {
                attacker = source.getEntity().getName().getString();
                if (source.getEntity() instanceof Player player) {
                    attackerSnapshot = PlayerSnapshot.from(player);
                }
            } else if (source != null && source.getDirectEntity() != null) {
                attacker = source.getDirectEntity().getName().getString();
                if (source.getDirectEntity() instanceof Player player) {
                    attackerSnapshot = PlayerSnapshot.from(player);
                }
            }

            BlockPos pos = entity.blockPosition();
            String position = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            return new DeathRecap(LocalTime.now().format(TIME_FORMAT), cause, attacker, position, SESSION.damageTaken, attackerSnapshot);
        }

        public boolean isEmpty() {
            return time.isEmpty();
        }

        public String shortLine() {
            String line = "Death recap: " + cause + " by " + attacker + " at " + position;
            if (attackerSnapshot.hasPlayerData()) {
                line += " | " + attacker + ": "
                        + formatOne(attackerSnapshot.health()) + " HP, "
                        + attackerSnapshot.totems() + " totems, "
                        + attackerSnapshot.gaps() + " gaps, "
                        + attackerSnapshot.enchantedGaps() + " egaps, "
                        + attackerSnapshot.pots() + " pots";
                if (!attackerSnapshot.fullInventoryKnown()) {
                    line += " visible";
                }
            }
            return line;
        }
    }

    public record PlayerSnapshot(boolean hasPlayerData, boolean fullInventoryKnown, float health, int totems, int gaps, int enchantedGaps, int pots) {
        public static PlayerSnapshot empty() {
            return new PlayerSnapshot(false, false, 0.0f, 0, 0, 0, 0);
        }

        public static PlayerSnapshot from(Player player) {
            if (player == null) return empty();
            Minecraft client = Minecraft.getInstance();
            boolean local = client.player != null && player.getUUID().equals(client.player.getUUID());
            ResourceCounts counts = local ? countFullInventory(player) : countVisibleStacks(player);
            float health = Math.max(0.0f, player.getHealth() + player.getAbsorptionAmount());
            return new PlayerSnapshot(true, local, health, counts.totems, counts.gaps, counts.enchantedGaps, counts.pots);
        }

        private static ResourceCounts countFullInventory(Player player) {
            Inventory inventory = player.getInventory();
            ResourceCounts counts = new ResourceCounts();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                counts.add(inventory.getItem(slot));
            }
            return counts;
        }

        private static ResourceCounts countVisibleStacks(Player player) {
            ResourceCounts counts = new ResourceCounts();
            counts.add(player.getMainHandItem());
            counts.add(player.getOffhandItem());
            counts.add(player.getItemBySlot(EquipmentSlot.HEAD));
            counts.add(player.getItemBySlot(EquipmentSlot.CHEST));
            counts.add(player.getItemBySlot(EquipmentSlot.LEGS));
            counts.add(player.getItemBySlot(EquipmentSlot.FEET));
            return counts;
        }
    }

    private static final class ResourceCounts {
        int totems;
        int crystals;
        int respawnAnchors;
        int gaps;
        int enchantedGaps;
        int pots;

        void add(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return;
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                totems += stack.getCount();
            } else if (stack.is(Items.END_CRYSTAL)) {
                crystals += stack.getCount();
            } else if (stack.is(Items.RESPAWN_ANCHOR)) {
                respawnAnchors += stack.getCount();
            } else if (stack.is(Items.GOLDEN_APPLE)) {
                gaps += stack.getCount();
            } else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                enchantedGaps += stack.getCount();
            } else if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
                pots += stack.getCount();
            }
        }
    }

    public static String formatOne(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String formatPopCount(int pops) {
        return pops == 1 ? "1 pop" : pops + " pops";
    }

    private static int winOddsGradientColor(int percent) {
        float t = Math.max(0.0f, Math.min(1.0f, percent / 100.0f));
        int red;
        int green;
        if (t < 0.5f) {
            red = 255;
            green = Math.round(t * 2.0f * 255.0f);
        } else {
            red = Math.round((1.0f - t) * 2.0f * 255.0f);
            green = 255;
        }
        return (red << 16) | (green << 8);
    }
}
