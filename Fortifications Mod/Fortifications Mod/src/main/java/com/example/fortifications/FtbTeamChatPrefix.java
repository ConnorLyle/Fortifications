package com.example.fortifications;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class FtbTeamChatPrefix {
    private FtbTeamChatPrefix() {
    }

    public static ChatType.Bound apply(ServerPlayer player, ChatType.Bound original) {
        boolean managerLoaded = FTBTeamsAPI.api().isManagerLoaded();
        Optional<Team> team = managerLoaded
                ? FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                : Optional.empty();
        if (team.isEmpty()) {
            return original;
        }

        Team resolvedTeam = team.get();
        Component teamName = resolvedTeam.getColoredName();
        if (!TeamChatPrefixPolicy.shouldShowPrefix(
                managerLoaded, true, resolvedTeam.isPlayerTeam(), teamName.getString())) {
            return original;
        }

        MutableComponent prefixedName = Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                .append(teamName.copy())
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(original.name().copy());
        return new ChatType.Bound(original.chatType(), prefixedName, original.targetName());
    }
}
