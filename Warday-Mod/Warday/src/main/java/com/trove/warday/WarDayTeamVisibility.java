package com.trove.warday;

import java.util.Optional;
import java.util.UUID;

public final class WarDayTeamVisibility {
    private WarDayTeamVisibility() {
    }

    public static boolean journeyMapVisible(
            boolean journeyMapVisible,
            Optional<UUID> receiverTeamId,
            Optional<UUID> remoteTeamId
    ) {
        return journeyMapVisible
                && receiverTeamId.isPresent()
                && remoteTeamId.isPresent()
                && receiverTeamId.get().equals(remoteTeamId.get());
    }
}
