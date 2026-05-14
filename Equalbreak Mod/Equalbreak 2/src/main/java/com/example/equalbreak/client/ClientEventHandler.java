package com.example.equalbreak.client;

import com.example.equalbreak.EqualBreakMod;
import com.example.equalbreak.network.TogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EqualBreakMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEventHandler {

    /** Tracks the local toggle state so we can flip it and show the correct message. */
    private static boolean toggled = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // consumeClick() handles key-repeat correctly (one logical press per tick)
        while (KeyBindings.TOGGLE_KEY.consumeClick()) {
            toggled = !toggled;

            // Tell the server about the new state
            PacketDistributor.sendToServer(new TogglePayload(toggled));

            // Show a brief hotbar message
            mc.player.displayClientMessage(
                    toggled
                            ? Component.literal("§a[Equal Break] §fON — all blocks break at deepslate speed")
                            : Component.literal("§c[Equal Break] §fOFF"),
                    true   // true = action bar (above hotbar), false = chat
            );
        }
    }
}
