package com.example.fortifications.mixin;

import com.example.fortifications.FtbTeamChatPrefix;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TeamChatPrefixMixin {
    @ModifyArg(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V"
            ),
            index = 2
    )
    private ChatType.Bound fortifications$prefixTeamName(ChatType.Bound original) {
        ServerGamePacketListenerImpl connection = (ServerGamePacketListenerImpl) (Object) this;
        return FtbTeamChatPrefix.apply(connection.player, original);
    }
}
