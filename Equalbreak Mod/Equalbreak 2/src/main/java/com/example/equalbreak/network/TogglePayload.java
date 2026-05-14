package com.example.equalbreak.network;

import com.example.equalbreak.EqualBreakMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client → server when the player presses the toggle key.
 * The server updates TOGGLED_PLAYERS accordingly.
 */
public record TogglePayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<TogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EqualBreakMod.MOD_ID, "toggle")
    );

    public static final StreamCodec<ByteBuf, TogglePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TogglePayload::enabled,
            TogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (payload.enabled()) {
                EqualBreakMod.TOGGLED_PLAYERS.add(player.getUUID());
            } else {
                EqualBreakMod.TOGGLED_PLAYERS.remove(player.getUUID());
            }
        });
    }
}
