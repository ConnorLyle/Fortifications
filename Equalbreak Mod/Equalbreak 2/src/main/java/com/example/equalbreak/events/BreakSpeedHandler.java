package com.example.equalbreak.events;

import com.example.equalbreak.EqualBreakMod;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = EqualBreakMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BreakSpeedHandler {

    private static final float TARGET_HARDNESS = 3.0f;

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        boolean active = EqualBreakMod.GLOBAL_ACTIVE
                || EqualBreakMod.TOGGLED_PLAYERS.contains(event.getEntity().getUUID());
        if (!active) return;

        float hardness = event.getState().getDestroySpeed(event.getEntity().level(), BlockPos.ZERO);

        if (hardness <= 0f) return;

        float newSpeed = event.getNewSpeed() * (hardness / TARGET_HARDNESS);
        event.setNewSpeed(newSpeed);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EqualBreakMod.TOGGLED_PLAYERS.remove(event.getEntity().getUUID());
    }
}