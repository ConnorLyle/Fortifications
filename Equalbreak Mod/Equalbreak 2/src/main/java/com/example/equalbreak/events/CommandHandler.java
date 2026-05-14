package com.example.equalbreak.events;

import com.example.equalbreak.EqualBreakMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = EqualBreakMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommandHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("equalbreak")
                .requires(src -> src.hasPermission(2)) // OP level 2
                .then(Commands.literal("all")
                    .executes(ctx -> toggleAll(ctx.getSource())))
        );
    }

    private static int toggleAll(CommandSourceStack source) {
        EqualBreakMod.GLOBAL_ACTIVE = !EqualBreakMod.GLOBAL_ACTIVE;

        Component message = EqualBreakMod.GLOBAL_ACTIVE
                ? Component.literal("§a[Equal Break] Enabled for ALL players.")
                : Component.literal("§c[Equal Break] Disabled for ALL players.");

        // Broadcast to everyone online so operators see it in chat too
        source.getServer().getPlayerList().broadcastSystemMessage(message, false);

        return 1;
    }
}
