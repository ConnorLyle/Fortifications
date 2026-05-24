package com.example.fortifications.network;

import com.example.fortifications.FortificationsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EqualbreakSyncPayload(boolean active) implements CustomPacketPayload {
    public static final Type<EqualbreakSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FortificationsMod.MOD_ID, "equalbreak_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EqualbreakSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            EqualbreakSyncPayload::active,
            EqualbreakSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EqualbreakSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FortificationsMod.GLOBAL_ACTIVE = payload.active());
    }
}
