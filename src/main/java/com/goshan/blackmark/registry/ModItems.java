package com.goshan.blackmark.registry;

import com.goshan.blackmark.BlackMarkMod;
import com.goshan.blackmark.mark.BlackMarkItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BlackMarkMod.MODID);

    /**
     * Deliberately absent from every creative tab. The mark is not something you take.
     */
    public static final RegistryObject<Item> BLACK_MARK =
            ITEMS.register("black_mark", BlackMarkItem::new);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ModItems() {
    }
}
