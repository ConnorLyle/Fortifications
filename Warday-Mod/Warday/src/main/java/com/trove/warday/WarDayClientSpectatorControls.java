package com.trove.warday;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WarDayMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class WarDayClientSpectatorControls {
    private static boolean respawnSpectating;

    private WarDayClientSpectatorControls() {
    }

    static void setRespawnSpectating(boolean active) {
        respawnSpectating = active;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!respawnSpectating || minecraft.player == null || !minecraft.player.isSpectator()) {
            return;
        }

        int direction;
        if (event.isAttack()) {
            direction = -1;
        } else if (event.isUseItem()) {
            direction = 1;
        } else {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        PacketDistributor.sendToServer(new CycleRespawnSpectatorPayload(direction));
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
