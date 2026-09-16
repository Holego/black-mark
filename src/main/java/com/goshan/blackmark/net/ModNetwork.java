package com.goshan.blackmark.net;

import com.goshan.blackmark.BlackMarkMod;
import com.goshan.blackmark.mark.MarkData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BlackMarkMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int nextId = 0;

    public static void init() {
        CHANNEL.messageBuilder(S2CMarkSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CMarkSyncPacket::encode)
                .decoder(S2CMarkSyncPacket::decode)
                .consumerMainThread(S2CMarkSyncPacket::handle)
                .add();
    }

    /** The bearer's client needs the whole picture: state, curses, swallowed slots. It is a tiny tag. */
    public static void syncTo(ServerPlayer player, MarkData data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CMarkSyncPacket(data.serializeNBT()));
    }

    private ModNetwork() {
    }
}
