package com.trove.warday;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = WarDayMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class WarDayNetwork {
    private WarDayNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        RespawnSpectatorStatePayload.TYPE,
                        RespawnSpectatorStatePayload.STREAM_CODEC,
                        RespawnSpectatorStatePayload::handle
                )
                .playToServer(
                        CycleRespawnSpectatorPayload.TYPE,
                        CycleRespawnSpectatorPayload.STREAM_CODEC,
                        CycleRespawnSpectatorPayload::handle
                );
    }

    static void syncRespawnSpectatorState(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayer(player, new RespawnSpectatorStatePayload(active));
    }
}
