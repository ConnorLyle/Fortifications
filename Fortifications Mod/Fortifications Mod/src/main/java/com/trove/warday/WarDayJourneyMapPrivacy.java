package com.trove.warday;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class WarDayJourneyMapPrivacy {
    private WarDayJourneyMapPrivacy() {
    }

    public static void register() {
        try {
            Class<?> registryClass = Class.forName("journeymap.api.v2.common.event.ServerEventRegistry");
            Field playerRadarEventField = registryClass.getField("PLAYER_RADAR_UPDATE_EVENT");
            Object playerRadarEvent = playerRadarEventField.get(null);
            Method subscribe = playerRadarEvent.getClass().getMethod("subscribe", String.class, Consumer.class);
            Consumer<Object> listener = WarDayJourneyMapPrivacy::filterRadarUpdate;
            subscribe.invoke(playerRadarEvent, WarDayMod.MODID, listener);
            WarDayMod.LOGGER.info("JourneyMap player radar privacy enabled: only members of the same FTB team remain visible.");
        } catch (ReflectiveOperationException exception) {
            WarDayMod.LOGGER.error("JourneyMap is loaded, but its server radar API is unavailable; player radar privacy could not be registered.", exception);
        }
    }

    private static void filterRadarUpdate(Object event) {
        try {
            Method getReceiver = event.getClass().getMethod("getReceiver");
            Method getRemoteId = event.getClass().getMethod("getRemoteId");
            Method isVisible = event.getClass().getMethod("isVisible");
            Method setVisible = event.getClass().getMethod("setVisible", boolean.class);

            ServerPlayer receiver = (ServerPlayer) getReceiver.invoke(event);
            UUID remoteId = (UUID) getRemoteId.invoke(event);
            boolean visible = (boolean) isVisible.invoke(event);

            Optional<UUID> receiverTeamId = Optional.empty();
            Optional<UUID> remoteTeamId = Optional.empty();
            if (FTBTeamsAPI.api().isManagerLoaded()) {
                TeamManager teamManager = FTBTeamsAPI.api().getManager();
                receiverTeamId = teamManager.getTeamForPlayerID(receiver.getUUID()).map(team -> team.getId());
                remoteTeamId = teamManager.getTeamForPlayerID(remoteId).map(team -> team.getId());
            }

            setVisible.invoke(event, WarDayTeamVisibility.journeyMapVisible(
                    visible,
                    receiverTeamId,
                    remoteTeamId
            ));
        } catch (ReflectiveOperationException | ClassCastException exception) {
            WarDayMod.LOGGER.error("Unable to apply JourneyMap player radar privacy to an update event.", exception);
        }
    }
}
