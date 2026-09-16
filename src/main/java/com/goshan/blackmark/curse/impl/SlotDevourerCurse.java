package com.goshan.blackmark.curse.impl;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.Curses;
import com.goshan.blackmark.mark.MarkData;
import com.goshan.blackmark.mark.MarkManager;
import com.goshan.blackmark.mark.SlotLock;
import com.goshan.blackmark.util.Lore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The mark sticks where it lands and the pack rots outward from it.
 * Slots go black one by one, and every attempt to be rid of it costs another.
 */
public class SlotDevourerCurse implements Curse {

    private static final String TIMER = "Timer";
    private static final int PACK_SIZE = 36;
    private static final int HOTBAR_SIZE = 9;

    @Override
    public ResourceLocation id() {
        return Curses.SLOT_DEVOURER;
    }

    @Override
    public int weight() {
        return 14;
    }

    @Override
    public void onApply(ServerPlayer player, MarkData data, CompoundTag state) {
        if (!state.contains("Blocked")) {
            SlotLock.set(data, new int[0]);
        }
    }

    @Override
    public void serverTick(ServerPlayer player, MarkData data, CompoundTag state) {
        int timer = state.getInt(TIMER) + 1;
        int period = Config.SLOTS_GROWTH_MINUTES.get() * 60 * 20;
        if (timer >= period) {
            timer = 0;
            devour(player, data, 1);
        }
        state.putInt(TIMER, timer);

        // Anything shoved into a swallowed slot is spat back out.
        enforce(player, data);
    }

    @Override
    public void onDefiance(ServerPlayer player, MarkData data, CompoundTag state, int amount) {
        int extra = Config.SLOTS_PER_DEFIANCE.get() * amount;
        if (extra > 0) {
            devour(player, data, extra);
        }
    }

    /** Takes the {@code count} free slots nearest the mark, so the blackness creeps outward from it. */
    private void devour(ServerPlayer player, MarkData data, int count) {
        int max = Config.SLOTS_MAX_DEVOURED.get();
        int[] blocked = SlotLock.blocked(data);
        if (blocked.length >= max) {
            return;
        }

        List<Integer> taken = new ArrayList<>(blocked.length + count);
        for (int slot : blocked) {
            taken.add(slot);
        }

        int markSlot = data.getMarkSlot();
        int added = 0;
        for (int n = 0; n < count && taken.size() < max; n++) {
            int best = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int slot = 0; slot < PACK_SIZE; slot++) {
                if (slot == markSlot || taken.contains(slot)) {
                    continue;
                }
                int distance = gridDistance(slot, markSlot);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = slot;
                }
            }
            if (best == -1) {
                break;
            }
            taken.add(best);
            added++;
        }

        if (added == 0) {
            return;
        }

        int[] updated = new int[taken.size()];
        for (int i = 0; i < updated.length; i++) {
            updated[i] = taken.get(i);
        }
        SlotLock.set(data, updated);

        enforce(player, data);

        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.35F, 0.35F);
        Lore.whisper(player, Lore.variant(player.getRandom(), "message.blackmark.slots.devour", 4));
    }

    /** Empties every swallowed slot, handing the contents back where there is still room. */
    private void enforce(ServerPlayer player, MarkData data) {
        int[] blocked = SlotLock.blocked(data);
        if (blocked.length == 0) {
            return;
        }

        Inventory inv = player.getInventory();
        boolean changed = false;

        for (int slot : blocked) {
            if (slot < 0 || slot >= PACK_SIZE) {
                continue;
            }
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || MarkManager.isMark(stack)) {
                continue;
            }
            inv.setItem(slot, ItemStack.EMPTY);
            MarkManager.giveOrDrop(player, stack);
            changed = true;
        }

        // A blackened hotbar slot cannot be the one you are holding.
        if (SlotLock.isBlocked(data, inv.selected)) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                if (!SlotLock.isBlocked(data, i)) {
                    inv.selected = i;
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        }
    }

    /**
     * Manhattan distance across the inventory as it is drawn: three rows of nine, hotbar underneath.
     */
    private static int gridDistance(int a, int b) {
        if (b < 0) {
            return Math.abs(a);
        }
        return Math.abs(row(a) - row(b)) + Math.abs(column(a) - column(b));
    }

    private static int row(int slot) {
        return slot < HOTBAR_SIZE ? 3 : (slot - HOTBAR_SIZE) / 9;
    }

    private static int column(int slot) {
        return slot < HOTBAR_SIZE ? slot : (slot - HOTBAR_SIZE) % 9;
    }
}
