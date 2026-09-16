package com.goshan.blackmark.client;

import com.goshan.blackmark.BlackMarkMod;
import com.goshan.blackmark.curse.impl.DementiaCurse;
import com.goshan.blackmark.curse.impl.EternalWoundsCurse;
import com.goshan.blackmark.curse.impl.PirateCurse;
import com.goshan.blackmark.mark.MarkData;
import com.goshan.blackmark.mark.MarkManager;
import com.goshan.blackmark.mark.MarkState;
import com.goshan.blackmark.mark.SlotLock;
import com.goshan.blackmark.util.Garble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The visible half of the curse. Everything here is drawn from the synced {@link ClientMarkCache}
 * and never decides anything - the server has already decided.
 */
@Mod.EventBusSubscriber(modid = BlackMarkMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final int SLOT_SIZE = 16;

    /** A swallowed slot: solid black with a faint bruise at the edge. */
    private static final int DEVOURED_FILL = 0xEE050307;
    private static final int DEVOURED_EDGE = 0x66301A3A;
    /** The mark while it is only mist: you can see it, you cannot take it. */
    private static final int FOG_FILL = 0x99101018;

    private static final int WOUND_FILL = 0xDD140206;
    private static final int WOUND_EDGE = 0x88521424;

    /** Gold haze at the edges of sight while the pirate has the wheel. */
    private static final int FRENZY_TINT = 0x00FFC020;

    // --- inventory screens ------------------------------------------------

    @SubscribeEvent
    public static void renderContainerScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !ClientMarkCache.isMarked()) {
            return;
        }

        MarkData data = ClientMarkCache.get();
        GuiGraphics graphics = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory)) {
                continue;
            }

            int index = slot.getContainerSlot();
            int x = left + slot.x;
            int y = top + slot.y;

            if (SlotLock.isBlocked(data, index)) {
                paintDevoured(graphics, x, y);
            } else if (index == data.getMarkSlot() && data.getState() == MarkState.FOG) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, FOG_FILL);
            }
        }

        // Anything lifted onto the cursor has been handled, and is remembered from now on.
        DementiaVeil.remember(screen.getMenu().getCarried());
    }

    /** A blackened slot cannot be clicked, and mist cannot be picked up. */
    @SubscribeEvent
    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !ClientMarkCache.isMarked()) {
            return;
        }

        Slot hovered = screen.getSlotUnderMouse();
        if (hovered == null || !(hovered.container instanceof Inventory)) {
            return;
        }

        MarkData data = ClientMarkCache.get();
        int index = hovered.getContainerSlot();

        // Poking counts as handling, even when the click goes nowhere.
        DementiaVeil.remember(hovered.getItem());

        if (SlotLock.isBlocked(data, index)) {
            event.setCanceled(true);
            return;
        }

        if (MarkManager.isMark(hovered.getItem()) && data.getState() == MarkState.FOG) {
            event.setCanceled(true);
        }
    }

    /** Number keys and the swap key would otherwise route around the mouse block. */
    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !ClientMarkCache.isMarked()) {
            return;
        }

        MarkData data = ClientMarkCache.get();
        Slot hovered = screen.getSlotUnderMouse();

        if (hovered != null && hovered.container instanceof Inventory) {
            int index = hovered.getContainerSlot();
            if (SlotLock.isBlocked(data, index)
                    || (MarkManager.isMark(hovered.getItem()) && data.getState() == MarkState.FOG)) {
                event.setCanceled(true);
                return;
            }
        }

        // Digits 1-9 target a hotbar slot directly, wherever the mouse happens to be.
        int digit = event.getKeyCode() - 49;
        if (digit >= 0 && digit < 9 && SlotLock.isBlocked(data, digit)) {
            event.setCanceled(true);
        }
    }

    // --- heads-up display -------------------------------------------------

    @SubscribeEvent
    public static void renderHotbar(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || !ClientMarkCache.isMarked()) {
            return;
        }

        MarkData data = ClientMarkCache.get();
        int[] blocked = SlotLock.blocked(data);
        if (blocked.length == 0) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int left = event.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = event.getWindow().getGuiScaledHeight() - 22;

        for (int slot : blocked) {
            if (slot < 0 || slot >= 9) {
                continue;
            }
            paintDevoured(graphics, left + slot * 20 + 3, top + 3);
        }
    }

    /** Hearts the mark has taken, drawn dark beyond the ones still beating. */
    @SubscribeEvent
    public static void renderHearts(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type() || !ClientMarkCache.isMarked()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int wounds = EternalWoundsCurse.wounds(ClientMarkCache.get());
        if (wounds <= 0) {
            return;
        }

        int livingHearts = Math.max(0, Mth.ceil(minecraft.player.getMaxHealth() / 2.0F));
        int lostHearts = Mth.ceil(wounds / 2.0F);

        GuiGraphics graphics = event.getGuiGraphics();
        int left = event.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = event.getWindow().getGuiScaledHeight() - 39;

        for (int i = 0; i < lostHearts; i++) {
            int index = livingHearts + i;
            int x = left + (index % 10) * 8;
            int y = top - (index / 10) * 10;
            graphics.fill(x + 1, y + 1, x + 8, y + 8, WOUND_FILL);
            graphics.fill(x + 1, y, x + 8, y + 1, WOUND_EDGE);
            graphics.fill(x + 1, y + 8, x + 8, y + 9, WOUND_EDGE);
        }
    }

    /** The gold frenzy: the world goes warm and narrow at the edges. */
    @SubscribeEvent
    public static void renderFrenzy(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type() || !ClientMarkCache.isMarked()) {
            return;
        }
        if (!PirateCurse.isFrenzied(ClientMarkCache.get()) || Minecraft.getInstance().options.hideGui) {
            return;
        }

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int band = Math.max(24, height / 5);

        // A slow heartbeat, so it reads as a fever rather than a status bar.
        double pulse = 0.5D + 0.5D * Math.sin(System.currentTimeMillis() / 420.0D);
        int alpha = (int) (0x40 + 0x40 * pulse) << 24;
        int strong = alpha | (FRENZY_TINT & 0x00FFFFFF);

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fillGradient(0, 0, width, band, strong, FRENZY_TINT);
        graphics.fillGradient(0, height - band, width, height, FRENZY_TINT, strong);
    }

    // --- dementia ---------------------------------------------------------

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!ClientMarkCache.isMarked()) {
            return;
        }

        double chance = DementiaCurse.nameGarbleChance(ClientMarkCache.get());
        if (chance <= 0.0D) {
            return;
        }

        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();
        if (stack.isEmpty() || lines.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getEntity() != null ? event.getEntity() : minecraft.player;

        // Which lines are enchantments? Those follow their own rule and are never letter-slurred.
        Map<Enchantment, Integer> real = DementiaVeil.realEnchantments(stack);
        Set<Integer> enchantLines = DementiaVeil.enchantmentLines(lines, real);

        // Name and description slur until the bearer has handled this thing themselves.
        if (!DementiaVeil.isHandled(stack)) {
            long bucket = minecraft.level == null ? 0L : minecraft.level.getGameTime() / 200L;
            for (int i = 0; i < lines.size(); i++) {
                if (enchantLines.contains(i)) {
                    continue;
                }
                Component line = lines.get(i);
                String original = line.getString();
                String slipped = Garble.text(original, chance, bucket + i);
                if (!slipped.equals(original)) {
                    lines.set(i, Component.literal(slipped).withStyle(line.getStyle()));
                }
            }
        }

        // Enchantments read as somebody else's until the thing is actually worn or held.
        if (!real.isEmpty() && !DementiaVeil.isEquipped(player, stack)) {
            DementiaVeil.swapEnchantments(lines, real, DementiaVeil.keyOf(stack));
        }
    }

    // --- housekeeping -----------------------------------------------------

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMarkCache.clear();
        DementiaVeil.forgetEverything();
    }

    private static void paintDevoured(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, DEVOURED_FILL);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, DEVOURED_EDGE);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, DEVOURED_EDGE);
    }

    private ClientEvents() {
    }
}
