package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BreakSpeedHandler {

    private static final float TARGET_HARDNESS = 3.0f;
    private static final ResourceLocation WARDAY_NEXUS = ResourceLocation.fromNamespaceAndPath("warday", "nexus");
    private static final ResourceLocation WARDAY_FORWARD_MARKER = ResourceLocation.fromNamespaceAndPath("warday", "forward_marker");

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!FortificationsMod.GLOBAL_ACTIVE) return;
        if (isWardayMarker(event)) return;

        float hardness = event.getState().getDestroySpeed(event.getEntity().level(), BlockPos.ZERO);

        if (hardness <= 0f) return;

        float newSpeed = event.getNewSpeed() * (hardness / TARGET_HARDNESS);
        event.setNewSpeed(newSpeed);
    }

    private static boolean isWardayMarker(PlayerEvent.BreakSpeed event) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        return WARDAY_NEXUS.equals(blockId) || WARDAY_FORWARD_MARKER.equals(blockId);
    }
}
