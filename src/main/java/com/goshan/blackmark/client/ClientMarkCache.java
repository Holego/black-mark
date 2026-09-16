package com.goshan.blackmark.client;

import com.goshan.blackmark.mark.MarkData;
import net.minecraft.nbt.CompoundTag;

/**
 * The client's copy of its own mark. Read by the overlays; never written by anything but the sync packet.
 */
public final class ClientMarkCache {

    private static final MarkData DATA = new MarkData();

    public static void accept(CompoundTag tag) {
        DATA.deserializeNBT(tag);
    }

    public static MarkData get() {
        return DATA;
    }

    public static boolean isMarked() {
        return DATA.isMarked();
    }

    /** Leaving a world must not leave the overlays painted on the next one. */
    public static void clear() {
        DATA.reset();
    }

    private ClientMarkCache() {
    }
}
