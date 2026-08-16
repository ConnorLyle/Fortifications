package com.trove.warday;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RespawnSpectatorStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<RespawnSpectatorStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WarDayMod.MODID, "respawn_spectator_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RespawnSpectatorStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RespawnSpectatorStatePayload::active,
            RespawnSpectatorStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RespawnSpectatorStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WarDayClientSpectatorControls.setRespawnSpectating(payload.active()));
    }
}
