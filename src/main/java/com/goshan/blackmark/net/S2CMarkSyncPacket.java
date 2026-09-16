package com.goshan.blackmark.net;

import com.goshan.blackmark.client.ClientMarkCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server tells one client everything about its own mark. */
public class S2CMarkSyncPacket {

    private final CompoundTag tag;

    public S2CMarkSyncPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public static S2CMarkSyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag read = buf.readNbt();
        return new S2CMarkSyncPacket(read == null ? new CompoundTag() : read);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
    }

    public static void handle(S2CMarkSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMarkCache.accept(packet.tag));
        ctx.get().setPacketHandled(true);
    }
}
