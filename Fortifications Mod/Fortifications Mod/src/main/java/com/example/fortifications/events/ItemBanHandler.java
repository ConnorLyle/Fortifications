package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import com.example.fortifications.items.BannedItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ItemBanHandler {
    private ItemBanHandler() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ItemEntity itemEntity) {
            removeBannedItemEntity(itemEntity);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            removeBannedPlayerItems(player);
        }
    }

    private static void removeBannedItemEntity(ItemEntity itemEntity) {
        if (isBanned(itemEntity.getItem())) {
            itemEntity.discard();
        }
    }

    private static void removeBannedPlayerItems(ServerPlayer player) {
        boolean removed = false;
        Inventory inventory = player.getInventory();

        removed |= removeBannedStacks(inventory.items);
        removed |= removeBannedStacks(inventory.armor);
        removed |= removeBannedStacks(inventory.offhand);
        removed |= removeBannedCurios(player);

        if (removed) {
            player.displayClientMessage(
                    Component.literal("A banned item was removed from your inventory.").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private static boolean removeBannedStacks(NonNullList<ItemStack> stacks) {
        boolean removed = false;

        for (int index = 0; index < stacks.size(); index++) {
            if (isBanned(stacks.get(index))) {
                stacks.set(index, ItemStack.EMPTY);
                removed = true;
            }
        }

        return removed;
    }

    private static boolean removeBannedCurios(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> removeBannedHandlerStacks(handler.getEquippedCurios()))
                .orElse(false);
    }

    private static boolean removeBannedHandlerStacks(IItemHandlerModifiable handler) {
        boolean removed = false;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (isBanned(handler.getStackInSlot(slot))) {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
                removed = true;
            }
        }

        return removed;
    }

    private static boolean isBanned(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return BannedItemRegistry.isBanned(itemId);
    }
}
