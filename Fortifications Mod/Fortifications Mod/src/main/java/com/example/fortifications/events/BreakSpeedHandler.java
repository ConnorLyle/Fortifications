package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BreakSpeedHandler {

    private static final float TARGET_HARDNESS = 3.0f;

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!FortificationsMod.GLOBAL_ACTIVE) return;

        float hardness = event.getState().getDestroySpeed(event.getEntity().level(), BlockPos.ZERO);

        if (hardness <= 0f) return;

        float newSpeed = event.getNewSpeed() * (hardness / TARGET_HARDNESS);
        event.setNewSpeed(newSpeed);
    }
}
