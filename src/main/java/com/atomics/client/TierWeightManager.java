package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TierWeightManager {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final long CACHE_TTL_MS = 10L * 60L * 1000L;
    private static final long FAILED_TTL_MS = 90L * 1000L;
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<TierProfile>> PENDING = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> MODE_TITLE_CACHE = new ConcurrentHashMap<>();
    private static final Pattern NUMERIC_TIER_PATTERN = Pattern.compile("[1-5]");
    private static final int HEADER_ORANGE = 0xFF9A2E;
    private static final FontDescription.Resource TIER_TAGGER_ICON_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(AtomicsClient.MOD_ID, "tiertagger_icons"));
    private static final ProviderEndpoint[] PROVIDER_ENDPOINTS = new ProviderEndpoint[] {
            new ProviderEndpoint("MCTiers", "https://mctiers.com/api", "/v2/profile/by-name/", "/v2/mode/list", TierSchema.MCTIERS),
            new ProviderEndpoint("PvPTiers", "https://api.skypractice.xyz/api/metatl", "/profile/by-name/", "/mode/list", TierSchema.SKY)
    };

    private TierWeightManager() {
    }

    public static float getAdjustment(Player localPlayer, Player opponent, float scale) {
        if (localPlayer == null || opponent == null || scale <= 0.0f) return 0.0f;

        TierProfile local = getOrRequest(localPlayer.getName().getString());
        TierProfile enemy = getOrRequest(opponent.getName().getString());
        if (local == null || enemy == null || !local.hasScore() || !enemy.hasScore()) {
            return 0.0f;
        }

        float diff = local.score() - enemy.score();
        return Math.max(-14.0f, Math.min(14.0f, diff * scale));
    }

    public static Component getNameSuffix(Player player) {
        if (!shouldShowNameSuffix(player)) {
            return null;
        }

        TierProfile profile = getOrRequest(player.getName().getString());
        if (profile == null || !profile.hasData()) {
            return null;
        }

        TierLine bestLine = bestTierLine(profile);
        String tier = bestLine == null ? "" : normalizeTierTaggerLabel(bestLine.tier());
        if (tier.isBlank()) {
            return null;
        }
        String format = TpsConfig.normalizeOpponentStatsNametagFormat(AtomicsClient.CONFIG.pvp.opponentStatsNametagFormat);
        MutableComponent result = Component.empty();
        if (TpsConfig.OPPONENT_STATS_NAMETAG_ICON_TIER.equals(format)) {
            String icon = modeIcon(bestLine.mode());
            if (!icon.isBlank()) {
                result.append(Component.literal(icon).withStyle(style -> style.withFont(TIER_TAGGER_ICON_FONT)));
            }
        } else if (TpsConfig.OPPONENT_STATS_NAMETAG_MODE_TIER.equals(format)) {
            String mode = compactMode(bestLine.mode());
            if (!mode.isBlank()) {
                result.append(Component.literal(mode + " ").withStyle(gameModeColor(bestLine.mode())));
            }
        }
        result.append(Component.literal(tier).withColor(tierTaggerColor(tier)));
        return result;
    }

    private static boolean shouldShowNameSuffix(Player player) {
        return player != null
                && !isLocalPlayer(player)
                && AtomicsClient.CONFIG != null
                && AtomicsClient.CONFIG.enabled
                && AtomicsClient.CONFIG.pvp != null
                && AtomicsClient.CONFIG.pvp.opponentStatsNametagEnabled;
    }

    public static void sendOpponentInfoChat(String username) {
        String cleanedUsername = cleanUsername(username);
        Minecraft client = Minecraft.getInstance();
        if (!shouldSendOpponentInfo(client, cleanedUsername)) {
            return;
        }

        String key = cleanedUsername.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && !cached.loading && isCacheFresh(cached, now)) {
            sendProfileChat(client, cleanedUsername, cached.profile);
            return;
        }

        requestProfile(cleanedUsername).thenAccept(profile -> {
            Minecraft callbackClient = Minecraft.getInstance();
            if (callbackClient == null) {
                return;
            }
            callbackClient.execute(() -> {
                if (shouldSendOpponentInfo(callbackClient, cleanedUsername)) {
                    sendProfileChat(callbackClient, cleanedUsername, profile);
                }
            });
        });
    }

    private static TierProfile getOrRequest(String username) {
        if (username == null || username.isBlank()) return null;
        String key = username.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        CacheEntry cached = CACHE.get(key);
        if (cached != null) {
            if (cached.loading || isCacheFresh(cached, now)) {
                return cached.profile;
            }
        }

        requestProfile(username);
        return cached == null ? null : cached.profile;
    }

    private static CompletableFuture<TierProfile> requestProfile(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        return PENDING.computeIfAbsent(key, ignored -> {
            CACHE.put(key, new CacheEntry(TierProfile.empty(), System.currentTimeMillis(), true));
            CompletableFuture<TierProfile> future = CompletableFuture
                    .supplyAsync(() -> fetch(username))
                    .handle((profile, error) -> profile == null ? TierProfile.empty() : profile);
            future.whenComplete((profile, error) -> {
                CACHE.put(key, new CacheEntry(profile == null ? TierProfile.empty() : profile, System.currentTimeMillis(), false));
                PENDING.remove(key);
            });
            return future;
        });
    }

    private static boolean isCacheFresh(CacheEntry cached, long now) {
        return cached != null && now - cached.timeMs < (cached.profile.hasData() ? CACHE_TTL_MS : FAILED_TTL_MS);
    }

    private static void sendProfileChat(Minecraft client, String username, TierProfile profile) {
        if (client == null || client.player == null) {
            return;
        }

        AtomicsClient.sendClientMessage(client.player, Component.literal("Opponent Stats:").withStyle(style -> style.withColor(HEADER_ORANGE).withBold(true)), false);
        if (profile == null || !profile.hasData()) {
            AtomicsClient.sendClientMessage(client.player, Component.literal("No tier data found").withStyle(ChatFormatting.GRAY), false);
            return;
        }

        List<TierProviderStats> providers = profile.providers();
        if (providers == null || providers.isEmpty()) {
            String detail = profile.summary().isBlank() ? String.format(Locale.US, "%.0f", profile.score()) : profile.summary();
            AtomicsClient.sendClientMessage(client.player, tierLineText("Overall", detail), false);
            return;
        }

        for (TierProviderStats provider : providers) {
            if (provider == null || provider.lines().isEmpty()) {
                continue;
            }
            AtomicsClient.sendClientMessage(client.player, Component.literal(provider.name() + ":").withStyle(ChatFormatting.BOLD), false);
            for (TierLine line : provider.lines()) {
                AtomicsClient.sendClientMessage(client.player, tierLineText(line.mode(), line.tier()), false);
            }
        }
    }

    private static boolean shouldSendOpponentInfo(Minecraft client, String username) {
        return client != null
                && client.player != null
                && username != null
                && !username.isBlank()
                && AtomicsClient.CONFIG != null
                && AtomicsClient.CONFIG.enabled
                && AtomicsClient.CONFIG.combat != null
                && AtomicsClient.CONFIG.combat.opponentInfoEnabled
                && !username.equalsIgnoreCase(client.player.getGameProfile().name());
    }

    private static String cleanUsername(String username) {
        if (username == null) return "";
        String cleaned = username.trim();
        while (!cleaned.isEmpty() && !Character.isLetterOrDigit(cleaned.charAt(0)) && cleaned.charAt(0) != '_') {
            cleaned = cleaned.substring(1);
        }

        int end = 0;
        while (end < cleaned.length()) {
            char c = cleaned.charAt(end);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                break;
            }
            end++;
        }
        return end <= 0 ? "" : cleaned.substring(0, Math.min(end, 16));
    }

    private static TierProfile fetch(String username) {
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
        float total = 0.0f;
        int scored = 0;
        boolean found = false;
        Set<String> summaries = new LinkedHashSet<>();
        Map<String, List<TierLine>> providerLines = new LinkedHashMap<>();
        Set<String> seenLines = new LinkedHashSet<>();
        for (ProviderEndpoint endpoint : PROVIDER_ENDPOINTS) {
            Optional<TierProfile> profile = fetchProfile(endpoint, encoded);
            if (profile.isPresent() && profile.get().hasData()) {
                found = true;
                if (profile.get().hasScore()) {
                    total += profile.get().score();
                    scored++;
                }
                if (!profile.get().summary().isBlank()) {
                    summaries.add(profile.get().summary());
                }
                if (profile.get().lines().isEmpty() && !profile.get().summary().isBlank()) {
                    addProviderLine(providerLines, seenLines, endpoint.name(), new TierLine("Overall", profile.get().summary()));
                }
                for (TierLine line : profile.get().lines()) {
                    addProviderLine(providerLines, seenLines, endpoint.name(), line);
                }
            }
        }
        if (!found) {
            return TierProfile.empty();
        }
        List<TierProviderStats> providers = new ArrayList<>();
        for (Map.Entry<String, List<TierLine>> entry : providerLines.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                providers.add(new TierProviderStats(entry.getKey(), List.copyOf(entry.getValue())));
            }
        }
        float score = scored <= 0 ? Float.NaN : total / scored;
        return new TierProfile(score, joinSummaries(summaries), List.copyOf(providers), List.of());
    }

    private static Optional<TierProfile> fetchProfile(ProviderEndpoint endpoint, String encodedUsername) {
        Optional<JsonElement> root = fetchJson(endpoint.profileUrl(encodedUsername));
        if (root.isEmpty()) {
            return Optional.empty();
        }
        return switch (endpoint.schema()) {
            case MCTIERS -> profileFromMctiersJson(root.get(), endpoint);
            case SKY -> profileFromSkyJson(root.get(), endpoint);
        };
    }

    private static Optional<JsonElement> fetchJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .header("User-Agent", "AtomicsClientTierWeight/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(JsonParser.parseString(response.body()));
        } catch (IOException | InterruptedException | RuntimeException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private static Optional<TierProfile> profileFromMctiersJson(JsonElement element, ProviderEndpoint endpoint) {
        JsonObject profile = profileObject(element);
        if (profile == null) {
            return Optional.empty();
        }

        JsonObject rankings = getObject(profile, "rankings");
        if (rankings == null || rankings.isEmpty()) {
            return Optional.empty();
        }

        TierAccumulator accumulator = new TierAccumulator();
        for (Map.Entry<String, JsonElement> entry : rankings.entrySet()) {
            if (entry.getKey() == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject ranking = entry.getValue().getAsJsonObject();
            int tier = getInt(ranking, "tier", -1);
            int pos = getInt(ranking, "pos", getInt(ranking, "position", 1));
            if (tier < 1 || tier > 5) {
                continue;
            }
            addTierLabel((pos <= 0 ? "HT" : "LT") + tier, accumulator, modeTitle(endpoint, entry.getKey()));
        }
        return profileFromAccumulator(accumulator);
    }

    private static Optional<TierProfile> profileFromSkyJson(JsonElement element, ProviderEndpoint endpoint) {
        JsonObject profile = profileObject(element);
        if (profile == null) {
            return Optional.empty();
        }

        JsonObject rankings = getObject(profile, "rankings");
        if (rankings == null || rankings.isEmpty()) {
            return Optional.empty();
        }

        TierAccumulator accumulator = new TierAccumulator();
        for (Map.Entry<String, JsonElement> entry : rankings.entrySet()) {
            if (entry.getKey() == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject ranking = entry.getValue().getAsJsonObject();
            String rawTier = firstNonBlank(getString(ranking, "tier"), getString(ranking, "tierName"), getString(ranking, "tier_name"));
            String tierLabel = extractTierLabel(rawTier);
            if (tierLabel != null) {
                addTierLabel(tierLabel, accumulator, modeTitle(endpoint, entry.getKey()));
            } else if (!rawTier.isBlank()) {
                accumulator.lines.add(new TierLine(modeTitle(endpoint, entry.getKey()), rawTier));
            }
        }
        return profileFromAccumulator(accumulator);
    }

    private static Optional<TierProfile> profileFromAccumulator(TierAccumulator accumulator) {
        if (!accumulator.lines.isEmpty()) {
            float score = accumulator.tierScores <= 0 ? Float.NaN : (float) (accumulator.tierScoreTotal / accumulator.tierScores);
            return Optional.of(new TierProfile(score, joinSummaries(accumulator.tierLabels), List.of(), List.copyOf(accumulator.lines)));
        }
        if (accumulator.pointsScores > 0) {
            float score = (float) Math.min(100.0, accumulator.pointsTotal / accumulator.pointsScores / 10.0);
            return Optional.of(new TierProfile(score, String.format(Locale.US, "%.0f", score), List.of(), List.of()));
        }
        return Optional.empty();
    }

    private static JsonObject profileObject(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("error") && obj.get("error").isJsonPrimitive() && obj.get("error").getAsBoolean()) {
            return null;
        }
        JsonObject data = getObject(obj, "data");
        return data == null ? obj : data;
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) {
            return null;
        }
        JsonElement value = obj.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) {
            return "";
        }
        JsonElement value = obj.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String modeTitle(ProviderEndpoint endpoint, String modeId) {
        if (modeId == null || modeId.isBlank()) {
            return "Overall";
        }
        Map<String, String> titles = MODE_TITLE_CACHE.computeIfAbsent(endpoint.modeListUrl(), ignored -> fetchModeTitles(endpoint));
        return titles.getOrDefault(modeId, prettifyMode(modeId));
    }

    private static Map<String, String> fetchModeTitles(ProviderEndpoint endpoint) {
        Map<String, String> titles = new LinkedHashMap<>(fallbackModeTitles());
        Optional<JsonElement> root = fetchJson(endpoint.modeListUrl());
        if (root.isEmpty() || !root.get().isJsonObject()) {
            return titles;
        }
        JsonObject obj = root.get().getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getKey() == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            String title = getString(entry.getValue().getAsJsonObject(), "title");
            if (!title.isBlank()) {
                titles.put(entry.getKey(), title);
            }
        }
        return titles;
    }

    private static Map<String, String> fallbackModeTitles() {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("axe", "Axe");
        titles.put("crystal", "Crystal");
        titles.put("diapot", "DiaPot");
        titles.put("mace", "Mace");
        titles.put("nethop", "Netherite OP");
        titles.put("neth_pot", "Netherite OP");
        titles.put("nethpot", "NethPot");
        titles.put("pot", "Pot");
        titles.put("smp", "SMP");
        titles.put("sword", "Sword");
        titles.put("uhc", "UHC");
        titles.put("vanilla", "Vanilla");
        return titles;
    }

    private static Optional<TierProfile> profileFromJson(JsonElement element) {
        TierAccumulator accumulator = new TierAccumulator();
        scanJson(element, accumulator, "");
        if (accumulator.tierScores > 0) {
            return Optional.of(new TierProfile(
                    (float) (accumulator.tierScoreTotal / accumulator.tierScores),
                    joinSummaries(accumulator.tierLabels),
                    List.of(),
                    List.copyOf(accumulator.lines)
            ));
        }
        if (accumulator.pointsScores > 0) {
            float score = (float) Math.min(100.0, accumulator.pointsTotal / accumulator.pointsScores / 10.0);
            return Optional.of(new TierProfile(score, String.format(Locale.US, "%.0f", score), List.of(), List.of()));
        }
        return Optional.empty();
    }

    private static void scanJson(JsonElement element, TierAccumulator accumulator, String contextKey) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                addTierString(element.getAsString(), accumulator, modeFromContext(contextKey));
            }
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) scanJson(child, accumulator, contextKey);
            return;
        }
        if (!element.isJsonObject()) return;

        JsonObject obj = element.getAsJsonObject();
        String mode = findModeName(obj, contextKey);
        boolean hasCurrentTier = obj.has("tier") && obj.get("tier").isJsonPrimitive();
        if (obj.has("tier") && obj.get("tier").isJsonPrimitive()) {
            JsonElement tierElement = obj.get("tier");
            if (tierElement.getAsJsonPrimitive().isNumber()) {
                int tier = tierElement.getAsInt();
                int pos = getInt(obj, "pos", getInt(obj, "position", getInt(obj, "tier_pos", 1)));
                addNumericTier(tier, pos, accumulator, mode);
            } else if (tierElement.getAsJsonPrimitive().isString()) {
                addTierString(tierElement.getAsString(), accumulator, mode);
            }
        }
        if (!hasCurrentTier && obj.has("peak_tier") && obj.get("peak_tier").isJsonPrimitive() && obj.get("peak_tier").getAsJsonPrimitive().isNumber()) {
            int tier = obj.get("peak_tier").getAsInt();
            int pos = getInt(obj, "peak_pos", 1);
            addNumericTier(tier, pos, accumulator, mode.isBlank() ? "Peak" : mode + " Peak");
        }
        for (String key : new String[] {"points", "score", "rating", "elo", "skill_rating", "skillRating"}) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isNumber()) {
                accumulator.pointsTotal += obj.get(key).getAsDouble();
                accumulator.pointsScores++;
            }
        }
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (!isHandledTierField(entry.getKey())) {
                scanJson(entry.getValue(), accumulator, entry.getKey());
            }
        }
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        try {
            return obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isNumber()
                    ? obj.get(key).getAsInt()
                    : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static void addNumericTier(int tier, int pos, TierAccumulator accumulator, String mode) {
        if (tier < 1 || tier > 5) return;
        // MCTiers-style: tier 1 is best, tier 5 is worst. pos 0/high is better than pos 1/low.
        addTierLabel((pos <= 0 ? "HT" : "LT") + tier, accumulator, mode);
    }

    private static void addTierString(String raw, TierAccumulator accumulator, String mode) {
        if (raw == null) return;
        String tierLabel = extractTierLabel(raw);
        if (tierLabel == null) return;
        addTierLabel(tierLabel, accumulator, mode);
    }

    private static void addTierLabel(String tierLabel, TierAccumulator accumulator, String mode) {
        int number = Character.digit(tierLabel.charAt(2), 10);
        if (number < 1 || number > 5) return;
        String displayMode = mode == null || mode.isBlank() ? "Overall" : mode;
        String scoreKey = displayMode.toLowerCase(Locale.ROOT) + "|" + tierLabel;
        if (!accumulator.scoreKeys.add(scoreKey)) {
            return;
        }
        double score = (6 - number) * 18.0 + (tierLabel.charAt(0) == 'H' ? 10.0 : 0.0);
        accumulator.tierScoreTotal += score;
        accumulator.tierScores++;
        accumulator.tierLabels.add(tierLabel);
        accumulator.lines.add(new TierLine(displayMode, tierLabel));
    }

    private static void addProviderLine(Map<String, List<TierLine>> providerLines, Set<String> seenLines, String provider, TierLine line) {
        if (line == null || line.tier() == null || line.tier().isBlank()) {
            return;
        }
        String providerName = provider == null || provider.isBlank() ? "Tiers" : provider;
        String mode = line.mode() == null || line.mode().isBlank() ? "Overall" : line.mode();
        String key = providerName + "|" + mode.toLowerCase(Locale.ROOT) + "|" + line.tier().toUpperCase(Locale.ROOT);
        if (!seenLines.add(key)) {
            return;
        }
        providerLines.computeIfAbsent(providerName, ignored -> new ArrayList<>()).add(new TierLine(mode, line.tier()));
    }

    private static MutableComponent tierLineText(String mode, String tier) {
        String displayMode = mode == null || mode.isBlank() ? "Overall" : mode;
        String displayTier = tier == null || tier.isBlank() ? "Unknown" : tier;
        MutableComponent text = Component.literal(displayMode + ": ").withStyle(gameModeColor(displayMode), ChatFormatting.BOLD);
        text.append(Component.literal(displayTier).withStyle(tierColor(displayTier), ChatFormatting.BOLD));
        return text;
    }

    private static TierLine bestTierLine(TierProfile profile) {
        TierLine best = null;
        int bestRank = Integer.MAX_VALUE;
        if (profile.providers() != null) {
            for (TierProviderStats provider : profile.providers()) {
                if (provider == null || provider.lines() == null) continue;
                for (TierLine line : provider.lines()) {
                    int rank = tierRank(line == null ? null : line.tier());
                    if (rank < bestRank) {
                        best = line;
                        bestRank = rank;
                    }
                }
            }
        }
        if (profile.lines() != null) {
            for (TierLine line : profile.lines()) {
                int rank = tierRank(line == null ? null : line.tier());
                if (rank < bestRank) {
                    best = line;
                    bestRank = rank;
                }
            }
        }
        return best;
    }

    private static int tierRank(String tier) {
        String normalized = tier == null ? "" : tier.toUpperCase(Locale.ROOT).replace(" ", "");
        if (normalized.length() < 3) return Integer.MAX_VALUE;
        int number = Character.digit(normalized.charAt(2), 10);
        if (number < 1 || number > 5) return Integer.MAX_VALUE;
        int highLow = normalized.startsWith("HT") ? 0 : normalized.startsWith("LT") ? 1 : 2;
        return number * 10 + highLow;
    }

    private static String compactMode(String mode) {
        String key = normalizeModeKey(mode);
        return switch (key) {
            case "crystal", "cpvp" -> "CPVP";
            case "sword", "swordpvp" -> "Sword";
            case "axe", "axepvp" -> "Axe";
            case "uhc" -> "UHC";
            case "smp", "survival" -> "SMP";
            case "nethpot", "netheritepot", "nethop", "netheriteop" -> "Neth";
            case "pot", "potpvp" -> "Pot";
            case "diamond", "dia", "diapot" -> "Dia";
            case "mace" -> "Mace";
            case "bow" -> "Bow";
            case "overall" -> "";
            default -> {
                String pretty = prettifyMode(mode);
                yield pretty.length() > 8 ? pretty.substring(0, 8) : pretty;
            }
        };
    }

    private static ChatFormatting gameModeColor(String mode) {
        String key = normalizeModeKey(mode);
        return switch (key) {
            case "crystal", "cpvp" -> ChatFormatting.LIGHT_PURPLE;
            case "sword", "swordpvp" -> ChatFormatting.AQUA;
            case "axe", "axepvp" -> ChatFormatting.RED;
            case "uhc" -> ChatFormatting.GOLD;
            case "smp", "survival" -> ChatFormatting.GREEN;
            case "nethpot", "netheritepot", "pot", "potpvp" -> ChatFormatting.DARK_PURPLE;
            case "diamond", "dia", "diapot" -> ChatFormatting.BLUE;
            case "mace" -> ChatFormatting.YELLOW;
            case "bow" -> ChatFormatting.DARK_GREEN;
            default -> ChatFormatting.WHITE;
        };
    }

    private static String modeIcon(String mode) {
        String key = normalizeModeKey(mode);
        return switch (key) {
            case "axe", "axepvp" -> "\uE701";
            case "mace" -> "\uE702";
            case "nethop", "netheriteop", "nethpot", "netheritepot" -> "\uE703";
            case "pot", "potpvp" -> "\uE704";
            case "smp", "survival" -> "\uE705";
            case "sword", "swordpvp" -> "\uE706";
            case "uhc" -> "\uE707";
            case "vanilla", "crystal", "cpvp" -> "\uE708";
            case "bed", "bedfight" -> "\uE801";
            case "bow" -> "\uE802";
            case "creeper" -> "\uE803";
            case "debuff" -> "\uE804";
            case "diacrystal", "diamondcrystal", "diapot", "diamond", "dia" -> "\uE805";
            case "diasmp", "diamondsmp" -> "\uE806";
            case "elytra" -> "\uE807";
            case "manhunt" -> "\uE808";
            case "minecart" -> "\uE809";
            case "ogvanilla" -> "\uE810";
            case "speed" -> "\uE811";
            case "trident" -> "\uE812";
            default -> "";
        };
    }

    private static String normalizeTierTaggerLabel(String tier) {
        if (tier == null) {
            return "";
        }
        String normalized = tier.trim()
                .toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
        if (normalized.startsWith("R") && normalized.length() >= 4) {
            String activeTier = normalized.substring(1);
            if (activeTier.matches("[HL]T[1-5]")) {
                return "R" + activeTier;
            }
        }
        return normalized.matches("[HL]T[1-5]") ? normalized : "";
    }

    private static int tierTaggerColor(String tier) {
        String normalized = tier == null ? "" : tier.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("R")) return 10671871;
        return switch (normalized) {
            case "HT1" -> 15252026;
            case "LT1" -> 14005077;
            case "HT2" -> 12899303;
            case "LT2" -> 10528690;
            case "HT3" -> 16293722;
            case "LT3" -> 13007682;
            case "HT4" -> 8483994;
            case "LT4" -> 6642553;
            case "HT5" -> 9405096;
            case "LT5" -> 6642553;
            default -> 13882323;
        };
    }

    private static ChatFormatting tierColor(String tier) {
        String normalized = tier == null ? "" : tier.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("HT1") || normalized.startsWith("LT1")) return ChatFormatting.RED;
        if (normalized.startsWith("HT2") || normalized.startsWith("LT2")) return ChatFormatting.GOLD;
        if (normalized.startsWith("HT3") || normalized.startsWith("LT3")) return ChatFormatting.YELLOW;
        if (normalized.startsWith("HT4") || normalized.startsWith("LT4")) return ChatFormatting.GREEN;
        if (normalized.startsWith("HT5") || normalized.startsWith("LT5")) return ChatFormatting.GRAY;
        return ChatFormatting.WHITE;
    }

    private static String extractTierLabel(String raw) {
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
        if (normalized.length() < 2) return null;
        for (int tier = 1; tier <= 5; tier++) {
            if (normalized.contains("HT" + tier) || normalized.contains("HIGHTIER" + tier)) {
                return "HT" + tier;
            }
            if (normalized.contains("LT" + tier) || normalized.contains("LOWTIER" + tier)) {
                return "LT" + tier;
            }
        }
        if (normalized.startsWith("TIER") || normalized.startsWith("T")) {
            Matcher matcher = NUMERIC_TIER_PATTERN.matcher(normalized);
            if (matcher.find()) {
                return "LT" + matcher.group();
            }
        }
        return null;
    }

    private static String findModeName(JsonObject obj, String contextKey) {
        for (String key : new String[] {"mode", "gamemode", "gameMode", "kit", "category", "kit_name", "kitName", "type"}) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isString()) {
                String mode = prettifyMode(obj.get(key).getAsString());
                if (!mode.isBlank()) {
                    return mode;
                }
            }
        }
        return modeFromContext(contextKey);
    }

    private static String modeFromContext(String contextKey) {
        if (contextKey == null || contextKey.isBlank() || isGenericContextKey(contextKey)) {
            return "";
        }
        return prettifyMode(contextKey);
    }

    private static String prettifyMode(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim().replace('_', ' ').replace('-', ' ');
        if (cleaned.isBlank() || isGenericContextKey(cleaned)) {
            return "";
        }
        String[] parts = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private static boolean isHandledTierField(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return normalized.equals("tier")
                || normalized.equals("peak_tier")
                || normalized.equals("pos")
                || normalized.equals("position")
                || normalized.equals("tier_pos")
                || normalized.equals("peak_pos");
    }

    private static boolean isGenericContextKey(String key) {
        String normalized = normalizeModeKey(key);
        return normalized.isBlank()
                || normalized.equals("data")
                || normalized.equals("player")
                || normalized.equals("profile")
                || normalized.equals("rankings")
                || normalized.equals("ranking")
                || normalized.equals("tiers")
                || normalized.equals("tier")
                || normalized.equals("entries")
                || normalized.equals("categories")
                || normalized.equals("results")
                || normalized.equals("records")
                || normalized.equals("stats")
                || normalized.equals("username")
                || normalized.equals("uuid");
    }

    private static String normalizeModeKey(String mode) {
        return mode == null
                ? ""
                : mode.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    private static boolean isLocalPlayer(Player player) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && player.getUUID().equals(client.player.getUUID());
    }

    private static String joinSummaries(Set<String> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String summary : summaries) {
            if (summary == null || summary.isBlank()) continue;
            if (builder.length() > 0) builder.append('/');
            builder.append(summary);
            if (++count >= 3) break;
        }
        return builder.toString();
    }

    private record CacheEntry(TierProfile profile, long timeMs, boolean loading) {
    }

    private enum TierSchema {
        MCTIERS,
        SKY
    }

    private record ProviderEndpoint(String name, String baseUrl, String profileByNamePath, String modeListPath,
                                    TierSchema schema) {
        private String profileUrl(String encodedUsername) {
            return baseUrl + profileByNamePath + encodedUsername;
        }

        private String modeListUrl() {
            return baseUrl + modeListPath;
        }
    }

    private record TierProviderStats(String name, List<TierLine> lines) {
    }

    private record TierLine(String mode, String tier) {
    }

    private record TierProfile(float score, String summary, List<TierProviderStats> providers, List<TierLine> lines) {
        static TierProfile empty() {
            return new TierProfile(Float.NaN, "", List.of(), List.of());
        }

        boolean hasScore() {
            return Float.isFinite(score);
        }

        boolean hasData() {
            return hasScore()
                    || (summary != null && !summary.isBlank())
                    || (providers != null && !providers.isEmpty())
                    || (lines != null && !lines.isEmpty());
        }
    }

    private static final class TierAccumulator {
        double tierScoreTotal;
        int tierScores;
        double pointsTotal;
        int pointsScores;
        Set<String> tierLabels = new LinkedHashSet<>();
        Set<String> scoreKeys = new LinkedHashSet<>();
        Set<TierLine> lines = new LinkedHashSet<>();
    }
}
