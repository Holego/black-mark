package com.goshan.blackmark.curse;

import com.goshan.blackmark.BlackMarkMod;
import net.minecraft.resources.ResourceLocation;

/** Stable ids, so client code and NBT never depend on class names. */
public final class Curses {

    public static final ResourceLocation SLOT_DEVOURER = id("slot_devourer");
    public static final ResourceLocation DEMENTIA = id("dementia");
    public static final ResourceLocation BLIGHT = id("blight");
    public static final ResourceLocation ETERNAL_WOUNDS = id("eternal_wounds");
    public static final ResourceLocation PIRATE = id("pirate");

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BlackMarkMod.MODID, path);
    }

    private Curses() {
    }
}
