package com.trove.warday;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WarDayNetwork {
    private WarDayNetwork() {
    }

    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar
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
