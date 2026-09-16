package com.goshan.blackmark.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * What the feeble-minded bearer believes about the things in their pack.
 * <p>
 * Two separate lies, each with its own way out:
 * <ul>
 *   <li><b>Enchantments</b> are swapped for other, entirely real-looking ones. The truth comes back
 *       only while the item is actually worn or held - you have to put it on to find out what it does.</li>
 *   <li><b>Name and description</b> stay slurred until the bearer has handled that item themselves.
 *       Pick it up once and you remember it; everything you have never touched stays a stranger.</li>
 * </ul>
 * All of it is client-side dressing. The item keeps working exactly as it really is - the bearer is
 * confused, the sword is not.
 */
public final class DementiaVeil {

    /** Items the bearer has picked up or poked this session. Cleared on disconnect. */
    private static final Set<String> HANDLED = new HashSet<>();

    private static List<Enchantment> pool;

    // --- what the bearer has handled --------------------------------------

    /**
     * Identifies a kind of item, not one stack: registry name plus NBT. Moving a stack between slots
     * keeps both, so once you have held a thing you keep recognising it.
     */
    public static String keyOf(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String name = id == null ? "?" : id.toString();
        return stack.getTag() == null
                ? name
                : name + '#' + Integer.toHexString(stack.getTag().hashCode());
    }

    public static void remember(ItemStack stack) {
        if (!stack.isEmpty()) {
            HANDLED.add(keyOf(stack));
        }
    }

    public static boolean isHandled(ItemStack stack) {
        return !stack.isEmpty() && HANDLED.contains(keyOf(stack));
    }

    public static void forgetEverything() {
        HANDLED.clear();
    }

    // --- worn or held -----------------------------------------------------

    /** Identity comparison: inventory screens hand out the very stacks the player is wearing. */
    public static boolean isEquipped(@Nullable Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) {
            return false;
        }
        if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
            return true;
        }
        for (ItemStack worn : player.getInventory().armor) {
            if (worn == stack) {
                return true;
            }
        }
        return false;
    }

    // --- the enchantment lie ----------------------------------------------

    /**
     * Finds the tooltip lines that are enchantment names, by matching what the game would have
     * written for the enchantments this stack really has.
     */
    public static Set<Integer> enchantmentLines(List<Component> lines, Map<Enchantment, Integer> real) {
        Set<Integer> found = new HashSet<>();
        if (real.isEmpty()) {
            return found;
        }

        Set<String> names = new HashSet<>();
        for (Map.Entry<Enchantment, Integer> entry : real.entrySet()) {
            names.add(entry.getKey().getFullname(entry.getValue()).getString());
        }

        for (int i = 0; i < lines.size(); i++) {
            if (names.contains(lines.get(i).getString())) {
                found.add(i);
            }
        }
        return found;
    }

    /**
     * Replaces each real enchantment line with a different, plausible one. The lie is stable for a
     * given item, so the bearer can be consistently wrong rather than dizzy.
     */
    public static void swapEnchantments(List<Component> lines, Map<Enchantment, Integer> real, String key) {
        Map<String, Component> lies = new HashMap<>();
        int index = 0;
        for (Map.Entry<Enchantment, Integer> entry : real.entrySet()) {
            String truth = entry.getKey().getFullname(entry.getValue()).getString();
            lies.put(truth, invent(key, index++));
        }

        for (int i = 0; i < lines.size(); i++) {
            Component lie = lies.get(lines.get(i).getString());
            if (lie != null) {
                lines.set(i, lie);
            }
        }
    }

    private static Component invent(String key, int index) {
        List<Enchantment> candidates = pool();
        if (candidates.isEmpty()) {
            return Component.empty();
        }

        Random random = new Random(key.hashCode() * 31L + index * 7919L);
        Enchantment pick = candidates.get(random.nextInt(candidates.size()));
        int level = 1 + random.nextInt(Math.max(1, pick.getMaxLevel()));
        return pick.getFullname(level);
    }

    /** Built lazily: enchantment registries are not populated when this class first loads. */
    private static List<Enchantment> pool() {
        if (pool == null) {
            pool = new ArrayList<>(ForgeRegistries.ENCHANTMENTS.getValues());
        }
        return pool;
    }

    /** Convenience for the tooltip hook. */
    public static Map<Enchantment, Integer> realEnchantments(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack);
    }

    private DementiaVeil() {
    }
}
