package com.atomics.client;

import com.atomics.client.config.TpsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameMode;

import java.util.UUID;

public final class VillagerProfessionLeveler {
    private static final int MERCHANT_OUTPUT_SLOT = 2;
    private static final int MAX_TRADES_PER_TICK = 3;
    private static final int TRADE_COOLDOWN_TICKS = 2;
    private static final double TARGET_SEARCH_RANGE = TpsConfig.DEFAULT_AUTO_VILLAGER_TRADER_RANGE;
    private static final int MAX_VILLAGER_LEVEL = 5;
    private static final int HIGHLIGHT_COLOR = 0xFFFFD54A;
    private static final long MERCHANT_INTERACTION_WINDOW_MILLIS = 3000L;

    private static int cooldownTicks;
    private static int activeMerchantSyncId = -1;
    private static UUID lastInteractedVillagerUuid;
    private static UUID activeMerchantVillagerUuid;
    private static long lastMerchantInteractionMillis;
    private static long lastMessageMillis;

    private VillagerProfessionLeveler() {
    }

    public static void markLookedAtVillager(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        Entity entity = null;
        if (client.crosshairTarget instanceof EntityHitResult hitResult) {
            entity = hitResult.getEntity();
        }
        if (!(entity instanceof VillagerEntity) && client.targetedEntity instanceof VillagerEntity targetedVillager) {
            entity = targetedVillager;
        }

        if (!(entity instanceof VillagerEntity villager) || !villager.isAlive()) {
            notify(client, "Look at an adult villager to mark it for leveling", Formatting.YELLOW);
            return;
        }

        if (AtomicsClient.CONFIG == null) {
            AtomicsClient.CONFIG = new TpsConfig();
        }
        AtomicsClient.CONFIG.normalize();
        if (villager.getUuidAsString().equals(AtomicsClient.CONFIG.utility.villagerLevelerTargetUuid)) {
            AtomicsClient.CONFIG.utility.villagerLevelerTargetUuid = TpsConfig.DEFAULT_VILLAGER_LEVELER_TARGET_UUID;
            AtomicsClient.saveConfigQuietly();
            notifyNow(client, "Unmarked villager leveler", Formatting.YELLOW);
            return;
        }

        if (villager.isBaby()) {
            notify(client, "Look at an adult villager to mark it for leveling", Formatting.YELLOW);
            return;
        }
        String professionId = professionId(villager);
        if (!isLevelableProfession(professionId)) {
            notify(client, "Look at a villager with a profession to mark it for leveling", Formatting.YELLOW);
            return;
        }

        AtomicsClient.CONFIG.utility.villagerLevelerTargetUuid = villager.getUuidAsString();
        AtomicsClient.saveConfigQuietly();
        notifyNow(client, "Marked villager leveler: " + professionLabel(villager), Formatting.GREEN);
    }

    public static boolean tick(MinecraftClient client) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        if (!canRun(client, cfg)) {
            resetActiveMerchant();
            return false;
        }
        if (AutoVillagerTrader.isPaymentChestSearchActive()) {
            AutoVillagerTrader.tickPaymentChestSearch(client, "Villager leveler");
            return true;
        }
        if (!(client.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            resetActiveMerchant();
            return false;
        }

        VillagerEntity target = findTargetVillager(client, cfg.utility.villagerLevelerTargetUuid);
        if (target == null) {
            return false;
        }
        if (!isTargetMerchantScreen(handler, target)) {
            return false;
        }

        if (target.getVillagerData().level() >= MAX_VILLAGER_LEVEL) {
            notify(client, "Villager leveler target is already max level", Formatting.YELLOW);
            return true;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return true;
        }

        handleMerchant(client, handler, cfg.utility);
        return true;
    }

    public static boolean isMarkedLevelerTarget(VillagerEntity villager) {
        TpsConfig cfg = AtomicsClient.CONFIG;
        return villager != null
                && cfg != null
                && cfg.enabled
                && cfg.utility != null
                && villager.getUuidAsString().equals(cfg.utility.villagerLevelerTargetUuid);
    }

    public static int highlightColor() {
        return HIGHLIGHT_COLOR;
    }

    public static void rememberInteractedVillager(Entity entity) {
        if (entity instanceof VillagerEntity villager) {
            lastInteractedVillagerUuid = villager.getUuid();
            lastMerchantInteractionMillis = System.currentTimeMillis();
        }
    }

    private static boolean canRun(MinecraftClient client, TpsConfig cfg) {
        if (client == null || cfg == null || !cfg.enabled || cfg.utility == null) {
            return false;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return false;
        }
        if (cfg.utility.villagerLevelerTargetUuid == null || cfg.utility.villagerLevelerTargetUuid.isBlank()) {
            return false;
        }
        return client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL;
    }

    private static boolean isTargetMerchantScreen(MerchantScreenHandler handler, VillagerEntity target) {
        if (handler.syncId != activeMerchantSyncId) {
            activeMerchantSyncId = handler.syncId;
            activeMerchantVillagerUuid = recentInteractedVillagerUuid();
        }
        return activeMerchantVillagerUuid != null && activeMerchantVillagerUuid.equals(target.getUuid());
    }

    private static UUID recentInteractedVillagerUuid() {
        long elapsed = System.currentTimeMillis() - lastMerchantInteractionMillis;
        return elapsed <= MERCHANT_INTERACTION_WINDOW_MILLIS ? lastInteractedVillagerUuid : null;
    }

    private static void resetActiveMerchant() {
        cooldownTicks = 0;
        activeMerchantSyncId = -1;
        activeMerchantVillagerUuid = null;
    }

