package com.example.fortifications.events;

import com.example.fortifications.FortChestBlockEntity;
import com.example.fortifications.FortificationsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class FortChestAccessHandler {
    private FortChestAccessHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level().getBlockEntity(event.getPos()) instanceof FortChestBlockEntity fortChest)) {
            return;
        }
        if (ensureAccess(player, fortChest)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof FortChestBlockEntity fortChest)) {
            return;
        }
        if (!ensureAccess(player, fortChest)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos ->
                event.getLevel().getBlockEntity(pos) instanceof FortChestBlockEntity);
    }

    private static boolean ensureAccess(ServerPlayer player, FortChestBlockEntity fortChest) {
        boolean wasBound = fortChest.isBound();
        boolean authorized = fortChest.bindToPlayerTeam(player);
        if (authorized) {
            if (!wasBound) {
                player.displayClientMessage(Component.translatable(
                        "message.fortifications.fort_chest.bound", fortChest.ownerTeamName())
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return true;
        }
        MutableComponent failure = fortChest.isBound()
                ? Component.translatable(
                        "message.fortifications.fort_chest.locked", fortChest.ownerTeamName())
                : Component.translatable("message.fortifications.fort_chest.team_required");
        player.displayClientMessage(failure.withStyle(ChatFormatting.RED), true);
        return false;
    }
}
