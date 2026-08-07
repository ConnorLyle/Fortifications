package com.trove.warday;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleRespawnSpectatorPayload(int direction) implements CustomPacketPayload {
    public static final Type<CycleRespawnSpectatorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WarDayMod.MODID, "cycle_respawn_spectator")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleRespawnSpectatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CycleRespawnSpectatorPayload::direction,
            CycleRespawnSpectatorPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleRespawnSpectatorPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            WarDayCommands.cycleRespawnSpectator(player, payload.direction());
        }
    }
}
