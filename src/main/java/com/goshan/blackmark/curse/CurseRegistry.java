package com.goshan.blackmark.curse;

import com.goshan.blackmark.curse.impl.BlightCurse;
import com.goshan.blackmark.curse.impl.DementiaCurse;
import com.goshan.blackmark.curse.impl.EternalWoundsCurse;
import com.goshan.blackmark.curse.impl.PirateCurse;
import com.goshan.blackmark.curse.impl.SlotDevourerCurse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CurseRegistry {

    private static final Map<ResourceLocation, Curse> REGISTRY = new LinkedHashMap<>();

    /**
     * Add new curses here. One line per curse.
     */
    public static void bootstrap() {
        REGISTRY.clear();
        register(new SlotDevourerCurse());
        register(new DementiaCurse());
        register(new BlightCurse());
        register(new EternalWoundsCurse());
        register(new PirateCurse());
    }

    public static void register(Curse curse) {
        REGISTRY.put(curse.id(), curse);
    }

    @Nullable
    public static Curse get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static Collection<Curse> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Picks {@code count} distinct curses, weighted by rarity.
     * Returns fewer than asked if the registry cannot supply that many.
     */
    public static List<Curse> roll(RandomSource random, int count) {
        List<Curse> pool = new ArrayList<>(REGISTRY.values());
        List<Curse> picked = new ArrayList<>(count);

        for (int n = 0; n < count && !pool.isEmpty(); n++) {
            int total = 0;
            for (Curse c : pool) {
                total += Math.max(0, c.weight());
            }
            if (total <= 0) {
                picked.add(pool.remove(random.nextInt(pool.size())));
                continue;
            }

            int roll = random.nextInt(total);
            for (int i = 0; i < pool.size(); i++) {
                roll -= Math.max(0, pool.get(i).weight());
                if (roll < 0) {
                    picked.add(pool.remove(i));
                    break;
                }
            }
        }
        return picked;
    }

    private CurseRegistry() {
    }
}
