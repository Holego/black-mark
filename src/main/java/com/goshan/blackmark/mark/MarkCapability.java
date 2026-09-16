package com.goshan.blackmark.mark;

import com.goshan.blackmark.BlackMarkMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class MarkCapability {

    public static final Capability<MarkData> MARK = CapabilityManager.get(new CapabilityToken<MarkData>() {
    });

    public static final ResourceLocation ID = new ResourceLocation(BlackMarkMod.MODID, "mark");

    public static Optional<MarkData> of(@Nullable Player player) {
        return player == null ? Optional.empty() : player.getCapability(MARK).resolve();
    }

    private MarkCapability() {
    }

    /** Attached to every player by {@code ForgeEvents}. */
    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final MarkData data = new MarkData();
        private final LazyOptional<MarkData> holder = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == MARK ? holder.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }

        public void invalidate() {
            holder.invalidate();
        }
    }

    @Mod.EventBusSubscriber(modid = BlackMarkMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusHandler {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(MarkData.class);
        }
    }
}
