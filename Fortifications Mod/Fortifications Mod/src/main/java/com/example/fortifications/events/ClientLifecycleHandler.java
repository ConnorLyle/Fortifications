package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ClientLifecycleHandler {
    private ClientLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        FortificationsMod.GLOBAL_ACTIVE = false;
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FortificationsMod.GLOBAL_ACTIVE = false;
    }
}
