package com.trove.warday;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import journeymap.api.v2.common.event.ServerEventRegistry;
import journeymap.api.v2.server.event.PlayerRadarUpdateEvent;

import java.util.Optional;
import java.util.UUID;

public final class WarDayJourneyMapPrivacy {
    private WarDayJourneyMapPrivacy() {
    }

    public static void register() {
        ServerEventRegistry.PLAYER_RADAR_UPDATE_EVENT.subscribe(WarDayMod.MODID, WarDayJourneyMapPrivacy::filterRadarUpdate);
        WarDayMod.LOGGER.info("JourneyMap player radar privacy enabled: only members of the same FTB team remain visible.");
    }

    private static void filterRadarUpdate(PlayerRadarUpdateEvent event) {
        Optional<UUID> receiverTeamId = Optional.empty();
        Optional<UUID> remoteTeamId = Optional.empty();
        if (FTBTeamsAPI.api().isManagerLoaded()) {
            TeamManager teamManager = FTBTeamsAPI.api().getManager();
            receiverTeamId = teamManager.getTeamForPlayerID(event.getReceiver().getUUID()).map(team -> team.getId());
            remoteTeamId = teamManager.getTeamForPlayerID(event.getRemoteId()).map(team -> team.getId());
        }

        event.setVisible(WarDayTeamVisibility.journeyMapVisible(
                event.isVisible(),
                receiverTeamId,
                remoteTeamId
        ));
    }
}
