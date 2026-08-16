package com.example.fortifications;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ClassResetTokenItem extends Item {
    public ClassResetTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        ClassResetService.Validation validation = ClassResetService.validate(serverPlayer);
        if (validation != ClassResetService.Validation.READY) {
            ClassResetService.sendValidationMessage(serverPlayer, validation);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) ->
                        new ClassResetConfirmMenu(containerId, inventory, serverPlayer),
                Component.translatable("container.fortifications.class_reset.title")
        ));
        return InteractionResultHolder.success(stack);
    }
}
