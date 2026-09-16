package com.goshan.blackmark.mark;

import com.goshan.blackmark.curse.Curses;
import net.minecraft.nbt.CompoundTag;

/**
 * Shared view of which inventory slots the mark has swallowed.
 * <p>
 * The slot devourer curse writes it; the inventory sweep and the client renderer read it.
 * Keeping it here means neither of those has to know the curse exists.
 */
public final class SlotLock {

    private static final String KEY = "Blocked";
    private static final int[] NONE = new int[0];

    public static int[] blocked(MarkData data) {
        CompoundTag state = data.peekCurseState(Curses.SLOT_DEVOURER);
        return state == null ? NONE : state.getIntArray(KEY);
    }

    public static boolean isBlocked(MarkData data, int slot) {
        for (int s : blocked(data)) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    public static void set(MarkData data, int[] slots) {
        data.curseState(Curses.SLOT_DEVOURER).putIntArray(KEY, slots);
        data.markDirty();
    }

    private SlotLock() {
    }
}
