package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AutoVillagerTrader {
    private static final int MERCHANT_OUTPUT_SLOT = 2;
    private static final double FIXED_INTERACTION_RANGE = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
    private static final int MAX_TRADES_PER_TICK = 3;
    private static final int MAX_CHEST_QUICK_MOVES_PER_TICK = 6;
    private static final int TRADE_COOLDOWN_TICKS = 2;
    private static final int INTERACT_COOLDOWN_TICKS = 8;
    private static final int CHEST_COOLDOWN_TICKS = 4;
    private static final int PAUSE_COOLDOWN_TICKS = 50;
    private static final int FAILED_VILLAGER_BACKOFF_TICKS = 120;
    private static final int SEARCHED_CHEST_BACKOFF_TICKS = 160;
    private static final int CHEST_SCAN_COOLDOWN_TICKS = 20;
    private static final long SCREEN_SUPPRESS_WINDOW_MILLIS = 1200L;

    private static ItemStack neededFirst = ItemStack.EMPTY;
    private static ItemStack neededSecond = ItemStack.EMPTY;
    private static boolean checkingChest;
    private static int cooldownTicks;
    private static int chestScanCooldownTicks;
    private static int activeVillagerId = -1;
    private static int chestSearchExhaustedVillagerId = -1;
    private static BlockPos activeChestPos;
    private static BlockPos cachedChestOrigin;
    private static double cachedChestRange = -1.0;
    private static int cachedChestIndex;
    private static long suppressScreenUntilMillis;
    private static long lastMessageMillis;
    private static final List<BlockPos> cachedChestCandidates = new ArrayList<>();
    private static final Map<Integer, Integer> villagerBackoffTicks = new HashMap<>();
    private static final Map<BlockPos, Integer> chestBackoffTicks = new HashMap<>();

    private AutoVillagerTrader() {
    }

    public static void tick(MinecraftClient client) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (!canRun(client, cfg)) {
            closeHiddenAutomationHandler(client);
            reset();
            return;
        }

        TpsConfig.UtilitySettings settings = cfg.utility;
        String professionId = AutoVillagerTradeCatalog.normalizeProfessionId(settings.autoVillagerTraderProfession);
        AutoVillagerTradeCatalog.TradeOption tradeOption = AutoVillagerTradeCatalog.trade(professionId, settings.autoVillagerTraderTrade);
        String enchantmentQuery = normalizeQuery(settings.autoVillagerTraderEnchantment);
        if (tradeOption.requiresEnchantment() && enchantmentQuery.isEmpty()) {
            notify(client, "Auto trader needs an enchantment for librarian books", Formatting.YELLOW);
            return;
        }
        tickBackoffs();

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (client.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
            handleMerchant(client, handler, settings, tradeOption, enchantmentQuery);
            return;
        }

        if (checkingChest) {
            tickPaymentChestSearch(client, settings, "Auto trader");
            return;
        }

        if (client.currentScreen != null || client.player.currentScreenHandler != client.player.playerScreenHandler) {
            return;
        }

        VillagerEntity villager = findNearestVillager(client, professionId, FIXED_INTERACTION_RANGE);
        if (villager == null) {
            activeVillagerId = -1;
            return;
        }

        activeVillagerId = villager.getId();
        suppressNextHandledScreen();
        client.interactionManager.interactEntity(client.player, villager, Hand.MAIN_HAND);
        cooldownTicks = INTERACT_COOLDOWN_TICKS;
    }

    public static boolean shouldSuppressScreen(Screen screen) {
        if (screen == null || System.currentTimeMillis() > suppressScreenUntilMillis) {
            return false;
        }
        if (screen instanceof GenericContainerScreen) {
            suppressScreenUntilMillis = 0L;
            return true;
        }
        if (screen instanceof MerchantScreen) {
            suppressScreenUntilMillis = 0L;
            TpsConfig cfg = AtomicsClient.CONFIG;
            return cfg != null
                    && cfg.utility != null
                    && cfg.utility.autoVillagerTraderAutoCloseMerchant;
        }
        return false;
    }

    public static boolean isPaymentChestSearchActive() {
        return checkingChest;
    }

    public static boolean startPaymentChestSearch(MinecraftClient client, ItemStack first, ItemStack second) {
        if (!canUsePaymentChestSearch(client)) {
            return false;
        }

        ItemStack firstCopy = first == null ? ItemStack.EMPTY : first.copy();
        ItemStack secondCopy = second == null ? ItemStack.EMPTY : second.copy();
        if (hasPayment(client.player.getInventory(), firstCopy, secondCopy)) {
            return false;
        }

        neededFirst = firstCopy;
        neededSecond = secondCopy;
        checkingChest = true;
        activeChestPos = null;
        if (client.player.currentScreenHandler != client.player.playerScreenHandler) {
            client.player.closeHandledScreen();
        }
        cooldownTicks = CHEST_COOLDOWN_TICKS;
        return true;
    }

    public static boolean tickPaymentChestSearch(MinecraftClient client, String label) {
        if (!checkingChest) {
            return false;
        }
        if (!canUsePaymentChestSearch(client)) {
            checkingChest = false;
            activeChestPos = null;
            return true;
        }

        tickBackoffs();
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return true;
        }

        tickPaymentChestSearch(client, AtomicsClient.CONFIG == null ? null : AtomicsClient.CONFIG.utility, label);
        return true;
    }

    private static boolean canRun(MinecraftClient client, TpsConfig cfg) {
        if (client == null || cfg == null || !cfg.enabled || cfg.utility == null || !cfg.utility.autoVillagerTraderEnabled) {
            return false;
        }
        return canUsePaymentChestSearch(client);
    }

    private static boolean canUsePaymentChestSearch(MinecraftClient client) {
        if (client == null) {
            return false;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return false;
        }
        return client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL;
    }

    private static void handleMerchant(MinecraftClient client, MerchantScreenHandler handler, TpsConfig.UtilitySettings settings, AutoVillagerTradeCatalog.TradeOption tradeOption, String enchantmentQuery) {
        TradeMatch match = findTrade(handler.getRecipes(), tradeOption, enchantmentQuery);
        if (match == null) {
            stopActiveMerchant(client, handler, settings, "Auto trader could not find that trade");
            return;
        }

        neededFirst = match.offer.getDisplayedFirstBuyItem();
        neededSecond = match.offer.getDisplayedSecondBuyItem();

        if (match.offer.isDisabled()) {
            stopActiveMerchant(client, handler, settings, "Auto trader trade is out of stock");
            return;
        }

        if (!hasPayment(client.player.getInventory(), neededFirst, neededSecond)) {
            if (settings.autoVillagerTraderCheckChests && shouldSearchChestsForCurrentVillager()) {
                startPaymentChestSearch(client, neededFirst, neededSecond);
            } else {
                stopActiveMerchant(client, handler, settings, "Auto trader is missing payment items");
            }
            return;
        }

        chestSearchExhaustedVillagerId = -1;
        int traded = 0;
        for (int attempts = 0; attempts < MAX_TRADES_PER_TICK; attempts++) {
            if (match.offer.isDisabled() || !hasPayment(client.player.getInventory(), neededFirst, neededSecond)) {
                break;
            }
            selectTrade(client, handler, match.index);
            if (handler.getSlot(MERCHANT_OUTPUT_SLOT).getStack().isEmpty()) {
                break;
            }
            client.interactionManager.clickSlot(
                    handler.syncId,
                    MERCHANT_OUTPUT_SLOT,
                    0,
                    SlotActionType.QUICK_MOVE,
                    client.player
            );
            traded++;
        }
        if (traded > 0 && match.offer.isDisabled()) {
            stopActiveMerchant(client, handler, settings, "Auto trader finished trading");
            return;
        }
        if (traded > 0 && !hasPayment(client.player.getInventory(), neededFirst, neededSecond)) {
            if (settings.autoVillagerTraderCheckChests) {
                startPaymentChestSearch(client, neededFirst, neededSecond);
            } else {
                stopActiveMerchant(client, handler, settings, "Auto trader finished trading");
            }
            return;
        }
        if (traded == 0) {
            stopActiveMerchant(client, handler, settings, "Auto trader could not complete the trade");
            return;
        }
        cooldownTicks = TRADE_COOLDOWN_TICKS;
    }

    private static boolean shouldSearchChestsForCurrentVillager() {
        return activeVillagerId == -1 || chestSearchExhaustedVillagerId != activeVillagerId;
    }

    private static void handleContainer(MinecraftClient client, GenericContainerScreenHandler handler, String label) {
        int moved = 0;
        int containerSlots = handler.getRows() * 9;
        int missingFirst = missingCount(client.player.getInventory(), neededFirst);
        int missingSecond = missingCount(client.player.getInventory(), neededSecond);

        for (int slot = 0; slot < containerSlots && moved < MAX_CHEST_QUICK_MOVES_PER_TICK; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) {
                continue;
            }

            if (missingFirst > 0 && stackMatchesPayment(stack, neededFirst)) {
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
                missingFirst -= stack.getCount();
                moved++;
            } else if (missingSecond > 0 && stackMatchesPayment(stack, neededSecond)) {
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
                missingSecond -= stack.getCount();
                moved++;
            }
        }

        if (moved > 0 && !hasPayment(client.player.getInventory(), neededFirst, neededSecond)) {
            cooldownTicks = CHEST_COOLDOWN_TICKS;
            return;
        }

        if (moved == 0 && !hasPayment(client.player.getInventory(), neededFirst, neededSecond)) {
            markActiveChestSearched();
            notify(client, label + " did not find payment items in that chest", Formatting.YELLOW);
            checkingChest = true;
        } else {
            checkingChest = false;
            chestSearchExhaustedVillagerId = -1;
        }
        client.player.closeHandledScreen();
        activeChestPos = null;
        cooldownTicks = checkingChest ? CHEST_COOLDOWN_TICKS : 1;
    }

    private static TradeMatch findTrade(TradeOfferList offers, AutoVillagerTradeCatalog.TradeOption tradeOption, String enchantmentQuery) {
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            if (matchesTrade(offer, tradeOption, enchantmentQuery)) {
                return new TradeMatch(i, offer);
            }
        }
        return null;
    }

    private static boolean matchesTrade(TradeOffer offer, AutoVillagerTradeCatalog.TradeOption tradeOption, String enchantmentQuery) {
        ItemStack sellItem = offer.copySellItem();
        ItemStack firstBuyItem = offer.getDisplayedFirstBuyItem();
        ItemStack secondBuyItem = offer.getDisplayedSecondBuyItem();
        if (!matchesAnyTerm(sellItem, tradeOption.sellTerms())) {
            return false;
        }
        if (!matchesAnyBuyTerm(firstBuyItem, secondBuyItem, tradeOption.buyTerms())) {
            return false;
        }
        return !tradeOption.requiresEnchantment() || stackMatchesQuery(sellItem, enchantmentQuery);
    }

    private static boolean matchesAnyTerm(ItemStack stack, Iterable<String> terms) {
        boolean hasTerms = false;
        for (String term : terms) {
            hasTerms = true;
            if (stackMatchesQuery(stack, term)) {
                return true;
            }
        }
        return !hasTerms;
    }

    private static boolean matchesAnyBuyTerm(ItemStack firstBuyItem, ItemStack secondBuyItem, Iterable<String> terms) {
        boolean hasTerms = false;
        for (String term : terms) {
            hasTerms = true;
            if (stackMatchesQuery(firstBuyItem, term) || stackMatchesQuery(secondBuyItem, term)) {
                return true;
            }
        }
        return !hasTerms;
    }

    private static void selectTrade(MinecraftClient client, MerchantScreenHandler handler, int index) {
        handler.setRecipeIndex(index);
        handler.switchTo(index);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(index));
        }
    }

    private static VillagerEntity findNearestVillager(MinecraftClient client, String professionQuery, double range) {
        Box box = client.player.getBoundingBox().expand(range);
        double maxDistance = range * range;
        VillagerEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (VillagerEntity villager : client.world.getEntitiesByClass(VillagerEntity.class, box, VillagerEntity::isAlive)) {
            double distance = villager.squaredDistanceTo(client.player);
            if (villager.isBaby()
                    || distance > maxDistance
                    || distance >= bestDistance
                    || villagerBackoffTicks.containsKey(villager.getId())
                    || !professionMatches(villager, professionQuery)) {
                continue;
            }
            if (activeVillagerId != -1 && villager.getId() == activeVillagerId) {
                return villager;
            }
            best = villager;
            bestDistance = distance;
        }
        return best;
    }

    private static void tickPaymentChestSearch(MinecraftClient client, TpsConfig.UtilitySettings settings, String label) {
        if (client.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            handleContainer(client, handler, label);
            return;
        }

        if (client.currentScreen != null || client.player.currentScreenHandler != client.player.playerScreenHandler) {
            return;
        }

        if (!openReachableChest(client, FIXED_INTERACTION_RANGE)) {
            if (activeVillagerId != -1 && settings != null && !settings.autoVillagerTraderAutoCloseMerchant) {
                chestSearchExhaustedVillagerId = activeVillagerId;
                cooldownTicks = 1;
            } else if (activeVillagerId != -1) {
                villagerBackoffTicks.put(activeVillagerId, FAILED_VILLAGER_BACKOFF_TICKS / 2);
                activeVillagerId = -1;
                cooldownTicks = PAUSE_COOLDOWN_TICKS;
            } else {
                cooldownTicks = PAUSE_COOLDOWN_TICKS;
            }
            notify(client, label + " found no reachable chest with payment items", Formatting.YELLOW);
            checkingChest = false;
            activeChestPos = null;
        }
    }

    private static boolean professionMatches(VillagerEntity villager, String professionQuery) {
        RegistryEntry<VillagerProfession> profession = villager.getVillagerData().profession();
        String id = profession.getKey()
                .map(key -> key.getValue().toString())
                .orElse(profession.getIdAsString());
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String translated = profession.value().id().getString();
        String haystack = normalizeQuery(id + " " + path + " " + translated);
        return haystack.contains(professionQuery) || haystack.contains(stripMinecraftNamespace(professionQuery));
    }

    private static boolean openReachableChest(MinecraftClient client, double range) {
        BlockPos origin = client.player.getBlockPos();
        if (shouldRebuildChestCache(origin, range)) {
            rebuildChestCache(client, origin, range);
        }

        BlockPos bestPos = nextCachedChest(client, range);
        if (bestPos == null) {
            return false;
        }

        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(bestPos), Direction.UP, bestPos, false);
        activeChestPos = bestPos;
        suppressNextHandledScreen();
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        cooldownTicks = CHEST_COOLDOWN_TICKS;
        return true;
    }

    private static boolean shouldRebuildChestCache(BlockPos origin, double range) {
        if (cachedChestOrigin == null || !cachedChestOrigin.equals(origin) || Math.abs(cachedChestRange - range) > 0.001) {
            return true;
        }
        return cachedChestCandidates.isEmpty() && chestScanCooldownTicks <= 0;
    }

    private static void rebuildChestCache(MinecraftClient client, BlockPos origin, double range) {
        cachedChestCandidates.clear();
        cachedChestOrigin = origin.toImmutable();
        cachedChestRange = range;
        cachedChestIndex = 0;
        chestScanCooldownTicks = CHEST_SCAN_COOLDOWN_TICKS;

        int radius = (int) Math.ceil(range);
        double maxDistance = range * range;
        Vec3d eyePos = client.player.getEyePos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.add(x, y, z).toImmutable();
                    if (eyePos.squaredDistanceTo(Vec3d.ofCenter(pos)) > maxDistance) {
                        continue;
                    }
                    if (isChestLike(client.world.getBlockState(pos))) {
                        cachedChestCandidates.add(pos);
                    }
                }
            }
        }
        cachedChestCandidates.sort(Comparator.comparingDouble(pos -> eyePos.squaredDistanceTo(Vec3d.ofCenter(pos))));
    }

    private static BlockPos nextCachedChest(MinecraftClient client, double range) {
        if (cachedChestCandidates.isEmpty()) {
            return null;
        }

        double maxDistance = range * range;
        Vec3d eyePos = client.player.getEyePos();
        int checked = 0;
        while (checked < cachedChestCandidates.size()) {
            if (cachedChestIndex >= cachedChestCandidates.size()) {
                cachedChestIndex = 0;
            }
            BlockPos pos = cachedChestCandidates.get(cachedChestIndex++);
            checked++;
            if (chestBackoffTicks.containsKey(pos)
                    || eyePos.squaredDistanceTo(Vec3d.ofCenter(pos)) > maxDistance
                    || !isChestLike(client.world.getBlockState(pos))
                    || !hasLineOfSightToBlock(client, pos)) {
                continue;
            }
            return pos;
        }
        return null;
    }

    private static boolean isChestLike(BlockState state) {
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST);
    }

    private static boolean hasLineOfSightToBlock(MinecraftClient client, BlockPos pos) {
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                client.player.getEyePos(),
                Vec3d.ofCenter(pos),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    private static boolean hasPayment(PlayerInventory inventory, ItemStack first, ItemStack second) {
        return missingCount(inventory, first) == 0 && missingCount(inventory, second) == 0;
    }

    private static int missingCount(PlayerInventory inventory, ItemStack required) {
        if (required == null || required.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stackMatchesPayment(stack, required)) {
                count += stack.getCount();
            }
        }
        return Math.max(0, required.getCount() - count);
    }

    private static boolean stackMatchesPayment(ItemStack stack, ItemStack required) {
        return stack != null
                && required != null
                && !stack.isEmpty()
                && !required.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(stack, required);
    }

    private static boolean stackMatchesQuery(ItemStack stack, String tradeQuery) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String id = Registries.ITEM.getId(stack.getItem()).toString();
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String haystack = normalizeQuery(id + " " + path.replace('_', ' ') + " " + stack.getName().getString() + " " + stack.getComponents());
        String strippedQuery = stripMinecraftNamespace(tradeQuery);
        return haystack.contains(tradeQuery) || (!strippedQuery.isEmpty() && haystack.contains(strippedQuery));
    }

    private static String normalizeQuery(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripMinecraftNamespace(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    private static void notify(MinecraftClient client, String message, Formatting formatting) {
        long now = System.currentTimeMillis();
        if (client != null && client.player != null && now - lastMessageMillis > 2000L) {
            client.player.sendMessage(Text.literal(message).formatted(formatting), true);
            lastMessageMillis = now;
        }
    }

    private static void stopActiveMerchant(MinecraftClient client, MerchantScreenHandler handler, TpsConfig.UtilitySettings settings, String message) {
        if (activeVillagerId != -1) {
            villagerBackoffTicks.put(activeVillagerId, FAILED_VILLAGER_BACKOFF_TICKS);
        }
        if (settings.autoVillagerTraderAutoCloseMerchant && client.player != null) {
            activeVillagerId = -1;
            client.player.closeHandledScreen();
        }
        notify(client, message, Formatting.YELLOW);
        cooldownTicks = settings.autoVillagerTraderAutoCloseMerchant ? INTERACT_COOLDOWN_TICKS : PAUSE_COOLDOWN_TICKS;
    }

    private static void markActiveChestSearched() {
        if (activeChestPos != null) {
            chestBackoffTicks.put(activeChestPos, SEARCHED_CHEST_BACKOFF_TICKS);
        }
    }

    private static void suppressNextHandledScreen() {
        suppressScreenUntilMillis = System.currentTimeMillis() + SCREEN_SUPPRESS_WINDOW_MILLIS;
    }

    private static void tickBackoffs() {
        tickBackoffMap(villagerBackoffTicks);
        tickBackoffMap(chestBackoffTicks);
        if (chestScanCooldownTicks > 0) {
            chestScanCooldownTicks--;
        }
    }

    private static <T> void tickBackoffMap(Map<T, Integer> backoffs) {
        Iterator<Map.Entry<T, Integer>> iterator = backoffs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<T, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private static void closeHiddenAutomationHandler(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }
        boolean ownedByAutomation = checkingChest
                || activeVillagerId != -1
                || activeChestPos != null
                || suppressScreenUntilMillis > 0L;
        if (ownedByAutomation && client.currentScreen == null && client.player.currentScreenHandler != client.player.playerScreenHandler) {
            client.player.closeHandledScreen();
        }
    }

    private static void reset() {
        neededFirst = ItemStack.EMPTY;
        neededSecond = ItemStack.EMPTY;
        checkingChest = false;
        cooldownTicks = 0;
        chestScanCooldownTicks = 0;
        activeVillagerId = -1;
        chestSearchExhaustedVillagerId = -1;
        activeChestPos = null;
        cachedChestOrigin = null;
        cachedChestRange = -1.0;
        cachedChestIndex = 0;
        suppressScreenUntilMillis = 0L;
        cachedChestCandidates.clear();
        villagerBackoffTicks.clear();
        chestBackoffTicks.clear();
    }

    private record TradeMatch(int index, TradeOffer offer) {
    }
}
