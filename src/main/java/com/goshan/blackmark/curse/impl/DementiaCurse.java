package com.goshan.blackmark.curse.impl;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.Curses;
import com.goshan.blackmark.mark.MarkData;
import com.goshan.blackmark.util.Lore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Feeble-mindedness. Names slip, the hotbar rearranges itself behind the bearer's back,
 * and what they say is not quite what comes out of their mouth.
 * <p>
 * The visible half of this curse lives on the client (tooltips, item names); the server only
 * keeps the intensity and syncs it.
 */
public class DementiaCurse implements Curse {

    private static final String INTENSITY = "Intensity";
    private static final String TIMER = "Timer";
    private static final String SCRAMBLE_TIMER = "ScrambleTimer";
    /** Intensity normalised to 0..1 on the server, so the client needs no config of its own. */
    private static final String SLIP = "Slip";
    private static final int HOTBAR_SIZE = 9;

    @Override
    public ResourceLocation id() {
        return Curses.DEMENTIA;
    }

    @Override
    public int weight() {
        return 10;
    }

    @Override
    public void onApply(ServerPlayer player, MarkData data, CompoundTag state) {
        if (!state.contains(INTENSITY)) {
            int max = Math.max(1, Config.DEMENTIA_MAX_INTENSITY.get());
            state.putInt(INTENSITY, 1);
            state.putFloat(SLIP, 1.0F / max);
            data.markDirty();
        }
    }

    @Override
    public void serverTick(ServerPlayer player, MarkData data, CompoundTag state) {
        int timer = state.getInt(TIMER) + 1;
        int period = Config.DEMENTIA_GROWTH_MINUTES.get() * 60 * 20;
        if (timer >= period) {
            timer = 0;
            worsen(player, data, state, 1);
        }
        state.putInt(TIMER, timer);

        int scrambleTimer = state.getInt(SCRAMBLE_TIMER) + 1;
        int scramblePeriod = Config.DEMENTIA_SCRAMBLE_SECONDS.get() * 20;
        if (scrambleTimer >= scramblePeriod) {
            scrambleTimer = 0;
            maybeScramble(player, state);
        }
        state.putInt(SCRAMBLE_TIMER, scrambleTimer);
    }

    /**
     * The bearer reaches for a sword and comes up holding a torch.
     * Two hotbar slots quietly change places; nothing is lost, only misplaced.
     */
    private void maybeScramble(ServerPlayer player, CompoundTag state) {
        double chance = Config.DEMENTIA_SCRAMBLE_CHANCE.get() * state.getInt(INTENSITY);
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }

        Inventory inv = player.getInventory();
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (!inv.getItem(i).isEmpty()) {
                filled.add(i);
            }
        }
        if (filled.size() < 2) {
            return;
        }

        int a = filled.get(player.getRandom().nextInt(filled.size()));
        int b = a;
        while (b == a) {
            b = filled.get(player.getRandom().nextInt(filled.size()));
        }

        ItemStack first = inv.getItem(a);
        inv.setItem(a, inv.getItem(b));
        inv.setItem(b, first);
        player.inventoryMenu.broadcastChanges();

        Lore.whisper(player, Lore.variant(player.getRandom(), "message.blackmark.dementia.scramble", 4));
    }

    @Override
    public void onDefiance(ServerPlayer player, MarkData data, CompoundTag state, int amount) {
        worsen(player, data, state, amount);
    }

    private void worsen(ServerPlayer player, MarkData data, CompoundTag state, int steps) {
        int max = Config.DEMENTIA_MAX_INTENSITY.get();
        int current = state.getInt(INTENSITY);
        int next = Math.min(max, current + steps);
        if (next == current) {
            return;
        }
        state.putInt(INTENSITY, next);
        state.putFloat(SLIP, (float) next / Math.max(1, max));
        data.markDirty();
        Lore.whisper(player, Lore.variant(player.getRandom(), "message.blackmark.dementia.worsen", 4));
    }

    // --- read by the chat, crafting and tooltip hooks ----------------------

    public static int intensity(MarkData data) {
        CompoundTag state = data.peekCurseState(Curses.DEMENTIA);
        return state == null ? 0 : state.getInt(INTENSITY);
    }

    /** 0 when unafflicted, rising to the configured chat garble chance at full intensity. */
    public static double garbleChance(MarkData data) {
        int level = intensity(data);
        if (level <= 0) {
            return 0.0D;
        }
        int max = Math.max(1, Config.DEMENTIA_MAX_INTENSITY.get());
        return Config.DEMENTIA_CHAT_GARBLE_CHANCE.get() * Math.min(1.0D, (double) level / max);
    }

    /**
     * How badly tooltips and item names slip, 0..1. Safe to call on the client:
     * it reads only what the server synced.
     */
    public static double nameGarbleChance(MarkData data) {
        CompoundTag state = data.peekCurseState(Curses.DEMENTIA);
        if (state == null) {
            return 0.0D;
        }
        return 0.35D * Math.min(1.0F, state.getFloat(SLIP));
    }
}
