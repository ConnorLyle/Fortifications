package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import com.example.fortifications.FortChestTeamStorage;
import com.example.fortifications.network.FortificationsNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ServerLifecycleHandler {
    private ServerLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        FortificationsMod.GLOBAL_ACTIVE = false;
        FortificationsNetwork.syncToAllPlayers();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FortificationsMod.GLOBAL_ACTIVE = false;
        FortChestTeamStorage.clear(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FortificationsNetwork.syncToPlayer(player);
        }
    }
}
