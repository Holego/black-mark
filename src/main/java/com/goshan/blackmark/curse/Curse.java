package com.goshan.blackmark.curse;

import com.goshan.blackmark.mark.MarkData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * One affliction the mark may carry.
 * <p>
 * To add a new one: implement this interface, then register it in
 * {@link CurseRegistry#bootstrap()}. Nothing else in the mod needs to change.
 * <p>
 * The {@code state} tag handed to every method is that curse's own scratch space on that bearer.
 * It is saved with the player and synced to their client, so anything the client needs to draw
 * belongs in there. Call {@link MarkData#markDirty()} after changing it.
 */
public interface Curse {

    ResourceLocation id();

    /** Relative rarity when the mark picks what to inflict. Higher is more common. */
    default int weight() {
        return 10;
    }

    /** Translation key for the curse name, e.g. {@code curse.blackmark.blight}. */
    default String nameKey() {
        return "curse." + id().getNamespace() + "." + id().getPath();
    }

    /** Translation key for the line whispered to the bearer when it takes hold. */
    default String onsetKey() {
        return nameKey() + ".onset";
    }

    default Component displayName() {
        return Component.translatable(nameKey());
    }

    /** Called once, on the server, the moment the curse takes hold. */
    default void onApply(ServerPlayer player, MarkData data, CompoundTag state) {
    }

    /** Called once when the wake frees the bearer. Undo any lasting change here. */
    default void onRemove(ServerPlayer player, MarkData data, CompoundTag state) {
    }

    /** Every server tick while the bearer is online. Keep it cheap; gate real work on a timer. */
    default void serverTick(ServerPlayer player, MarkData data, CompoundTag state) {
    }

    /** The bearer tried to drop, stash or destroy the mark. The curse may answer. */
    default void onDefiance(ServerPlayer player, MarkData data, CompoundTag state, int amount) {
    }

    /**
     * Called after respawn so the curse can re-apply anything that does not survive death
     * (attribute modifiers, for instance).
     */
    default void onRespawn(ServerPlayer player, MarkData data, CompoundTag state) {
        onApply(player, data, state);
    }
}
