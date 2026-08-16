package com.example.fortifications;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ClassResetConfirmMenu extends AbstractContainerMenu {
    private static final int CONFIRM_SLOT = 2;
    private static final int INFO_SLOT = 4;
    private static final int CANCEL_SLOT = 6;

    private final ServerPlayer resettingPlayer;
    private final SimpleContainer choices = new SimpleContainer(9);

    ClassResetConfirmMenu(int containerId, Inventory inventory, ServerPlayer resettingPlayer) {
        super(MenuType.GENERIC_9x1, containerId);
        this.resettingPlayer = resettingPlayer;

        choices.setItem(CONFIRM_SLOT, namedStack(
                Items.LIME_WOOL, "menu.fortifications.class_reset.confirm"));
        choices.setItem(INFO_SLOT, namedStack(
                Items.EXPERIENCE_BOTTLE, "menu.fortifications.class_reset.info"));
        choices.setItem(CANCEL_SLOT, namedStack(
                Items.RED_WOOL, "menu.fortifications.class_reset.cancel"));

        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(choices, slot, 8 + slot * 18, 18) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 50 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 108));
        }
    }

    private static ItemStack namedStack(net.minecraft.world.item.Item item, String translationKey) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable(translationKey));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player == resettingPlayer && slotId >= 0 && slotId < 9) {
            if (slotId == CONFIRM_SLOT) {
                ClassResetService.resetClass(resettingPlayer);
            } else if (slotId == CANCEL_SLOT) {
                resettingPlayer.closeContainer();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == resettingPlayer && !resettingPlayer.isRemoved();
    }
}
