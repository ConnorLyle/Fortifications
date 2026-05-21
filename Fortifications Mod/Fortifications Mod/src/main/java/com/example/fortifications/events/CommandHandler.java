package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommandHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("fortifications")
                .requires(src -> src.hasPermission(2)) // OP level 2
                .then(Commands.literal("all")
                    .executes(ctx -> toggleAll(ctx.getSource())))
        );
    }

    private static int toggleAll(CommandSourceStack source) {
        FortificationsMod.GLOBAL_ACTIVE = !FortificationsMod.GLOBAL_ACTIVE;

        Component message = FortificationsMod.GLOBAL_ACTIVE
                ? Component.literal("[Equalbreak] Enabled for ALL players.").withStyle(ChatFormatting.GREEN)
                : Component.literal("[Equalbreak] Disabled for ALL players.").withStyle(ChatFormatting.RED);

        // Broadcast to everyone online so operators see it in chat too
        source.getServer().getPlayerList().broadcastSystemMessage(message, false);

        return 1;
    }
}
