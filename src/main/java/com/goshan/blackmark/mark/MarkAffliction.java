package com.goshan.blackmark.mark;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.CurseRegistry;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * The mark has no reason. It was noticed on coastal ground, on the days the pirates sacked the towns,
 * saluting the dead - but it has taken people tied to nothing at all.
 * <p>
 * This is the whole of that: a weighted coin flip, biased by where you stand and what day it is.
 */
public final class MarkAffliction {

    private static int cooldown = 0;

    public static void serverTick(MinecraftServer server) {
        if (--cooldown > 0) {
            return;
        }
        cooldown = Config.CHECK_INTERVAL_SECONDS.get() * 20;

        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        if (online.size() < Config.MIN_ONLINE_PLAYERS.get()) {
            return;
        }

        int alreadyMarked = 0;
        List<ServerPlayer> candidates = new ArrayList<>();

        for (ServerPlayer player : online) {
            MarkData data = MarkCapability.of(player).orElse(null);
            if (data == null) {
                continue;
            }
            if (data.isMarked()) {
                alreadyMarked++;
            } else if (!player.isSpectator() && !player.isCreative()) {
                candidates.add(player);
            }
        }

        if (alreadyMarked >= Config.MAX_MARKED_PLAYERS.get() || candidates.isEmpty()) {
            return;
        }

        RandomSource random = server.overworld().getRandom();
        shuffle(candidates, random);

        for (ServerPlayer player : candidates) {
            double chance = Config.BASE_CHANCE.get() * omenMultiplier(player);
            if (random.nextDouble() < chance) {
                afflict(player, random);
                return;
            }
        }
    }

    /** Rolls the curses and hands the player to the mark. Also used by the admin command. */
    public static void afflict(ServerPlayer player, RandomSource random) {
        int count = random.nextDouble() < Config.SECOND_CURSE_CHANCE.get() ? 2 : 1;
        List<Curse> curses = CurseRegistry.roll(random, count);
        MarkManager.afflict(player, curses);
    }

    /**
     * Coast, dark and storm all make it likelier; the anniversary of a sacking most of all.
     */
    public static double omenMultiplier(ServerPlayer player) {
        double multiplier = 1.0D;

        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        if (isCoastal(biome)) {
            multiplier *= Config.COASTAL_MULTIPLIER.get();
        }
        if (player.level().isNight()) {
            multiplier *= Config.NIGHT_MULTIPLIER.get();
        }
        if (player.level().isThundering()) {
            multiplier *= Config.STORM_MULTIPLIER.get();
        }
        if (isRaidDay(player.level())) {
            multiplier *= Config.RAID_DAY_MULTIPLIER.get();
        }
        return multiplier;
    }

    public static boolean isCoastal(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_BEACH)
                || biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_DEEP_OCEAN);
    }

    /** Every Nth in-game day the dead are saluted, and the mark is closer to the surface. */
    public static boolean isRaidDay(net.minecraft.world.level.Level level) {
        int interval = Config.RAID_DAY_INTERVAL.get();
        if (interval <= 0) {
            return false;
        }
        long day = level.getDayTime() / 24000L;
        return day % interval == 0L;
    }

    /** Fisher-Yates on Minecraft's own RNG, so world seed and server tick drive it. */
    private static void shuffle(List<ServerPlayer> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            ServerPlayer tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    /** Kept so a reloaded server does not immediately roll. */
    public static void resetCooldown() {
        cooldown = Config.CHECK_INTERVAL_SECONDS.get() * 20;
    }

    private MarkAffliction() {
    }
}
