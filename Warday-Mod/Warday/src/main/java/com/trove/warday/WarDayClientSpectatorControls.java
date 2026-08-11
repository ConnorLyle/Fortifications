package com.trove.warday;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.OptionalInt;

@EventBusSubscriber(modid = WarDayMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class WarDayClientSpectatorControls {
    private static boolean respawnSpectating;

    private WarDayClientSpectatorControls() {
    }

    static void setRespawnSpectating(boolean active) {
        respawnSpectating = active;
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!respawnSpectating || minecraft.player == null || !minecraft.player.isSpectator()
                || minecraft.screen != null) {
            return;
        }

        OptionalInt direction = WarDaySpectatorInput.cycleDirection(event.getButton(), event.getAction());
        if (direction.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        PacketDistributor.sendToServer(new CycleRespawnSpectatorPayload(direction.getAsInt()));
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        respawnSpectating = false;
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        respawnSpectating = false;
    }
}
