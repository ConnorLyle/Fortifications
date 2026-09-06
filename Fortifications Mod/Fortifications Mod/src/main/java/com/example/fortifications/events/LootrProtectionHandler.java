package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import com.example.fortifications.LootrProtectionPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class LootrProtectionHandler {
    private LootrProtectionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!isProtectedContainer(event.getState())) {
            return;
        }

        boolean fakePlayer = event.getPlayer() instanceof FakePlayer;
        if (!LootrProtectionPolicy.mayPlayerBreak(
                event.getPlayer().isCreative(), event.getPlayer().isShiftKeyDown(), fakePlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> isProtectedContainer(event.getLevel().getBlockState(pos)));
    }

    private static boolean isProtectedContainer(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return LootrProtectionPolicy.isProtectedContainer(id.getNamespace(), id.getPath());
    }
}
