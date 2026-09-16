package com.goshan.blackmark.curse.impl;

import com.goshan.blackmark.Config;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.Curses;
import com.goshan.blackmark.mark.MarkData;
import com.goshan.blackmark.util.Lore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Sickness. Provisions turn in the pack, meals sit badly, and torn flesh will not knit on its own.
 */
public class BlightCurse implements Curse {

    private static final String TIMER = "Timer";

    @Override
    public ResourceLocation id() {
        return Curses.BLIGHT;
    }

    @Override
    public int weight() {
        return 12;
    }

    @Override
    public void serverTick(ServerPlayer player, MarkData data, CompoundTag state) {
        int timer = state.getInt(TIMER) + 1;
        int period = Config.BLIGHT_ROT_MINUTES.get() * 60 * 20;
        if (timer >= period) {
            timer = 0;
            rotSomething(player);
        }
        state.putInt(TIMER, timer);
    }

    @Override
    public void onDefiance(ServerPlayer player, MarkData data, CompoundTag state, int amount) {
        for (int i = 0; i < amount; i++) {
            rotSomething(player);
        }
    }

    /** One stack of provisions turns. Never the whole pack at once - it is a sickness, not a purge. */
    private void rotSomething(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<Integer> edible = new ArrayList<>();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.isEdible() && !stack.is(Items.ROTTEN_FLESH)) {
                edible.add(i);
            }
        }
        if (edible.isEmpty()) {
            return;
        }

        int slot = edible.get(player.getRandom().nextInt(edible.size()));
        ItemStack stack = inv.getItem(slot);
        int turned = Math.min(stack.getCount(), 1 + player.getRandom().nextInt(2));

        stack.shrink(turned);
        if (stack.isEmpty()) {
            inv.setItem(slot, ItemStack.EMPTY);
        }

        ItemStack rot = new ItemStack(Items.ROTTEN_FLESH, turned);
        if (!inv.add(rot)) {
            player.drop(rot, false);
        }

        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.3F, 0.7F);
        Lore.whisper(player, Lore.variant(player.getRandom(), "message.blackmark.blight.rot", 4));
    }

    // --- read by the healing and eating hooks -----------------------------

    public static boolean blocksNaturalRegen(MarkData data) {
        return data.hasCurse(Curses.BLIGHT) && Config.BLIGHT_BLOCK_REGEN.get();
    }

    public static float saturationLeft(MarkData data) {
        if (!data.hasCurse(Curses.BLIGHT)) {
            return 1.0F;
        }
        return (float) Math.max(0.0D, 1.0D - Config.BLIGHT_SATURATION_PENALTY.get());
    }
}
