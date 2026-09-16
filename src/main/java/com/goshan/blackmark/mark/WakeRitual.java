package com.goshan.blackmark.mark;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.util.Lore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * The wake.
 * <p>
 * Once, at a gathering for a lost shipmate, the mark was seen to dissolve from his ashes into the air.
 * That is the only account anyone has of it leaving, so that is the rite: a lit pyre on coastal ground
 * after dark, a tribute in the off hand, and company. The mark decides whether it has had enough.
 */
public final class WakeRitual {

    private static final double GATHERING_RADIUS = 12.0D;
    private static final int BLOCK_UPDATE = 3;

    /**
     * @return true if the interaction was the rite (successful or not) and should be consumed
     */
    public static boolean attempt(ServerPlayer player, MarkData data, BlockPos pos, BlockState state) {
        if (!isPyre(state)) {
            return false;
        }

        ItemStack main = player.getMainHandItem();
        if (!MarkManager.isMark(main)) {
            return false;
        }

        Level level = player.level();

        // The mark leaves when it decides to, not when it is asked.
        long required = (long) Config.MIN_CARRY_MINUTES.get() * 60L * 20L;
        if (data.getCarriedTicks() < required) {
            deny(player, "message.blackmark.wake.too_soon");
            return true;
        }

        if (Config.WAKE_REQUIRE_NIGHT.get() && !level.isNight()) {
            deny(player, "message.blackmark.wake.not_night");
            return true;
        }

        if (Config.WAKE_REQUIRE_COAST.get() && !MarkAffliction.isCoastal(level.getBiome(pos))) {
            deny(player, "message.blackmark.wake.not_coast");
            return true;
        }

        int needed = Config.WAKE_NEARBY_PLAYERS.get();
        int present = countMourners(level, pos);
        if (present < needed) {
            deny(player, "message.blackmark.wake.alone", present, needed);
            return true;
        }

        Item tribute = tributeItem();
        ItemStack offhand = player.getOffhandItem();
        if (tribute == null || !offhand.is(tribute)) {
            deny(player, "message.blackmark.wake.no_tribute",
                    tribute == null ? ItemStack.EMPTY.getHoverName() : tribute.getDescription());
            return true;
        }

        // Everything the last will asked for.
        offhand.shrink(1);
        level.setBlock(pos, state.setValue(CampfireBlock.LIT, Boolean.FALSE), BLOCK_UPDATE);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    80, 0.4D, 0.6D, 0.4D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                    60, 0.5D, 0.8D, 0.5D, 0.01D);
        }
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);

        MarkManager.free(player, true);
        Lore.whisper(player, "message.blackmark.wake.dissolved");
        return true;
    }

    public static boolean isPyre(BlockState state) {
        return state.getBlock() instanceof CampfireBlock
                && state.hasProperty(CampfireBlock.LIT)
                && state.getValue(CampfireBlock.LIT);
    }

    private static int countMourners(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(GATHERING_RADIUS);
        List<Player> nearby = level.getEntitiesOfClass(Player.class, area, p -> !p.isSpectator());
        return nearby.size();
    }

    private static Item tributeItem() {
        ResourceLocation id = ResourceLocation.tryParse(Config.WAKE_TRIBUTE_ITEM.get());
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    private static void deny(ServerPlayer player, String key, Object... args) {
        Lore.whisper(player, key, args);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    private WakeRitual() {
    }
}