    private static void handleMerchant(MinecraftClient client, MerchantScreenHandler handler, TpsConfig.UtilitySettings settings) {
        int traded = 0;
        for (int attempts = 0; attempts < MAX_TRADES_PER_TICK; attempts++) {
            TradeChoice choice = findBestTrade(handler.getRecipes(), client.player.getInventory(), true);
            if (choice == null) {
                if (traded == 0) {
                    if (settings.autoVillagerTraderCheckChests && startChestSearchForBestTrade(client, handler)) {
                        cooldownTicks = 1;
                        return;
                    }
                    notify(client, "Villager leveler has no affordable leveling trades", Formatting.YELLOW);
                }
                break;
            }

            selectTrade(client, handler, choice.index());
            if (handler.getSlot(MERCHANT_OUTPUT_SLOT).getStack().isEmpty()) {
                break;
            }

            if (settings.villagerLevelerAutoDropItems) {
                client.interactionManager.clickSlot(
                        handler.syncId,
                        MERCHANT_OUTPUT_SLOT,
                        1,
                        SlotActionType.THROW,
                        client.player
                );
            } else {
                client.interactionManager.clickSlot(
                        handler.syncId,
                        MERCHANT_OUTPUT_SLOT,
                        0,
                        SlotActionType.QUICK_MOVE,
                        client.player
                );
            }
            traded++;
        }

        cooldownTicks = traded > 0 ? TRADE_COOLDOWN_TICKS : 1;
    }

    private static boolean startChestSearchForBestTrade(MinecraftClient client, MerchantScreenHandler handler) {
        TradeChoice choice = findBestTrade(handler.getRecipes(), client.player.getInventory(), false);
        if (choice == null) {
            return false;
        }

        TradeOffer offer = handler.getRecipes().get(choice.index());
        return AutoVillagerTrader.startPaymentChestSearch(
                client,
                offer.getDisplayedFirstBuyItem(),
                offer.getDisplayedSecondBuyItem()
        );
    }

    private static TradeChoice findBestTrade(TradeOfferList offers, PlayerInventory inventory, boolean requirePayment) {
        TradeChoice best = null;
        for (int index = 0; index < offers.size(); index++) {
            TradeOffer offer = offers.get(index);
            int xp = offer == null ? 0 : offer.getMerchantExperience();
            if (offer == null || offer.isDisabled() || xp <= 0) {
                continue;
            }

            ItemStack first = offer.getDisplayedFirstBuyItem();
            ItemStack second = offer.getDisplayedSecondBuyItem();
            if (requirePayment && !hasPayment(inventory, first, second)) {
                continue;
            }

            TradeChoice candidate = new TradeChoice(
                    index,
                    xp,
                    emeraldCost(first) + emeraldCost(second),
                    paymentItemCount(first) + paymentItemCount(second)
            );
            if (best == null || candidate.isBetterThan(best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static int emeraldCost(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.EMERALD) ? stack.getCount() : 0;
    }

    private static int paymentItemCount(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : stack.getCount();
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

    private static void selectTrade(MinecraftClient client, MerchantScreenHandler handler, int index) {
        handler.setRecipeIndex(index);
        handler.switchTo(index);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(index));
        }
    }

    private static VillagerEntity findTargetVillager(MinecraftClient client, String uuidText) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Box box = client.player.getBoundingBox().expand(TARGET_SEARCH_RANGE);
        double maxDistance = TARGET_SEARCH_RANGE * TARGET_SEARCH_RANGE;
        for (VillagerEntity villager : client.world.getEntitiesByClass(VillagerEntity.class, box, VillagerEntity::isAlive)) {
            if (!villager.isBaby()
                    && villager.getUuid().equals(uuid)
                    && villager.squaredDistanceTo(client.player) <= maxDistance) {
                return villager;
            }
        }
        return null;
    }

    private static String professionLabel(VillagerEntity villager) {
        String id = professionId(villager);
        for (AutoVillagerTradeCatalog.ProfessionOption profession : AutoVillagerTradeCatalog.professions()) {
            if (profession.id().equals(id)) {
                return profession.label();
            }
        }
        return titleCase(stripNamespace(id).replace('_', ' '));
    }

    private static String professionId(VillagerEntity villager) {
        RegistryEntry<VillagerProfession> profession = villager.getVillagerData().profession();
        return profession.getKey()
                .map(key -> key.getValue().toString())
                .orElse(profession.getIdAsString());
    }

    private static boolean isLevelableProfession(String id) {
        String path = stripNamespace(id);
        return !path.isBlank() && !"none".equals(path) && !"nitwit".equals(path);
    }

    private static String stripNamespace(String id) {
        if (id == null) {
            return "";
        }
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "Villager";
        }
        StringBuilder builder = new StringBuilder(value.length());
        boolean upperNext = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                builder.append(c);
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static void notify(MinecraftClient client, String message, Formatting formatting) {
        long now = System.currentTimeMillis();
        if (client != null && client.player != null && now - lastMessageMillis > 2000L) {
            notifyNow(client, message, formatting);
        }
    }

    private static void notifyNow(MinecraftClient client, String message, Formatting formatting) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message).formatted(formatting), true);
            lastMessageMillis = System.currentTimeMillis();
        }
    }

    private record TradeChoice(int index, int xp, int emeraldCost, int paymentItems) {
        private boolean isBetterThan(TradeChoice other) {
            long thisCost = (long) emeraldCost * other.xp;
            long otherCost = (long) other.emeraldCost * xp;
            if (thisCost != otherCost) {
                return thisCost < otherCost;
            }
            if (xp != other.xp) {
                return xp > other.xp;
            }
            if (paymentItems != other.paymentItems) {
                return paymentItems < other.paymentItems;
            }
            return index < other.index;
        }
    }
}
