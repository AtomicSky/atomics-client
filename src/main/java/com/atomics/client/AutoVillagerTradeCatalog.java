package com.atomics.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AutoVillagerTradeCatalog {
    public static final String DEFAULT_PROFESSION_ID = "minecraft:librarian";
    public static final String DEFAULT_TRADE_ID = "enchanted_book";

    private static final List<ProfessionOption> PROFESSIONS = List.of(
            profession("minecraft:armorer", "Armorer",
                    trade("coal_for_emerald", "Coal -> Emerald", "minecraft:emerald", "minecraft:coal"),
                    trade("iron_for_emerald", "Iron -> Emerald", "minecraft:emerald", "minecraft:iron_ingot"),
                    trade("lava_for_emerald", "Lava Bucket -> Emerald", "minecraft:emerald", "minecraft:lava_bucket"),
                    trade("diamond_for_emerald", "Diamond -> Emerald", "minecraft:emerald", "minecraft:diamond"),
                    trade("iron_armor", "Emerald -> Iron Armor", "iron_helmet|iron_chestplate|iron_leggings|iron_boots", "minecraft:emerald"),
                    trade("chainmail_armor", "Emerald -> Chainmail Armor", "chainmail_helmet|chainmail_chestplate|chainmail_leggings|chainmail_boots", "minecraft:emerald"),
                    trade("diamond_armor", "Emerald -> Diamond Armor", "diamond_helmet|diamond_chestplate|diamond_leggings|diamond_boots", "minecraft:emerald"),
                    trade("shield", "Emerald -> Shield", "minecraft:shield", "minecraft:emerald"),
                    trade("bell", "Emerald -> Bell", "minecraft:bell", "minecraft:emerald")
            ),
            profession("minecraft:butcher", "Butcher",
                    trade("chicken_for_emerald", "Raw Chicken -> Emerald", "minecraft:emerald", "minecraft:chicken"),
                    trade("porkchop_for_emerald", "Raw Porkchop -> Emerald", "minecraft:emerald", "minecraft:porkchop"),
                    trade("rabbit_for_emerald", "Raw Rabbit -> Emerald", "minecraft:emerald", "minecraft:rabbit"),
                    trade("mutton_for_emerald", "Raw Mutton -> Emerald", "minecraft:emerald", "minecraft:mutton"),
                    trade("beef_for_emerald", "Raw Beef -> Emerald", "minecraft:emerald", "minecraft:beef"),
                    trade("dried_kelp_for_emerald", "Dried Kelp Block -> Emerald", "minecraft:emerald", "minecraft:dried_kelp_block"),
                    trade("sweet_berries_for_emerald", "Sweet Berries -> Emerald", "minecraft:emerald", "minecraft:sweet_berries"),
                    trade("rabbit_stew", "Emerald -> Rabbit Stew", "minecraft:rabbit_stew", "minecraft:emerald"),
                    trade("cooked_porkchop", "Emerald -> Cooked Porkchop", "minecraft:cooked_porkchop", "minecraft:emerald"),
                    trade("cooked_chicken", "Emerald -> Cooked Chicken", "minecraft:cooked_chicken", "minecraft:emerald")
            ),
            profession("minecraft:cartographer", "Cartographer",
                    trade("paper_for_emerald", "Paper -> Emerald", "minecraft:emerald", "minecraft:paper"),
                    trade("glass_pane_for_emerald", "Glass Pane -> Emerald", "minecraft:emerald", "minecraft:glass_pane"),
                    trade("empty_map", "Emerald -> Empty Map", "minecraft:map", "minecraft:emerald"),
                    trade("ocean_map", "Emerald -> Ocean Map", "filled_map ocean|ocean_explorer_map", "minecraft:emerald"),
                    trade("woodland_map", "Emerald -> Woodland Map", "filled_map woodland|woodland_explorer_map", "minecraft:emerald"),
                    trade("trial_map", "Emerald -> Trial Map", "filled_map trial|trial_explorer_map", "minecraft:emerald"),
                    trade("item_frame", "Emerald -> Item Frame", "minecraft:item_frame", "minecraft:emerald"),
                    trade("banner_pattern", "Emerald -> Banner Pattern", "banner_pattern", "minecraft:emerald")
            ),
            profession("minecraft:cleric", "Cleric",
                    trade("rotten_flesh_for_emerald", "Rotten Flesh -> Emerald", "minecraft:emerald", "minecraft:rotten_flesh"),
                    trade("gold_for_emerald", "Gold Ingot -> Emerald", "minecraft:emerald", "minecraft:gold_ingot"),
                    trade("rabbit_foot_for_emerald", "Rabbit Foot -> Emerald", "minecraft:emerald", "minecraft:rabbit_foot"),
                    trade("scute_for_emerald", "Scute -> Emerald", "minecraft:emerald", "scute"),
                    trade("nether_wart_for_emerald", "Nether Wart -> Emerald", "minecraft:emerald", "minecraft:nether_wart"),
                    trade("redstone", "Emerald -> Redstone", "minecraft:redstone", "minecraft:emerald"),
                    trade("lapis", "Emerald -> Lapis", "minecraft:lapis_lazuli", "minecraft:emerald"),
                    trade("glowstone", "Emerald -> Glowstone", "minecraft:glowstone", "minecraft:emerald"),
                    trade("ender_pearl", "Emerald -> Ender Pearl", "minecraft:ender_pearl", "minecraft:emerald"),
                    trade("bottle_o_enchanting", "Emerald -> XP Bottle", "minecraft:experience_bottle", "minecraft:emerald")
            ),
            profession("minecraft:farmer", "Farmer",
                    trade("wheat_for_emerald", "Wheat -> Emerald", "minecraft:emerald", "minecraft:wheat"),
                    trade("potato_for_emerald", "Potato -> Emerald", "minecraft:emerald", "minecraft:potato"),
                    trade("carrot_for_emerald", "Carrot -> Emerald", "minecraft:emerald", "minecraft:carrot"),
                    trade("beetroot_for_emerald", "Beetroot -> Emerald", "minecraft:emerald", "minecraft:beetroot"),
                    trade("pumpkin_for_emerald", "Pumpkin -> Emerald", "minecraft:emerald", "minecraft:pumpkin"),
                    trade("melon_for_emerald", "Melon -> Emerald", "minecraft:emerald", "minecraft:melon"),
                    trade("bread", "Emerald -> Bread", "minecraft:bread", "minecraft:emerald"),
                    trade("pumpkin_pie", "Emerald -> Pumpkin Pie", "minecraft:pumpkin_pie", "minecraft:emerald"),
                    trade("apple", "Emerald -> Apple", "minecraft:apple", "minecraft:emerald"),
                    trade("cookie", "Emerald -> Cookie", "minecraft:cookie", "minecraft:emerald"),
                    trade("suspicious_stew", "Emerald -> Suspicious Stew", "minecraft:suspicious_stew", "minecraft:emerald"),
                    trade("cake", "Emerald -> Cake", "minecraft:cake", "minecraft:emerald"),
                    trade("golden_carrot", "Emerald -> Golden Carrot", "minecraft:golden_carrot", "minecraft:emerald"),
                    trade("glistering_melon", "Emerald -> Glistering Melon", "minecraft:glistering_melon_slice", "minecraft:emerald")
            ),
            profession("minecraft:fisherman", "Fisherman",
                    trade("coal_for_emerald", "Coal -> Emerald", "minecraft:emerald", "minecraft:coal"),
                    trade("cod_for_emerald", "Raw Cod -> Emerald", "minecraft:emerald", "minecraft:cod"),
                    trade("salmon_for_emerald", "Raw Salmon -> Emerald", "minecraft:emerald", "minecraft:salmon"),
                    trade("string_for_emerald", "String -> Emerald", "minecraft:emerald", "minecraft:string"),
                    trade("pufferfish_for_emerald", "Pufferfish -> Emerald", "minecraft:emerald", "minecraft:pufferfish"),
                    trade("tropical_fish_for_emerald", "Tropical Fish -> Emerald", "minecraft:emerald", "minecraft:tropical_fish"),
                    trade("boat_for_emerald", "Boat -> Emerald", "minecraft:emerald", "boat"),
                    trade("cod_bucket", "Emerald -> Cod Bucket", "minecraft:cod_bucket", "minecraft:emerald"),
                    trade("cooked_cod", "Emerald -> Cooked Cod", "minecraft:cooked_cod", "minecraft:emerald"),
                    trade("campfire", "Emerald -> Campfire", "minecraft:campfire", "minecraft:emerald"),
                    trade("fishing_rod", "Emerald -> Fishing Rod", "fishing_rod", "minecraft:emerald"),
                    trade("cooked_salmon", "Emerald -> Cooked Salmon", "minecraft:cooked_salmon", "minecraft:emerald")
            ),
            profession("minecraft:fletcher", "Fletcher",
                    trade("stick_for_emerald", "Sticks -> Emerald", "minecraft:emerald", "minecraft:stick"),
                    trade("flint_for_emerald", "Flint -> Emerald", "minecraft:emerald", "minecraft:flint"),
                    trade("string_for_emerald", "String -> Emerald", "minecraft:emerald", "minecraft:string"),
                    trade("feather_for_emerald", "Feather -> Emerald", "minecraft:emerald", "minecraft:feather"),
                    trade("tripwire_hook_for_emerald", "Tripwire Hook -> Emerald", "minecraft:emerald", "minecraft:tripwire_hook"),
                    trade("arrows", "Emerald -> Arrows", "minecraft:arrow", "minecraft:emerald"),
                    trade("bow", "Emerald -> Bow", "minecraft:bow", "minecraft:emerald"),
                    trade("crossbow", "Emerald -> Crossbow", "minecraft:crossbow", "minecraft:emerald"),
                    trade("enchanted_bow", "Emerald -> Enchanted Bow", "minecraft:bow", "minecraft:emerald"),
                    trade("enchanted_crossbow", "Emerald -> Enchanted Crossbow", "minecraft:crossbow", "minecraft:emerald"),
                    trade("tipped_arrow", "Emerald -> Tipped Arrows", "minecraft:tipped_arrow", "minecraft:emerald")
            ),
            profession("minecraft:leatherworker", "Leatherworker",
                    trade("leather_for_emerald", "Leather -> Emerald", "minecraft:emerald", "minecraft:leather"),
                    trade("rabbit_hide_for_emerald", "Rabbit Hide -> Emerald", "minecraft:emerald", "minecraft:rabbit_hide"),
                    trade("scute_for_emerald", "Scute -> Emerald", "minecraft:emerald", "scute"),
                    trade("leather_armor", "Emerald -> Leather Armor", "leather_helmet|leather_chestplate|leather_leggings|leather_boots", "minecraft:emerald"),
                    trade("leather_horse_armor", "Emerald -> Horse Armor", "minecraft:leather_horse_armor", "minecraft:emerald"),
                    trade("saddle", "Emerald -> Saddle", "minecraft:saddle", "minecraft:emerald")
            ),
            profession("minecraft:librarian", "Librarian",
                    trade("enchanted_book", "Emerald + Book -> Enchanted Book", "minecraft:enchanted_book", "minecraft:emerald|minecraft:book", true),
                    trade("paper_for_emerald", "Paper -> Emerald", "minecraft:emerald", "minecraft:paper"),
                    trade("book_for_emerald", "Book -> Emerald", "minecraft:emerald", "minecraft:book"),
                    trade("ink_sac_for_emerald", "Ink Sac -> Emerald", "minecraft:emerald", "minecraft:ink_sac"),
                    trade("book_and_quill_for_emerald", "Book & Quill -> Emerald", "minecraft:emerald", "minecraft:writable_book"),
                    trade("bookshelf", "Emerald -> Bookshelf", "minecraft:bookshelf", "minecraft:emerald"),
                    trade("lantern", "Emerald -> Lantern", "minecraft:lantern", "minecraft:emerald"),
                    trade("glass", "Emerald -> Glass", "minecraft:glass", "minecraft:emerald"),
                    trade("clock", "Emerald -> Clock", "minecraft:clock", "minecraft:emerald"),
                    trade("compass", "Emerald -> Compass", "minecraft:compass", "minecraft:emerald"),
                    trade("name_tag", "Emerald -> Name Tag", "minecraft:name_tag", "minecraft:emerald")
            ),
            profession("minecraft:mason", "Mason",
                    trade("clay_for_emerald", "Clay -> Emerald", "minecraft:emerald", "minecraft:clay_ball"),
                    trade("stone_for_emerald", "Stone -> Emerald", "minecraft:emerald", "minecraft:stone"),
                    trade("granite_for_emerald", "Granite -> Emerald", "minecraft:emerald", "minecraft:granite"),
                    trade("diorite_for_emerald", "Diorite -> Emerald", "minecraft:emerald", "minecraft:diorite"),
                    trade("andesite_for_emerald", "Andesite -> Emerald", "minecraft:emerald", "minecraft:andesite"),
                    trade("quartz_for_emerald", "Quartz -> Emerald", "minecraft:emerald", "minecraft:quartz"),
                    trade("bricks", "Emerald -> Bricks", "minecraft:brick", "minecraft:emerald"),
                    trade("chiseled_stone", "Emerald -> Chiseled Stone", "minecraft:chiseled_stone_bricks", "minecraft:emerald"),
                    trade("terracotta", "Emerald -> Terracotta", "terracotta", "minecraft:emerald"),
                    trade("glazed_terracotta", "Emerald -> Glazed Terracotta", "glazed_terracotta", "minecraft:emerald"),
                    trade("quartz_block", "Emerald -> Quartz Block", "minecraft:quartz_block", "minecraft:emerald"),
                    trade("quartz_pillar", "Emerald -> Quartz Pillar", "minecraft:quartz_pillar", "minecraft:emerald")
            ),
            profession("minecraft:shepherd", "Shepherd",
                    trade("wool_for_emerald", "Wool -> Emerald", "minecraft:emerald", "wool"),
                    trade("dye_for_emerald", "Dye -> Emerald", "minecraft:emerald", "dye|black_dye|blue_dye|brown_dye|cyan_dye|gray_dye|green_dye|light_blue_dye|light_gray_dye|lime_dye|magenta_dye|orange_dye|pink_dye|purple_dye|red_dye|white_dye|yellow_dye"),
                    trade("shears", "Emerald -> Shears", "minecraft:shears", "minecraft:emerald"),
                    trade("wool", "Emerald -> Wool", "wool", "minecraft:emerald"),
                    trade("carpet", "Emerald -> Carpet", "carpet", "minecraft:emerald"),
                    trade("bed", "Emerald -> Bed", "bed", "minecraft:emerald"),
                    trade("banner", "Emerald -> Banner", "banner", "minecraft:emerald"),
                    trade("painting", "Emerald -> Painting", "minecraft:painting", "minecraft:emerald")
            ),
            profession("minecraft:toolsmith", "Toolsmith",
                    trade("coal_for_emerald", "Coal -> Emerald", "minecraft:emerald", "minecraft:coal"),
                    trade("iron_for_emerald", "Iron -> Emerald", "minecraft:emerald", "minecraft:iron_ingot"),
                    trade("flint_for_emerald", "Flint -> Emerald", "minecraft:emerald", "minecraft:flint"),
                    trade("diamond_for_emerald", "Diamond -> Emerald", "minecraft:emerald", "minecraft:diamond"),
                    trade("stone_tools", "Emerald -> Stone Tools", "stone_axe|stone_shovel|stone_pickaxe|stone_hoe", "minecraft:emerald"),
                    trade("iron_tools", "Emerald -> Iron Tools", "iron_axe|iron_shovel|iron_pickaxe", "minecraft:emerald"),
                    trade("diamond_tools", "Emerald -> Diamond Tools", "diamond_axe|diamond_shovel|diamond_pickaxe|diamond_hoe", "minecraft:emerald"),
                    trade("bell", "Emerald -> Bell", "minecraft:bell", "minecraft:emerald")
            ),
            profession("minecraft:weaponsmith", "Weaponsmith",
                    trade("coal_for_emerald", "Coal -> Emerald", "minecraft:emerald", "minecraft:coal"),
                    trade("iron_for_emerald", "Iron -> Emerald", "minecraft:emerald", "minecraft:iron_ingot"),
                    trade("flint_for_emerald", "Flint -> Emerald", "minecraft:emerald", "minecraft:flint"),
                    trade("diamond_for_emerald", "Diamond -> Emerald", "minecraft:emerald", "minecraft:diamond"),
                    trade("iron_axe", "Emerald -> Iron Axe", "minecraft:iron_axe", "minecraft:emerald"),
                    trade("iron_sword", "Emerald -> Iron Sword", "minecraft:iron_sword", "minecraft:emerald"),
                    trade("diamond_axe", "Emerald -> Diamond Axe", "minecraft:diamond_axe", "minecraft:emerald"),
                    trade("diamond_sword", "Emerald -> Diamond Sword", "minecraft:diamond_sword", "minecraft:emerald"),
                    trade("bell", "Emerald -> Bell", "minecraft:bell", "minecraft:emerald")
            )
    );

    private AutoVillagerTradeCatalog() {
    }

    public static List<ProfessionOption> professions() {
        return PROFESSIONS;
    }

    public static ProfessionOption profession(String id) {
        String normalized = normalizeProfessionId(id);
        for (ProfessionOption profession : PROFESSIONS) {
            if (profession.id().equals(normalized)) {
                return profession;
            }
        }
        return PROFESSIONS.get(0);
    }

    public static TradeOption trade(String professionId, String tradeId) {
        ProfessionOption profession = profession(professionId);
        String normalized = normalizeTradeId(profession.id(), tradeId);
        for (TradeOption trade : profession.trades()) {
            if (trade.id().equals(normalized)) {
                return trade;
            }
        }
        return profession.trades().get(0);
    }

    public static String normalizeProfessionId(String value) {
        String normalized = normalizeMinecraftId(value);
        for (ProfessionOption profession : PROFESSIONS) {
            if (profession.id().equals(normalized) || normalize(profession.label()).equals(normalize(value))) {
                return profession.id();
            }
        }
        return DEFAULT_PROFESSION_ID;
    }

    public static String normalizeTradeId(String professionId, String value) {
        ProfessionOption profession = profession(professionId);
        String normalized = normalize(value).replace(' ', '_');
        for (TradeOption trade : profession.trades()) {
            if (trade.id().equals(normalized) || normalize(trade.label()).equals(normalize(value))) {
                return trade.id();
            }
        }
        return profession.trades().get(0).id();
    }

    public static boolean requiresEnchantment(String professionId, String tradeId) {
        return trade(professionId, tradeId).requiresEnchantment();
    }

    public static String labelForProfession(String id) {
        return profession(id).label();
    }

    public static String labelForTrade(String professionId, String tradeId) {
        return trade(professionId, tradeId).label();
    }

    private static ProfessionOption profession(String id, String label, TradeOption... trades) {
        return new ProfessionOption(id, label, List.of(trades));
    }

    private static TradeOption trade(String id, String label, String sellTerms, String buyTerms) {
        return trade(id, label, sellTerms, buyTerms, false);
    }

    private static TradeOption trade(String id, String label, String sellTerms, String buyTerms, boolean requiresEnchantment) {
        return new TradeOption(id, label, splitTerms(sellTerms), splitTerms(buyTerms), requiresEnchantment);
    }

    private static List<String> splitTerms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] pieces = value.split("\\|");
        ArrayList<String> terms = new ArrayList<>(pieces.length);
        for (String piece : pieces) {
            String normalized = normalize(piece);
            if (!normalized.isEmpty()) {
                terms.add(normalized);
            }
        }
        return List.copyOf(terms);
    }

    private static String normalizeMinecraftId(String value) {
        String normalized = normalize(value).replace(' ', '_');
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public interface Choice {
        String id();
        String label();
    }

    public record ProfessionOption(String id, String label, List<TradeOption> trades) implements Choice {
    }

    public record TradeOption(String id, String label, List<String> sellTerms, List<String> buyTerms, boolean requiresEnchantment) implements Choice {
    }
}
