package com.goshan.blackmark.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * The mark never shouts. Everything it says arrives as a rumour.
 */
public final class Lore {

    /** Said only to the bearer, low and grey. */
    public static void whisper(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** Said only to the bearer, and meant to be felt. */
    public static void omen(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    /** Said to everyone aboard. */
    public static void broadcast(MinecraftServer server, String key, Object... args) {
        MutableComponent line = Component.translatable(key, args)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        server.getPlayerList().broadcastSystemMessage(line, false);
    }

    /** Picks one of {@code key.1 .. key.count}, so the same event never reads the same way twice. */
    public static String variant(RandomSource random, String key, int count) {
        return key + "." + (1 + random.nextInt(count));
    }

    private Lore() {
    }
}
