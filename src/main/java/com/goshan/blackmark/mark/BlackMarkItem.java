package com.goshan.blackmark.mark;

import com.goshan.blackmark.util.Lore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * It has no physical shell of its own. What sits in the inventory is only the part
 * that has decided, for the moment, to be touchable.
 */
public class BlackMarkItem extends Item {

    private static final int WHISPER_COOLDOWN = 60;

    public BlackMarkItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    /** No shulker box, no bundle, no clever stash. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /**
     * Q never works. The mark does not become an entity in the world, so there is nothing to pick up,
     * despawn or hopper away - and the attempt itself is noted.
     */
    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MarkCapability.of(serverPlayer).ifPresent(data -> {
                if (data.isMarked()) {
                    MarkManager.defy(serverPlayer, data,
                            Lore.variant(serverPlayer.getRandom(), "message.blackmark.defy.drop", 4));
                }
            });
        }
        return false;
    }

    /** Holding it and pressing use just makes it speak. No penalty - it enjoys being listened to. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && !player.getCooldowns().isOnCooldown(this)) {
            player.getCooldowns().addCooldown(this, WHISPER_COOLDOWN);
            Lore.whisper(serverPlayer, Lore.variant(serverPlayer.getRandom(), "message.blackmark.idle", 6));
            MarkManager.spawnMist(serverPlayer, 8);
        }
        return InteractionResultHolder.consume(held);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.blackmark.black_mark.tooltip.1")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.blackmark.black_mark.tooltip.2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
