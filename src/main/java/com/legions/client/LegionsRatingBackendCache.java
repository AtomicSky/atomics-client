package com.legions.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LegionsRatingBackendCache {
    private static final String RATING_ENDPOINT = "http://217.154.170.41:3000/rating/";
    private static final long TTL_MILLIS = 60_000L;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final Map<String, Double> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LegionsClient-RatingBackend");
        thread.setDaemon(true);
        return thread;
    });

    private LegionsRatingBackendCache() {
    }

    public static Double getCached(String playerName) {
        String key = normalizeKey(playerName);
        return key.isEmpty() ? null : CACHE.get(key);
    }

    public static void preload(String playerName) {
        String key = normalizeKey(playerName);
        if (key.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastAttempt = TIMESTAMPS.get(key);
        if (lastAttempt != null && now - lastAttempt < TTL_MILLIS) {
            return;
        }
        if (!IN_FLIGHT.add(key)) {
            return;
        }

        TIMESTAMPS.put(key, now);
        EXECUTOR.execute(() -> fetchRating(key));
    }

    private static void fetchRating(String key) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RATING_ENDPOINT + key))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("rating") || json.get("rating").isJsonNull()) {
                return;
            }

            double rating = json.get("rating").getAsDouble();
            if (Double.isFinite(rating)) {
                CACHE.put(key, rating);
            }
        } catch (Exception e) {
            LegionsClient.LOGGER.debug("Failed to fetch Legions backend rating for {}.", key, e);
        } finally {
            IN_FLIGHT.remove(key);
        }
    }

    private static String normalizeKey(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }
}
