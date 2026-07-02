package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InventorySorter {
    public static final int SLOT_COUNT = TpsConfig.InventorySortKit.SLOT_COUNT;

    private static final int[] SLOT_IDS = createSlotIds();
    private static final int SORT_DELAY_TICKS = 0;
    private static final int MAX_SWAPS_PER_TICK = 3;
    private static final int START_COOLDOWN_TICKS = 12;
    private static final int MAX_ACTIVE_STEPS = 200;
    private static final int CACHE_LIMIT = 2048;
    private static final Map<String, ItemStack> DESERIALIZED_STACK_CACHE = new HashMap<>();
    private static final Map<String, String> ITEM_TYPE_KEY_CACHE = new HashMap<>();

    private static String activeKitId;
    private static String activeServer;
    private static int sortDelayTicks;
    private static int startCooldownTicks;
    private static int activeSteps;
    private static long lastMessageMillis;

    private InventorySorter() {
    }

    public static void tick(MinecraftClient client) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (client == null || cfg == null || !cfg.enabled || cfg.inventorySorter == null || !cfg.inventorySorter.enabled) {
            resetActive();
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) {
            resetActive();
            return;
        }
        if (!canUsePlayerInventoryHandler(client) || client.player.playerScreenHandler == null) {
            resetActive();
            return;
        }
        PlayerScreenHandler handler = client.player.playerScreenHandler;
        if (!handler.getCursorStack().isEmpty()) {
            resetActive();
            return;
        }

        String serverAddress = currentServerAddress(client);
        if (serverAddress.isEmpty()) {
            resetActive();
            return;
        }

        if (startCooldownTicks > 0) {
            startCooldownTicks--;
        }
        if (sortDelayTicks > 0) {
            sortDelayTicks--;
            return;
        }

        List<String> current = captureEncodedSlots(handler, client);
        TpsConfig.InventorySortKit kit = findActiveOrMatchingKit(cfg, serverAddress, current, client);
        if (kit == null) {
            resetActive();
            return;
        }

        List<String> afterKeys = itemTypeKeys(kit.afterSlots, client);
        for (int i = 0; i < MAX_SWAPS_PER_TICK; i++) {
            List<String> currentKeys = itemTypeKeys(current, client);
            if (layoutMatches(currentKeys, afterKeys)) {
                resetActive();
                startCooldownTicks = START_COOLDOWN_TICKS;
                return;
            }
            if (++activeSteps > MAX_ACTIVE_STEPS) {
                notify(client, "Inventory sorter paused: layout could not be completed", Formatting.YELLOW);
                resetActive();
                startCooldownTicks = START_COOLDOWN_TICKS * 4;
                return;
            }

            if (!performNextSwap(client, handler, current, currentKeys, afterKeys)) {
                notify(client, "Inventory sorter paused: after kit does not match available items", Formatting.YELLOW);
                resetActive();
                startCooldownTicks = START_COOLDOWN_TICKS * 4;
                return;
            }
        }
        sortDelayTicks = SORT_DELAY_TICKS;
    }

    public static List<String> captureCurrentInventory(MinecraftClient client) {
        if (client == null || client.player == null || client.player.playerScreenHandler == null) {
            return emptySlots();
        }
        return captureEncodedSlots(client.player.playerScreenHandler, client);
    }

    public static ItemStack deserializeStack(String encoded, MinecraftClient client) {
        if (encoded == null || encoded.isBlank()) {
            return ItemStack.EMPTY;
        }
        ItemStack cached = DESERIALIZED_STACK_CACHE.get(encoded);
        if (cached != null) {
            return cached.copy();
        }
        try {
            JsonElement json = JsonParser.parseString(encoded);
            ItemStack stack = ItemStack.CODEC.parse(registryOps(client), json).result().orElse(ItemStack.EMPTY);
            cacheDeserializedStack(encoded, stack);
            return stack.copy();
        } catch (RuntimeException e) {
            cacheDeserializedStack(encoded, ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
    }

    public static String slotLabel(int index) {
        if (index >= 0 && index < 9) {
            return Integer.toString(index + 1);
        }
        if (index >= 9 && index < 36) {
            return Integer.toString(index - 8);
        }
        return switch (index) {
            case 36 -> "H";
            case 37 -> "C";
            case 38 -> "L";
            case 39 -> "B";
            case 40 -> "O";
            default -> "";
        };
    }

    public static int screenSlotId(int index) {
        return index >= 0 && index < SLOT_IDS.length ? SLOT_IDS[index] : -1;
    }

    public static List<String> emptySlots() {
        ArrayList<String> slots = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add("");
        }
        return slots;
    }

    private static TpsConfig.InventorySortKit findActiveOrMatchingKit(TpsConfig cfg, String serverAddress, List<String> current, MinecraftClient client) {
        if (activeKitId != null && activeServer != null && normalizeServer(activeServer).equals(normalizeServer(serverAddress))) {
            for (TpsConfig.InventorySortKit kit : cfg.inventorySorter.kits) {
                if (kit != null && activeKitId.equals(kit.id) && kit.enabled && serverMatches(kit, serverAddress)) {
                    return kit;
                }
            }
            resetActive();
        }
        if (startCooldownTicks > 0 || cfg.inventorySorter.kits == null) {
            return null;
        }
        for (TpsConfig.InventorySortKit kit : cfg.inventorySorter.kits) {
            if (kit == null || !kit.enabled || !serverMatches(kit, serverAddress)) {
                continue;
            }
            if (hasConfiguredSlots(kit.beforeSlots)
                    && hasConfiguredSlots(kit.afterSlots)
                    && layoutMatches(current, kit.beforeSlots, client)
                    && sameItemTypeSet(kit.beforeSlots, kit.afterSlots, client)
                    && !layoutMatches(current, kit.afterSlots, client)) {
                activeKitId = kit.id;
                activeServer = serverAddress;
                activeSteps = 0;
                return kit;
            }
        }
        return null;
    }

    private static boolean performNextSwap(MinecraftClient client, ScreenHandler handler, List<String> current, List<String> currentKeys, List<String> afterKeys) {
        for (int target = 0; target < SLOT_COUNT; target++) {
            String wanted = afterKeys.get(target);
            if (wanted.equals(currentKeys.get(target))) {
                continue;
            }

            int source = findSourceSlot(currentKeys, afterKeys, target, wanted);
            if (source < 0) {
                return false;
            }
            return swapTrackedSlots(client, handler, current, source, target);
        }
        return true;
    }

    private static int findSourceSlot(List<String> currentKeys, List<String> afterKeys, int target, String wanted) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i != target && wanted.equals(currentKeys.get(i)) && !currentKeys.get(i).equals(afterKeys.get(i))) {
                return i;
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i != target && wanted.equals(currentKeys.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean swapTrackedSlots(MinecraftClient client, ScreenHandler handler, List<String> current, int source, int target) {
        int sourceSlot = SLOT_IDS[source];
        int targetSlot = SLOT_IDS[target];
        if (tryHotbarSwap(client, handler, current, source, target)) {
            return true;
        }

        if (!canPlace(handler, source, current.get(target), client) || !canPlace(handler, target, current.get(source), client)) {
            return tryDisplaceThroughEmptySlot(client, handler, current, source, target);
        }

        int temp = findSafeEmptyTempSlot(current, source, target);
        if (temp >= 0 && mayMergeOnDirectSwap(current.get(source), current.get(target), client)) {
            click(client, handler, sourceSlot);
            click(client, handler, SLOT_IDS[temp]);
            click(client, handler, targetSlot);
            click(client, handler, sourceSlot);
            click(client, handler, SLOT_IDS[temp]);
            click(client, handler, targetSlot);
            swapCurrent(current, source, target);
            return true;
        }
        if (temp < 0 && mayMergeOnDirectSwap(current.get(source), current.get(target), client)) {
            return false;
        }

        click(client, handler, sourceSlot);
        click(client, handler, targetSlot);
        click(client, handler, sourceSlot);
        swapCurrent(current, source, target);
        return true;
    }

    private static boolean tryHotbarSwap(MinecraftClient client, ScreenHandler handler, List<String> current, int source, int target) {
        if (source < 9 && canPlace(handler, target, current.get(source), client) && canPlace(handler, source, current.get(target), client)) {
            swapWithHotbar(client, handler, SLOT_IDS[target], source);
            swapCurrent(current, source, target);
            return true;
        }
        if (target < 9 && canPlace(handler, source, current.get(target), client) && canPlace(handler, target, current.get(source), client)) {
            swapWithHotbar(client, handler, SLOT_IDS[source], target);
            swapCurrent(current, source, target);
            return true;
        }
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (hotbar == source || hotbar == target) {
                continue;
            }
            if (canPlace(handler, source, current.get(hotbar), client)
                    && canPlace(handler, source, current.get(target), client)
                    && canPlace(handler, target, current.get(source), client)) {
                swapWithHotbar(client, handler, SLOT_IDS[source], hotbar);
                swapWithHotbar(client, handler, SLOT_IDS[target], hotbar);
                swapWithHotbar(client, handler, SLOT_IDS[source], hotbar);
                swapCurrent(current, source, target);
                return true;
            }
        }
        return false;
    }

    private static boolean tryDisplaceThroughEmptySlot(MinecraftClient client, ScreenHandler handler, List<String> current, int source, int target) {
        if (!canPlace(handler, target, current.get(source), client)) {
            return false;
        }
        int temp = findEmptyTempSlotFor(handler, current, source, target, current.get(target), client);
        if (temp < 0) {
            return false;
        }

        String oldSource = current.get(source);
        String oldTarget = current.get(target);
        click(client, handler, SLOT_IDS[target]);
        click(client, handler, SLOT_IDS[temp]);
        click(client, handler, SLOT_IDS[source]);
        click(client, handler, SLOT_IDS[target]);
        current.set(source, "");
        current.set(target, oldSource);
        current.set(temp, oldTarget);
        return true;
    }

    private static void swapCurrent(List<String> current, int source, int target) {
        String oldSource = current.get(source);
        current.set(source, current.get(target));
        current.set(target, oldSource);
    }

    private static boolean canPlace(ScreenHandler handler, int slot, String encodedStack, MinecraftClient client) {
        if (encodedStack == null || encodedStack.isEmpty()) {
            return true;
        }
        ItemStack stack = deserializeStack(encodedStack, client);
        return stack.isEmpty() || handler.getSlot(SLOT_IDS[slot]).canInsert(stack);
    }

    private static int findSafeEmptyTempSlot(List<String> current, int source, int target) {
        for (int i = 0; i < 36; i++) {
            if (i != source && i != target && current.get(i).isEmpty()) {
                return i;
            }
        }
        if (source != 40 && target != 40 && current.get(40).isEmpty()) {
            return 40;
        }
        return -1;
    }

    private static int findEmptyTempSlotFor(ScreenHandler handler, List<String> current, int source, int target, String stack, MinecraftClient client) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i != source && i != target && current.get(i).isEmpty() && canPlace(handler, i, stack, client)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean mayMergeOnDirectSwap(String source, String target, MinecraftClient client) {
        if (source == null || source.isEmpty() || target == null || target.isEmpty()) {
            return false;
        }
        ItemStack sourceStack = deserializeStack(source, client);
        ItemStack targetStack = deserializeStack(target, client);
        return !sourceStack.isEmpty()
                && !targetStack.isEmpty()
                && sourceStack.getCount() != targetStack.getCount()
                && ItemStack.areItemsAndComponentsEqual(sourceStack, targetStack);
    }

    private static void click(MinecraftClient client, ScreenHandler handler, int slotId) {
        client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }

    private static void swapWithHotbar(MinecraftClient client, ScreenHandler handler, int slotId, int hotbarButton) {
        client.interactionManager.clickSlot(handler.syncId, slotId, hotbarButton, SlotActionType.SWAP, client.player);
    }

    private static List<String> captureEncodedSlots(ScreenHandler handler, MinecraftClient client) {
        ArrayList<String> slots = new ArrayList<>(SLOT_COUNT);
        for (int slotId : SLOT_IDS) {
            ItemStack stack = handler.getSlot(slotId).getStack();
            slots.add(serializeStack(stack, client));
        }
        return slots;
    }

    private static String serializeStack(ItemStack stack, MinecraftClient client) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            return ItemStack.CODEC.encodeStart(registryOps(client), stack)
                    .result()
                    .map(JsonElement::toString)
                    .orElse("");
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static RegistryOps<JsonElement> registryOps(MinecraftClient client) {
        DynamicRegistryManager registryManager = DynamicRegistryManager.EMPTY;
        if (client != null && client.getNetworkHandler() != null) {
            registryManager = client.getNetworkHandler().getRegistryManager();
        } else if (client != null && client.world != null) {
            registryManager = client.world.getRegistryManager();
        }
        return RegistryOps.of(JsonOps.INSTANCE, registryManager);
    }

    private static boolean layoutMatches(List<String> current, List<String> layout, MinecraftClient client) {
        return layoutMatches(itemTypeKeys(current, client), itemTypeKeys(layout, client));
    }

    private static boolean layoutMatches(List<String> currentKeys, List<String> layoutKeys) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!currentKeys.get(i).equals(layoutKeys.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameItemTypeSet(List<String> a, List<String> b, MinecraftClient client) {
        List<String> left = itemTypeKeys(a, client);
        List<String> right = itemTypeKeys(b, client);
        Map<String, Integer> counts = new HashMap<>();
        for (String slot : left) {
            counts.put(slot, counts.getOrDefault(slot, 0) + 1);
        }
        for (String slot : right) {
            Integer count = counts.get(slot);
            if (count == null) {
                return false;
            }
            if (count == 1) {
                counts.remove(slot);
            } else {
                counts.put(slot, count - 1);
            }
        }
        return counts.isEmpty();
    }

    private static List<String> itemTypeKeys(List<String> slots, MinecraftClient client) {
        List<String> normalized = normalizeSlots(slots);
        ArrayList<String> keys = new ArrayList<>(SLOT_COUNT);
        for (String slot : normalized) {
            keys.add(itemTypeKey(slot, client));
        }
        return keys;
    }

    private static String itemTypeKey(String encodedStack, MinecraftClient client) {
        if (encodedStack == null || encodedStack.isEmpty()) {
            return "";
        }
        String cached = ITEM_TYPE_KEY_CACHE.get(encodedStack);
        if (cached != null) {
            return cached;
        }
        ItemStack stack = deserializeStack(encodedStack, client);
        String key = stack.isEmpty() ? "" : Registries.ITEM.getId(stack.getItem()).toString();
        cacheItemTypeKey(encodedStack, key);
        return key;
    }

    private static void cacheDeserializedStack(String encoded, ItemStack stack) {
        if (DESERIALIZED_STACK_CACHE.size() >= CACHE_LIMIT) {
            DESERIALIZED_STACK_CACHE.clear();
        }
        DESERIALIZED_STACK_CACHE.put(encoded, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    private static void cacheItemTypeKey(String encoded, String key) {
        if (ITEM_TYPE_KEY_CACHE.size() >= CACHE_LIMIT) {
            ITEM_TYPE_KEY_CACHE.clear();
        }
        ITEM_TYPE_KEY_CACHE.put(encoded, key == null ? "" : key);
    }

    private static boolean hasConfiguredSlots(List<String> slots) {
        if (slots == null) {
            return false;
        }
        for (String slot : slots) {
            if (slot != null && !slot.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> normalizeSlots(List<String> slots) {
        ArrayList<String> normalized = new ArrayList<>(SLOT_COUNT);
        if (slots != null) {
            for (String slot : slots) {
                normalized.add(slot == null ? "" : slot);
                if (normalized.size() >= SLOT_COUNT) {
                    break;
                }
            }
        }
        while (normalized.size() < SLOT_COUNT) {
            normalized.add("");
        }
        return normalized;
    }

    private static boolean serverMatches(TpsConfig.InventorySortKit kit, String serverAddress) {
        return kit != null && normalizeServer(kit.serverAddress).equals(normalizeServer(serverAddress));
    }

    private static String currentServerAddress(MinecraftClient client) {
        if (client == null || client.isInSingleplayer()) {
            return "";
        }
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            return client.getCurrentServerEntry().address.trim();
        }
        if (client.getNetworkHandler() != null
                && client.getNetworkHandler().getServerInfo() != null
                && client.getNetworkHandler().getServerInfo().address != null) {
            return client.getNetworkHandler().getServerInfo().address.trim();
        }
        return "";
    }

    private static String normalizeServer(String serverAddress) {
        return serverAddress == null ? "" : serverAddress.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean canUsePlayerInventoryHandler(MinecraftClient client) {
        if (client == null || client.player == null || client.player.playerScreenHandler == null) {
            return false;
        }
        if (client.player.currentScreenHandler != client.player.playerScreenHandler) {
            return false;
        }
        return client.currentScreen == null || client.currentScreen instanceof HandledScreen<?>;
    }

    private static void notify(MinecraftClient client, String message, Formatting formatting) {
        long now = System.currentTimeMillis();
        if (client != null && client.player != null && now - lastMessageMillis > 2000L) {
            client.player.sendMessage(Text.literal(message).formatted(formatting), true);
            lastMessageMillis = now;
        }
    }

    private static int[] createSlotIds() {
        int[] slots = new int[SLOT_COUNT];
        for (int i = 0; i < 9; i++) {
            slots[i] = PlayerScreenHandler.HOTBAR_START + i;
        }
        for (int i = 9; i < 36; i++) {
            slots[i] = PlayerScreenHandler.INVENTORY_START + (i - 9);
        }
        slots[36] = PlayerScreenHandler.EQUIPMENT_START;
        slots[37] = PlayerScreenHandler.EQUIPMENT_START + 1;
        slots[38] = PlayerScreenHandler.EQUIPMENT_START + 2;
        slots[39] = PlayerScreenHandler.EQUIPMENT_START + 3;
        slots[40] = PlayerScreenHandler.OFFHAND_ID;
        return slots;
    }

    private static void resetActive() {
        activeKitId = null;
        activeServer = null;
        sortDelayTicks = 0;
        activeSteps = 0;
    }
}
