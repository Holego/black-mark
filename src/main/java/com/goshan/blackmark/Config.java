package com.goshan.blackmark;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Every number the mark obeys. Server-authoritative (COMMON type).
 */
public final class Config {

    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    // --- affliction: when and to whom the mark appears --------------------
    public static final ForgeConfigSpec.IntValue CHECK_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BASE_CHANCE;
    public static final ForgeConfigSpec.IntValue MAX_MARKED_PLAYERS;
    public static final ForgeConfigSpec.IntValue MIN_ONLINE_PLAYERS;
    public static final ForgeConfigSpec.DoubleValue COASTAL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue NIGHT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STORM_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue RAID_DAY_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue RAID_DAY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SECOND_CURSE_CHANCE;
    public static final ForgeConfigSpec.BooleanValue BROADCAST_TO_SERVER;
    public static final ForgeConfigSpec.BooleanValue NAME_THE_BEARER;

    // --- the mark itself --------------------------------------------------
    public static final ForgeConfigSpec.IntValue FOG_TICKS_MIN;
    public static final ForgeConfigSpec.IntValue FOG_TICKS_MAX;
    public static final ForgeConfigSpec.IntValue FLESH_TICKS_MIN;
    public static final ForgeConfigSpec.IntValue FLESH_TICKS_MAX;
    public static final ForgeConfigSpec.IntValue MIN_CARRY_MINUTES;

    // --- the wake: the only way out ---------------------------------------
    public static final ForgeConfigSpec.BooleanValue WAKE_REQUIRE_NIGHT;
    public static final ForgeConfigSpec.BooleanValue WAKE_REQUIRE_COAST;
    public static final ForgeConfigSpec.IntValue WAKE_NEARBY_PLAYERS;
    public static final ForgeConfigSpec.ConfigValue<String> WAKE_TRIBUTE_ITEM;

    // --- curse: slot devourer ---------------------------------------------
    public static final ForgeConfigSpec.IntValue SLOTS_GROWTH_MINUTES;
    public static final ForgeConfigSpec.IntValue SLOTS_MAX_DEVOURED;
    public static final ForgeConfigSpec.IntValue SLOTS_PER_DEFIANCE;

    // --- curse: dementia --------------------------------------------------
    public static final ForgeConfigSpec.IntValue DEMENTIA_GROWTH_MINUTES;
    public static final ForgeConfigSpec.IntValue DEMENTIA_MAX_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue DEMENTIA_SCRAMBLE_CHANCE;
    public static final ForgeConfigSpec.IntValue DEMENTIA_SCRAMBLE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DEMENTIA_CHAT_GARBLE_CHANCE;

    // --- curse: blight ----------------------------------------------------
    public static final ForgeConfigSpec.IntValue BLIGHT_ROT_MINUTES;
    public static final ForgeConfigSpec.BooleanValue BLIGHT_BLOCK_REGEN;
    public static final ForgeConfigSpec.DoubleValue BLIGHT_SATURATION_PENALTY;

    // --- curse: eternal wounds --------------------------------------------
    public static final ForgeConfigSpec.DoubleValue WOUNDS_DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.IntValue WOUNDS_MAX;

    // --- curse: the dead man's tongue -------------------------------------
    public static final ForgeConfigSpec.IntValue PIRATE_SHOUT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue PIRATE_SHOUT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue PIRATE_SHOUT_ON_SEND;
    public static final ForgeConfigSpec.IntValue PIRATE_STRIKE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue PIRATE_STRIKE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue PIRATE_STRIKE_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue PIRATE_STRIKE_PLAYERS;
    public static final ForgeConfigSpec.IntValue PIRATE_GOLD_SCAN_SECONDS;
    public static final ForgeConfigSpec.IntValue PIRATE_GOLD_RADIUS;
    public static final ForgeConfigSpec.IntValue PIRATE_FRENZY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue PIRATE_FRENZY_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue PIRATE_HOARD_GOLD;

    public static final ForgeConfigSpec SPEC;

