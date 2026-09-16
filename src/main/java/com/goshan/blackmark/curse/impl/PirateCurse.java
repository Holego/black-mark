package com.goshan.blackmark.curse.impl;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.Curses;
import com.goshan.blackmark.mark.MarkData;
import com.goshan.blackmark.util.Lore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The dead man's tongue.
 * <p>
 * The mark carries out a pirate's last will, and after a while the pirate starts showing through.
 * The bearer swears by things they have never seen, bellows at nobody, swings at their own crew,
 * and cannot keep their hands off gold.
 */
public class PirateCurse implements Curse {

    private static final String SHOUT_TIMER = "ShoutTimer";
    private static final String STRIKE_TIMER = "StrikeTimer";
    private static final String GOLD_TIMER = "GoldTimer";
    private static final String FRENZY_TICKS = "FrenzyTicks";
    /** Synced to the client so it can tint the screen. Only written on a transition. */
    private static final String FRENZIED = "Frenzied";

    private static final double STRIKE_REACH = 3.5D;

    @Override
    public ResourceLocation id() {
        return Curses.PIRATE;
    }

    @Override
    public int weight() {
        return 11;
    }

    @Override
    public void serverTick(ServerPlayer player, MarkData data, CompoundTag state) {
        tickFrenzy(player, data, state);
        tickShouting(player, state);
        tickStriking(player, state);
    }

    @Override
    public void onRemove(ServerPlayer player, MarkData data, CompoundTag state) {
        state.putInt(FRENZY_TICKS, 0);
        state.putBoolean(FRENZIED, false);
    }

    /** Cross it and the pirate takes the wheel for a while. */
    @Override
    public void onDefiance(ServerPlayer player, MarkData data, CompoundTag state, int amount) {
        startFrenzy(player, data, state);
    }

    // --- gold madness -----------------------------------------------------

    private void tickFrenzy(ServerPlayer player, MarkData data, CompoundTag state) {
        int remaining = state.getInt(FRENZY_TICKS);

        if (remaining > 0) {
            remaining--;
            state.putInt(FRENZY_TICKS, remaining);
            if (remaining == 0) {
                state.putBoolean(FRENZIED, false);
                data.markDirty();
                Lore.whisper(player, "message.blackmark.pirate.calm");
            }
            return;
        }

        int timer = state.getInt(GOLD_TIMER) + 1;
        if (timer >= Config.PIRATE_GOLD_SCAN_SECONDS.get() * 20) {
            timer = 0;
            if (seesGold(player)) {
                startFrenzy(player, data, state);
            }
        }
        state.putInt(GOLD_TIMER, timer);
    }

    private void startFrenzy(ServerPlayer player, MarkData data, CompoundTag state) {
        boolean wasCalm = state.getInt(FRENZY_TICKS) <= 0;
        state.putInt(FRENZY_TICKS, Config.PIRATE_FRENZY_SECONDS.get() * 20);

        if (wasCalm) {
            state.putBoolean(FRENZIED, true);
            data.markDirty();

            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_SCREAM,
                    SoundSource.PLAYERS, 0.5F, 0.6F);
            bellow(player, Lore.variant(player.getRandom(), "message.blackmark.pirate.gold", 4));
        }
    }

    /** Gold in the pack, gold on the ground, or gold in the walls around them. */
    private boolean seesGold(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isGold(inv.getItem(i))) {
                return true;
            }
        }

        Level level = player.level();
        AABB nearby = player.getBoundingBox().inflate(8.0D);
        List<net.minecraft.world.entity.item.ItemEntity> loose =
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, nearby,
                        entity -> isGold(entity.getItem()));
        if (!loose.isEmpty()) {
            return true;
        }

        int radius = Config.PIRATE_GOLD_RADIUS.get();
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    if (isGold(level.getBlockState(cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isGold(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(Tags.Items.INGOTS_GOLD)
                || stack.is(Tags.Items.NUGGETS_GOLD)
                || stack.is(Tags.Items.STORAGE_BLOCKS_GOLD)
                || stack.is(Tags.Items.ORES_GOLD)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().contains("gold");
    }

    private static boolean isGold(BlockState state) {
        if (state.is(Tags.Blocks.ORES_GOLD) || state.is(Tags.Blocks.STORAGE_BLOCKS_GOLD)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && (id.getPath().contains("gold") || id.getPath().contains("gilded"));
    }

    // --- bellowing --------------------------------------------------------

    private void tickShouting(ServerPlayer player, CompoundTag state) {
        int timer = state.getInt(SHOUT_TIMER) + 1;
        if (timer < Config.PIRATE_SHOUT_SECONDS.get() * 20) {
            state.putInt(SHOUT_TIMER, timer);
            return;
        }
        state.putInt(SHOUT_TIMER, 0);

        double chance = Config.PIRATE_SHOUT_CHANCE.get() * frenzyScale(state);
        if (player.getRandom().nextDouble() < chance) {
            bellow(player, Lore.variant(player.getRandom(), "message.blackmark.pirate.outburst", 8));
        }
    }

    /** Goes out as chat, because everyone aboard should hear it. */
    private static void bellow(ServerPlayer player, String key) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Component line = Component.literal("<")
                .append(player.getDisplayName())
                .append("> ")
                .append(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        server.getPlayerList().broadcastSystemMessage(line, false);
    }

    // --- swinging at the crew ---------------------------------------------

    private void tickStriking(ServerPlayer player, CompoundTag state) {
        int timer = state.getInt(STRIKE_TIMER) + 1;
        if (timer < Config.PIRATE_STRIKE_SECONDS.get() * 20) {
            state.putInt(STRIKE_TIMER, timer);
            return;
        }
        state.putInt(STRIKE_TIMER, 0);

        double chance = Config.PIRATE_STRIKE_CHANCE.get() * frenzyScale(state);
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }

        LivingEntity target = nearestAlly(player);
        if (target == null) {
            return;
        }

        player.swing(InteractionHand.MAIN_HAND, true);
        // A bare-handed backhand, not the sword they happen to be holding.
        // On a server with PvP off, vanilla's own check quietly refuses this.
        target.hurt(player.damageSources().playerAttack(player),
                Config.PIRATE_STRIKE_DAMAGE.get().floatValue());

        bellow(player, Lore.variant(player.getRandom(), "message.blackmark.pirate.strike", 4));
        Lore.whisper(player, "message.blackmark.pirate.strike_self");
    }

    @Nullable
    private LivingEntity nearestAlly(ServerPlayer player) {
        AABB reach = player.getBoundingBox().inflate(STRIKE_REACH);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, reach,
                entity -> isAlly(player, entity));

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = candidate.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isAlly(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player other) {
            return Config.PIRATE_STRIKE_PLAYERS.get() && !other.isSpectator() && !other.isCreative();
        }
        if (entity instanceof TamableAnimal tame) {
            return tame.isTame();
        }
        return entity instanceof AbstractVillager || entity instanceof IronGolem;
    }

    // --- read by the chat hook and the client -----------------------------

    private static double frenzyScale(CompoundTag state) {
        return state.getBoolean(FRENZIED) ? Config.PIRATE_FRENZY_MULTIPLIER.get() : 1.0D;
    }

    /** Safe on the client: reads only what the server synced. */
    public static boolean isFrenzied(MarkData data) {
        CompoundTag state = data.peekCurseState(Curses.PIRATE);
        return state != null && state.getBoolean(FRENZIED);
    }
}
