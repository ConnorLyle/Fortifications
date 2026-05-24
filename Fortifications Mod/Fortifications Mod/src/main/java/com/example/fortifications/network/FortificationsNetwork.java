package com.example.fortifications.network;

import com.example.fortifications.FortificationsMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FortificationsNetwork {
    private FortificationsNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToClient(EqualbreakSyncPayload.TYPE, EqualbreakSyncPayload.STREAM_CODEC, EqualbreakSyncPayload::handle);
    }

    public static void syncToAllPlayers() {
        PacketDistributor.sendToAllPlayers(new EqualbreakSyncPayload(FortificationsMod.GLOBAL_ACTIVE));
    }

    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new EqualbreakSyncPayload(FortificationsMod.GLOBAL_ACTIVE));
    }
}