    static {
        B.comment("When and to whom the mark appears.").push("affliction");
        CHECK_INTERVAL_SECONDS = B
                .comment("How often the server rolls for a new bearer, in seconds.")
                .defineInRange("checkIntervalSeconds", 300, 10, 72000);
        BASE_CHANCE = B
                .comment("Chance per online player, per check, before any multiplier.",
                        "0.004 with the default 300s interval is roughly one bearer per 20 hours of playtime.")
                .defineInRange("baseChance", 0.004D, 0.0D, 1.0D);
        MAX_MARKED_PLAYERS = B
                .comment("How many players may carry a mark at the same time.")
                .defineInRange("maxMarkedPlayers", 1, 1, 100);
        MIN_ONLINE_PLAYERS = B
                .comment("The mark ignores an empty world. Minimum players online before it rolls at all.")
                .defineInRange("minOnlinePlayers", 1, 1, 200);
        COASTAL_MULTIPLIER = B
                .comment("Multiplier while the player stands in a beach or ocean biome.")
                .defineInRange("coastalMultiplier", 6.0D, 0.0D, 1000.0D);
        NIGHT_MULTIPLIER = B
                .comment("Multiplier at night.")
                .defineInRange("nightMultiplier", 2.0D, 0.0D, 1000.0D);
        STORM_MULTIPLIER = B
                .comment("Multiplier during a thunderstorm.")
                .defineInRange("stormMultiplier", 2.5D, 0.0D, 1000.0D);
        RAID_DAY_INTERVAL = B
                .comment("Every Nth in-game day is a raid day, the anniversary of a sacking. Set 0 to disable.")
                .defineInRange("raidDayInterval", 13, 0, 10000);
        RAID_DAY_MULTIPLIER = B
                .comment("Multiplier on a raid day.")
                .defineInRange("raidDayMultiplier", 8.0D, 0.0D, 1000.0D);
        SECOND_CURSE_CHANCE = B
                .comment("Chance the mark brings a second curse along with the first.")
                .defineInRange("secondCurseChance", 0.06D, 0.0D, 1.0D);
        BROADCAST_TO_SERVER = B
                .comment("Announce to the whole server when someone is marked or freed.")
                .define("broadcastToServer", true);
        NAME_THE_BEARER = B
                .comment("Include the bearer's name in the broadcast. Off keeps it a rumour.")
                .define("nameTheBearer", false);
        B.pop();

        B.comment("Fog and flesh: how the mark behaves in the inventory.").push("mark");
        FOG_TICKS_MIN = B.defineInRange("fogTicksMin", 200, 20, 100000);
        FOG_TICKS_MAX = B.defineInRange("fogTicksMax", 700, 20, 100000);
        FLESH_TICKS_MIN = B.defineInRange("fleshTicksMin", 60, 20, 100000);
        FLESH_TICKS_MAX = B.defineInRange("fleshTicksMax", 200, 20, 100000);
        MIN_CARRY_MINUTES = B
                .comment("The mark refuses the wake until it has been carried this long, in real minutes.")
                .defineInRange("minCarryMinutes", 25, 0, 100000);
        B.pop();

        B.comment("The wake: the rite that satisfies the last will.").push("wake");
        WAKE_REQUIRE_NIGHT = B.define("requireNight", true);
        WAKE_REQUIRE_COAST = B.define("requireCoastalBiome", true);
        WAKE_NEARBY_PLAYERS = B
                .comment("Players required within 12 blocks, the bearer included. 2 makes it a real gathering.")
                .defineInRange("nearbyPlayers", 1, 1, 20);
        WAKE_TRIBUTE_ITEM = B
                .comment("Held in the off hand and consumed by the rite.")
                .define("tributeItem", "minecraft:gold_ingot");
        B.pop();

        B.push("curses");

        B.comment("The mark sticks to the inventory and eats the slots around it.").push("slot_devourer");
        SLOTS_GROWTH_MINUTES = B.defineInRange("growthMinutes", 8, 1, 10000);
        SLOTS_MAX_DEVOURED = B.defineInRange("maxDevoured", 12, 1, 35);
        SLOTS_PER_DEFIANCE = B
                .comment("Extra slots eaten each time the bearer tries to be rid of the mark.")
                .defineInRange("perDefiance", 1, 0, 10);
        B.pop();

        B.comment("Feeble-mindedness. The world stops making sense to the bearer.").push("dementia");
        DEMENTIA_GROWTH_MINUTES = B.defineInRange("growthMinutes", 10, 1, 10000);
        DEMENTIA_MAX_INTENSITY = B.defineInRange("maxIntensity", 5, 1, 10);
        DEMENTIA_SCRAMBLE_CHANCE = B
                .comment("Chance per intensity step, per check, that two hotbar slots swap places.",
                        "The bearer reaches for a sword and comes up holding a torch.")
                .defineInRange("scrambleChance", 0.12D, 0.0D, 1.0D);
        DEMENTIA_SCRAMBLE_SECONDS = B
                .comment("Seconds between scramble checks.")
                .defineInRange("scrambleCheckSeconds", 45, 5, 10000);
        DEMENTIA_CHAT_GARBLE_CHANCE = B
                .comment("Chance per character that a typed letter comes out wrong at full intensity.")
                .defineInRange("chatGarbleChance", 0.5D, 0.0D, 1.0D);
        B.pop();

        B.comment("Sickness. Food turns, and flesh will not knit.").push("blight");
        BLIGHT_ROT_MINUTES = B.defineInRange("rotMinutes", 3, 1, 10000);
        BLIGHT_BLOCK_REGEN = B
                .comment("Block natural regeneration. Potions, golden apples and beacons still work.")
                .define("blockNaturalRegen", true);
        BLIGHT_SATURATION_PENALTY = B
                .comment("Fraction of saturation stripped from every meal.")
                .defineInRange("saturationPenalty", 0.5D, 0.0D, 1.0D);
        B.pop();

        B.comment("Everlasting wounds. Hearts taken and not given back.").push("eternal_wounds");
        WOUNDS_DAMAGE_THRESHOLD = B
                .comment("A single hit of at least this much damage opens a wound.")
                .defineInRange("damageThreshold", 4.0D, 0.5D, 100.0D);
        WOUNDS_MAX = B
                .comment("Maximum half-hearts the wounds may take.")
                .defineInRange("maxWounds", 8, 1, 18);
        B.pop();

        B.comment("The dead man's tongue. The pirate starts showing through the bearer.").push("pirate");
        PIRATE_SHOUT_SECONDS = B
                .comment("Seconds between checks for an involuntary bellow into chat.")
                .defineInRange("shoutCheckSeconds", 90, 5, 10000);
        PIRATE_SHOUT_CHANCE = B
                .comment("Chance per check that the pirate shouts on his own.")
                .defineInRange("shoutChance", 0.5D, 0.0D, 1.0D);
        PIRATE_SHOUT_ON_SEND = B
                .comment("Chance that a message the bearer actually typed comes out as a bellow, in capitals.")
                .defineInRange("shoutOnSendChance", 0.25D, 0.0D, 1.0D);
        PIRATE_STRIKE_SECONDS = B
                .comment("Seconds between checks for swinging at whoever is standing too close.")
                .defineInRange("strikeCheckSeconds", 45, 5, 10000);
        PIRATE_STRIKE_CHANCE = B
                .comment("Chance per check that the bearer strikes a nearby ally.")
                .defineInRange("strikeChance", 0.15D, 0.0D, 1.0D);
        PIRATE_STRIKE_DAMAGE = B
                .comment("Damage of that backhand. 2.0 is one heart.")
                .defineInRange("strikeDamage", 2.0D, 0.0D, 100.0D);
        PIRATE_STRIKE_PLAYERS = B
                .comment("Allow striking other players. Tamed animals, villagers and golems are always fair game.",
                        "With PvP disabled on the server, vanilla refuses the hit anyway.")
                .define("strikePlayers", true);
        PIRATE_GOLD_SCAN_SECONDS = B
                .comment("Seconds between looking around for gold.")
                .defineInRange("goldScanSeconds", 3, 1, 600);
        PIRATE_GOLD_RADIUS = B
                .comment("How far the bearer notices gold in the walls. Raising this costs server time.")
                .defineInRange("goldScanRadius", 5, 0, 16);
        PIRATE_FRENZY_SECONDS = B
                .comment("How long the gold frenzy lasts once it takes hold.")
                .defineInRange("frenzySeconds", 20, 1, 10000);
        PIRATE_FRENZY_MULTIPLIER = B
                .comment("Shouting and striking are this much likelier during the frenzy.")
                .defineInRange("frenzyMultiplier", 4.0D, 1.0D, 100.0D);
        PIRATE_HOARD_GOLD = B
                .comment("During the frenzy the bearer cannot bring themselves to drop gold.")
                .define("hoardGold", true);
        B.pop();

        B.pop();

        SPEC = B.build();
    }

    private Config() {
    }
}
