package com.goshan.blackmark.mark;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.CurseRegistry;
import com.goshan.blackmark.net.ModNetwork;
import com.goshan.blackmark.registry.ModItems;
import com.goshan.blackmark.util.Lore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side authority over the mark: who carries it, where it sits, and what it does each tick.
 */
public final class MarkManager {

    /** Main inventory plus hotbar. Armour and off-hand sit above this. */
    private static final int PACK_SIZE = 36;
    private static final int HOTBAR_SIZE = 9;
    /** One frantic drag is one refusal, not twenty. */
    private static final int DEFY_COOLDOWN = 40;

    public static boolean isMark(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.BLACK_MARK.get());
    }

    // --- lifecycle --------------------------------------------------------

    /** The mark chooses this player. Does nothing if they already carry it. */
    public static void afflict(ServerPlayer player, List<Curse> curses) {
        MarkCapability.of(player).ifPresent(data -> {
            if (data.isMarked() || curses.isEmpty()) {
                return;
            }

            data.setMarked(true);
            data.setState(MarkState.FOG);
            data.setStateTimer(rollFogTicks(player.getRandom()));

            for (Curse curse : curses) {
                data.addCurse(curse.id());
                curse.onApply(player, data, data.curseState(curse.id()));
            }

            sweep(player, data);

            player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS, 1.0F, 0.4F);
            spawnMist(player, 40);

            Lore.omen(player, "message.blackmark.marked.self");
            for (Curse curse : curses) {
                Lore.whisper(player, curse.onsetKey());
            }

            if (Config.BROADCAST_TO_SERVER.get() && player.getServer() != null) {
                if (Config.NAME_THE_BEARER.get()) {
                    Lore.broadcast(player.getServer(), "message.blackmark.marked.broadcast_named",
                            player.getDisplayName());
                } else {
                    Lore.broadcast(player.getServer(), "message.blackmark.marked.broadcast");
                }
            }

            sync(player, data);
        });
    }

    /** The last will is satisfied. The mark dissolves into the air. */
    public static void free(ServerPlayer player, boolean broadcast) {
        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked()) {
                return;
            }

            for (ResourceLocation id : new ArrayList<>(data.curseIds())) {
                Curse curse = CurseRegistry.get(id);
                if (curse != null) {
                    curse.onRemove(player, data, data.curseState(id));
                }
            }

            data.reset();
            removeAllMarkItems(player);

            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.7F, 1.6F);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(),
                        60, 0.5D, 0.9D, 0.5D, 0.02D);
            }

            Lore.whisper(player, "message.blackmark.freed.self");
            if (broadcast && Config.BROADCAST_TO_SERVER.get() && player.getServer() != null) {
                Lore.broadcast(player.getServer(), "message.blackmark.freed.broadcast");
            }

            sync(player, data);
        });
    }

    // --- per-tick ---------------------------------------------------------

    public static void serverTick(ServerPlayer player) {
        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked()) {
                // Nobody should be holding a mark they were not given.
                if (removeAllMarkItems(player) > 0) {
                    data.markDirty();
                }
                if (data.isDirty()) {
                    sync(player, data);
                }
                return;
            }

            data.tickCarried();
            data.tickDefyCooldown();
            tickState(player, data);
            sweep(player, data);

            for (ResourceLocation id : new ArrayList<>(data.curseIds())) {
                Curse curse = CurseRegistry.get(id);
                if (curse != null) {
                    curse.serverTick(player, data, data.curseState(id));
                }
            }

            if (data.isDirty()) {
                sync(player, data);
            }
        });
    }

    /** Mist thickens into a shell, the shell thins back into mist. */
    private static void tickState(ServerPlayer player, MarkData data) {
        int timer = data.getStateTimer() - 1;
        if (timer > 0) {
            data.setStateTimer(timer);
            return;
        }

        MarkState next = data.getState().flip();
        data.setState(next);
        data.setStateTimer(next == MarkState.FOG
                ? rollFogTicks(player.getRandom())
                : rollFleshTicks(player.getRandom()));

        if (next == MarkState.FLESH) {
            spawnMist(player, 5);
        }
    }

    // --- the mark refuses to leave ---------------------------------------

    /**
     * Called whenever the bearer tries to drop, stash or destroy the mark.
     * It keeps score, changes its nature at the touch, and lets every curse answer.
     */
    public static void defy(ServerPlayer player, MarkData data, String messageKey) {
        data.addDefiance(1);

        for (ResourceLocation id : new ArrayList<>(data.curseIds())) {
            Curse curse = CurseRegistry.get(id);
            if (curse != null) {
                curse.onDefiance(player, data, data.curseState(id), 1);
            }
        }

        // Touch it and it becomes the other thing.
        MarkState next = data.getState().flip();
        data.setState(next);
        data.setStateTimer(next == MarkState.FOG
                ? rollFogTicks(player.getRandom())
                : rollFleshTicks(player.getRandom()));

        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.6F, 0.4F);
        spawnMist(player, 12);
        Lore.whisper(player, messageKey);

        sync(player, data);
    }

    // --- inventory bookkeeping -------------------------------------------

    /**
     * Guarantees the invariant: a bearer holds exactly one mark, inside the pack, and it stays put.
     * This is the backstop that catches every route out - shulkers, death drops, /clear, other mods.
     */
    public static void sweep(ServerPlayer player, MarkData data) {
        Inventory inv = player.getInventory();
        int primary = -1;
        boolean changed = false;

        changed |= reclaimFromForeignSlots(player, data);

        // While it wears a shell the bearer may lift it and move it around their own pack -
        // that is the only way it ever reaches a hand, and the wake needs it in one.
        // A mark on the cursor is still accounted for, so no second one is conjured underneath it.
        boolean onCursor = player.containerMenu != null && isMark(player.containerMenu.getCarried());

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!isMark(stack)) {
                continue;
            }
            if (stack.getCount() != 1) {
                stack.setCount(1);
                changed = true;
            }
            if (primary == -1 && i < PACK_SIZE) {
                primary = i;
            } else {
                // A duplicate, or it wandered into armour / off-hand. Neither is allowed.
                inv.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (primary == -1 && !onCursor) {
            int target = choosePlacement(player, data);
            ItemStack displaced = inv.getItem(target);
            inv.setItem(target, new ItemStack(ModItems.BLACK_MARK.get()));
            if (!displaced.isEmpty()) {
                giveOrDrop(player, displaced);
            }
            primary = target;
            changed = true;
        }

        // Remember wherever the bearer last set it down; while it is in hand there is nothing to remember.
        if (primary != -1 && data.getMarkSlot() != primary) {
            data.setMarkSlot(primary);
        }

        if (changed) {
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        }
    }

    /**
     * A chest, a crafting grid, a furnace - any slot that is not the bearer's own pack.
     * The stash is emptied the same tick, and the sweep below puts a mark back where it belongs.
     */
    private static boolean reclaimFromForeignSlots(ServerPlayer player, MarkData data) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return false;
        }

        boolean found = false;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (isMark(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
                found = true;
            }
        }

        if (found && data.claimDefyWindow(DEFY_COOLDOWN)) {
            defy(player, data, Lore.variant(player.getRandom(), "message.blackmark.defy.stash", 4));
        }
        return found;
    }

    /** Where the mark settles. It prefers where it already was, then anywhere it is not shut out of. */
    private static int choosePlacement(ServerPlayer player, MarkData data) {
        Inventory inv = player.getInventory();
        int remembered = data.getMarkSlot();
        if (remembered >= 0 && remembered < PACK_SIZE && !SlotLock.isBlocked(data, remembered)) {
            return remembered;
        }

        // Empty slot in the pack proper, chosen at random so it never lands in the same corner.
        List<Integer> free = new ArrayList<>();
        for (int i = HOTBAR_SIZE; i < PACK_SIZE; i++) {
            if (inv.getItem(i).isEmpty() && !SlotLock.isBlocked(data, i)) {
                free.add(i);
            }
        }
        if (free.isEmpty()) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                if (inv.getItem(i).isEmpty() && !SlotLock.isBlocked(data, i)) {
                    free.add(i);
                }
            }
        }
        if (!free.isEmpty()) {
            return free.get(player.getRandom().nextInt(free.size()));
        }

        // Pack is full: it takes a slot anyway and whatever was there is shoved aside.
        for (int i = HOTBAR_SIZE; i < PACK_SIZE; i++) {
            if (!SlotLock.isBlocked(data, i)) {
                return i;
            }
        }
        return 0;
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || isMark(stack)) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static int removeAllMarkItems(ServerPlayer player) {
        Inventory inv = player.getInventory();
        int removed = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isMark(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
                removed++;
            }
        }
        if (removed > 0) {
            player.inventoryMenu.broadcastChanges();
        }
        return removed;
    }

    // --- helpers ----------------------------------------------------------

    public static void sync(ServerPlayer player, MarkData data) {
        ModNetwork.syncTo(player, data);
        data.clearDirty();
    }

    public static void spawnMist(ServerPlayer player, int count) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.1D, player.getZ(),
                    count, 0.35D, 0.5D, 0.35D, 0.01D);
        }
    }

    public static int rollFogTicks(RandomSource random) {
        return roll(random, Config.FOG_TICKS_MIN.get(), Config.FOG_TICKS_MAX.get());
    }

    public static int rollFleshTicks(RandomSource random) {
        return roll(random, Config.FLESH_TICKS_MIN.get(), Config.FLESH_TICKS_MAX.get());
    }

    private static int roll(RandomSource random, int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return lo + (hi > lo ? random.nextInt(hi - lo) : 0);
    }

    private MarkManager() {
    }
}
