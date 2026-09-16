package com.goshan.blackmark.event;

import com.goshan.blackmark.BlackMarkMod;
import com.goshan.blackmark.Config;
import com.goshan.blackmark.command.BlackMarkCommand;
import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.CurseRegistry;
import com.goshan.blackmark.curse.Curses;
import com.goshan.blackmark.curse.impl.BlightCurse;
import com.goshan.blackmark.curse.impl.DementiaCurse;
import com.goshan.blackmark.curse.impl.EternalWoundsCurse;
import com.goshan.blackmark.curse.impl.PirateCurse;
import com.goshan.blackmark.mark.MarkAffliction;
import com.goshan.blackmark.mark.MarkCapability;
import com.goshan.blackmark.mark.MarkManager;
import com.goshan.blackmark.mark.WakeRitual;
import com.goshan.blackmark.util.Garble;
import com.goshan.blackmark.util.Lore;
import com.goshan.blackmark.util.PirateTongue;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;

/** Everything the mark hooks into. */
@Mod.EventBusSubscriber(modid = BlackMarkMod.MODID)
public final class ForgeEvents {

    /** Full food bar plus a one-point heal is vanilla's natural regeneration, near enough. */
    private static final int SATED_FOOD_LEVEL = 18;

    // --- capability plumbing ---------------------------------------------

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(MarkCapability.ID, new MarkCapability.Provider());
        }
    }

    /** Death does not free anyone. Neither does the End portal. */
    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        MarkCapability.of(original).ifPresent(old ->
                MarkCapability.of(event.getEntity()).ifPresent(fresh -> fresh.copyFrom(old)));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restore(player);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restore(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restore(player);
        }
    }

    /** Re-seats the mark and re-applies anything that does not survive a new player entity. */
    private static void restore(ServerPlayer player) {
        MarkCapability.of(player).ifPresent(data -> {
            if (data.isMarked()) {
                for (ResourceLocation id : new ArrayList<>(data.curseIds())) {
                    Curse curse = CurseRegistry.get(id);
                    if (curse != null) {
                        curse.onRespawn(player, data, data.curseState(id));
                    }
                }
                MarkManager.sweep(player, data);
            } else {
                MarkManager.removeAllMarkItems(player);
            }
            MarkManager.sync(player, data);
        });
    }

    // --- ticking ----------------------------------------------------------

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            MarkManager.serverTick(player);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            MarkAffliction.serverTick(server);
        }
    }

    // --- it will not be thrown away --------------------------------------

    @SubscribeEvent
    public static void itemTossed(ItemTossEvent event) {
        if (MarkManager.isMark(event.getEntity().getItem())) {
            event.setCanceled(true);
            return;
        }

        // Mine. In the grip of the frenzy the bearer's hand simply will not open on gold.
        if (!Config.PIRATE_HOARD_GOLD.get()
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !PirateCurse.isGold(event.getEntity().getItem())) {
            return;
        }

        MarkCapability.of(player).ifPresent(data -> {
            if (data.isMarked() && data.hasCurse(Curses.PIRATE) && PirateCurse.isFrenzied(data)) {
                event.setCanceled(true);
                Lore.whisper(player, "message.blackmark.pirate.hoard");
            }
        });
    }

    /** Belt and braces: no mark ever exists as an entity lying in the world. */
    @SubscribeEvent
    public static void entityJoined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ItemEntity item && MarkManager.isMark(item.getItem())) {
            event.setCanceled(true);
        }
    }

    // --- the wake ---------------------------------------------------------

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked()) {
                return;
            }
            if (WakeRitual.attempt(player, data, event.getPos(), event.getLevel().getBlockState(event.getPos()))) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
    }

    // --- eternal wounds ---------------------------------------------------

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked() || !data.hasCurse(Curses.ETERNAL_WOUNDS)) {
                return;
            }
            if (event.getAmount() < Config.WOUNDS_DAMAGE_THRESHOLD.get()) {
                return;
            }
            if (CurseRegistry.get(Curses.ETERNAL_WOUNDS) instanceof EternalWoundsCurse wounds) {
                wounds.onHardHit(player, data, data.curseState(Curses.ETERNAL_WOUNDS));
            }
        });
    }

    // --- blight -----------------------------------------------------------

    /**
     * Natural regeneration only. A one-point heal on a full stomach with no Regeneration effect
     * is vanilla topping the player up; potions, golden apples and beacons heal by other amounts.
     */
    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked() || !BlightCurse.blocksNaturalRegen(data)) {
                return;
            }
            boolean naturalTopUp = event.getAmount() <= 1.0F
                    && !player.hasEffect(MobEffects.REGENERATION)
                    && player.getFoodData().getFoodLevel() >= SATED_FOOD_LEVEL;
            if (naturalTopUp) {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void finishedEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItem().isEdible()) {
            return;
        }
        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked() || !data.hasCurse(Curses.BLIGHT)) {
                return;
            }
            FoodData food = player.getFoodData();
            food.setSaturation(food.getSaturationLevel() * BlightCurse.saturationLeft(data));
        });
    }

    // --- dementia ---------------------------------------------------------

    @SubscribeEvent
    public static void chat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        MarkCapability.of(player).ifPresent(data -> {
            if (!data.isMarked()) {
                return;
            }

            String original = event.getRawText();
            String spoken = original;

            // The dead man gets the first word. Whatever he says can still come out slurred after.
            if (data.hasCurse(Curses.PIRATE)) {
                boolean bellow = PirateCurse.isFrenzied(data)
                        || player.getRandom().nextDouble() < Config.PIRATE_SHOUT_ON_SEND.get();
                spoken = PirateTongue.speak(spoken, player.getRandom(), bellow);
            }

            if (data.hasCurse(Curses.DEMENTIA)) {
                double chance = DementiaCurse.garbleChance(data);
                if (chance > 0.0D) {
                    spoken = Garble.text(spoken, chance, player.level().getGameTime());
                }
            }

            if (!spoken.equals(original)) {
                event.setMessage(Component.literal(spoken));
            }
        });
    }

    // --- admin ------------------------------------------------------------

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        BlackMarkCommand.register(event.getDispatcher());
    }

    private ForgeEvents() {
    }
}
