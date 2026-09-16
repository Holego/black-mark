package com.goshan.blackmark.mark;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Everything the mark remembers about one bearer. Survives death, dimension change and logout.
 */
public class MarkData implements INBTSerializable<CompoundTag> {

    private boolean marked;
    /** Server ticks the mark has been carried. Only counts while the bearer is online. */
    private long carriedTicks;
    /** How often the bearer has tried to be rid of it. The mark keeps score. */
    private int defiance;
    private MarkState state = MarkState.FOG;
    private int stateTimer;
    /** Inventory slot the mark currently occupies, or -1 when it is not placed yet. */
    private int markSlot = -1;

    /** Curse id -> that curse's private state bag. Usually one entry, rarely two. */
    private final Map<ResourceLocation, CompoundTag> curses = new LinkedHashMap<>();

    private transient boolean dirty = true;
    /** Runtime-only. Stops a single frantic click storm from counting as twenty separate refusals. */
    private transient int defyCooldown;

    // --- bearer state -----------------------------------------------------

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean value) {
        if (this.marked != value) {
            this.marked = value;
            markDirty();
        }
    }

    public long getCarriedTicks() {
        return carriedTicks;
    }

    public void tickCarried() {
        carriedTicks++;
    }

    public int getDefiance() {
        return defiance;
    }

    /** True once per cooldown window, so one messy drag is one refusal and not a dozen. */
    public boolean claimDefyWindow(int cooldownTicks) {
        if (defyCooldown > 0) {
            return false;
        }
        defyCooldown = cooldownTicks;
        return true;
    }

    public void tickDefyCooldown() {
        if (defyCooldown > 0) {
            defyCooldown--;
        }
    }

    public void addDefiance(int amount) {
        this.defiance += amount;
        markDirty();
    }

    public MarkState getState() {
        return state;
    }

    public void setState(MarkState state) {
        if (this.state != state) {
            this.state = state;
            markDirty();
        }
    }

    public int getStateTimer() {
        return stateTimer;
    }

    public void setStateTimer(int stateTimer) {
        this.stateTimer = stateTimer;
    }

    public int getMarkSlot() {
        return markSlot;
    }

    public void setMarkSlot(int slot) {
        if (this.markSlot != slot) {
            this.markSlot = slot;
            markDirty();
        }
    }

    // --- curses -----------------------------------------------------------

    public Set<ResourceLocation> curseIds() {
        return Collections.unmodifiableSet(curses.keySet());
    }

    public boolean hasCurse(ResourceLocation id) {
        return curses.containsKey(id);
    }

    /** The curse's own scratch space. Mutating it does not flag a sync - call {@link #markDirty()} yourself. */
    public CompoundTag curseState(ResourceLocation id) {
        return curses.computeIfAbsent(id, k -> new CompoundTag());
    }

    /** Read-only lookup that does not silently grant the curse. Null when the bearer does not have it. */
    @Nullable
    public CompoundTag peekCurseState(ResourceLocation id) {
        return curses.get(id);
    }

    public void addCurse(ResourceLocation id) {
        curses.putIfAbsent(id, new CompoundTag());
        markDirty();
    }

    public void removeCurse(ResourceLocation id) {
        if (curses.remove(id) != null) {
            markDirty();
        }
    }

    public void clearCurses() {
        if (!curses.isEmpty()) {
            curses.clear();
            markDirty();
        }
    }

    /** Wipes the bearer clean. Used by the wake and by the admin command. */
    public void reset() {
        marked = false;
        carriedTicks = 0L;
        defiance = 0;
        state = MarkState.FOG;
        stateTimer = 0;
        markSlot = -1;
        curses.clear();
        markDirty();
    }

    // --- sync bookkeeping -------------------------------------------------

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    /** Copies everything across a death or a dimension change. */
    public void copyFrom(MarkData other) {
        this.marked = other.marked;
        this.carriedTicks = other.carriedTicks;
        this.defiance = other.defiance;
        this.state = other.state;
        this.stateTimer = other.stateTimer;
        this.markSlot = other.markSlot;
        this.curses.clear();
        for (Map.Entry<ResourceLocation, CompoundTag> e : other.curses.entrySet()) {
            this.curses.put(e.getKey(), e.getValue().copy());
        }
        markDirty();
    }

    // --- nbt --------------------------------------------------------------

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Marked", marked);
        tag.putLong("CarriedTicks", carriedTicks);
        tag.putInt("Defiance", defiance);
        tag.putInt("State", state.id());
        tag.putInt("StateTimer", stateTimer);
        tag.putInt("MarkSlot", markSlot);

        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, CompoundTag> e : curses.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", e.getKey().toString());
            entry.put("State", e.getValue());
            list.add(entry);
        }
        tag.put("Curses", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        marked = tag.getBoolean("Marked");
        carriedTicks = tag.getLong("CarriedTicks");
        defiance = tag.getInt("Defiance");
        state = MarkState.byId(tag.getInt("State"));
        stateTimer = tag.getInt("StateTimer");
        markSlot = tag.contains("MarkSlot") ? tag.getInt("MarkSlot") : -1;

        curses.clear();
        ListTag list = tag.getList("Curses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            if (id != null) {
                curses.put(id, entry.getCompound("State").copy());
            }
        }
        markDirty();
    }
}
