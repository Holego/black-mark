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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Everlasting wounds. A hard blow takes a half-heart the bearer never gets back,
 * for as long as the mark stays.
 */
public class EternalWoundsCurse implements Curse {

    private static final String WOUNDS = "Wounds";
    private static final UUID MODIFIER_ID = UUID.fromString("6b1f0d3a-9c4e-4f7b-8a21-3d5e7c9b0a44");
    private static final String MODIFIER_NAME = "blackmark.eternal_wounds";

    @Override
    public ResourceLocation id() {
        return Curses.ETERNAL_WOUNDS;
    }

    @Override
    public int weight() {
        return 9;
    }

    @Override
    public void onApply(ServerPlayer player, MarkData data, CompoundTag state) {
        applyModifier(player, state.getInt(WOUNDS));
    }

    @Override
    public void onRemove(ServerPlayer player, MarkData data, CompoundTag state) {
        clearModifier(player);
    }

    /** A single blow of at least the configured size tears something that will not close. */
    public void onHardHit(ServerPlayer player, MarkData data, CompoundTag state) {
        int max = Config.WOUNDS_MAX.get();
        int wounds = state.getInt(WOUNDS);
        if (wounds >= max) {
            return;
        }

        wounds++;
        state.putInt(WOUNDS, wounds);
        data.markDirty();
        applyModifier(player, wounds);

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 0.6F, 0.5F);
        Lore.omen(player, Lore.variant(player.getRandom(), "message.blackmark.wounds.open", 4));
    }

    private static void applyModifier(ServerPlayer player, int wounds) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(MODIFIER_ID);
        if (wounds > 0) {
            attribute.addTransientModifier(new AttributeModifier(
                    MODIFIER_ID, MODIFIER_NAME, -wounds, AttributeModifier.Operation.ADDITION));
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void clearModifier(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER_ID);
        }
    }

    /** Half-hearts taken, for the heart overlay on the client. */
    public static int wounds(MarkData data) {
        CompoundTag state = data.peekCurseState(Curses.ETERNAL_WOUNDS);
        return state == null ? 0 : state.getInt(WOUNDS);
    }
}
