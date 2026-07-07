package com.legions.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import com.legions.client.render.LegionsPlayerOverlayColorContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegionsFeatures {
    private static final Pattern LEGIONS_TAB_TAG_PATTERN = Pattern.compile("\\[(\\d\\.\\d|\\?)\\]");
    private static final boolean ATOMICS_CLIENT_LOADED = FabricLoader.getInstance().isModLoaded("atomics_client");
    private static final int QUIP_UNKNOWN_COLOR = 0xA0A0A0;
    private static final int MIN_QUIP_OVERLAY_RATING = 100;
    private static final int MAX_QUIP_OVERLAY_RATING = 2000;
    private static final int MIN_QUIP_FULL_OVERLAY_ALPHA = 51;
    private static final int DEFAULT_FOE_FULL_OVERLAY_ALPHA = 128;
    private static final Set<UUID> visibleOpponentCache = new HashSet<>();
    private static final ArrayList<VisibleOpponent> visibleOpponentScratch = new ArrayList<>();
    private static final Map<String, TabListTag> tabListTagCache = new HashMap<>();
    private static long visibleOpponentCacheTick = Long.MIN_VALUE;
    private static UUID visibleOpponentCacheLocalPlayer;
    private static int visibleOpponentCacheLimit = -1;
    private static boolean visibleOpponentCacheEnabled;
    private static int visibleOpponentCachePlayerCount = -1;
    private static Object tabListTagCacheHandler;
    private static long tabListTagCacheTick = Long.MIN_VALUE;
    private static int tabListTagCacheSize = -1;
    private static long atomicsTierSlotCacheTick = Long.MIN_VALUE;
    private static boolean atomicsTierSlotEnabledCache;
    private static Method atomicsTierSuffixMethod;
    private static boolean atomicsTierSuffixMethodChecked;

    private LegionsFeatures() {
    }

    public static boolean isLegionsServer(MinecraftClient client) {
        if (client == null) {
            return false;
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null || server.address == null) {
            return client.isIntegratedServerRunning();
        }
        String address = normalizeServerAddress(server.address);
        if (LegionsClient.CONFIG == null || LegionsClient.CONFIG.allowedServerAddresses == null) {
            return address.contains("legions");
        }
        for (String allowedAddress : LegionsClient.CONFIG.allowedServerAddresses) {
            String normalizedAllowedAddress = normalizeServerAddress(allowedAddress);
            if (!normalizedAllowedAddress.isBlank()
                    && (address.equals(normalizedAllowedAddress) || address.contains(normalizedAllowedAddress))) {
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftClient client) {
    }

    public static String serverAddressesText() {
        if (LegionsClient.CONFIG == null || LegionsClient.CONFIG.allowedServerAddresses == null) {
            return "legions";
        }
        return String.join(", ", LegionsClient.CONFIG.allowedServerAddresses);
    }

    public static void setServerAddressesText(String text) {
        if (LegionsClient.CONFIG == null) {
            return;
        }
        LegionsClient.CONFIG.allowedServerAddresses = new ArrayList<>();
        if (text != null) {
            for (String part : text.split(",")) {
                String address = normalizeServerAddress(part);
                if (!address.isBlank() && !LegionsClient.CONFIG.allowedServerAddresses.contains(address)) {
                    LegionsClient.CONFIG.allowedServerAddresses.add(address);
                }
            }
        }
        LegionsClient.CONFIG.normalize();
    }

    private static String normalizeServerAddress(String address) {
        if (address == null) {
            return "";
        }
        String normalized = address.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring("http://".length());
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring("https://".length());
        }
        int slash = normalized.indexOf('/');
        return slash >= 0 ? normalized.substring(0, slash) : normalized;
    }

    public static Text customizeNametag(PlayerEntity player, Text original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.ratingNametagsEnabled) {
            return original;
        }
        if (shouldUseAtomicsTierSlot(player)) {
            return original;
        }
        TabListTag tag = getTabListTag(client, realUsername(player));
        if (tag == null) {
            return original;
        }
        Text suffix = getNametagSuffix(player, tag);
        if (suffix == null) {
            return original;
        }
        Text base = ATOMICS_CLIENT_LOADED ? Text.literal(realUsername(player)) : original;
        String originalText = original.getString();
        if (!ATOMICS_CLIENT_LOADED && originalText.endsWith(suffix.getString())) {
            return original;
        }
        return Text.empty().append(base).append(suffix);
    }

    private static Text getNametagSuffix(PlayerEntity player, TabListTag tag) {
        if (tag.isUnknown()) {
            Text atomicsTier = getAtomicsTierSuffix(player);
            if (atomicsTier != null && !atomicsTier.getString().isBlank()) {
                return Text.empty().append(Text.literal(" ")).append(atomicsTier);
            }
        }

        String separator = ATOMICS_CLIENT_LOADED ? " " : " | ";
        return Text.empty().append(Text.literal(separator)).append(formatLegionsTag(tag));
    }

    public static int getOutlineColor(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || client.player == null) {
            return 0;
        }
        if (LegionsPingManager.isMarkedPlayer(player)) {
            return 0xFFFFD84A;
        }
        if (shouldHighlightTeamAsSpectator(client, player)) {
            return getTeamOutlineColor(player);
        }
        if (LegionsClient.CONFIG.automaticFoeOutlinesEnabled && isOpponent(client.player, player)) {
            return getTeamOutlineColor(player);
        }
        return 0;
    }

    public static int getOverlayStyle(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || client.player == null || player == null) {
            return LegionsPlayerOverlayColorContext.STYLE_OUTLINE;
        }
        if (LegionsPingManager.isMarkedPlayer(player) || shouldHighlightTeamAsSpectator(client, player)) {
            return LegionsPlayerOverlayColorContext.STYLE_OUTLINE;
        }
        if (LegionsClient.CONFIG.automaticFoeOutlinesEnabled && isOpponent(client.player, player)) {
            return LegionsClient.CONFIG.automaticFoeRenderStyle;
        }
        return LegionsPlayerOverlayColorContext.STYLE_OUTLINE;
    }

    public static int getFilledOverlayColor(PlayerEntity player, int overlayColor, int overlayStyle) {
        if (overlayColor == 0 || !usesFilledOverlay(overlayStyle)) {
            return -1;
        }
        if (!isAutomaticFoeOverlay(player)) {
            return overlayColor;
        }

        int alpha = foeFullOverlayAlpha(player);
        return (overlayColor & 0x00FFFFFF) | (alpha << 24);
    }

    private static boolean usesFilledOverlay(int overlayStyle) {
        return overlayStyle == LegionsPlayerOverlayColorContext.STYLE_FULL
                || overlayStyle == LegionsPlayerOverlayColorContext.STYLE_OUTLINE_FULL
                || overlayStyle == LegionsPlayerOverlayColorContext.STYLE_PULSE;
    }

    private static boolean isAutomaticFoeOverlay(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return LegionsClient.enabled(client)
                && client.player != null
                && player != null
                && LegionsClient.CONFIG.automaticFoeOutlinesEnabled
                && isOpponent(client.player, player)
                && !LegionsPingManager.isMarkedPlayer(player)
                && !shouldHighlightTeamAsSpectator(client, player);
    }

    private static int foeFullOverlayAlpha(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        TabListTag tag = getTabListTag(client, realUsername(player));
        if (tag == null) {
            return DEFAULT_FOE_FULL_OVERLAY_ALPHA;
        }
        if (tag.isUnknown()) {
            return 0;
        }

        int clampedRating = clamp(tag.numericRating, MIN_QUIP_OVERLAY_RATING, MAX_QUIP_OVERLAY_RATING);
        float opacity = (float) (clampedRating - MIN_QUIP_OVERLAY_RATING)
                / (MAX_QUIP_OVERLAY_RATING - MIN_QUIP_OVERLAY_RATING);
        return MIN_QUIP_FULL_OVERLAY_ALPHA + Math.round(opacity * (255.0f - MIN_QUIP_FULL_OVERLAY_ALPHA));
    }

    public static boolean shouldHidePlayerModel(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || client.world == null || client.player == null || player == null) {
            return false;
        }
        if (player == client.player || LegionsPingManager.isMarkedPlayer(player)) {
            return false;
        }
        if (LegionsClient.CONFIG.spectatorGlowEnabled && isSpectatorTeam(client.player)) {
            return false;
        }

        boolean opponent = isOpponent(client.player, player);
        if (opponent && LegionsClient.CONFIG.opponentLimitEnabled && !visibleOpponentCache(client).contains(player.getUuid())) {
            return true;
        }

        return shouldCullForRenderOptimization(client, player);
    }

    private static boolean shouldCullForRenderOptimization(MinecraftClient client, PlayerEntity player) {
        if (!LegionsClient.CONFIG.playerRenderOptimizationEnabled) {
            return false;
        }

        int renderDistance = LegionsClient.CONFIG.playerRenderDistance;
        double maxDistanceSquared = (double) renderDistance * renderDistance;
        return player.squaredDistanceTo(client.player) > maxDistanceSquared;
    }

    private static Set<UUID> visibleOpponentCache(MinecraftClient client) {
        int playerCount = client.world.getPlayers().size();
        long tick = client.world.getTime();
        int visibleOpponents = Math.max(0, LegionsClient.CONFIG.opponentLimit);
        if (visibleOpponentCacheTick == tick
                && visibleOpponentCacheLocalPlayer != null
                && visibleOpponentCacheLocalPlayer.equals(client.player.getUuid())
                && visibleOpponentCacheLimit == visibleOpponents
                && visibleOpponentCacheEnabled == LegionsClient.CONFIG.opponentLimitEnabled
                && visibleOpponentCachePlayerCount == playerCount) {
            return visibleOpponentCache;
        }

        visibleOpponentCache.clear();
        visibleOpponentCacheTick = tick;
        visibleOpponentCacheLocalPlayer = client.player.getUuid();
        visibleOpponentCacheLimit = visibleOpponents;
        visibleOpponentCacheEnabled = LegionsClient.CONFIG.opponentLimitEnabled;
        visibleOpponentCachePlayerCount = playerCount;

        if (!LegionsClient.CONFIG.opponentLimitEnabled) {
            return visibleOpponentCache;
        }

        visibleOpponentScratch.clear();
        PlayerEntity localPlayer = client.player;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (isOpponent(localPlayer, candidate)) {
                visibleOpponentScratch.add(new VisibleOpponent(candidate, candidate.squaredDistanceTo(localPlayer)));
            }
        }
        visibleOpponentScratch.sort((first, second) -> Double.compare(first.distanceSquared, second.distanceSquared));
        int limit = Math.min(visibleOpponents, visibleOpponentScratch.size());
        for (int i = 0; i < limit; i++) {
            visibleOpponentCache.add(visibleOpponentScratch.get(i).player.getUuid());
        }
        visibleOpponentScratch.clear();
        return visibleOpponentCache;
    }

    public static boolean shouldHidePlayerRenderState(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || state == null) {
            return false;
        }
        Entity entity = client.world.getEntityById(state.id);
        return entity instanceof PlayerEntity player && shouldHidePlayerModel(player);
    }

    public static int getRating(MinecraftClient client, String playerName) {
        TabListTag tag = getTabListTag(client, playerName);
        return tag == null ? -1 : tag.numericRating;
    }

    public static boolean shouldSuppressAtomicsTierSuffix(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!LegionsClient.enabled(client) || !LegionsClient.CONFIG.ratingNametagsEnabled || player == null) {
            return false;
        }
        TabListTag tag = getTabListTag(client, realUsername(player));
        return tag != null && !tag.isUnknown();
    }

    public static Text getAtomicsTierSlotReplacement(PlayerEntity player) {
        if (!shouldUseAtomicsTierSlot(player)) {
            return null;
        }
        TabListTag tag = getTabListTag(MinecraftClient.getInstance(), realUsername(player));
        return tag == null || tag.isUnknown() ? null : formatLegionsTag(tag);
    }

    public static Text getAtomicsUnknownTierSlotFallback(PlayerEntity player) {
        if (!shouldUseAtomicsTierSlot(player)) {
            return null;
        }
        TabListTag tag = getTabListTag(MinecraftClient.getInstance(), realUsername(player));
        return tag != null && tag.isUnknown() ? formatLegionsTag(tag) : null;
    }

    private static boolean shouldUseAtomicsTierSlot(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ATOMICS_CLIENT_LOADED || player == null || !LegionsClient.enabled(client) || !LegionsClient.CONFIG.ratingNametagsEnabled) {
            return false;
        }
        return atomicsTierSlotEnabled(client);
    }

    private static boolean atomicsTierSlotEnabled(MinecraftClient client) {
        long tick = client.world == null ? Long.MIN_VALUE : client.world.getTime();
        if (atomicsTierSlotCacheTick == tick) {
            return atomicsTierSlotEnabledCache;
        }

        atomicsTierSlotCacheTick = tick;
        try {
            Class<?> atomicsClient = Class.forName("com.atomics.client.AtomicsClient");
            Object config = getStaticField(atomicsClient, "CONFIG");
            if (config == null || !getBooleanField(config, "enabled")) {
                atomicsTierSlotEnabledCache = false;
                return false;
            }
            Object pvp = getField(config, "pvp");
            atomicsTierSlotEnabledCache = pvp != null && getBooleanField(pvp, "opponentStatsNametagEnabled");
            return atomicsTierSlotEnabledCache;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Atomics tier slot config is not available.", e);
            atomicsTierSlotEnabledCache = false;
            return false;
        }
    }

    private static TabListTag getTabListTag(MinecraftClient client, String playerName) {
        if (client == null || client.getNetworkHandler() == null || playerName == null) {
            return null;
        }
        refreshTabListTagCache(client);
        return tabListTagCache.get(playerName.toLowerCase(Locale.ROOT));
    }

    private static void refreshTabListTagCache(MinecraftClient client) {
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        int size = networkHandler.getPlayerList().size();
        long tick = client.world == null ? Long.MIN_VALUE : client.world.getTime();
        if (tabListTagCacheHandler == networkHandler && tabListTagCacheTick == tick && tabListTagCacheSize == size) {
            return;
        }

        tabListTagCache.clear();
        tabListTagCacheHandler = networkHandler;
        tabListTagCacheTick = tick;
        tabListTagCacheSize = size;

        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            Text displayName = entry.getDisplayName();
            String text = displayName == null ? entry.getProfile().name() : displayName.getString();
            TabListTag tag = parseTabListTag(text);
            if (tag != null) {
                tabListTagCache.put(entry.getProfile().name().toLowerCase(Locale.ROOT), tag);
            }
        }
    }

    private static Text getAtomicsTierSuffix(PlayerEntity player) {
        Method method = getAtomicsTierSuffixMethod();
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(null, player);
            return value instanceof Text text ? text : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Failed to get Atomics tier suffix for {}.", realUsername(player), e);
            return null;
        }
    }

    private static Method getAtomicsTierSuffixMethod() {
        if (!ATOMICS_CLIENT_LOADED) {
            return null;
        }
        if (atomicsTierSuffixMethodChecked) {
            return atomicsTierSuffixMethod;
        }
        atomicsTierSuffixMethodChecked = true;
        try {
            Class<?> manager = Class.forName("com.atomics.client.TierWeightManager");
            atomicsTierSuffixMethod = manager.getDeclaredMethod("getNameSuffix", PlayerEntity.class);
            atomicsTierSuffixMethod.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegionsClient.LOGGER.debug("Atomics tier suffix fallback is not available.", e);
            atomicsTierSuffixMethod = null;
        }
        return atomicsTierSuffixMethod;
    }

    private static TabListTag parseTabListTag(String text) {
        Matcher matcher = LEGIONS_TAB_TAG_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        if ("?".equals(value)) {
            return new TabListTag(value, -1);
        }

        int rating = ((value.charAt(0) - '0') * 1000) + ((value.charAt(2) - '0') * 100);
        return new TabListTag(value, rating);
    }

    private static Style quipStyle(TabListTag tag) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(quipColor(tag)));
        return tag.numericRating >= 2000 ? style.withBold(true) : style;
    }

    private static Text formatLegionsTag(TabListTag tag) {
        Style style = quipStyle(tag);
        return Text.empty()
                .append(Text.literal("[").setStyle(style))
                .append(Text.literal(tag.value).setStyle(style))
                .append(Text.literal("]").setStyle(style));
    }

    private static int quipColor(TabListTag tag) {
        int rating = tag.numericRating;
        if (rating < 0) {
            return QUIP_UNKNOWN_COLOR;
        }
        if (rating >= 2000) {
            return 0xFC3200;
        }
        if (rating >= 1900) {
            return 0xFC5400;
        }
        if (rating >= 1800) {
            return 0xFC8700;
        }
        if (rating >= 1700) {
            return 0xEBA800;
        }
        if (rating >= 1600) {
            return 0xC9C900;
        }
        if (rating >= 1500) {
            return 0xA8EB00;
        }
        if (rating >= 1400) {
            return 0x85C700;
        }
        if (rating >= 1300) {
            return 0x64A811;
        }
        if (rating >= 1200) {
            return 0x448744;
        }
        if (rating >= 1100) {
            return 0x226575;
        }
        if (rating >= 1000) {
            return 0x1143A8;
        }
        if (rating >= 900) {
            return 0x1064C9;
        }
        if (rating >= 800) {
            return 0x3285D9;
        }
        if (rating >= 700) {
            return 0x729BE8;
        }
        if (rating >= 600) {
            return 0xA7C1F2;
        }
        if (rating >= 500) {
            return 0xCBDAF7;
        }
        if (rating >= 400) {
            return 0xDCE7FC;
        }
        if (rating >= 300) {
            return 0xF0F5FF;
        }
        if (rating >= 200) {
            return 0xF0F0F0;
        }
        if (rating >= 100) {
            return 0xFFFFFF;
        }
        return 0xFCFCFC;
    }

    private static Object getStaticField(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object getField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static boolean getBooleanField(Object owner, String name) throws ReflectiveOperationException {
        Object value = getField(owner, name);
        return value instanceof Boolean bool && bool;
    }

    private static int getTeamOutlineColor(PlayerEntity player) {
        Team team = player.getScoreboardTeam();
        if (team == null) {
            return 0xFFFFFFFF;
        }

        int namedColor = getNamedTeamColor(team.getName());
        if (namedColor != 0) {
            return namedColor;
        }

        Formatting color = team.getColor();
        Integer rgb = color == null ? null : color.getColorValue();
        return rgb == null ? 0xFFFFFFFF : 0xFF000000 | rgb;
    }

    private static int getNamedTeamColor(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.contains("red")) {
            return 0xFFFF5555;
        }
        if (normalized.contains("blue")) {
            return 0xFF5555FF;
        }
        if (normalized.contains("green")) {
            return 0xFF55FF55;
        }
        if (normalized.contains("yellow")) {
            return 0xFFFFFF55;
        }
        return 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean shouldHighlightTeamAsSpectator(MinecraftClient client, PlayerEntity player) {
        return client != null
                && LegionsClient.CONFIG.spectatorGlowEnabled
                && client.player != null
                && player != null
                && player != client.player
                && isSpectatorTeam(client.player)
                && !isSpectatorTeam(player);
    }

    public static boolean isTeammate(PlayerEntity local, PlayerEntity other) {
        if (local == null || other == null) {
            return false;
        }
        if (local == other) {
            return true;
        }
        Team localTeam = local.getScoreboardTeam();
        Team otherTeam = other.getScoreboardTeam();
        return localTeam != null && otherTeam != null && localTeam.getName().equals(otherTeam.getName());
    }

    public static boolean isOpponent(PlayerEntity local, PlayerEntity other) {
        if (local == null || other == null || local == other) {
            return false;
        }
        if (isSpectatorTeam(local) || isSpectatorTeam(other)) {
            return false;
        }
        Team localTeam = local.getScoreboardTeam();
        Team otherTeam = other.getScoreboardTeam();
        return localTeam != null && otherTeam != null && !localTeam.getName().equals(otherTeam.getName());
    }

    public static boolean isSpectatorTeam(PlayerEntity player) {
        if (player == null || player.isSpectator()) {
            return true;
        }
        Team team = player.getScoreboardTeam();
        return team != null && team.getName().toLowerCase(Locale.ROOT).contains("spectator");
    }

    public static String realUsername(PlayerEntity player) {
        return player.getGameProfile().name();
    }

    private static final class TabListTag {
        private final String value;
        private final int numericRating;

        private TabListTag(String value, int numericRating) {
            this.value = value;
            this.numericRating = numericRating;
        }

        private boolean isUnknown() {
            return numericRating < 0;
        }
    }

    private record VisibleOpponent(PlayerEntity player, double distanceSquared) {
    }
}
